package budgetbot.tools;

import budgetbot.DatabasePaths;
import budgetbot.model.Category;
import budgetbot.model.TransactionType;
import budgetbot.persistence.BudgetDatabase;
import budgetbot.persistence.BudgetPersistenceException;
import budgetbot.service.BudgetService;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/** Resets BudgetBot databases and creates a deterministic current-month demo scenario. */
public final class DatabaseTool {
  private static final String RESET_COMMAND = "reset";
  private static final String SEED_COMMAND = "seed";
  private static final String FORCE = "--force";
  private static final String DATABASE_OPTION = "--database";
  private static final String HOUSING = "Housing & Utilities";
  private static final String GROCERIES = "Groceries";
  private static final String DINING = "Dining";
  private static final String TRANSPORT = "Transport";
  private static final String ENTERTAINMENT = "Entertainment";

  private DatabaseTool() {
    throw new AssertionError("Utility class");
  }

  /**
   * Runs the database tool from the command line.
   *
   * @param args command arguments
   */
  public static void main(String[] args) {
    int exitCode = run(args, System.out, System.err);
    if (exitCode != 0) {
      System.exit(exitCode);
    }
  }

  /**
   * Executes a database-tool command.
   *
   * @param args command arguments
   * @param output destination for normal output
   * @param error destination for error output
   * @return zero when the operation succeeds, otherwise a non-zero exit code
   */
  public static int run(String[] args, PrintStream output, PrintStream error) {
    try {
      Command command = Command.parse(args);
      Path databasePath = DatabasePaths.resolve(command.databasePath());
      output.println("Database target: " + databasePath);
      if (RESET_COMMAND.equals(command.operation())) {
        if (!command.force()) {
          error.println("Reset requires --force. No database files were changed.");
          return 2;
        }
        reset(databasePath);
        output.println("Database reset and initialized with default settings and categories.");
        return 0;
      }
      seed(databasePath);
      output.println("Demo data created for " + YearMonth.now() + ".");
      return 0;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      error.println(exception.getMessage());
      return 2;
    } catch (IOException | BudgetPersistenceException exception) {
      error.println("Database operation failed: " + exception.getMessage());
      return 1;
    }
  }

  private static void reset(Path databasePath) throws IOException {
    deleteIfPresent(databasePath);
    deleteIfPresent(sidecarPath(databasePath, "-wal"));
    deleteIfPresent(sidecarPath(databasePath, "-shm"));
    deleteIfPresent(sidecarPath(databasePath, "-journal"));
    try (BudgetDatabase database = new BudgetDatabase(databasePath)) {
      database.categories();
    }
  }

  private static void seed(Path databasePath) {
    try (BudgetDatabase database = new BudgetDatabase(databasePath)) {
      if (database.hasTransactions()) {
        throw new IllegalStateException(
            "The target database already contains transactions. Run reset before seeding again.");
      }
      BudgetService service = new BudgetService(database);
      YearMonth month = YearMonth.now();
      long housing = categoryId(service.categories(), HOUSING);
      long groceries = categoryId(service.categories(), GROCERIES);
      long dining = categoryId(service.categories(), DINING);
      long transport = categoryId(service.categories(), TRANSPORT);
      long entertainment = categoryId(service.categories(), ENTERTAINMENT);

      service.setMonthlyBudget(housing, month, money("1600.00"));
      service.setMonthlyBudget(groceries, month, money("400.00"));
      service.setMonthlyBudget(dining, month, money("180.00"));
      service.setMonthlyBudget(transport, month, money("180.00"));
      service.setMonthlyBudget(entertainment, month, money("120.00"));

      service.addTransaction(
          TransactionType.INCOME, money("4500.00"), month.atDay(1), "Salary", null);
      service.addTransaction(
          TransactionType.INCOME, money("250.00"), month.atDay(8), "Freelance work", null);
      service.addTransaction(
          TransactionType.EXPENSE, money("1500.00"), month.atDay(2), "Rent", housing);
      service.addTransaction(
          TransactionType.EXPENSE, money("250.00"), month.atDay(5), "Weekly groceries", groceries);
      service.addTransaction(
          TransactionType.EXPENSE, money("80.00"), month.atDay(10), "Lunches with friends", dining);
      service.addTransaction(
          TransactionType.EXPENSE, money("120.00"), month.atDay(12), "Transit pass", transport);
      service.addTransaction(
          TransactionType.EXPENSE, money("60.00"), month.atDay(15), "Cinema", entertainment);
    }
  }

  private static long categoryId(List<Category> categories, String name) {
    return categories.stream()
        .filter(category -> name.equals(category.name()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Missing default category: " + name))
        .id();
  }

  private static BigDecimal money(String value) {
    return new BigDecimal(value);
  }

  private static Path sidecarPath(Path databasePath, String suffix) {
    return databasePath.resolveSibling(databasePath.getFileName() + suffix);
  }

  private static void deleteIfPresent(Path path) throws IOException {
    Files.deleteIfExists(path);
  }

  private record Command(String operation, Path databasePath, boolean force) {
    private static Command parse(String... arguments) {
      if (arguments.length == 0 || arguments.length > 4) {
        throw usage();
      }
      String operation = arguments[0];
      if (!RESET_COMMAND.equals(operation) && !SEED_COMMAND.equals(operation)) {
        throw usage();
      }
      Path databasePath = null;
      boolean force = false;
      List<String> options = Arrays.asList(arguments).subList(1, arguments.length);
      for (Iterator<String> iterator = options.iterator(); iterator.hasNext(); ) {
        String argument = iterator.next();
        if (FORCE.equals(argument)) {
          force = true;
        } else if (DATABASE_OPTION.equals(argument) && iterator.hasNext()) {
          databasePath = Path.of(iterator.next());
        } else {
          throw usage();
        }
      }
      if (SEED_COMMAND.equals(operation) && force) {
        throw new IllegalArgumentException("The seed operation does not accept --force.");
      }
      return new Command(operation, databasePath, force);
    }

    private static IllegalArgumentException usage() {
      return new IllegalArgumentException(
          "Usage: reset --force [--database <path>] | seed [--database <path>]");
    }
  }
}
