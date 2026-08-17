package budgetbot.persistence;

/** Signals that the local budget database could not complete an operation. */
public final class BudgetPersistenceException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /** Creates an exception with an explanatory message. */
  public BudgetPersistenceException(String message) {
    super(message);
  }

  /** Creates an exception with an explanatory message and root cause. */
  public BudgetPersistenceException(String message, Throwable cause) {
    super(message, cause);
  }
}
