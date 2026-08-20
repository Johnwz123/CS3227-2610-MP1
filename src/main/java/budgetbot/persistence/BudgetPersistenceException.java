package budgetbot.persistence;

/**
 * Signals that the local budget database could not complete an operation.
 *
 * <p>This unchecked exception wraps SQLite and local-file failures so callers can present an
 * actionable message.
 */
public final class BudgetPersistenceException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Creates an exception with an explanatory message.
   *
   * @param message description of the failed persistence operation
   */
  public BudgetPersistenceException(String message) {
    super(message);
  }

  /**
   * Creates an exception with an explanatory message and root cause.
   *
   * @param message description of the failed persistence operation
   * @param cause underlying file-system or database failure
   */
  public BudgetPersistenceException(String message, Throwable cause) {
    super(message, cause);
  }
}
