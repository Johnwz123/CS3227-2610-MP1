package budgetbot;

import budgetbot.model.BudgetSettings;
import budgetbot.model.Category;
import budgetbot.model.CategorySummary;
import budgetbot.model.DashboardSnapshot;
import budgetbot.model.Transaction;
import budgetbot.model.TransactionType;
import budgetbot.persistence.BudgetPersistenceException;
import budgetbot.service.BudgetService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Builds and coordinates the BudgetBot JavaFX views. */
final class BudgetBotWindow {
  private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM uuuu");
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM uuuu");
  private static final String AMOUNT_LABEL = "Amount";
  private static final String TABLE_ACTION_STYLE_CLASS = "table-action";
  private static final String INCOME_VALUE_STYLE_CLASS = "income-value";
  private static final String EXPENSE_VALUE_STYLE_CLASS = "expense-value";
  private static final String NORMAL_REMAINING_STYLE_CLASS = "normal-remaining";
  private static final String WARNING_REMAINING_STYLE_CLASS = "warning-remaining";
  private static final String OVER_BUDGET_REMAINING_STYLE_CLASS = "over-budget-remaining";

  private final Stage stage;
  private final BudgetService service;
  private final BorderPane layout = new BorderPane();
  private YearMonth selectedMonth = YearMonth.now();

  BudgetBotWindow(Stage stage, BudgetService service) {
    this.stage = stage;
    this.service = service;
  }

  /** Shows the main window. */
  void show() {
    layout.setLeft(navigation());
    layout.setCenter(dashboardView());
    Scene scene = new Scene(layout, 1120, 720);
    scene.getStylesheets().add(BudgetBotWindow.class.getResource("budgetbot.css").toExternalForm());
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
    dashboard.setOnAction(event -> layout.setCenter(dashboardView()));
    transactions.setOnAction(event -> layout.setCenter(transactionsView()));
    budgets.setOnAction(event -> layout.setCenter(budgetsView()));
    settings.setOnAction(event -> layout.setCenter(settingsView()));
    VBox navigation = new VBox(12, title, dashboard, transactions, budgets, settings);
    navigation.setPadding(new Insets(24));
    navigation.setMinWidth(180);
    navigation.getStyleClass().add("navigation");
    return navigation;
  }

  private VBox dashboardView() {
    DashboardSnapshot snapshot = service.dashboard(selectedMonth);
    Label balance = new Label("Overall balance: " + money(snapshot.overallBalance()));
    balance.getStyleClass().add("balance");
    TableView<CategorySummary> budgets = budgetSummaryTable(snapshot.categorySummaries(), false);
    TableView<Transaction> recent = transactionTable(snapshot.recentTransactions(), false);
    SplitPane tables =
        new SplitPane(
            tableSection("Budget status", budgets), tableSection("Recent activity", recent));
    tables.setOrientation(javafx.geometry.Orientation.VERTICAL);
    tables.setDividerPositions(0.58);
    VBox content = new VBox(18, monthControls(this::dashboardView), balance, tables);
    content.setPadding(new Insets(28));
    VBox.setVgrow(tables, Priority.ALWAYS);
    return content;
  }

  private HBox monthControls(java.util.function.Supplier<VBox> currentView) {
    Button previous = new Button("<");
    Button next = new Button(">");
    Label month = new Label(selectedMonth.format(MONTH_FORMAT));
    month.getStyleClass().add("section-title");
    previous.setOnAction(
        event -> {
          selectedMonth = selectedMonth.minusMonths(1);
          layout.setCenter(currentView.get());
        });
    next.setOnAction(
        event -> {
          selectedMonth = selectedMonth.plusMonths(1);
          layout.setCenter(currentView.get());
        });
    HBox controls = new HBox(10, previous, month, next);
    controls.setAlignment(Pos.CENTER_LEFT);
    return controls;
  }

  private VBox transactionsView() {
    TableView<Transaction> transactions =
        transactionTable(service.transactions(selectedMonth), true);
    Button add = new Button("Add transaction");
    add.setOnAction(event -> editTransaction(null, transactions));
    VBox content =
        new VBox(
            16,
            monthControls(this::transactionsView),
            new Label("Transactions"),
            transactions,
            new HBox(10, add));
    content.setPadding(new Insets(28));
    VBox.setVgrow(transactions, Priority.ALWAYS);
    return content;
  }

  private VBox budgetsView() {
    TableView<CategorySummary> budgets =
        budgetSummaryTable(service.dashboard(selectedMonth).categorySummaries(), true);
    Button add = new Button("Add");
    add.setOnAction(event -> addCategory(budgets));
    VBox content =
        new VBox(
            16, monthControls(this::budgetsView), new Label("Budgets"), budgets, new HBox(10, add));
    content.setPadding(new Insets(28));
    VBox.setVgrow(budgets, Priority.ALWAYS);
    return content;
  }

  private VBox settingsView() {
    BudgetSettings current = service.settings();
    CheckBox rollover = new CheckBox("Enable rollover for the whole budget");
    rollover.setSelected(current.rolloverEnabled());
    Spinner<Integer> threshold = new Spinner<>();
    threshold.setValueFactory(
        new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, current.warningThreshold()));
    Button save = new Button("Save settings");
    save.setOnAction(
        event -> {
          try {
            service.saveSettings(rollover.isSelected(), threshold.getValue());
            showInformation(
                "Settings saved",
                "New settings will apply to the next month started in BudgetBot.");
          } catch (IllegalArgumentException | BudgetPersistenceException exception) {
            showError(exception);
          }
        });
    VBox content =
        new VBox(
            16,
            new Label("Settings"),
            rollover,
            new Label("Warning threshold (%)"),
            threshold,
            save);
    content.setPadding(new Insets(28));
    return content;
  }

  private void editTransaction(Transaction transaction, TableView<Transaction> table) {
    Transaction existing = transaction;
    Dialog<Transaction> dialog = new Dialog<>();
    dialog.setTitle(transaction == null ? "Add transaction" : "Edit transaction");
    ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
    ComboBox<TransactionType> type =
        new ComboBox<>(FXCollections.observableArrayList(TransactionType.values()));
    type.setValue(existing == null ? TransactionType.EXPENSE : existing.type());
    TextField amount = new TextField(existing == null ? "" : existing.amount().toPlainString());
    DatePicker date = new DatePicker(existing == null ? LocalDate.now() : existing.date());
    date.setPromptText("Choose a date");
    date.setEditable(false);
    TextField description = new TextField(existing == null ? "" : existing.description());
    ComboBox<Category> category =
        new ComboBox<>(FXCollections.observableArrayList(service.categories()));
    if (existing != null && existing.categoryId() != null) {
      category.setValue(
          service.categories().stream()
              .filter(item -> item.id() == existing.categoryId())
              .findFirst()
              .orElse(null));
    }
    category.setCellFactory(ignored -> new CategoryCell());
    category.setButtonCell(new CategoryCell());
    type.valueProperty()
        .addListener(
            (ignored, oldValue, newValue) ->
                category.setDisable(newValue == TransactionType.INCOME));
    category.setDisable(type.getValue() == TransactionType.INCOME);
    GridPane fields = new GridPane();
    fields.setHgap(10);
    fields.setVgap(10);
    fields.addRow(0, new Label("Type"), type);
    fields.addRow(1, new Label(AMOUNT_LABEL), amount);
    fields.addRow(2, new Label("Date"), date);
    fields.addRow(3, new Label("Description"), description);
    fields.addRow(4, new Label("Category"), category);
    Label validation = validationMessage();
    dialog.getDialogPane().setContent(new VBox(10, fields, validation));
    Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
    saveButton.addEventFilter(
        ActionEvent.ACTION,
        event -> {
          try {
            parseMoney(amount.getText(), AMOUNT_LABEL, false);
            if (date.getValue() == null) {
              throw new IllegalArgumentException("Choose a transaction date.");
            }
            if (type.getValue() == TransactionType.EXPENSE && category.getValue() == null) {
              throw new IllegalArgumentException("Choose a category for an expense.");
            }
            validation.setText("");
          } catch (IllegalArgumentException exception) {
            validation.setText(exception.getMessage());
            event.consume();
          }
        });
    dialog.setResultConverter(
        button -> {
          if (button != saveType) {
            return null;
          }
          return new Transaction(
              existing == null ? 0 : existing.id(),
              type.getValue(),
              parseMoney(amount.getText(), AMOUNT_LABEL, false),
              date.getValue(),
              description.getText(),
              category.getValue() == null ? null : category.getValue().id());
        });
    dialog
        .showAndWait()
        .ifPresent(
            saved -> {
              try {
                if (saved.id() == 0) {
                  service.addTransaction(
                      saved.type(),
                      saved.amount(),
                      saved.date(),
                      saved.description(),
                      saved.categoryId());
                } else {
                  service.updateTransaction(saved);
                }
                table.setItems(
                    FXCollections.observableArrayList(service.transactions(selectedMonth)));
              } catch (IllegalArgumentException | BudgetPersistenceException exception) {
                showError(exception);
              }
            });
  }

  private void deleteTransaction(Transaction transaction, TableView<Transaction> table) {
    if (confirm("Delete transaction", "Delete this transaction?")) {
      service.deleteTransaction(transaction.id());
      table.setItems(FXCollections.observableArrayList(service.transactions(selectedMonth)));
    }
  }

  private void addCategory(TableView<CategorySummary> table) {
    textPrompt("Add category", "Category name")
        .ifPresent(
            name -> {
              try {
                service.addCategory(name);
                refreshBudgetTable(table);
              } catch (IllegalArgumentException | BudgetPersistenceException exception) {
                showError(exception);
              }
            });
  }

  private void renameCategory(Category category, TableView<CategorySummary> table) {
    textPrompt("Rename category", "New category name", category.name())
        .ifPresent(
            name -> {
              try {
                service.renameCategory(category.id(), name);
                refreshBudgetTable(table);
              } catch (IllegalArgumentException | BudgetPersistenceException exception) {
                showError(exception);
              }
            });
  }

  private void removeCategory(Category category, TableView<CategorySummary> table) {
    ComboBox<Category> replacement =
        new ComboBox<>(FXCollections.observableArrayList(service.categories()));
    replacement.getItems().remove(category);
    replacement.setCellFactory(ignored -> new CategoryCell());
    replacement.setButtonCell(new CategoryCell());
    Dialog<Category> dialog = new Dialog<>();
    dialog.setTitle("Remove " + category.name());
    ButtonType remove = new ButtonType("Reassign and remove", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(remove, ButtonType.CANCEL);
    Label validation = validationMessage();
    dialog
        .getDialogPane()
        .setContent(new VBox(8, new Label("Reassign expenses to:"), replacement, validation));
    Button removeButton = (Button) dialog.getDialogPane().lookupButton(remove);
    removeButton.addEventFilter(
        ActionEvent.ACTION,
        event -> {
          if (replacement.getValue() == null) {
            validation.setText("Choose a replacement category before removing this one.");
            event.consume();
          }
        });
    dialog.setResultConverter(button -> button == remove ? replacement.getValue() : null);
    dialog
        .showAndWait()
        .ifPresent(
            target -> {
              try {
                service.removeCategory(category.id(), target.id());
                refreshBudgetTable(table);
              } catch (IllegalArgumentException | BudgetPersistenceException exception) {
                showError(exception);
              }
            });
  }

  private void saveBudget(Category category, TableView<CategorySummary> table) {
    moneyPrompt("Set " + category.name() + " budget", "Monthly base amount", true)
        .ifPresent(
            amount -> {
              try {
                service.setMonthlyBudget(category.id(), selectedMonth, amount);
                refreshBudgetTable(table);
              } catch (IllegalArgumentException | BudgetPersistenceException exception) {
                showError(exception);
              }
            });
  }

  private VBox tableSection(String title, TableView<?> table) {
    Label heading = new Label(title);
    heading.getStyleClass().add("section-title");
    VBox section = new VBox(8, heading, table);
    VBox.setVgrow(table, Priority.ALWAYS);
    return section;
  }

  private TableView<Transaction> transactionTable(
      java.util.List<Transaction> transactions, boolean includeActions) {
    TableView<Transaction> table = createTable(transactions, "No transactions for this month.");
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
                        + money(transaction.amount()),
                120));
    if (includeActions) {
      table.getColumns().add(transactionActionsColumn(table));
    }
    return table;
  }

  private TableView<CategorySummary> budgetSummaryTable(
      java.util.List<CategorySummary> summaries, boolean includeActions) {
    TableView<CategorySummary> table = createTable(summaries, "No budget categories yet.");
    table.getColumns().add(textColumn("Category", summary -> summary.category().name(), 180));
    table.getColumns().add(textColumn("Available", summary -> money(summary.available()), 130));
    table.getColumns().add(textColumn("Spent", summary -> money(summary.spent()), 130));
    table.getColumns().add(remainingColumn());
    if (includeActions) {
      table.getColumns().add(budgetActionsColumn(table));
    }
    return table;
  }

  private <T> TableView<T> createTable(java.util.List<T> items, String emptyText) {
    TableView<T> table = new TableView<>(FXCollections.observableArrayList(items));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
    table.setPlaceholder(new Label(emptyText));
    table.getStyleClass().add("data-table");
    return table;
  }

  private <T> TableColumn<T, String> textColumn(
      String title, java.util.function.Function<T, String> value, double width) {
    TableColumn<T, String> column = new TableColumn<>(title);
    column.setCellValueFactory(data -> new ReadOnlyStringWrapper(value.apply(data.getValue())));
    column.setPrefWidth(width);
    return column;
  }

  private TableColumn<Transaction, String> transactionValueColumn(
      String title, java.util.function.Function<Transaction, String> value, double width) {
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

  private TableColumn<CategorySummary, String> remainingColumn() {
    TableColumn<CategorySummary, String> column =
        textColumn("Remaining", summary -> money(summary.remaining()), 130);
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

  private String remainingStyleClass(CategorySummary summary) {
    return switch (summary.state()) {
      case NORMAL -> NORMAL_REMAINING_STYLE_CLASS;
      case WARNING -> WARNING_REMAINING_STYLE_CLASS;
      case OVER_BUDGET -> OVER_BUDGET_REMAINING_STYLE_CLASS;
    };
  }

  private TableColumn<Transaction, Void> transactionActionsColumn(TableView<Transaction> table) {
    TableColumn<Transaction, Void> actions = new TableColumn<>("Actions");
    actions.setPrefWidth(150);
    actions.setCellFactory(
        ignored ->
            new TableCell<>() {
              private final Button edit = new Button("Edit");
              private final Button delete = new Button("Delete");
              private final HBox buttons = new HBox(6, edit, delete);

              {
                buttons.getStyleClass().add("row-actions");
                edit.getStyleClass().add(TABLE_ACTION_STYLE_CLASS);
                delete.getStyleClass().addAll(TABLE_ACTION_STYLE_CLASS, "danger-action");
                edit.setOnAction(event -> editTransaction(row(), table));
                delete.setOnAction(event -> deleteTransaction(row(), table));
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
    return actions;
  }

  private TableColumn<CategorySummary, Void> budgetActionsColumn(TableView<CategorySummary> table) {
    TableColumn<CategorySummary, Void> actions = new TableColumn<>("Actions");
    actions.setPrefWidth(270);
    actions.setCellFactory(
        ignored ->
            new TableCell<>() {
              private final Button setBudget = new Button("Set budget");
              private final Button rename = new Button("Rename");
              private final Button remove = new Button("Remove");
              private final HBox buttons = new HBox(6, setBudget, rename, remove);

              {
                buttons.getStyleClass().add("row-actions");
                setBudget.getStyleClass().add(TABLE_ACTION_STYLE_CLASS);
                rename.getStyleClass().add(TABLE_ACTION_STYLE_CLASS);
                remove.getStyleClass().addAll(TABLE_ACTION_STYLE_CLASS, "danger-action");
                setBudget.setOnAction(event -> saveBudget(row().category(), table));
                rename.setOnAction(event -> renameCategory(row().category(), table));
                remove.setOnAction(event -> removeCategory(row().category(), table));
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
    return actions;
  }

  private void refreshBudgetTable(TableView<CategorySummary> table) {
    table.setItems(
        FXCollections.observableArrayList(service.dashboard(selectedMonth).categorySummaries()));
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

  private String money(BigDecimal amount) {
    return "$" + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
  }

  private java.util.Optional<String> textPrompt(String title, String prompt) {
    return textPrompt(title, prompt, "");
  }

  private java.util.Optional<String> textPrompt(String title, String prompt, String value) {
    Dialog<String> dialog = new Dialog<>();
    dialog.setTitle(title);
    dialog.setHeaderText(null);
    ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
    TextField input = new TextField(value);
    Label validation = validationMessage();
    dialog.getDialogPane().setContent(new VBox(8, new Label(prompt), input, validation));
    Button saveButton = (Button) dialog.getDialogPane().lookupButton(save);
    saveButton.addEventFilter(
        ActionEvent.ACTION,
        event -> {
          if (input.getText().trim().isEmpty()) {
            validation.setText(prompt + " cannot be empty.");
            event.consume();
          }
        });
    dialog.setResultConverter(button -> button == save ? input.getText().trim() : null);
    return dialog.showAndWait();
  }

  private java.util.Optional<BigDecimal> moneyPrompt(
      String title, String prompt, boolean zeroAllowed) {
    Dialog<BigDecimal> dialog = new Dialog<>();
    dialog.setTitle(title);
    dialog.setHeaderText(null);
    ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
    TextField input = new TextField();
    input.setPromptText("0.00");
    Label validation = validationMessage();
    dialog.getDialogPane().setContent(new VBox(8, new Label(prompt), input, validation));
    Button saveButton = (Button) dialog.getDialogPane().lookupButton(save);
    saveButton.addEventFilter(
        ActionEvent.ACTION,
        event -> {
          try {
            parseMoney(input.getText(), prompt, zeroAllowed);
            validation.setText("");
          } catch (IllegalArgumentException exception) {
            validation.setText(exception.getMessage());
            event.consume();
          }
        });
    dialog.setResultConverter(
        button -> button == save ? parseMoney(input.getText(), prompt, zeroAllowed) : null);
    return dialog.showAndWait();
  }

  private BigDecimal parseMoney(String input, String label, boolean zeroAllowed) {
    String normalized = input == null ? "" : input.trim();
    if (!normalized.matches("\\d+(?:\\.\\d{1,2})?")) {
      throw new IllegalArgumentException(
          label + " must be a number with at most two decimal places.");
    }
    BigDecimal amount = new BigDecimal(normalized);
    if (zeroAllowed ? amount.signum() < 0 : amount.signum() <= 0) {
      throw new IllegalArgumentException(
          label + (zeroAllowed ? " must be zero or greater." : " must be greater than zero."));
    }
    return amount;
  }

  private Label validationMessage() {
    Label validation = new Label();
    validation.getStyleClass().add("validation-message");
    validation.setWrapText(true);
    return validation;
  }

  private boolean confirm(String title, String message) {
    Alert alert =
        new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
    alert.setTitle(title);
    return alert.showAndWait().filter(button -> button == ButtonType.OK).isPresent();
  }

  private void showInformation(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.showAndWait();
  }

  private void showError(Exception exception) {
    Alert alert = new Alert(Alert.AlertType.ERROR, exception.getMessage(), ButtonType.OK);
    alert.setTitle("BudgetBot could not save your change");
    alert.setHeaderText(null);
    alert.showAndWait();
  }

  private final class CategoryCell extends javafx.scene.control.ListCell<Category> {
    @Override
    protected void updateItem(Category item, boolean empty) {
      super.updateItem(item, empty);
      setText(empty || item == null ? null : item.name());
    }
  }
}
