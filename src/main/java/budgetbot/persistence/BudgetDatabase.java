package budgetbot.persistence;

import budgetbot.model.BudgetSettings;
import budgetbot.model.Category;
import budgetbot.model.MonthlyBudget;
import budgetbot.model.Transaction;
import budgetbot.model.TransactionType;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite-backed storage for BudgetBot's single local budget.
 *
 * <p>Each instance owns one database connection and must be closed when no longer needed.
 */
public final class BudgetDatabase implements AutoCloseable {
  private static final List<String> DEFAULT_CATEGORIES =
      List.of(
          "Housing & Utilities",
          "Groceries",
          "Dining",
          "Transport",
          "Health",
          "Entertainment",
          "Shopping",
          "Education",
          "Miscellaneous");

  private final Connection connection;

  /**
   * Opens a database, creates its parent directories and schema when absent, and seeds the default
   * categories.
   *
   * @param databasePath location of the SQLite database file
   * @throws BudgetPersistenceException if the directory cannot be created or the database cannot be
   *     opened or initialized
   */
  public BudgetDatabase(Path databasePath) {
    try {
      Path parent = databasePath.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
      connection.createStatement().execute("PRAGMA foreign_keys = ON");
      initialize();
    } catch (SQLException | java.io.IOException exception) {
      throw new BudgetPersistenceException(
          "BudgetBot could not open its local database.", exception);
    }
  }

  /**
   * Returns all active expense categories in case-insensitive name order.
   *
   * @return the categories currently available for expense transactions
   * @throws BudgetPersistenceException if the categories cannot be read
   */
  public List<Category> categories() {
    List<Category> categories = new ArrayList<>();
    String sql = "SELECT id, name FROM categories ORDER BY name COLLATE NOCASE";
    try (PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet results = statement.executeQuery()) {
      while (results.next()) {
        categories.add(new Category(results.getLong("id"), results.getString("name")));
      }
      return categories;
    } catch (SQLException exception) {
      throw failure("read categories", exception);
    }
  }

  /**
   * Adds an expense category.
   *
   * @param name category name to store
   * @return the generated category identifier
   * @throws BudgetPersistenceException if the category cannot be added or no identifier is returned
   */
  public long addCategory(String name) {
    String sql = "INSERT INTO categories(name) VALUES (?)";
    try (PreparedStatement statement =
        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      statement.setString(1, name);
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) {
          return keys.getLong(1);
        }
      }
      throw new BudgetPersistenceException("The new category did not receive an identifier.");
    } catch (SQLException exception) {
      throw failure("add a category", exception);
    }
  }

  /**
   * Renames a category without changing transactions that refer to it.
   *
   * @param categoryId identifier of the category to rename
   * @param name replacement category name
   * @throws BudgetPersistenceException if the category cannot be updated
   */
  public void renameCategory(long categoryId, String name) {
    executeUpdate("UPDATE categories SET name = ? WHERE id = ?", name, categoryId);
  }

  /**
   * Reassigns a category's expenses and then removes the category as one transaction.
   *
   * @param categoryId identifier of the category to remove
   * @param replacementCategoryId identifier of the category that receives the removed category's
   *     expenses
   * @throws BudgetPersistenceException if both identifiers are the same or the reassignment cannot
   *     be completed
   */
  public void removeCategory(long categoryId, long replacementCategoryId) {
    if (categoryId == replacementCategoryId) {
      throw new BudgetPersistenceException("Choose a different category for reassignment.");
    }
    try {
      connection.setAutoCommit(false);
      executeUpdate(
          "UPDATE transactions SET category_id = ? WHERE category_id = ?",
          replacementCategoryId,
          categoryId);
      executeUpdate("DELETE FROM categories WHERE id = ?", categoryId);
      connection.commit();
    } catch (SQLException exception) {
      rollback();
      throw failure("remove a category", exception);
    } finally {
      restoreAutoCommit();
    }
  }

  /**
   * Returns the configured global settings.
   *
   * @return the budget settings shared by subsequently created monthly budgets
   * @throws BudgetPersistenceException if the settings cannot be read or are missing
   */
  public BudgetSettings settings() {
    String sql = "SELECT rollover_enabled, warning_threshold FROM settings WHERE id = 1";
    try (PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet results = statement.executeQuery()) {
      if (results.next()) {
        return new BudgetSettings(results.getInt(1) == 1, results.getInt(2));
      }
      throw new BudgetPersistenceException("Budget settings are missing.");
    } catch (SQLException exception) {
      throw failure("read settings", exception);
    }
  }

  /**
   * Saves the settings that will be copied into subsequently started months.
   *
   * @param settings settings to persist
   * @throws BudgetPersistenceException if the settings cannot be saved
   */
  public void saveSettings(BudgetSettings settings) {
    executeUpdate(
        "UPDATE settings SET rollover_enabled = ?, warning_threshold = ? WHERE id = 1",
        settings.rolloverEnabled() ? 1 : 0,
        settings.warningThreshold());
  }

  /**
   * Returns transactions in the selected calendar month, newest first.
   *
   * @param month calendar month to query
   * @return transactions in {@code month}, ordered by date and then identifier descending
   * @throws BudgetPersistenceException if the transactions cannot be read
   */
  public List<Transaction> transactions(YearMonth month) {
    String sql =
        "SELECT id, type, amount, transaction_date, description, category_id "
            + "FROM transactions WHERE transaction_date >= ? AND transaction_date < ? "
            + "ORDER BY transaction_date DESC, id DESC";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, month.atDay(1).toString());
      statement.setString(2, month.plusMonths(1).atDay(1).toString());
      return readTransactions(statement);
    } catch (SQLException exception) {
      throw failure("read transactions", exception);
    }
  }

  /**
   * Returns the newest transactions across all months.
   *
   * @param limit maximum number of transactions to return
   * @return at most {@code limit} transactions, ordered by date and then identifier descending
   * @throws BudgetPersistenceException if the transactions cannot be read
   */
  public List<Transaction> recentTransactions(int limit) {
    String sql =
        "SELECT id, type, amount, transaction_date, description, category_id "
            + "FROM transactions ORDER BY transaction_date DESC, id DESC LIMIT ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, limit);
      return readTransactions(statement);
    } catch (SQLException exception) {
      throw failure("read recent transactions", exception);
    }
  }

  /**
   * Stores a new transaction.
   *
   * @param transaction transaction to store
   * @return the generated transaction identifier
   * @throws BudgetPersistenceException if the transaction cannot be added or no identifier is
   *     returned
   */
  public long addTransaction(Transaction transaction) {
    String sql =
        "INSERT INTO transactions(type, amount, transaction_date, description, category_id) "
            + "VALUES (?, ?, ?, ?, ?)";
    try (PreparedStatement statement =
        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      setTransaction(statement, transaction);
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) {
          return keys.getLong(1);
        }
      }
      throw new BudgetPersistenceException("The new transaction did not receive an identifier.");
    } catch (SQLException exception) {
      throw failure("add a transaction", exception);
    }
  }

  /**
   * Updates an existing transaction.
   *
   * @param transaction replacement transaction data, including the identifier to update
   * @throws BudgetPersistenceException if the transaction cannot be updated
   */
  public void updateTransaction(Transaction transaction) {
    String sql =
        "UPDATE transactions SET type = ?, amount = ?, transaction_date = ?, description = ?, "
            + "category_id = ? WHERE id = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      setTransaction(statement, transaction);
      statement.setLong(6, transaction.id());
      statement.executeUpdate();
    } catch (SQLException exception) {
      throw failure("update a transaction", exception);
    }
  }

  /**
   * Deletes a transaction by identifier.
   *
   * @param transactionId identifier of the transaction to delete
   * @throws BudgetPersistenceException if the transaction cannot be deleted
   */
  public void deleteTransaction(long transactionId) {
    executeUpdate("DELETE FROM transactions WHERE id = ?", transactionId);
  }

  /**
   * Creates any missing category snapshots for a month and returns all of that month's budgets.
   *
   * @param month calendar month to read or initialize
   * @return the category budget snapshots for {@code month}
   * @throws BudgetPersistenceException if the snapshots cannot be created or read
   */
  public List<MonthlyBudget> monthlyBudgets(YearMonth month) {
    ensureMonthlyBudgets(month);
    List<MonthlyBudget> budgets = new ArrayList<>();
    String sql =
        "SELECT category_id, month, base_amount, carryover, rollover_enabled, warning_threshold "
            + "FROM monthly_budgets WHERE month = ? ORDER BY category_id";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, month.toString());
      try (ResultSet results = statement.executeQuery()) {
        while (results.next()) {
          budgets.add(readBudget(results));
        }
      }
      return budgets;
    } catch (SQLException exception) {
      throw failure("read monthly budgets", exception);
    }
  }

  /**
   * Changes the base amount in the selected month's category snapshot, creating missing snapshots
   * first.
   *
   * @param categoryId identifier of the category to update
   * @param month calendar month of the budget snapshot
   * @param amount replacement base amount
   * @throws BudgetPersistenceException if the snapshot cannot be created or updated
   */
  public void setMonthlyBaseAmount(long categoryId, YearMonth month, BigDecimal amount) {
    ensureMonthlyBudgets(month);
    executeUpdate(
        "UPDATE monthly_budgets SET base_amount = ? WHERE category_id = ? AND month = ?",
        amount.toPlainString(),
        categoryId,
        month.toString());
  }

  /**
   * Returns the total expenses in one category for a selected month.
   *
   * @param categoryId identifier of the expense category
   * @param month calendar month to query
   * @return the sum of matching expense amounts, or zero when there are no matching transactions
   * @throws BudgetPersistenceException if the total cannot be calculated
   */
  public BigDecimal expenseTotal(long categoryId, YearMonth month) {
    String sql =
        "SELECT amount FROM transactions WHERE type = 'EXPENSE' AND category_id = ? AND transaction_date >= ? "
            + "AND transaction_date < ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, categoryId);
      statement.setString(2, month.atDay(1).toString());
      statement.setString(3, month.plusMonths(1).atDay(1).toString());
      try (ResultSet results = statement.executeQuery()) {
        return sumAmounts(results);
      }
    } catch (SQLException exception) {
      throw failure("calculate category spending", exception);
    }
  }

  /**
   * Returns all-time income minus expenses.
   *
   * @return the sum of all income amounts less the sum of all expense amounts
   * @throws BudgetPersistenceException if the balance cannot be calculated
   */
  public BigDecimal overallBalance() {
    String sql = "SELECT type, amount FROM transactions";
    try (PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet results = statement.executeQuery()) {
      BigDecimal total = BigDecimal.ZERO;
      while (results.next()) {
        BigDecimal amount = new BigDecimal(results.getString("amount"));
        total =
            TransactionType.INCOME.name().equals(results.getString("type"))
                ? total.add(amount)
                : total.subtract(amount);
      }
      return total;
    } catch (SQLException exception) {
      throw failure("calculate the overall balance", exception);
    }
  }

  /**
   * Closes the database connection owned by this instance.
   *
   * @throws BudgetPersistenceException if the connection cannot be closed
   */
  @Override
  public void close() {
    try {
      connection.close();
    } catch (SQLException exception) {
      throw failure("close the database", exception);
    }
  }

  private void initialize() throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(
          "CREATE TABLE IF NOT EXISTS categories ("
              + "id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE COLLATE NOCASE)");
      statement.execute(
          "CREATE TABLE IF NOT EXISTS settings ("
              + "id INTEGER PRIMARY KEY CHECK (id = 1), rollover_enabled INTEGER NOT NULL, "
              + "warning_threshold INTEGER NOT NULL)");
      statement.execute(
          "CREATE TABLE IF NOT EXISTS transactions ("
              + "id INTEGER PRIMARY KEY, type TEXT NOT NULL, amount TEXT NOT NULL, "
              + "transaction_date TEXT NOT NULL, description TEXT NOT NULL, category_id INTEGER, "
              + "FOREIGN KEY(category_id) REFERENCES categories(id))");
      statement.execute(
          "CREATE TABLE IF NOT EXISTS monthly_budgets ("
              + "category_id INTEGER NOT NULL, month TEXT NOT NULL, base_amount TEXT NOT NULL, "
              + "carryover TEXT NOT NULL, rollover_enabled INTEGER NOT NULL, "
              + "warning_threshold INTEGER NOT NULL, PRIMARY KEY(category_id, month), "
              + "FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE CASCADE)");
      statement.execute(
          "INSERT OR IGNORE INTO settings(id, rollover_enabled, warning_threshold) VALUES (1, 0, 80)");
    }
    if (categories().isEmpty()) {
      for (String category : DEFAULT_CATEGORIES) {
        addCategory(category);
      }
    }
  }

  private void ensureMonthlyBudgets(YearMonth month) {
    for (Category category : categories()) {
      if (!monthlyBudgetExists(category.id(), month)) {
        createMonthlyBudget(category.id(), month);
      }
    }
  }

  private boolean monthlyBudgetExists(long categoryId, YearMonth month) {
    String sql = "SELECT 1 FROM monthly_budgets WHERE category_id = ? AND month = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, categoryId);
      statement.setString(2, month.toString());
      try (ResultSet results = statement.executeQuery()) {
        return results.next();
      }
    } catch (SQLException exception) {
      throw failure("inspect monthly budgets", exception);
    }
  }

  private void createMonthlyBudget(long categoryId, YearMonth month) {
    BudgetSettings settings = settings();
    MonthlyBudget priorBudget = findMonthlyBudget(categoryId, month.minusMonths(1));
    BigDecimal baseAmount =
        priorBudget == null ? latestBaseAmount(categoryId, month) : priorBudget.baseAmount();
    BigDecimal carryover = BigDecimal.ZERO;
    if (settings.rolloverEnabled() && priorBudget != null) {
      carryover =
          priorBudget.availableAmount().subtract(expenseTotal(categoryId, month.minusMonths(1)));
    }
    String sql =
        "INSERT INTO monthly_budgets(category_id, month, base_amount, carryover, rollover_enabled, "
            + "warning_threshold) VALUES (?, ?, ?, ?, ?, ?)";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, categoryId);
      statement.setString(2, month.toString());
      statement.setString(3, baseAmount.toPlainString());
      statement.setString(4, carryover.toPlainString());
      statement.setInt(5, settings.rolloverEnabled() ? 1 : 0);
      statement.setInt(6, settings.warningThreshold());
      statement.executeUpdate();
    } catch (SQLException exception) {
      throw failure("create a monthly budget", exception);
    }
  }

  private MonthlyBudget findMonthlyBudget(long categoryId, YearMonth month) {
    String sql =
        "SELECT category_id, month, base_amount, carryover, rollover_enabled, warning_threshold "
            + "FROM monthly_budgets WHERE category_id = ? AND month = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, categoryId);
      statement.setString(2, month.toString());
      try (ResultSet results = statement.executeQuery()) {
        return results.next() ? readBudget(results) : null;
      }
    } catch (SQLException exception) {
      throw failure("read a monthly budget", exception);
    }
  }

  private BigDecimal latestBaseAmount(long categoryId, YearMonth beforeMonth) {
    String sql =
        "SELECT base_amount FROM monthly_budgets WHERE category_id = ? AND month < ? "
            + "ORDER BY month DESC LIMIT 1";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, categoryId);
      statement.setString(2, beforeMonth.toString());
      try (ResultSet results = statement.executeQuery()) {
        return results.next() ? new BigDecimal(results.getString(1)) : BigDecimal.ZERO;
      }
    } catch (SQLException exception) {
      throw failure("find the latest budget amount", exception);
    }
  }

  private List<Transaction> readTransactions(PreparedStatement statement) throws SQLException {
    List<Transaction> transactions = new ArrayList<>();
    try (ResultSet results = statement.executeQuery()) {
      while (results.next()) {
        long categoryId = results.getLong("category_id");
        boolean hasCategory = !results.wasNull();
        transactions.add(
            new Transaction(
                results.getLong("id"),
                TransactionType.valueOf(results.getString("type")),
                new BigDecimal(results.getString("amount")),
                LocalDate.parse(results.getString("transaction_date")),
                results.getString("description"),
                hasCategory ? categoryId : null));
      }
    }
    return transactions;
  }

  private MonthlyBudget readBudget(ResultSet results) throws SQLException {
    return new MonthlyBudget(
        results.getLong("category_id"),
        YearMonth.parse(results.getString("month")),
        new BigDecimal(results.getString("base_amount")),
        new BigDecimal(results.getString("carryover")),
        results.getInt("rollover_enabled") == 1,
        results.getInt("warning_threshold"));
  }

  private BigDecimal sumAmounts(ResultSet results) throws SQLException {
    BigDecimal total = BigDecimal.ZERO;
    while (results.next()) {
      total = total.add(new BigDecimal(results.getString("amount")));
    }
    return total;
  }

  private void setTransaction(PreparedStatement statement, Transaction transaction)
      throws SQLException {
    statement.setString(1, transaction.type().name());
    statement.setString(2, transaction.amount().toPlainString());
    statement.setString(3, transaction.date().toString());
    statement.setString(4, transaction.description());
    if (transaction.categoryId() == null) {
      statement.setNull(5, java.sql.Types.INTEGER);
    } else {
      statement.setLong(5, transaction.categoryId());
    }
  }

  private void executeUpdate(String sql, Object... values) {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int index = 0; index < values.length; index++) {
        statement.setObject(index + 1, values[index]);
      }
      statement.executeUpdate();
    } catch (SQLException exception) {
      throw failure("update the local budget", exception);
    }
  }

  private void rollback() {
    try {
      connection.rollback();
    } catch (SQLException exception) {
      throw failure("roll back the category change", exception);
    }
  }

  private void restoreAutoCommit() {
    try {
      connection.setAutoCommit(true);
    } catch (SQLException exception) {
      throw failure("restore database state", exception);
    }
  }

  private BudgetPersistenceException failure(String action, SQLException cause) {
    return new BudgetPersistenceException("BudgetBot could not " + action + ".", cause);
  }
}
