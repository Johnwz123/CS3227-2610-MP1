package budgetbot.model;

import java.math.BigDecimal;
import java.util.Objects;

/** A category's calculated position for a selected month. */
public record CategorySummary(
    Category category,
    BigDecimal spent,
    BigDecimal available,
    BigDecimal remaining,
    BudgetState state) {
  public CategorySummary {
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(spent, "spent");
    Objects.requireNonNull(available, "available");
    Objects.requireNonNull(remaining, "remaining");
    Objects.requireNonNull(state, "state");
  }
}
