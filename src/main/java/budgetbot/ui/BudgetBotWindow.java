package budgetbot.ui;

import budgetbot.service.BudgetService;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Builds the primary window shell and its application navigation. */
public final class BudgetBotWindow {
  private final Stage stage;
  private final BorderPane layout = new BorderPane();
  private final ViewCoordinator coordinator;

  /**
   * Creates the primary window.
   *
   * @param stage stage that displays the BudgetBot scene
   * @param service service that supplies validated budget data and commands
   */
  public BudgetBotWindow(Stage stage, BudgetService service) {
    this.stage = stage;
    coordinator = new ViewCoordinator(layout, service);
  }

  /** Shows the main window with the dashboard selected. */
  public void show() {
    layout.setLeft(navigation());
    coordinator.showDashboard();
    Scene scene = new Scene(layout, 1120, 720);
    scene
        .getStylesheets()
        .add(BudgetBotWindow.class.getResource("/budgetbot/budgetbot.css").toExternalForm());
    stage.setTitle("BudgetBot");
    stage.setMinWidth(840);
    stage.setMinHeight(540);
    stage.setScene(scene);
    stage.show();
  }

  private VBox navigation() {
    Label title = new Label("BudgetBot");
    title.getStyleClass().add("title");
    Button dashboard = new Button("Dashboard");
    Button transactions = new Button("Transactions");
    Button budgets = new Button("Budgets");
    Button settings = new Button("Settings");
    dashboard.setOnAction(event -> coordinator.showDashboard());
    transactions.setOnAction(event -> coordinator.showTransactions());
    budgets.setOnAction(event -> coordinator.showBudgets());
    settings.setOnAction(event -> coordinator.showSettings());
    VBox navigation = new VBox(12, title, dashboard, transactions, budgets, settings);
    navigation.setPadding(new javafx.geometry.Insets(24));
    navigation.setMinWidth(180);
    navigation.getStyleClass().add("navigation");
    return navigation;
  }
}
