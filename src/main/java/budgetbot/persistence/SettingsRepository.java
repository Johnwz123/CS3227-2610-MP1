package budgetbot.persistence;

import budgetbot.model.BudgetSettings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Persists global settings copied into newly created monthly budgets. */
final class SettingsRepository {
  private final Connection connection;

  SettingsRepository(Connection connection) {
    this.connection = connection;
  }

  BudgetSettings load() {
    try (PreparedStatement s =
            connection.prepareStatement("SELECT warning_threshold FROM settings WHERE id = 1");
        ResultSet r = s.executeQuery()) {
      if (r.next()) {
        return new BudgetSettings(r.getInt(1));
      }
      throw new BudgetPersistenceException("Budget settings are missing.");
    } catch (SQLException e) {
      throw PersistenceSupport.failure("read settings", e);
    }
  }

  void save(BudgetSettings value) {
    try (PreparedStatement s =
        connection.prepareStatement("UPDATE settings SET warning_threshold = ? WHERE id = 1")) {
      s.setInt(1, value.warningThreshold());
      s.executeUpdate();
    } catch (SQLException e) {
      throw PersistenceSupport.failure("update the local budget", e);
    }
  }
}
