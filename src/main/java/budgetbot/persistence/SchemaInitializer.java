package budgetbot.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates the SQLite schema and seeds default settings and categories. */
final class SchemaInitializer {
  private static final String[] DEFAULT_CATEGORIES = {
    "Housing & Utilities",
    "Groceries",
    "Dining",
    "Transport",
    "Health",
    "Entertainment",
    "Shopping",
    "Education",
    "Miscellaneous"
  };

  private SchemaInitializer() {
    throw new AssertionError("Utility class");
  }

  static void initialize(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(
          "CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE COLLATE NOCASE)");
      statement.execute(
          "CREATE TABLE IF NOT EXISTS settings (id INTEGER PRIMARY KEY CHECK (id = 1), warning_threshold INTEGER NOT NULL)");
      statement.execute(
          "CREATE TABLE IF NOT EXISTS transactions (id INTEGER PRIMARY KEY, type TEXT NOT NULL, amount TEXT NOT NULL, transaction_date TEXT NOT NULL, description TEXT NOT NULL, category_id INTEGER, FOREIGN KEY(category_id) REFERENCES categories(id))");
      statement.execute(
          "CREATE TABLE IF NOT EXISTS monthly_budgets (category_id INTEGER NOT NULL, month TEXT NOT NULL, base_amount TEXT NOT NULL, warning_threshold INTEGER NOT NULL, PRIMARY KEY(category_id, month), FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE CASCADE)");
      statement.execute("INSERT OR IGNORE INTO settings(id, warning_threshold) VALUES (1, 80)");
      for (String category : DEFAULT_CATEGORIES) {
        statement.executeUpdate(
            "INSERT OR IGNORE INTO categories(name) VALUES ('" + category + "')");
      }
    }
  }
}
