package budgetbot.model;

/**
 * Global budget preferences applied to months created after a setting changes.
 *
 * @param warningThreshold percentage of the available amount at which a budget becomes a warning
 */
public record BudgetSettings(int warningThreshold) {
  public static final int DEFAULT_WARNING_THRESHOLD = 80;

  /**
   * Returns the default settings for a new budget.
   *
   * @return settings with the default warning threshold
   */
  public static BudgetSettings defaults() {
    return new BudgetSettings(DEFAULT_WARNING_THRESHOLD);
  }
}
