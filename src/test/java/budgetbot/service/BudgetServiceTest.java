package budgetbot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import budgetbot.model.BudgetState;
import budgetbot.model.Category;
import budgetbot.model.CategorySummary;
import budgetbot.model.Transaction;
import budgetbot.model.TransactionType;
import budgetbot.persistence.BudgetDatabase;
import budgetbot.persistence.BudgetPersistenceException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.YearMonth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BudgetServiceTest {
  @TempDir Path temporaryDirectory;

  private BudgetDatabase database;
  private BudgetService service;
  private YearMonth month;

  @BeforeEach
  void setUp() {
    database = new BudgetDatabase(temporaryDirectory.resolve("budgetbot.db"));
    service = new BudgetService(database);
    month = YearMonth.of(2026, 8);
  }

  @AfterEach
  void closeDatabase() {
    database.close();
  }

  @Test
  void initializesDefaultCategoriesAndSettings() {
    assertEquals(9, service.categories().size());
    assertTrue(
        service.categories().stream().anyMatch(category -> category.name().equals("Groceries")));
    assertFalse(service.settings().rolloverEnabled());
    assertEquals(80, service.settings().warningThreshold());
  }

  @Test
  void incomeIncreasesBalanceWithoutAffectingCategorySpending() {
    service.addTransaction(
        TransactionType.INCOME, new BigDecimal("1250.50"), month.atDay(1), "Pay", null);

    assertEquals(new BigDecimal("1250.50"), service.dashboard(month).overallBalance());
    assertTrue(
        service.dashboard(month).categorySummaries().stream()
            .allMatch(summary -> summary.spent().compareTo(BigDecimal.ZERO) == 0));
  }

  @Test
  void expenseRequiresCategoryAndDrivesWarningThenOverBudgetState() {
    Category groceries = category("Groceries");
    service.setMonthlyBudget(groceries.id(), month, new BigDecimal("100"));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.addTransaction(
                TransactionType.EXPENSE, new BigDecimal("1"), month.atDay(1), "Missing", null));

    service.addTransaction(
        TransactionType.EXPENSE, new BigDecimal("80"), month.atDay(2), "Market", groceries.id());
    assertEquals(BudgetState.WARNING, summary(groceries).state());

    service.addTransaction(
        TransactionType.EXPENSE,
        new BigDecimal("20"),
        month.atDay(3),
        "More market",
        groceries.id());
    assertEquals(BudgetState.OVER_BUDGET, summary(groceries).state());
  }

  @Test
  void rolloverCarriesPositiveAndNegativeRemainingAmounts() {
    Category groceries = category("Groceries");
    YearMonth previous = month.minusMonths(1);
    service.setMonthlyBudget(groceries.id(), previous, new BigDecimal("100"));
    service.addTransaction(
        TransactionType.EXPENSE, new BigDecimal("25"), previous.atDay(2), "Market", groceries.id());
    service.saveSettings(true, 80);

    assertEquals(new BigDecimal("175"), summary(groceries).available());

    service.setMonthlyBudget(groceries.id(), month, new BigDecimal("100"));
    service.addTransaction(
        TransactionType.EXPENSE,
        new BigDecimal("200"),
        month.atDay(2),
        "Large shop",
        groceries.id());
    YearMonth following = month.plusMonths(1);

    assertEquals(
        new BigDecimal("75"),
        service.dashboard(following).categorySummaries().stream()
            .filter(summary -> summary.category().id() == groceries.id())
            .findFirst()
            .orElseThrow()
            .available());
  }

  @Test
  void removesCategoryOnlyAfterReassigningTransactions() {
    Category dining = category("Dining");
    Category groceries = category("Groceries");
    service.addTransaction(
        TransactionType.EXPENSE, new BigDecimal("10"), month.atDay(1), "Lunch", dining.id());

    service.removeCategory(dining.id(), groceries.id());

    assertFalse(service.categories().contains(dining));
    Transaction reassigned = service.transactions(month).getFirst();
    assertEquals(groceries.id(), reassigned.categoryId());
  }

  @Test
  void rejectsInvalidSettingsAndBudgetAmounts() {
    assertThrows(IllegalArgumentException.class, () -> service.saveSettings(false, 100));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.setMonthlyBudget(category("Health").id(), month, new BigDecimal("-1")));
  }

  @Test
  void addsRenamesAndValidatesCategoryNames() {
    long categoryId = service.addCategory("  Gifts  ");

    assertEquals("Gifts", categoryById(categoryId).name());
    service.renameCategory(categoryId, "  Celebrations ");
    assertEquals("Celebrations", categoryById(categoryId).name());
    assertThrows(IllegalArgumentException.class, () -> service.addCategory("  "));
    assertThrows(IllegalArgumentException.class, () -> service.renameCategory(categoryId, null));
  }

  @Test
  void updatesDeletesAndNormalizesTransactions() {
    Category groceries = category("Groceries");
    long transactionId =
        service.addTransaction(
            TransactionType.EXPENSE,
            new BigDecimal("12.50"),
            month.atDay(1),
            "  Market  ",
            groceries.id());

    service.updateTransaction(
        new Transaction(
            transactionId,
            TransactionType.INCOME,
            new BigDecimal("15"),
            month.atDay(2),
            "  Refund ",
            groceries.id()));

    Transaction updated = service.transactions(month).getFirst();
    assertEquals(TransactionType.INCOME, updated.type());
    assertEquals("Refund", updated.description());
    assertNull(updated.categoryId());

    service.deleteTransaction(transactionId);
    assertTrue(service.transactions(month).isEmpty());
  }

  @Test
  void rejectsEveryInvalidTransactionInput() {
    Category groceries = category("Groceries");

    assertThrows(
        IllegalArgumentException.class,
        () -> service.addTransaction(null, BigDecimal.ONE, month.atDay(1), "", groceries.id()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.addTransaction(
                TransactionType.INCOME, BigDecimal.ZERO, month.atDay(1), "", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.addTransaction(TransactionType.INCOME, null, month.atDay(1), "", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.addTransaction(TransactionType.INCOME, BigDecimal.ONE, null, "", null));
  }

  @Test
  void distinguishesNormalZeroAndNegativeAvailableBudgetStates() {
    Category health = category("Health");
    YearMonth previous = month.minusMonths(1);
    service.setMonthlyBudget(health.id(), previous, new BigDecimal("10"));
    service.addTransaction(
        TransactionType.EXPENSE, new BigDecimal("21"), previous.atDay(1), "Medicine", health.id());
    service.saveSettings(true, 75);

    assertEquals(BudgetState.OVER_BUDGET, summary(health).state());

    Category groceries = category("Groceries");
    service.setMonthlyBudget(groceries.id(), month, new BigDecimal("100"));
    service.addTransaction(
        TransactionType.EXPENSE, new BigDecimal("74"), month.atDay(1), "Market", groceries.id());
    assertEquals(BudgetState.NORMAL, summary(groceries).state());

    Category dining = category("Dining");
    assertEquals(BudgetState.NORMAL, summary(dining).state());
  }

  @Test
  void savesValidSettingsAndProtectsTheFinalCategory() {
    Category groceries = category("Groceries");
    service.saveSettings(true, 75);
    assertTrue(service.settings().rolloverEnabled());
    assertEquals(75, service.settings().warningThreshold());
    assertThrows(
        BudgetPersistenceException.class,
        () -> service.removeCategory(groceries.id(), groceries.id()));

    while (service.categories().size() > 1) {
      Category source = service.categories().getFirst();
      Category replacement = service.categories().get(1);
      service.removeCategory(source.id(), replacement.id());
    }
    Category finalCategory = service.categories().getFirst();

    assertThrows(
        IllegalArgumentException.class,
        () -> service.removeCategory(finalCategory.id(), finalCategory.id()));
  }

  private Category category(String name) {
    return service.categories().stream()
        .filter(category -> category.name().equals(name))
        .findFirst()
        .orElseThrow();
  }

  private Category categoryById(long id) {
    return service.categories().stream()
        .filter(category -> category.id() == id)
        .findFirst()
        .orElseThrow();
  }

  private CategorySummary summary(Category category) {
    return summaryFor(category, month);
  }

  private CategorySummary summaryFor(Category category, YearMonth selectedMonth) {
    return service.dashboard(selectedMonth).categorySummaries().stream()
        .filter(summary -> summary.category().id() == category.id())
        .findFirst()
        .orElseThrow();
  }
}
