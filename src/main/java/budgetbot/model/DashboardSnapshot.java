package budgetbot.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/** All figures shown on the dashboard for a selected month. */
public record DashboardSnapshot(
    YearMonth month,
    BigDecimal overallBalance,
    List<Transaction> recentTransactions,
    List<CategorySummary> categorySummaries) {
  public DashboardSnapshot {
    Objects.requireNonNull(month, "month");
    Objects.requireNonNull(overallBalance, "overallBalance");
    recentTransactions = List.copyOf(recentTransactions);
    categorySummaries = List.copyOf(categorySummaries);
  }
}
