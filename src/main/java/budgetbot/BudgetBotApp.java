package budgetbot;

import budgetbot.persistence.BudgetDatabase;
import budgetbot.service.BudgetService;
import java.nio.file.Path;
import javafx.application.Application;
import javafx.stage.Stage;

/** Starts the BudgetBot desktop application. */
public final class BudgetBotApp extends Application {
  private BudgetDatabase database;

  @Override
  public void start(Stage stage) {
    database = new BudgetDatabase(defaultDatabasePath());
    BudgetService service = new BudgetService(database);
    new BudgetBotWindow(stage, service).show();
  }

  @Override
  public void stop() {
    if (database != null) {
      database.close();
    }
  }

  /** Launches BudgetBot. */
  public static void main(String[] args) {
    launch(args);
  }

  private Path defaultDatabasePath() {
    return Path.of(System.getProperty("user.home"), ".budgetbot", "budgetbot.db");
  }
}
