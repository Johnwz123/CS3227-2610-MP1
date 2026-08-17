package budgetbot.model;

/** Global budget preferences applied to months created after a setting changes. */
public record BudgetSettings(boolean rolloverEnabled, int warningThreshold) {
  public static final int DEFAULT_WARNING_THRESHOLD = 80;

  /** Returns the default settings for a new budget. */
  public static BudgetSettings defaults() {
    return new BudgetSettings(false, DEFAULT_WARNING_THRESHOLD);
  }
}
