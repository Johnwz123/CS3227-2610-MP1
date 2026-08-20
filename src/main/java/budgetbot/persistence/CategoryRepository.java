package budgetbot.persistence;

import budgetbot.model.Category;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Persists expense categories and their reassignment on removal. */
final class CategoryRepository {
  private final Connection connection;

  CategoryRepository(Connection connection) {
    this.connection = connection;
  }

  List<Category> findAll() {
    try (PreparedStatement s =
            connection.prepareStatement(
                "SELECT id, name FROM categories ORDER BY name COLLATE NOCASE");
        ResultSet r = s.executeQuery()) {
      List<Category> items = new ArrayList<>();
      while (r.next()) {
        items.add(new Category(r.getLong("id"), r.getString("name")));
      }
      return items;
    } catch (SQLException e) {
      throw PersistenceSupport.failure("read categories", e);
    }
  }

  long add(String name) {
    try (PreparedStatement s =
        connection.prepareStatement(
            "INSERT INTO categories(name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
      s.setString(1, name);
      s.executeUpdate();
      try (ResultSet k = s.getGeneratedKeys()) {
        if (k.next()) {
          return k.getLong(1);
        }
      }
      throw new BudgetPersistenceException("The new category did not receive an identifier.");
    } catch (SQLException e) {
      throw PersistenceSupport.failure("add a category", e);
    }
  }

  void rename(long id, String name) {
    update("UPDATE categories SET name = ? WHERE id = ?", name, id);
  }

  void remove(long id, long replacement) {
    if (id == replacement) {
      throw new BudgetPersistenceException("Choose a different category for reassignment.");
    }
    try {
      connection.setAutoCommit(false);
      update("UPDATE transactions SET category_id = ? WHERE category_id = ?", replacement, id);
      update("DELETE FROM categories WHERE id = ?", id);
      connection.commit();
    } catch (SQLException e) {
      rollback();
      throw PersistenceSupport.failure("remove a category", e);
    } finally {
      restoreAutoCommit();
    }
  }

  private void update(String sql, Object... values) {
    try (PreparedStatement s = connection.prepareStatement(sql)) {
      for (int i = 0; i < values.length; i++) {
        s.setObject(i + 1, values[i]);
      }
      s.executeUpdate();
    } catch (SQLException e) {
      throw PersistenceSupport.failure("update the local budget", e);
    }
  }

  private void rollback() {
    try {
      connection.rollback();
    } catch (SQLException e) {
      throw PersistenceSupport.failure("roll back the category change", e);
    }
  }

  private void restoreAutoCommit() {
    try {
      connection.setAutoCommit(true);
    } catch (SQLException e) {
      throw PersistenceSupport.failure("restore database state", e);
    }
  }
}
