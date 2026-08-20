package budgetbot.persistence;

import java.sql.SQLException;

/** Shared JDBC error conversion for BudgetBot repositories. */
final class PersistenceSupport {
  private PersistenceSupport() {
    throw new AssertionError("Utility class");
  }

  static BudgetPersistenceException failure(String action, SQLException cause) {
    return new BudgetPersistenceException("BudgetBot could not " + action + ".", cause);
  }
}
