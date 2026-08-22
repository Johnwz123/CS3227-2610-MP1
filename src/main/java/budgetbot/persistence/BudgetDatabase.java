package budgetbot.persistence;

import budgetbot.model.BudgetSettings;
import budgetbot.model.Category;
import budgetbot.model.MonthlyBudget;
import budgetbot.model.Transaction;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.List;

/** SQLite storage facade that owns one connection and delegates to focused repositories. */
public final class BudgetDatabase implements AutoCloseable {
  private final Connection connection;
  private final CategoryRepository categoryRepository;
  private final SettingsRepository settingsRepository;
  private final TransactionRepository transactionRepository;
  private final MonthlyBudgetRepository monthlyBudgetRepository;

  /**
   * Opens and initializes a local SQLite database.
   *
   * @param databasePath location of the SQLite database file
   * @throws BudgetPersistenceException if the database cannot be opened or initialized
   */
  public BudgetDatabase(Path databasePath) {
    try {
      Path parent = databasePath.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
      try (Statement statement = connection.createStatement()) {
        statement.execute("PRAGMA foreign_keys = ON");
      }
      SchemaInitializer.initialize(connection);
      categoryRepository = new CategoryRepository(connection);
      settingsRepository = new SettingsRepository(connection);
      transactionRepository = new TransactionRepository(connection);
      monthlyBudgetRepository =
          new MonthlyBudgetRepository(connection, categoryRepository, settingsRepository);
    } catch (SQLException | java.io.IOException exception) {
      throw new BudgetPersistenceException(
          "BudgetBot could not open its local database.", exception);
    }
  }

  /**
   * Lists all expense categories in alphabetical order.
   *
   * @return persisted categories
   * @throws BudgetPersistenceException if the categories cannot be read
   */
  public List<Category> categories() {
    return categoryRepository.findAll();
  }

  /**
   * Adds an expense category.
   *
   * @param name category name
   * @return generated category identifier
   * @throws BudgetPersistenceException if the category cannot be added
   */
  public long addCategory(String name) {
    return categoryRepository.add(name);
  }

  /**
   * Renames an existing expense category.
   *
   * @param categoryId category identifier
   * @param name replacement category name
   * @throws BudgetPersistenceException if the category cannot be renamed
   */
  public void renameCategory(long categoryId, String name) {
    categoryRepository.rename(categoryId, name);
  }

  /**
   * Removes a category after moving its transactions to a replacement category.
   *
   * @param categoryId identifier of the category to remove
   * @param replacementCategoryId identifier that receives existing transactions
   * @throws BudgetPersistenceException if reassignment or removal fails
   */
  public void removeCategory(long categoryId, long replacementCategoryId) {
    categoryRepository.remove(categoryId, replacementCategoryId);
  }

  /**
   * Loads the global settings used for new monthly budgets.
   *
   * @return persisted budget settings
   * @throws BudgetPersistenceException if settings cannot be read
   */
  public BudgetSettings settings() {
    return settingsRepository.load();
  }

  /**
   * Saves the global settings used for future monthly budgets.
   *
   * @param value settings to save
   * @throws BudgetPersistenceException if settings cannot be saved
   */
  public void saveSettings(BudgetSettings value) {
    settingsRepository.save(value);
  }

  /**
   * Lists transactions in a selected month, newest first.
   *
   * @param month month to query
   * @return matching transactions
   * @throws BudgetPersistenceException if transactions cannot be read
   */
  public List<Transaction> transactions(YearMonth month) {
    return transactionRepository.findByMonth(month);
  }

  /**
   * Determines whether the database contains at least one transaction.
   *
   * @return whether any transaction has been stored
   * @throws BudgetPersistenceException if transactions cannot be queried
   */
  public boolean hasTransactions() {
    return transactionRepository.hasAny();
  }

  /**
   * Stores a transaction.
   *
   * @param transaction transaction to store
   * @return generated transaction identifier
   * @throws BudgetPersistenceException if the transaction cannot be stored
   */
  public long addTransaction(Transaction transaction) {
    return transactionRepository.add(transaction);
  }

  /**
   * Replaces a stored transaction.
   *
   * @param transaction transaction values including its existing identifier
   * @throws BudgetPersistenceException if the transaction cannot be updated
   */
  public void updateTransaction(Transaction transaction) {
    transactionRepository.update(transaction);
  }

  /**
   * Deletes a transaction.
   *
   * @param transactionId identifier of the transaction to delete
   * @throws BudgetPersistenceException if the transaction cannot be deleted
   */
  public void deleteTransaction(long transactionId) {
    transactionRepository.delete(transactionId);
  }

  /**
   * Lists monthly category budgets, creating snapshots when needed.
   *
   * @param month month to query
   * @return monthly budget snapshots
   * @throws BudgetPersistenceException if snapshots cannot be read or created
   */
  public List<MonthlyBudget> monthlyBudgets(YearMonth month) {
    return monthlyBudgetRepository.findOrCreate(month);
  }

  /**
   * Changes a category's base budget amount for a month.
   *
   * @param categoryId category identifier
   * @param month budget month
   * @param amount new base amount
   * @throws BudgetPersistenceException if the amount cannot be saved
   */
  public void setMonthlyBaseAmount(long categoryId, YearMonth month, BigDecimal amount) {
    monthlyBudgetRepository.setBaseAmount(categoryId, month, amount);
  }

  /**
   * Calculates recorded expense spending for one category in a month.
   *
   * @param categoryId category identifier
   * @param month month to total
   * @return expense total
   * @throws BudgetPersistenceException if the total cannot be calculated
   */
  public BigDecimal expenseTotal(long categoryId, YearMonth month) {
    return transactionRepository.expenseTotal(categoryId, month);
  }

  /**
   * Calculates income minus expenses for a calendar month.
   *
   * @param month month to total
   * @return selected-month net cash flow
   * @throws BudgetPersistenceException if the total cannot be calculated
   */
  public BigDecimal netCashFlow(YearMonth month) {
    return transactionRepository.netCashFlow(month);
  }

  /**
   * Closes the underlying SQLite connection.
   *
   * @throws BudgetPersistenceException if the connection cannot be closed
   */
  @Override
  public void close() {
    try {
      connection.close();
    } catch (SQLException exception) {
      throw PersistenceSupport.failure("close the database", exception);
    }
  }
}
