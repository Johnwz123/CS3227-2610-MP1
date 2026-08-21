package budgetbot.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/**
 * All figures shown on the dashboard for a selected month.
 *
 * @param month month represented by the dashboard
 * @param netCashFlow selected-month income less selected-month expenses
 * @param categorySummaries calculated budget status for each category
 */
public record DashboardSnapshot(
    YearMonth month, BigDecimal netCashFlow, List<CategorySummary> categorySummaries) {
  /**
   * Creates a dashboard snapshot with immutable transaction and category-summary lists.
   *
   * @throws NullPointerException if a required component or the category summaries are null
   */
  public DashboardSnapshot {
    Objects.requireNonNull(month, "month");
    Objects.requireNonNull(netCashFlow, "netCashFlow");
    categorySummaries = List.copyOf(categorySummaries);
  }
}
