package budgetbot.persistence;

import budgetbot.model.BudgetSettings;
import budgetbot.model.Category;
import budgetbot.model.MonthlyBudget;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Persists monthly category snapshots, base amounts, and rollover values. */
final class MonthlyBudgetRepository {
  private final Connection connection;
  private final CategoryRepository categories;
  private final SettingsRepository settings;
  private final TransactionRepository transactions;

  MonthlyBudgetRepository(
      Connection connection,
      CategoryRepository categories,
      SettingsRepository settings,
      TransactionRepository transactions) {
    this.connection = connection;
    this.categories = categories;
    this.settings = settings;
    this.transactions = transactions;
  }

  List<MonthlyBudget> findOrCreate(YearMonth month) {
    ensure(month);
    try (PreparedStatement s =
        connection.prepareStatement(
            "SELECT category_id, month, base_amount, carryover, rollover_enabled, warning_threshold FROM monthly_budgets WHERE month = ? ORDER BY category_id")) {
      s.setString(1, month.toString());
      try (ResultSet r = s.executeQuery()) {
        List<MonthlyBudget> list = new ArrayList<>();
        while (r.next()) {
          list.add(read(r));
        }
        return list;
      }
    } catch (SQLException e) {
      throw PersistenceSupport.failure("read monthly budgets", e);
    }
  }

  void setBaseAmount(long id, YearMonth month, BigDecimal amount) {
    ensure(month);
    try (PreparedStatement s =
        connection.prepareStatement(
            "UPDATE monthly_budgets SET base_amount = ? WHERE category_id = ? AND month = ?")) {
      s.setString(1, amount.toPlainString());
      s.setLong(2, id);
      s.setString(3, month.toString());
      s.executeUpdate();
    } catch (SQLException e) {
      throw PersistenceSupport.failure("update the local budget", e);
    }
  }

  private void ensure(YearMonth month) {
    for (Category category : categories.findAll()) {
      if (find(category.id(), month) == null) {
        create(category.id(), month);
      }
    }
  }

  private void create(long id, YearMonth month) {
    BudgetSettings config = settings.load();
    MonthlyBudget prior = find(id, month.minusMonths(1));
    BigDecimal base = prior == null ? latestBaseAmount(id, month) : prior.baseAmount();
    BigDecimal carryover =
        config.rolloverEnabled() && prior != null
            ? prior.availableAmount().subtract(transactions.expenseTotal(id, month.minusMonths(1)))
            : BigDecimal.ZERO;
    try (PreparedStatement s =
        connection.prepareStatement(
            "INSERT INTO monthly_budgets(category_id, month, base_amount, carryover, rollover_enabled, warning_threshold) VALUES (?, ?, ?, ?, ?, ?)")) {
      s.setLong(1, id);
      s.setString(2, month.toString());
      s.setString(3, base.toPlainString());
      s.setString(4, carryover.toPlainString());
      s.setInt(5, config.rolloverEnabled() ? 1 : 0);
      s.setInt(6, config.warningThreshold());
      s.executeUpdate();
    } catch (SQLException e) {
      throw PersistenceSupport.failure("create a monthly budget", e);
    }
  }

  private MonthlyBudget find(long id, YearMonth month) {
    try (PreparedStatement s =
        connection.prepareStatement(
            "SELECT category_id, month, base_amount, carryover, rollover_enabled, warning_threshold FROM monthly_budgets WHERE category_id = ? AND month = ?")) {
      s.setLong(1, id);
      s.setString(2, month.toString());
      try (ResultSet r = s.executeQuery()) {
        return r.next() ? read(r) : null;
      }
    } catch (SQLException e) {
      throw PersistenceSupport.failure("read a monthly budget", e);
    }
  }

  private BigDecimal latestBaseAmount(long id, YearMonth month) {
    try (PreparedStatement s =
        connection.prepareStatement(
            "SELECT base_amount FROM monthly_budgets WHERE category_id = ? AND month < ? ORDER BY month DESC LIMIT 1")) {
      s.setLong(1, id);
      s.setString(2, month.toString());
      try (ResultSet r = s.executeQuery()) {
        return r.next() ? new BigDecimal(r.getString(1)) : BigDecimal.ZERO;
      }
    } catch (SQLException e) {
      throw PersistenceSupport.failure("find the latest budget amount", e);
    }
  }

  private MonthlyBudget read(ResultSet r) throws SQLException {
    return new MonthlyBudget(
        r.getLong("category_id"),
        YearMonth.parse(r.getString("month")),
        new BigDecimal(r.getString("base_amount")),
        new BigDecimal(r.getString("carryover")),
        r.getInt("rollover_enabled") == 1,
        r.getInt("warning_threshold"));
  }
}
