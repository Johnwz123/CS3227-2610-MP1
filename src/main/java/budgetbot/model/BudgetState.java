package budgetbot.model;

/** The dashboard state derived from a category's monthly spending. */
public enum BudgetState {
  /** Spending is below the warning threshold. */
  NORMAL,
  /** Spending has reached the warning threshold without exceeding the available amount. */
  WARNING,
  /** Spending has reached or exceeded the available amount. */
  OVER_BUDGET
}
