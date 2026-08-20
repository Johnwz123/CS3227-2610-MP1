package budgetbot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BudgetBotAppTest {
  @Test
  void usesAStableDatabasePathInsideTheUserHomeDirectory() {
    Path expected = Path.of(System.getProperty("user.home"), ".budgetbot", "budgetbot.db");

    assertEquals(expected, DatabasePaths.defaultDatabasePath());
  }
}
