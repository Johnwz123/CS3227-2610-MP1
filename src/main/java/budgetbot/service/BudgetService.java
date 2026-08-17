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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Applies BudgetBot's validation and monthly calculations. */
public final class BudgetService {
  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
  private static final int MAXIMUM_MONEY_SCALE = 2;
  private static final int MINIMUM_CATEGORY_COUNT = 1;

  private final BudgetDatabase database;

  /** Creates a service backed by the supplied database. */
  public BudgetService(BudgetDatabase database) {
    this.database = database;
  }

  /** Returns the user-maintained expense categories. */
  public List<Category> categories() {
    return database.categories();
  }

  /** Returns a transaction list for a calendar month. */
  public List<Transaction> transactions(YearMonth month) {
    return database.transactions(month);
  }

  /** Returns the active global settings. */
  public BudgetSettings settings() {
    return database.settings();
  }

  /** Saves settings that will be used for subsequently started months. */
  public void saveSettings(boolean rolloverEnabled, int warningThreshold) {
    if (warningThreshold < 1 || warningThreshold > 99) {
      throw new IllegalArgumentException("Warning threshold must be between 1 and 99 percent.");
    }
    database.saveSettings(new BudgetSettings(rolloverEnabled, warningThreshold));
  }

  /** Adds a non-empty category. */
  public long addCategory(String name) {
    return database.addCategory(requiredName(name, "Category name"));
  }

  /** Renames a category. */
  public void renameCategory(long categoryId, String name) {
    database.renameCategory(categoryId, requiredName(name, "Category name"));
  }

  /** Reassigns referenced expenses before removing a category. */
  public void removeCategory(long categoryId, long replacementCategoryId) {
    if (categories().size() <= MINIMUM_CATEGORY_COUNT) {
      throw new IllegalArgumentException("BudgetBot must keep at least one expense category.");
    }
    database.removeCategory(categoryId, replacementCategoryId);
  }

  /** Adds a valid income or expense transaction. */
  public long addTransaction(
      TransactionType type,
      BigDecimal amount,
      java.time.LocalDate date,
      String description,
      Long categoryId) {
    Transaction transaction = validateTransaction(0, type, amount, date, description, categoryId);
    return database.addTransaction(transaction);
  }

  /** Updates a valid income or expense transaction. */
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

  /** Deletes a transaction. */
  public void deleteTransaction(long transactionId) {
    database.deleteTransaction(transactionId);
  }

  /** Sets a non-negative base amount for a category in the selected month. */
  public void setMonthlyBudget(long categoryId, YearMonth month, BigDecimal amount) {
    if (amount == null || amount.signum() < 0) {
      throw new IllegalArgumentException("Monthly budget must be zero or greater.");
    }
    validateMoneyScale(amount, "Monthly budget");
    database.setMonthlyBaseAmount(categoryId, month, amount);
  }

  /** Calculates all dashboard figures for the selected month. */
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
