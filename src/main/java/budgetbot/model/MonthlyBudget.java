package budgetbot.model;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * A historical monthly budget snapshot for one category.
 *
 * @param categoryId persistent category identifier
 * @param month month represented by the snapshot
 * @param baseAmount configured monthly amount
 * @param warningThreshold warning percentage copied into the snapshot when it was created
 */
public record MonthlyBudget(
    long categoryId, YearMonth month, BigDecimal baseAmount, int warningThreshold) {
  /**
   * Returns the amount available to spend during the month.
   *
   * @return the fixed monthly base amount
   */
  public BigDecimal availableAmount() {
    return baseAmount;
  }
}
