package budgetbot.model;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * A historical monthly budget snapshot for one category.
 *
 * @param categoryId persistent category identifier
 * @param month month represented by the snapshot
 * @param baseAmount configured monthly amount before carryover
 * @param carryover amount carried forward from the preceding month
 * @param rolloverEnabled whether rollover was enabled when the snapshot was created
 * @param warningThreshold warning percentage copied into the snapshot when it was created
 */
public record MonthlyBudget(
    long categoryId,
    YearMonth month,
    BigDecimal baseAmount,
    BigDecimal carryover,
    boolean rolloverEnabled,
    int warningThreshold) {
  /**
   * Returns the amount available to spend during the month.
   *
   * @return the base amount plus carryover
   */
  public BigDecimal availableAmount() {
    return baseAmount.add(carryover);
  }
}
