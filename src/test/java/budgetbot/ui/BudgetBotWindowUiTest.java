package budgetbot.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import budgetbot.model.Category;
import budgetbot.model.TransactionType;
import budgetbot.persistence.BudgetDatabase;
import budgetbot.service.BudgetService;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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
  void navigatesViewsAndChangesTheSelectedMonth(FxRobot robot) throws Exception {
    click(robot, "Transactions");
    WaitForAsyncUtils.waitFor(
        5, TimeUnit.SECONDS, () -> robot.lookup("Add transaction").tryQuery().isPresent());
    assertTrue(robot.lookup("Add transaction").tryQuery().isPresent());
    click(robot, ">");
    WaitForAsyncUtils.waitFor(
        5, TimeUnit.SECONDS, () -> robot.lookup("Add transaction").tryQuery().isPresent());
    assertTrue(robot.lookup("Add transaction").tryQuery().isPresent());
    click(robot, "Budgets");
    WaitForAsyncUtils.waitFor(
        5, TimeUnit.SECONDS, () -> robot.lookup("Add").tryQuery().isPresent());
    assertTrue(robot.lookup("Add").tryQuery().isPresent());
    click(robot, "Settings");
    assertTrue(robot.lookup("Save settings").tryQuery().isPresent());
    click(robot, "Dashboard");
    Label netCashFlow = robot.lookup(".balance").queryAs(Label.class);
    assertTrue(netCashFlow.getText().startsWith("Net cash flow: "));
    assertFalse(robot.lookup("Overall balance:").tryQuery().isPresent());
    assertFalse(robot.lookup("Recent activity").tryQuery().isPresent());
  }

  @Test
  void addsEditsAndDeletesTransactionsThroughTheUi(FxRobot robot) throws TimeoutException {
    click(robot, "Transactions");
    click(robot, "Add transaction");
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
    click(robot, button(dialog, "Save"));
    assertTrue(
        service.transactions(YearMonth.now()).stream()
            .anyMatch(item -> item.description().equals("Coffee beans")));

    click(robot, "Edit");
    dialog = dialog(robot);
    fields = (GridPane) ((VBox) dialog.getContent()).getChildren().getFirst();
    description = (TextField) nodeAt(fields, 1, 3);
    TextField editDescription = description;
    robot.interact(() -> editDescription.setText("Edited market"));
    click(robot, button(dialog, "Save"));
    assertTrue(
        service.transactions(YearMonth.now()).stream()
            .anyMatch(item -> item.description().equals("Edited market")));

    click(robot, "Delete");
    click(robot, "OK");
    assertEquals(1, service.transactions(YearMonth.now()).size());
  }

  @Test
  void validatesTransactionFormWithoutClosingTheDialog(FxRobot robot) throws TimeoutException {
    click(robot, "Transactions");
    click(robot, "Add transaction");
    click(robot, button(dialog(robot), "Save"));
    Label validation = robot.lookup("#transaction-validation-message").queryAs(Label.class);
    assertEquals("Amount must be a number with at most two decimal places.", validation.getText());
    assertTrue(validation.isWrapText());
    assertTrue(validation.getHeight() > validation.getFont().getSize());
    assertTrue(dialog(robot).isVisible());
    click(robot, button(dialog(robot), "Cancel"));
  }

  @Test
  void appliesClearsAndValidatesTransactionFilters(FxRobot robot) {
    YearMonth month = YearMonth.now();
    service.addTransaction(
        TransactionType.EXPENSE,
        new BigDecimal("12"),
        month.atDay(2),
        "Coffee market",
        groceries.id());
    service.addTransaction(
        TransactionType.INCOME, new BigDecimal("100"), month.atDay(3), "Pay", null);

    click(robot, "Transactions");
    TextField search = robot.lookup("#transaction-search").queryAs(TextField.class);
    TextField minimum = robot.lookup("#transaction-minimum-amount").queryAs(TextField.class);
    TextField maximum = robot.lookup("#transaction-maximum-amount").queryAs(TextField.class);
    robot.interact(
        () -> {
          search.setText("MARKET");
          minimum.setText("20");
          maximum.setText("25");
        });
    click(robot, robot.lookup("#apply-transaction-filters").queryAs(Button.class));

    TableView<?> table = robot.lookup(".data-table").queryAs(TableView.class);
    assertEquals(1, table.getItems().size());
    assertEquals(
        "1 transaction", robot.lookup("#transaction-result-count").queryAs(Label.class).getText());

    click(robot, robot.lookup("#clear-transaction-filters").queryAs(Button.class));
    assertEquals(3, table.getItems().size());
    assertEquals(
        "3 transactions", robot.lookup("#transaction-result-count").queryAs(Label.class).getText());

    DatePicker start = robot.lookup("#transaction-start-date").queryAs(DatePicker.class);
    DatePicker end = robot.lookup("#transaction-end-date").queryAs(DatePicker.class);
    robot.interact(
        () -> {
          start.setValue(month.atDay(3));
          end.setValue(month.atDay(1));
        });
    click(robot, robot.lookup("#apply-transaction-filters").queryAs(Button.class));
    assertEquals(3, table.getItems().size());
    assertEquals(
        "Start date cannot be after end date.",
        robot.lookup("#transaction-filter-error").queryAs(Label.class).getText());
  }

  @Test
  void refreshesActiveTransactionFilterAfterAnEdit(FxRobot robot) throws TimeoutException {
    click(robot, "Transactions");
    TextField search = robot.lookup("#transaction-search").queryAs(TextField.class);
    robot.interact(() -> search.setText("Market"));
    click(robot, robot.lookup("#apply-transaction-filters").queryAs(Button.class));
    assertEquals(1, robot.lookup(".data-table").queryAs(TableView.class).getItems().size());

    click(robot, "Edit");
    GridPane fields = (GridPane) ((VBox) dialog(robot).getContent()).getChildren().getFirst();
    TextField description = (TextField) nodeAt(fields, 1, 3);
    robot.interact(() -> description.setText("Groceries"));
    click(robot, button(dialog(robot), "Save"));

    assertEquals(0, robot.lookup(".data-table").queryAs(TableView.class).getItems().size());
    assertEquals(
        "0 transactions", robot.lookup("#transaction-result-count").queryAs(Label.class).getText());
  }

  @Test
  void groupsAndCoordinatesTransactionTypeAndCategoryFilters(FxRobot robot) {
    click(robot, "Transactions");
    FlowPane criteria = robot.lookup("#transaction-filter-criteria").queryAs(FlowPane.class);
    HBox primary = robot.lookup("#transaction-filter-primary-group").queryAs(HBox.class);
    HBox secondary = robot.lookup("#transaction-filter-secondary-group").queryAs(HBox.class);
    @SuppressWarnings("unchecked")
    ComboBox<TransactionType> type =
        robot.lookup("#transaction-type-filter").queryAs(ComboBox.class);
    ComboBox<?> category = robot.lookup("#transaction-category-filter").queryAs(ComboBox.class);

    assertEquals(2, criteria.getChildren().size());
    assertEquals(primary, criteria.getChildren().getFirst());
    assertEquals(secondary, criteria.getChildren().get(1));
    assertEquals(type, secondary.getChildren().getFirst());
    assertEquals(category, secondary.getChildren().get(1));
    assertTrue(type.getMinWidth() >= 165);

    robot.interact(() -> type.setValue(TransactionType.INCOME));
    assertNull(category.getValue());
    assertFalse(category.isVisible());
    assertFalse(category.isManaged());

    robot.interact(() -> category.getSelectionModel().select(0));
    assertEquals(TransactionType.EXPENSE, type.getValue());
    assertTrue(category.isVisible());
    assertTrue(category.isManaged());
  }

  @Test
  void managesCategoriesBudgetsAndSettingsThroughTheUi(FxRobot robot) throws TimeoutException {
    click(robot, "Budgets");
    click(robot, "Add");
    DialogPane dialog = dialog(robot);
    TextField input = (TextField) ((VBox) dialog.getContent()).getChildren().get(1);
    TextField categoryInput = input;
    robot.interact(() -> categoryInput.setText("Subscriptions"));
    click(robot, button(dialog, "Save"));
    assertTrue(
        service.categories().stream()
            .anyMatch(category -> category.name().equals("Subscriptions")));

    click(robot, "Set budget");
    dialog = dialog(robot);
    input = (TextField) ((VBox) dialog.getContent()).getChildren().get(1);
    TextField budgetInput = input;
    robot.interact(() -> budgetInput.setText("45"));
    click(robot, button(dialog, "Save"));
    assertEquals(
        new BigDecimal("45"),
        service.dashboard(YearMonth.now()).categorySummaries().getFirst().available());

    click(robot, "Settings");
    assertFalse(robot.lookup("Enable rollover for the whole budget").tryQuery().isPresent());
    click(robot, "Save settings");
    click(robot, "OK");
    assertEquals(80, service.settings().warningThreshold());
  }

  private Category category(String name) {
    return service.categories().stream()
        .filter(category -> category.name().equals(name))
        .findFirst()
        .orElseThrow();
  }

  private static DialogPane dialog(FxRobot robot) throws TimeoutException {
    WaitForAsyncUtils.waitForFxEvents();
    WaitForAsyncUtils.waitFor(
        5, TimeUnit.SECONDS, () -> robot.lookup(".dialog-pane").tryQuery().isPresent());
    return robot.lookup(".dialog-pane").query();
  }

  private static void click(FxRobot robot, String text) {
    robot.clickOn(text);
    WaitForAsyncUtils.waitForFxEvents();
  }

  private static void click(FxRobot robot, Node node) {
    robot.clickOn(node);
    WaitForAsyncUtils.waitForFxEvents();
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
