package budgetbot.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import budgetbot.DatabasePaths;
import budgetbot.model.BudgetSettings;
import budgetbot.model.TransactionType;
import budgetbot.persistence.BudgetDatabase;
import budgetbot.service.BudgetService;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseToolTest {
  @TempDir Path temporaryDirectory;

  @Test
  void resolvesExplicitPathsToNormalizedAbsolutePaths() {
    Path requested = temporaryDirectory.resolve("nested").resolve("..").resolve("demo.db");

    assertEquals(
        temporaryDirectory.resolve("demo.db").toAbsolutePath(), DatabasePaths.resolve(requested));
  }

  @Test
  void rejectsAnUnconfirmedResetWithoutChangingTheDatabase() {
    Path databasePath = temporaryDirectory.resolve("budgetbot.db");
    addIncome(databasePath);
    ByteArrayOutputStream error = new ByteArrayOutputStream();

    int exitCode =
        DatabaseTool.run(
            new String[] {"reset", "--database", databasePath.toString()},
            output(),
            errorStream(error));

    assertEquals(2, exitCode);
    assertTrue(error.toString().contains("requires --force"));
    try (BudgetDatabase database = new BudgetDatabase(databasePath)) {
      assertTrue(database.hasTransactions());
    }
  }

  @Test
  void resetRemovesSidecarsAndRecreatesFirstStartDefaults() throws Exception {
    Path databasePath = temporaryDirectory.resolve("budgetbot.db");
    addIncome(databasePath);
    Path writeAheadLog = databasePath.resolveSibling("budgetbot.db-wal");
    Path sharedMemory = databasePath.resolveSibling("budgetbot.db-shm");
    Path journal = databasePath.resolveSibling("budgetbot.db-journal");
    Files.writeString(writeAheadLog, "stale");
    Files.writeString(sharedMemory, "stale");
    Files.writeString(journal, "stale");

    int exitCode =
        DatabaseTool.run(
            new String[] {"reset", "--force", "--database", databasePath.toString()},
            output(),
            error());

    assertEquals(0, exitCode);
    assertFalse(Files.exists(writeAheadLog));
    assertFalse(Files.exists(sharedMemory));
    assertFalse(Files.exists(journal));
    try (BudgetDatabase database = new BudgetDatabase(databasePath)) {
      assertFalse(database.hasTransactions());
      assertEquals(9, database.categories().size());
      assertEquals(new BudgetSettings(80), database.settings());
    }
  }

  @Test
  void seedsDeterministicCurrentMonthDataAndPreventsDuplicates() {
    Path databasePath = temporaryDirectory.resolve("budgetbot.db");

    assertEquals(
        0,
        DatabaseTool.run(
            new String[] {"seed", "--database", databasePath.toString()}, output(), error()));
    try (BudgetDatabase database = new BudgetDatabase(databasePath)) {
      BudgetService service = new BudgetService(database);
      assertEquals(7, service.transactions(YearMonth.now()).size());
      assertEquals(new BigDecimal("2740.00"), service.dashboard(YearMonth.now()).netCashFlow());
      assertEquals(
          new BigDecimal("400.00"),
          service.dashboard(YearMonth.now()).categorySummaries().stream()
              .filter(summary -> summary.category().name().equals("Groceries"))
              .findFirst()
              .orElseThrow()
              .available());
    }

    ByteArrayOutputStream error = new ByteArrayOutputStream();
    int exitCode =
        DatabaseTool.run(
            new String[] {"seed", "--database", databasePath.toString()},
            output(),
            errorStream(error));

    assertEquals(2, exitCode);
    assertTrue(error.toString().contains("already contains transactions"));
    try (BudgetDatabase database = new BudgetDatabase(databasePath)) {
      assertEquals(7, database.transactions(YearMonth.now()).size());
    }
  }

  private void addIncome(Path databasePath) {
    try (BudgetDatabase database = new BudgetDatabase(databasePath)) {
      BudgetService service = new BudgetService(database);
      service.addTransaction(
          TransactionType.INCOME, BigDecimal.ONE, YearMonth.now().atDay(1), "Pay", null);
    }
  }

  private PrintStream output() {
    return new PrintStream(new ByteArrayOutputStream());
  }

  private PrintStream error() {
    return new PrintStream(new ByteArrayOutputStream());
  }

  private PrintStream errorStream(ByteArrayOutputStream output) {
    return new PrintStream(output);
  }
}
