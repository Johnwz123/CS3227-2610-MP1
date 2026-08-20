package budgetbot.model;

/**
 * Global budget preferences applied to months created after a setting changes.
 *
 * @param rolloverEnabled whether unspent amounts carry into the next month's budget snapshot
 * @param warningThreshold percentage of the available amount at which a budget becomes a warning
 */
public record BudgetSettings(boolean rolloverEnabled, int warningThreshold) {
  public static final int DEFAULT_WARNING_THRESHOLD = 80;

  /**
   * Returns the default settings for a new budget.
   *
   * @return settings with rollover disabled and the default warning threshold
   */
  public static BudgetSettings defaults() {
    return new BudgetSettings(false, DEFAULT_WARNING_THRESHOLD);
  }
}
