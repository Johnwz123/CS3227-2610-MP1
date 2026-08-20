package budgetbot.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/**
 * All figures shown on the dashboard for a selected month.
 *
 * @param month month represented by the dashboard
 * @param overallBalance all-time income less all-time expenses
 * @param recentTransactions newest transactions across all months
 * @param categorySummaries calculated budget status for each category
 */
public record DashboardSnapshot(
    YearMonth month,
    BigDecimal overallBalance,
    List<Transaction> recentTransactions,
    List<CategorySummary> categorySummaries) {
  /**
   * Creates a dashboard snapshot with immutable transaction and category-summary lists.
   *
   * @throws NullPointerException if a required component or either list is null
   */
  public DashboardSnapshot {
    Objects.requireNonNull(month, "month");
    Objects.requireNonNull(overallBalance, "overallBalance");
    recentTransactions = List.copyOf(recentTransactions);
    categorySummaries = List.copyOf(categorySummaries);
  }
}
