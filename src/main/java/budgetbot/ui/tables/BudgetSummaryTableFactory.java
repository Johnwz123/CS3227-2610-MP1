package budgetbot.ui.tables;

import budgetbot.model.Category;
import budgetbot.model.CategorySummary;
import budgetbot.ui.MoneyInput;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

/** Builds category budget summary tables, state styling, and category row actions. */
public final class BudgetSummaryTableFactory {
  private static final String TABLE_ACTION_STYLE_CLASS = "table-action";
  private static final String NORMAL_REMAINING_STYLE_CLASS = "normal-remaining";
  private static final String WARNING_REMAINING_STYLE_CLASS = "warning-remaining";
  private static final String OVER_BUDGET_REMAINING_STYLE_CLASS = "over-budget-remaining";

  /**
   * Creates a category summary table without row actions.
   *
   * @param summaries category summaries to display
   * @return configured table
   */
  public TableView<CategorySummary> create(List<CategorySummary> summaries) {
    TableView<CategorySummary> table =
        new TableView<>(FXCollections.observableArrayList(summaries));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
    table.setPlaceholder(new Label("No budget categories yet."));
    table.getStyleClass().add("data-table");
    table.getColumns().add(textColumn("Category", summary -> summary.category().name(), 180));
    table
        .getColumns()
        .add(textColumn("Available", summary -> MoneyInput.format(summary.available()), 130));
    table.getColumns().add(textColumn("Spent", summary -> MoneyInput.format(summary.spent()), 130));
    table.getColumns().add(remainingColumn());
    return table;
  }

  /**
   * Adds budget-management actions to a category summary table.
   *
   * @param table table to extend
   * @param setBudget action for the selected category
   * @param rename action for the selected category
   * @param remove action for the selected category
   */
  public void addActions(
      TableView<CategorySummary> table,
      Consumer<Category> setBudget,
      Consumer<Category> rename,
      Consumer<Category> remove) {
    TableColumn<CategorySummary, Void> actions = new TableColumn<>("Actions");
    actions.setPrefWidth(270);
    actions.setCellFactory(
        ignored ->
            new TableCell<>() {
              private final Button setBudgetButton = new Button("Set budget");
              private final Button renameButton = new Button("Rename");
              private final Button removeButton = new Button("Remove");
              private final HBox buttons = new HBox(6, setBudgetButton, renameButton, removeButton);

              {
                buttons.getStyleClass().add("row-actions");
                setBudgetButton.getStyleClass().add(TABLE_ACTION_STYLE_CLASS);
                renameButton.getStyleClass().add(TABLE_ACTION_STYLE_CLASS);
                removeButton.getStyleClass().addAll(TABLE_ACTION_STYLE_CLASS, "danger-action");
                setBudgetButton.setOnAction(event -> setBudget.accept(row().category()));
                renameButton.setOnAction(event -> rename.accept(row().category()));
                removeButton.setOnAction(event -> remove.accept(row().category()));
              }

              @Override
              protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
              }

              private CategorySummary row() {
                return getTableView().getItems().get(getIndex());
              }
            });
    table.getColumns().add(actions);
  }

  /**
   * Returns the style class for a category's remaining amount.
   *
   * @param summary budget summary to style
   * @return CSS class matching the summary state
   */
  public static String remainingStyleClass(CategorySummary summary) {
    return switch (summary.state()) {
      case NORMAL -> NORMAL_REMAINING_STYLE_CLASS;
      case WARNING -> WARNING_REMAINING_STYLE_CLASS;
      case OVER_BUDGET -> OVER_BUDGET_REMAINING_STYLE_CLASS;
    };
  }

  private <T> TableColumn<T, String> textColumn(
      String title, Function<T, String> value, double width) {
    TableColumn<T, String> column = new TableColumn<>(title);
    column.setCellValueFactory(data -> new ReadOnlyStringWrapper(value.apply(data.getValue())));
    column.setPrefWidth(width);
    return column;
  }

  private TableColumn<CategorySummary, String> remainingColumn() {
    TableColumn<CategorySummary, String> column =
        textColumn("Remaining", summary -> MoneyInput.format(summary.remaining()), 130);
    column.setCellFactory(
        ignored ->
            new TableCell<>() {
              @Override
              protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass()
                    .removeAll(
                        NORMAL_REMAINING_STYLE_CLASS,
                        WARNING_REMAINING_STYLE_CLASS,
                        OVER_BUDGET_REMAINING_STYLE_CLASS);
                if (!empty && getTableRow().getItem() != null) {
                  getStyleClass().add(remainingStyleClass(getTableRow().getItem()));
                }
              }
            });
    return column;
  }
}
