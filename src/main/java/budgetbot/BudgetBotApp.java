package budgetbot;

import budgetbot.persistence.BudgetDatabase;
import budgetbot.service.BudgetService;
import budgetbot.ui.BudgetBotWindow;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX application entry point for BudgetBot.
 *
 * <p>The application stores its data in the current user's BudgetBot directory.
 */
public final class BudgetBotApp extends Application {
  private BudgetDatabase database;

  /** Creates an application instance for the JavaFX runtime. */
  public BudgetBotApp() {
    super();
  }

  /**
   * Opens the local database and shows the main BudgetBot window.
   *
   * @param stage JavaFX stage supplied by the runtime
   * @throws budgetbot.persistence.BudgetPersistenceException if the local database cannot be opened
   */
  @Override
  public void start(Stage stage) {
    database = new BudgetDatabase(DatabasePaths.defaultDatabasePath());
    BudgetService service = new BudgetService(database);
    new BudgetBotWindow(stage, service).show();
  }

  /**
   * Closes the local database before the JavaFX application stops.
   *
   * @throws budgetbot.persistence.BudgetPersistenceException if the database cannot be closed
   */
  @Override
  public void stop() {
    if (database != null) {
      database.close();
    }
  }

  /**
   * Launches BudgetBot.
   *
   * @param args command-line arguments forwarded to the JavaFX runtime
   */
  public static void main(String[] args) {
    launch(args);
  }
}
