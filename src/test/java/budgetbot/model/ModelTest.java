package budgetbot.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModelTest {
  @Test
  void defaultsAndMonthlyAvailabilityUseExpectedValues() {
    BudgetSettings settings = BudgetSettings.defaults();
    MonthlyBudget budget =
        new MonthlyBudget(
            3, YearMonth.of(2026, 8), new BigDecimal("100"), new BigDecimal("25"), true, 75);

    assertFalse(settings.rolloverEnabled());
    assertEquals(80, settings.warningThreshold());
    assertEquals(new BigDecimal("125"), budget.availableAmount());
    assertTrue(budget.rolloverEnabled());
  }

  @Test
  void recordsRejectMissingRequiredValuesAndNormalizeDescriptions() {
    assertThrows(NullPointerException.class, () -> new Category(1, null));
    assertThrows(
        NullPointerException.class,
        () ->
            new CategorySummary(
                null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BudgetState.NORMAL));
    assertThrows(
        NullPointerException.class,
        () -> new Transaction(1, null, BigDecimal.ONE, LocalDate.now(), "", null));

    Transaction transaction =
        new Transaction(
            1, TransactionType.INCOME, BigDecimal.ONE, LocalDate.of(2026, 8, 1), null, null);

    assertEquals("", transaction.description());
  }

  @Test
  void dashboardSnapshotCopiesItsLists() {
    Transaction transaction =
        new Transaction(
            1, TransactionType.INCOME, BigDecimal.ONE, LocalDate.of(2026, 8, 1), "Pay", null);
    CategorySummary summary =
        new CategorySummary(
            new Category(1, "Income"),
            BigDecimal.ZERO,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BudgetState.NORMAL);
    List<Transaction> transactions = new ArrayList<>(List.of(transaction));
    List<CategorySummary> summaries = new ArrayList<>(List.of(summary));

    DashboardSnapshot snapshot =
        new DashboardSnapshot(YearMonth.of(2026, 8), BigDecimal.ONE, transactions, summaries);
    transactions.clear();
    summaries.clear();

    assertEquals(1, snapshot.recentTransactions().size());
    assertEquals(1, snapshot.categorySummaries().size());
    assertThrows(
        UnsupportedOperationException.class, () -> snapshot.recentTransactions().add(transaction));
  }

  @Test
  void transactionAndBudgetStatesExposeTheirDomainValues() {
    assertEquals(TransactionType.INCOME, TransactionType.valueOf("INCOME"));
    assertEquals(TransactionType.EXPENSE, TransactionType.valueOf("EXPENSE"));
    assertEquals(
        List.of(BudgetState.NORMAL, BudgetState.WARNING, BudgetState.OVER_BUDGET),
        List.of(BudgetState.values()));
  }
}
