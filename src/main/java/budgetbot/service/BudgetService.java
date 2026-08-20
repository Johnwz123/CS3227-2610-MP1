package budgetbot.service;

import budgetbot.model.BudgetSettings;
import budgetbot.model.BudgetState;
import budgetbot.model.Category;
import budgetbot.model.CategorySummary;
import budgetbot.model.DashboardSnapshot;
import budgetbot.model.MonthlyBudget;
import budgetbot.model.Transaction;
import budgetbot.model.TransactionType;
import budgetbot.persistence.BudgetDatabase;
import budgetbot.persistence.BudgetPersistenceException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Applies BudgetBot's input validation and monthly calculations.
 *
 * <p>This service is the application-facing boundary for persistence operations: it validates input
 * before delegating to the database and derives dashboard values from stored data.
 */
public final class BudgetService {
  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
  private static final int MAXIMUM_MONEY_SCALE = 2;
  private static final int MINIMUM_CATEGORY_COUNT = 1;

  private final BudgetDatabase database;

  /**
   * Creates a service backed by the supplied database.
   *
   * @param database database used to store and retrieve budget data
   */
  public BudgetService(BudgetDatabase database) {
    this.database = database;
  }

  /**
   * Returns the user-maintained expense categories.
   *
   * @return categories available for expense transactions
   * @throws BudgetPersistenceException if the categories cannot be read
   */
  public List<Category> categories() {
    return database.categories();
  }

  /**
   * Returns transactions for a calendar month.
   *
   * @param month calendar month to query
   * @return transactions in {@code month}, newest first
   * @throws BudgetPersistenceException if the transactions cannot be read
   */
  public List<Transaction> transactions(YearMonth month) {
    return database.transactions(month);
  }

  /**
   * Returns the active global settings.
   *
   * @return settings used when a monthly budget snapshot is first created
   * @throws BudgetPersistenceException if the settings cannot be read
   */
  public BudgetSettings settings() {
    return database.settings();
  }

  /**
   * Saves settings that will be used for subsequently started months.
   *
   * @param rolloverEnabled whether unspent amounts should carry into the next month's snapshot
   * @param warningThreshold percentage at which a budget should enter the warning state
   * @throws IllegalArgumentException if {@code warningThreshold} is outside 1 through 99
   * @throws BudgetPersistenceException if the settings cannot be saved
   */
  public void saveSettings(boolean rolloverEnabled, int warningThreshold) {
    if (warningThreshold < 1 || warningThreshold > 99) {
      throw new IllegalArgumentException("Warning threshold must be between 1 and 99 percent.");
    }
    database.saveSettings(new BudgetSettings(rolloverEnabled, warningThreshold));
  }

  /**
   * Adds a non-empty category.
   *
   * @param name category name to add
   * @return the generated category identifier
   * @throws IllegalArgumentException if {@code name} is blank
   * @throws BudgetPersistenceException if the category cannot be added
   */
  public long addCategory(String name) {
    return database.addCategory(requiredName(name, "Category name"));
  }

  /**
   * Renames a category.
   *
   * @param categoryId identifier of the category to rename
   * @param name replacement category name
   * @throws IllegalArgumentException if {@code name} is blank
   * @throws BudgetPersistenceException if the category cannot be updated
   */
  public void renameCategory(long categoryId, String name) {
    database.renameCategory(categoryId, requiredName(name, "Category name"));
  }

  /**
   * Reassigns referenced expenses before removing a category.
   *
   * @param categoryId identifier of the category to remove
   * @param replacementCategoryId identifier of the category that receives its expenses
   * @throws IllegalArgumentException if removal would leave the budget without an expense category
   * @throws BudgetPersistenceException if the category cannot be reassigned and removed
   */
  public void removeCategory(long categoryId, long replacementCategoryId) {
    if (categories().size() <= MINIMUM_CATEGORY_COUNT) {
      throw new IllegalArgumentException("BudgetBot must keep at least one expense category.");
    }
    database.removeCategory(categoryId, replacementCategoryId);
  }

  /**
   * Adds a valid income or expense transaction.
   *
   * @param type whether the transaction is income or an expense
   * @param amount positive monetary amount with at most two decimal places
   * @param date date on which the transaction occurred
   * @param description optional user-entered description
   * @param categoryId required expense-category identifier, or {@code null} for income
   * @return the generated transaction identifier
   * @throws IllegalArgumentException if a required value is missing or the transaction is invalid
   * @throws BudgetPersistenceException if the transaction cannot be stored
   */
  public long addTransaction(
      TransactionType type,
      BigDecimal amount,
      java.time.LocalDate date,
      String description,
      Long categoryId) {
    Transaction transaction = validateTransaction(0, type, amount, date, description, categoryId);
    return database.addTransaction(transaction);
  }

  /**
   * Updates a valid income or expense transaction.
   *
   * @param transaction replacement transaction data, including the identifier to update
   * @throws IllegalArgumentException if a required value is missing or the transaction is invalid
   * @throws BudgetPersistenceException if the transaction cannot be updated
   */
  public void updateTransaction(Transaction transaction) {
    database.updateTransaction(
        validateTransaction(
            transaction.id(),
            transaction.type(),
            transaction.amount(),
            transaction.date(),
            transaction.description(),
            transaction.categoryId()));
  }

  /**
   * Deletes a transaction.
   *
   * @param transactionId identifier of the transaction to delete
   * @throws BudgetPersistenceException if the transaction cannot be deleted
   */
  public void deleteTransaction(long transactionId) {
    database.deleteTransaction(transactionId);
  }

  /**
   * Sets a non-negative base amount for a category in the selected month.
   *
   * @param categoryId identifier of the category to update
   * @param month calendar month of the budget snapshot
   * @param amount replacement base amount with at most two decimal places
   * @throws IllegalArgumentException if {@code amount} is negative, null, or has more than two
   *     decimal places
   * @throws BudgetPersistenceException if the budget snapshot cannot be updated
   */
  public void setMonthlyBudget(long categoryId, YearMonth month, BigDecimal amount) {
    if (amount == null || amount.signum() < 0) {
      throw new IllegalArgumentException("Monthly budget must be zero or greater.");
    }
    validateMoneyScale(amount, "Monthly budget");
    database.setMonthlyBaseAmount(categoryId, month, amount);
  }

  /**
   * Calculates all dashboard figures for the selected month.
   *
   * @param month calendar month represented by the dashboard
   * @return the overall balance, recent transactions, and per-category summaries for {@code month}
   * @throws BudgetPersistenceException if the required budget data cannot be read
   */
  public DashboardSnapshot dashboard(YearMonth month) {
    Map<Long, MonthlyBudget> budgets =
        database.monthlyBudgets(month).stream()
            .collect(Collectors.toMap(MonthlyBudget::categoryId, Function.identity()));
    List<CategorySummary> summaries =
        categories().stream()
            .map(category -> summary(category, budgets.get(category.id()), month))
            .sorted(
                Comparator.comparing(
                    summary -> summary.category().name(), String.CASE_INSENSITIVE_ORDER))
            .toList();
    return new DashboardSnapshot(
        month, database.overallBalance(), database.recentTransactions(8), summaries);
  }

  private CategorySummary summary(Category category, MonthlyBudget budget, YearMonth month) {
    BigDecimal spent = database.expenseTotal(category.id(), month);
    BigDecimal available = budget.availableAmount();
    return new CategorySummary(
        category,
        spent,
        available,
        available.subtract(spent),
        stateFor(spent, available, budget.warningThreshold()));
  }

  private BudgetState stateFor(BigDecimal spent, BigDecimal available, int warningThreshold) {
    if (spent.compareTo(available) >= 0 && (spent.signum() > 0 || available.signum() < 0)) {
      return BudgetState.OVER_BUDGET;
    }
    if (available.signum() <= 0) {
      return BudgetState.NORMAL;
    }
    BigDecimal spentPercentage =
        spent.multiply(ONE_HUNDRED).divide(available, 2, RoundingMode.HALF_UP);
    return spentPercentage.compareTo(BigDecimal.valueOf(warningThreshold)) >= 0
        ? BudgetState.WARNING
        : BudgetState.NORMAL;
  }

  private Transaction validateTransaction(
      long id,
      TransactionType type,
      BigDecimal amount,
      java.time.LocalDate date,
      String description,
      Long categoryId) {
    if (type == null) {
      throw new IllegalArgumentException("Select income or expense.");
    }
    if (amount == null || amount.signum() <= 0) {
      throw new IllegalArgumentException(
          "Amount must be greater than zero with at most two decimal places.");
    }
    validateMoneyScale(amount, "Amount");
    if (date == null) {
      throw new IllegalArgumentException("Select a transaction date.");
    }
    if (type == TransactionType.EXPENSE && categoryId == null) {
      throw new IllegalArgumentException("Select a category for an expense.");
    }
    return new Transaction(
        id,
        type,
        amount,
        date,
        description == null ? "" : description.trim(),
        type == TransactionType.INCOME ? null : categoryId);
  }

  private String requiredName(String value, String label) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(label + " cannot be empty.");
    }
    return value.trim();
  }

  private void validateMoneyScale(BigDecimal amount, String label) {
    if (amount.scale() > MAXIMUM_MONEY_SCALE) {
      throw new IllegalArgumentException(label + " can have at most two decimal places.");
    }
  }
}
