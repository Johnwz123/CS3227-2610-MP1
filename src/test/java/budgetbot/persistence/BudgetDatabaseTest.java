package budgetbot.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import budgetbot.model.BudgetSettings;
import budgetbot.model.Category;
import budgetbot.model.MonthlyBudget;
import budgetbot.model.Transaction;
import budgetbot.model.TransactionType;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BudgetDatabaseTest {
  @TempDir Path temporaryDirectory;

  private BudgetDatabase database;

  @AfterEach
  void closeDatabase() {
    if (database != null) {
      database.close();
    }
  }

  @Test
  void createsParentDirectoriesDefaultCategoriesAndSettings() throws IOException {
    Path databasePath = temporaryDirectory.resolve("nested").resolve("budgetbot.db");
    database = new BudgetDatabase(databasePath);

    assertTrue(Files.isRegularFile(databasePath));
    assertEquals(9, database.categories().size());
    assertEquals("Dining", database.categories().getFirst().name());
    assertEquals(new BudgetSettings(false, 80), database.settings());
  }

  @Test
  void reopensWithoutReseedingOrLosingRepositoryData() {
    BudgetDatabase storage = openDatabase();
    long categoryId = storage.addCategory("Travel");
    storage.saveSettings(new BudgetSettings(true, 65));
    storage.close();

    database = new BudgetDatabase(temporaryDirectory.resolve("budgetbot.db"));

    assertEquals(10, database.categories().size());
    assertTrue(database.categories().stream().anyMatch(category -> category.id() == categoryId));
    assertEquals(new BudgetSettings(true, 65), database.settings());
  }

  @Test
  void managesCategoriesAndReassignsTransactionsBeforeRemoval() {
    BudgetDatabase storage = openDatabase();
    Category groceries = category("Groceries");
    long travelId = storage.addCategory("Travel");
    storage.renameCategory(travelId, "Trips");
    storage.addTransaction(
        new Transaction(
            0,
            TransactionType.EXPENSE,
            new BigDecimal("18.50"),
            YearMonth.of(2026, 8).atDay(3),
            "Train",
            travelId));

    storage.removeCategory(travelId, groceries.id());

    assertFalse(storage.categories().stream().anyMatch(category -> category.id() == travelId));
    assertEquals(
        groceries.id(), storage.transactions(YearMonth.of(2026, 8)).getFirst().categoryId());
    assertThrows(
        BudgetPersistenceException.class,
        () -> storage.removeCategory(groceries.id(), groceries.id()));
  }

  @Test
  void storesQueriesUpdatesAndDeletesTransactions() {
    BudgetDatabase storage = openDatabase();
    Category groceries = category("Groceries");
    YearMonth month = YearMonth.of(2026, 8);
    long incomeId =
        storage.addTransaction(
            new Transaction(
                0, TransactionType.INCOME, new BigDecimal("100"), month.atDay(1), "Pay", null));
    long expenseId =
        storage.addTransaction(
            new Transaction(
                0,
                TransactionType.EXPENSE,
                new BigDecimal("25"),
                month.atDay(5),
                "Market",
                groceries.id()));
    storage.addTransaction(
        new Transaction(
            0,
            TransactionType.EXPENSE,
            new BigDecimal("10"),
            month.minusMonths(1).atDay(1),
            "Earlier",
            groceries.id()));

    assertEquals(
        List.of(expenseId, incomeId),
        storage.transactions(month).stream().map(Transaction::id).toList());
    assertEquals(2, storage.recentTransactions(2).size());
    assertEquals(new BigDecimal("25"), storage.expenseTotal(groceries.id(), month));
    assertEquals(new BigDecimal("65"), storage.overallBalance());

    storage.updateTransaction(
        new Transaction(
            expenseId,
            TransactionType.INCOME,
            new BigDecimal("30"),
            month.atDay(5),
            "Refund",
            null));
    assertEquals(TransactionType.INCOME, storage.transactions(month).getFirst().type());
    assertEquals(new BigDecimal("120"), storage.overallBalance());

    storage.deleteTransaction(incomeId);
    assertEquals(1, storage.transactions(month).size());
  }

  @Test
  void snapshotsBudgetsAndCarriesRemainingAmountsForward() {
    BudgetDatabase storage = openDatabase();
    Category groceries = category("Groceries");
    YearMonth month = YearMonth.of(2026, 8);
    storage.monthlyBudgets(month);
    storage.setMonthlyBaseAmount(groceries.id(), month, new BigDecimal("100"));
    storage.addTransaction(
        new Transaction(
            0,
            TransactionType.EXPENSE,
            new BigDecimal("25"),
            month.atDay(2),
            "Market",
            groceries.id()));
    storage.saveSettings(new BudgetSettings(true, 70));

    MonthlyBudget nextMonth = monthlyBudget(groceries.id(), month.plusMonths(1));

    assertEquals(new BigDecimal("100"), nextMonth.baseAmount());
    assertEquals(new BigDecimal("75"), nextMonth.carryover());
    assertEquals(new BigDecimal("175"), nextMonth.availableAmount());
    assertTrue(nextMonth.rolloverEnabled());
    assertEquals(70, nextMonth.warningThreshold());
  }

  private BudgetDatabase openDatabase() {
    database = new BudgetDatabase(temporaryDirectory.resolve("budgetbot.db"));
    return database;
  }

  private Category category(String name) {
    return database.categories().stream()
        .filter(category -> category.name().equals(name))
        .findFirst()
        .orElseThrow();
  }

  private MonthlyBudget monthlyBudget(long categoryId, YearMonth month) {
    return database.monthlyBudgets(month).stream()
        .filter(budget -> budget.categoryId() == categoryId)
        .findFirst()
        .orElseThrow();
  }
}
