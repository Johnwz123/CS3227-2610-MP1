package budgetbot.model;

/** Identifies whether a transaction adds to or subtracts from the overall balance. */
public enum TransactionType {
  /** Adds the transaction amount to the overall balance. */
  INCOME,
  /** Subtracts the transaction amount from the overall balance. */
  EXPENSE
}
