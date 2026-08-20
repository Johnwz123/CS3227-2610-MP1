package budgetbot.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A category's calculated position for a selected month.
 *
 * @param category category represented by this summary
 * @param spent total expense amount recorded for the category
 * @param available amount available to spend, including any carryover
 * @param remaining available amount less the recorded expense amount
 * @param state dashboard state derived from the spending and available amounts
 */
public record CategorySummary(
    Category category,
    BigDecimal spent,
    BigDecimal available,
    BigDecimal remaining,
    BudgetState state) {
  /**
   * Creates a summary with all calculated values present.
   *
   * @throws NullPointerException if any component is null
   */
  public CategorySummary {
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(spent, "spent");
    Objects.requireNonNull(available, "available");
    Objects.requireNonNull(remaining, "remaining");
    Objects.requireNonNull(state, "state");
  }
}
