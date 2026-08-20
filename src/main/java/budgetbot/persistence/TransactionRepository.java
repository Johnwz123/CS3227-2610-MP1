package budgetbot.persistence;

import budgetbot.model.Transaction;
import budgetbot.model.TransactionType;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Persists transactions and calculates transaction-derived totals. */
final class TransactionRepository {
  private final Connection connection;

  TransactionRepository(Connection connection) {
    this.connection = connection;
  }

  List<Transaction> findByMonth(YearMonth month) {
    try (PreparedStatement s =
        connection.prepareStatement(
            "SELECT id, type, amount, transaction_date, description, category_id FROM transactions WHERE transaction_date >= ? AND transaction_date < ? ORDER BY transaction_date DESC, id DESC")) {
      s.setString(1, month.atDay(1).toString());
      s.setString(2, month.plusMonths(1).atDay(1).toString());
      return read(s);
    } catch (SQLException e) {
      throw PersistenceSupport.failure("read transactions", e);
    }
  }

  List<Transaction> findRecent(int limit) {
    try (PreparedStatement s =
        connection.prepareStatement(
            "SELECT id, type, amount, transaction_date, description, category_id FROM transactions ORDER BY transaction_date DESC, id DESC LIMIT ?")) {
      s.setInt(1, limit);
      return read(s);
    } catch (SQLException e) {
      throw PersistenceSupport.failure("read recent transactions", e);
    }
  }

  long add(Transaction t) {
    try (PreparedStatement s =
        connection.prepareStatement(
            "INSERT INTO transactions(type, amount, transaction_date, description, category_id) VALUES (?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS)) {
      set(s, t);
      s.executeUpdate();
      try (ResultSet k = s.getGeneratedKeys()) {
        if (k.next()) {
          return k.getLong(1);
        }
      }
      throw new BudgetPersistenceException("The new transaction did not receive an identifier.");
    } catch (SQLException e) {
      throw PersistenceSupport.failure("add a transaction", e);
    }
  }

  void update(Transaction t) {
    try (PreparedStatement s =
        connection.prepareStatement(
            "UPDATE transactions SET type = ?, amount = ?, transaction_date = ?, description = ?, category_id = ? WHERE id = ?")) {
      set(s, t);
      s.setLong(6, t.id());
      s.executeUpdate();
    } catch (SQLException e) {
      throw PersistenceSupport.failure("update a transaction", e);
    }
  }

  void delete(long id) {
    try (PreparedStatement s =
        connection.prepareStatement("DELETE FROM transactions WHERE id = ?")) {
      s.setLong(1, id);
      s.executeUpdate();
    } catch (SQLException e) {
      throw PersistenceSupport.failure("update the local budget", e);
    }
  }

  BigDecimal expenseTotal(long id, YearMonth month) {
    try (PreparedStatement s =
        connection.prepareStatement(
            "SELECT amount FROM transactions WHERE type = 'EXPENSE' AND category_id = ? AND transaction_date >= ? AND transaction_date < ?")) {
      s.setLong(1, id);
      s.setString(2, month.atDay(1).toString());
      s.setString(3, month.plusMonths(1).atDay(1).toString());
      try (ResultSet r = s.executeQuery()) {
        return sum(r);
      }
    } catch (SQLException e) {
      throw PersistenceSupport.failure("calculate category spending", e);
    }
  }

  BigDecimal overallBalance() {
    try (PreparedStatement s =
            connection.prepareStatement("SELECT type, amount FROM transactions");
        ResultSet r = s.executeQuery()) {
      BigDecimal total = BigDecimal.ZERO;
      while (r.next()) {
        BigDecimal a = new BigDecimal(r.getString("amount"));
        total =
            TransactionType.INCOME.name().equals(r.getString("type"))
                ? total.add(a)
                : total.subtract(a);
      }
      return total;
    } catch (SQLException e) {
      throw PersistenceSupport.failure("calculate the overall balance", e);
    }
  }

  private List<Transaction> read(PreparedStatement s) throws SQLException {
    try (ResultSet r = s.executeQuery()) {
      List<Transaction> list = new ArrayList<>();
      while (r.next()) {
        long categoryId = r.getLong("category_id");
        boolean hasCategory = !r.wasNull();
        list.add(
            new Transaction(
                r.getLong("id"),
                TransactionType.valueOf(r.getString("type")),
                new BigDecimal(r.getString("amount")),
                LocalDate.parse(r.getString("transaction_date")),
                r.getString("description"),
                hasCategory ? categoryId : null));
      }
      return list;
    }
  }

  private BigDecimal sum(ResultSet r) throws SQLException {
    BigDecimal total = BigDecimal.ZERO;
    while (r.next()) {
      total = total.add(new BigDecimal(r.getString("amount")));
    }
    return total;
  }

  private void set(PreparedStatement s, Transaction t) throws SQLException {
    s.setString(1, t.type().name());
    s.setString(2, t.amount().toPlainString());
    s.setString(3, t.date().toString());
    s.setString(4, t.description());
    if (t.categoryId() == null) {
      s.setNull(5, Types.INTEGER);
    } else {
      s.setLong(5, t.categoryId());
    }
  }
}
