package budgetbot.model;

import java.math.BigDecimal;
import java.time.YearMonth;

/** A historical monthly budget snapshot for one category. */
public record MonthlyBudget(
    long categoryId,
    YearMonth month,
    BigDecimal baseAmount,
    BigDecimal carryover,
    boolean rolloverEnabled,
    int warningThreshold) {
  /** Returns the amount available to spend during the month. */
  public BigDecimal availableAmount() {
    return baseAmount.add(carryover);
  }
}
