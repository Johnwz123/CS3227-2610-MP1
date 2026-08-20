package budgetbot;

import java.nio.file.Path;

/** Resolves the locations used for BudgetBot SQLite databases. */
public final class DatabasePaths {
  private static final String DATABASE_DIRECTORY = ".budgetbot";
  private static final String DATABASE_FILE_NAME = "budgetbot.db";

  private DatabasePaths() {
    throw new AssertionError("Utility class");
  }

  /**
   * Returns BudgetBot's standard per-user database location.
   *
   * @return the default SQLite database path
   */
  public static Path defaultDatabasePath() {
    return Path.of(System.getProperty("user.home"), DATABASE_DIRECTORY, DATABASE_FILE_NAME);
  }

  /**
   * Resolves an optional database path to one normalized absolute path.
   *
   * @param requestedPath optional caller-provided database path
   * @return the normalized selected database path
   */
  public static Path resolve(Path requestedPath) {
    Path path = requestedPath == null ? defaultDatabasePath() : requestedPath;
    return path.toAbsolutePath().normalize();
  }
}
