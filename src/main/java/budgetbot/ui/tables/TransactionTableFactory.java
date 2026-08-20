package budgetbot.ui.tables;

import budgetbot.model.Category;
import budgetbot.model.Transaction;
import budgetbot.model.TransactionType;
import budgetbot.service.BudgetService;
import budgetbot.ui.MoneyInput;
import java.time.format.DateTimeFormatter;
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

/** Builds transaction tables, their amount styling, and transaction row actions. */
public final class TransactionTableFactory {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM uuuu");
  private static final String AMOUNT_LABEL = "Amount";
  private static final String TABLE_ACTION_STYLE_CLASS = "table-action";
  private static final String INCOME_VALUE_STYLE_CLASS = "income-value";
  private static final String EXPENSE_VALUE_STYLE_CLASS = "expense-value";

  private final BudgetService service;

  /**
   * Creates a transaction table factory.
   *
   * @param service service used to resolve category names
   */
  public TransactionTableFactory(BudgetService service) {
    this.service = service;
  }

  /**
   * Creates a transaction table without row actions.
   *
   * @param transactions transactions to display
   * @return configured table
   */
  public TableView<Transaction> create(List<Transaction> transactions) {
    TableView<Transaction> table = new TableView<>(FXCollections.observableArrayList(transactions));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
    table.setPlaceholder(new Label("No transactions for this month."));
    table.getStyleClass().add("data-table");
    table
        .getColumns()
        .add(textColumn("Date", transaction -> transaction.date().format(DATE_FORMAT), 120));
    table
        .getColumns()
        .add(transactionValueColumn("Type", transaction -> transaction.type().name(), 100));
    table
        .getColumns()
        .add(
            textColumn(
                "Description",
                transaction ->
                    transaction.description().isBlank()
                        ? transaction.type().name()
                        : transaction.description(),
                220));
    table
        .getColumns()
        .add(textColumn("Category", transaction -> categoryName(transaction.categoryId()), 150));
    table
        .getColumns()
        .add(
            transactionValueColumn(
                AMOUNT_LABEL,
                transaction ->
                    (transaction.type() == TransactionType.INCOME ? "+" : "-")
                        + MoneyInput.format(transaction.amount()),
                120));
    return table;
  }

  /**
   * Adds edit and delete actions to a transaction table.
   *
   * @param table table to extend
   * @param edit action for the selected transaction
   * @param delete action for the selected transaction
   */
  public void addActions(
      TableView<Transaction> table, Consumer<Transaction> edit, Consumer<Transaction> delete) {
    TableColumn<Transaction, Void> actions = new TableColumn<>("Actions");
    actions.setPrefWidth(150);
    actions.setCellFactory(
        ignored ->
            new TableCell<>() {
              private final Button editButton = new Button("Edit");
              private final Button deleteButton = new Button("Delete");
              private final HBox buttons = new HBox(6, editButton, deleteButton);

              {
                buttons.getStyleClass().add("row-actions");
                editButton.getStyleClass().add(TABLE_ACTION_STYLE_CLASS);
                deleteButton.getStyleClass().addAll(TABLE_ACTION_STYLE_CLASS, "danger-action");
                editButton.setOnAction(event -> edit.accept(row()));
                deleteButton.setOnAction(event -> delete.accept(row()));
              }

              @Override
              protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
              }

              private Transaction row() {
                return getTableView().getItems().get(getIndex());
              }
            });
    table.getColumns().add(actions);
  }

  private <T> TableColumn<T, String> textColumn(
      String title, Function<T, String> value, double width) {
    TableColumn<T, String> column = new TableColumn<>(title);
    column.setCellValueFactory(data -> new ReadOnlyStringWrapper(value.apply(data.getValue())));
    column.setPrefWidth(width);
    return column;
  }

  private TableColumn<Transaction, String> transactionValueColumn(
      String title, Function<Transaction, String> value, double width) {
    TableColumn<Transaction, String> column = textColumn(title, value, width);
    column.setCellFactory(
        ignored ->
            new TableCell<>() {
              @Override
              protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll(INCOME_VALUE_STYLE_CLASS, EXPENSE_VALUE_STYLE_CLASS);
                if (!empty && getTableRow().getItem() != null) {
                  getStyleClass()
                      .add(
                          getTableRow().getItem().type() == TransactionType.INCOME
                              ? INCOME_VALUE_STYLE_CLASS
                              : EXPENSE_VALUE_STYLE_CLASS);
                }
              }
            });
    return column;
  }

  private String categoryName(Long categoryId) {
    if (categoryId == null) {
      return "—";
    }
    return service.categories().stream()
        .filter(category -> category.id() == categoryId)
        .map(Category::name)
        .findFirst()
        .orElse("Removed category");
  }
}
