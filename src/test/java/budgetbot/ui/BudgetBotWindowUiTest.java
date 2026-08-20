package budgetbot.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import budgetbot.model.Category;
import budgetbot.model.TransactionType;
import budgetbot.persistence.BudgetDatabase;
import budgetbot.service.BudgetService;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.YearMonth;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

@ExtendWith(ApplicationExtension.class)
class BudgetBotWindowUiTest {
  @TempDir Path temporaryDirectory;

  private BudgetDatabase database;
  private BudgetService service;
  private Category groceries;

  @Start
  private void start(Stage stage) {
    database = new BudgetDatabase(temporaryDirectory.resolve("budgetbot.db"));
    service = new BudgetService(database);
    groceries = category("Groceries");
    YearMonth month = YearMonth.now();
    service.setMonthlyBudget(groceries.id(), month, new BigDecimal("100"));
    service.addTransaction(
        TransactionType.EXPENSE, new BigDecimal("25"), month.atDay(1), "Market", groceries.id());
    new BudgetBotWindow(stage, service).show();
  }

  @AfterEach
  void closeDatabase() {
    database.close();
  }

  @Test
  void navigatesViewsAndChangesTheSelectedMonth(FxRobot robot) {
    robot.clickOn("Transactions");
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(robot.lookup("Add transaction").tryQuery().isPresent());
    robot.clickOn(">");
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(robot.lookup("Add transaction").tryQuery().isPresent());
    robot.clickOn("Budgets");
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(robot.lookup("Add").tryQuery().isPresent());
    robot.clickOn("Settings");
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(robot.lookup("Save settings").tryQuery().isPresent());
    robot.clickOn("Dashboard");
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(robot.lookup(".balance").tryQuery().isPresent());
  }

  @Test
  void addsEditsAndDeletesTransactionsThroughTheUi(FxRobot robot) {
    robot.clickOn("Transactions");
    robot.clickOn("Add transaction");
    DialogPane dialog = dialog(robot);
    GridPane fields = (GridPane) ((VBox) dialog.getContent()).getChildren().getFirst();
    TextField amount = (TextField) nodeAt(fields, 1, 1);
    DatePicker date = (DatePicker) nodeAt(fields, 1, 2);
    TextField description = (TextField) nodeAt(fields, 1, 3);
    @SuppressWarnings("unchecked")
    ComboBox<Category> category = (ComboBox<Category>) nodeAt(fields, 1, 4);
    TextField newDescription = description;
    robot.interact(
        () -> {
          amount.setText("12.50");
          date.setValue(YearMonth.now().atDay(2));
          newDescription.setText("Coffee beans");
          category.setValue(groceries);
        });
    robot.clickOn(button(dialog, "Save"));
    assertTrue(
        service.transactions(YearMonth.now()).stream()
            .anyMatch(item -> item.description().equals("Coffee beans")));

    robot.clickOn("Edit");
    dialog = dialog(robot);
    fields = (GridPane) ((VBox) dialog.getContent()).getChildren().getFirst();
    description = (TextField) nodeAt(fields, 1, 3);
    TextField editDescription = description;
    robot.interact(() -> editDescription.setText("Edited market"));
    robot.clickOn(button(dialog, "Save"));
    assertTrue(
        service.transactions(YearMonth.now()).stream()
            .anyMatch(item -> item.description().equals("Edited market")));

    robot.clickOn("Delete");
    robot.clickOn("OK");
    assertEquals(1, service.transactions(YearMonth.now()).size());
  }

  @Test
  void validatesTransactionFormWithoutClosingTheDialog(FxRobot robot) {
    robot.clickOn("Transactions");
    robot.clickOn("Add transaction");
    robot.clickOn(button(dialog(robot), "Save"));
    assertTrue(
        robot
            .lookup("Amount must be a number with at most two decimal places.")
            .tryQuery()
            .isPresent());
    assertTrue(dialog(robot).isVisible());
    robot.clickOn(button(dialog(robot), "Cancel"));
  }

  @Test
  void managesCategoriesBudgetsAndSettingsThroughTheUi(FxRobot robot) {
    robot.clickOn("Budgets");
    robot.clickOn("Add");
    DialogPane dialog = dialog(robot);
    TextField input = (TextField) ((VBox) dialog.getContent()).getChildren().get(1);
    TextField categoryInput = input;
    robot.interact(() -> categoryInput.setText("Subscriptions"));
    robot.clickOn(button(dialog, "Save"));
    assertTrue(
        service.categories().stream()
            .anyMatch(category -> category.name().equals("Subscriptions")));

    robot.clickOn("Set budget");
    dialog = dialog(robot);
    input = (TextField) ((VBox) dialog.getContent()).getChildren().get(1);
    TextField budgetInput = input;
    robot.interact(() -> budgetInput.setText("45"));
    robot.clickOn(button(dialog, "Save"));
    assertEquals(
        new BigDecimal("45"),
        service.dashboard(YearMonth.now()).categorySummaries().getFirst().available());

    robot.clickOn("Settings");
    robot.clickOn("Enable rollover for the whole budget");
    robot.clickOn("Save settings");
    robot.clickOn("OK");
    assertTrue(service.settings().rolloverEnabled());
  }

  private Category category(String name) {
    return service.categories().stream()
        .filter(category -> category.name().equals(name))
        .findFirst()
        .orElseThrow();
  }

  private static DialogPane dialog(FxRobot robot) {
    return robot.lookup(".dialog-pane").query();
  }

  private static Button button(DialogPane dialog, String text) {
    return dialog.lookupAll(".button").stream()
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(button -> text.equals(button.getText()))
        .findFirst()
        .orElseThrow();
  }

  private static Node nodeAt(GridPane grid, int column, int row) {
    return grid.getChildren().stream()
        .filter(
            node ->
                GridPane.getColumnIndex(node) != null && GridPane.getColumnIndex(node) == column)
        .filter(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) == row)
        .findFirst()
        .orElseThrow();
  }
}
