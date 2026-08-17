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
import java.util.List;
import java.util.Locale;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
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
    Button categories = new Button("Categories & budgets");
    Button settings = new Button("Settings");
    dashboard.setOnAction(event -> layout.setCenter(dashboardView()));
    transactions.setOnAction(event -> layout.setCenter(transactionsView()));
    categories.setOnAction(event -> layout.setCenter(categoriesView()));
    settings.setOnAction(event -> layout.setCenter(settingsView()));
    VBox navigation = new VBox(12, title, dashboard, transactions, categories, settings);
    navigation.setPadding(new Insets(24));
    navigation.setMinWidth(180);
    navigation.getStyleClass().add("navigation");
    return navigation;
  }

  private VBox dashboardView() {
    DashboardSnapshot snapshot = service.dashboard(selectedMonth);
    Label balance = new Label("Overall balance: " + money(snapshot.overallBalance()));
    balance.getStyleClass().add("balance");
    VBox summaries = new VBox(10);
    for (CategorySummary summary : snapshot.categorySummaries()) {
      summaries.getChildren().add(categoryProgress(summary));
    }
    Label recentTitle = new Label("Recent activity");
    recentTitle.getStyleClass().add("section-title");
    ListView<Transaction> recent = transactionList(snapshot.recentTransactions());
    recent.setPrefHeight(190);
    VBox content =
        new VBox(
            18,
            monthControls(),
            balance,
            new Label("Category budgets"),
            summaries,
            recentTitle,
            recent);
    content.setPadding(new Insets(28));
    VBox.setVgrow(recent, Priority.ALWAYS);
    return content;
  }

  private HBox monthControls() {
    Button previous = new Button("<");
    Button next = new Button(">");
    Label month = new Label(selectedMonth.format(MONTH_FORMAT));
    month.getStyleClass().add("section-title");
    previous.setOnAction(
        event -> {
          selectedMonth = selectedMonth.minusMonths(1);
          layout.setCenter(dashboardView());
        });
    next.setOnAction(
        event -> {
          selectedMonth = selectedMonth.plusMonths(1);
          layout.setCenter(dashboardView());
        });
    HBox controls = new HBox(10, previous, month, next);
    controls.setAlignment(Pos.CENTER_LEFT);
    return controls;
  }

  private VBox categoryProgress(CategorySummary summary) {
    Label name = new Label(summary.category().name());
    Label amounts = new Label(money(summary.spent()) + " spent of " + money(summary.available()));
    Label state = new Label(stateText(summary));
    state.getStyleClass().add(summary.state().name().toLowerCase(Locale.ROOT) + "-state");
    ProgressBar progress = new ProgressBar(progress(summary));
    progress.setMaxWidth(Double.MAX_VALUE);
    VBox card = new VBox(4, new HBox(16, name, state), amounts, progress);
    card.getStyleClass().add("budget-card");
    return card;
  }

  private VBox transactionsView() {
    ListView<Transaction> transactions = transactionList(service.transactions(selectedMonth));
    Button add = new Button("Add transaction");
    Button edit = new Button("Edit selected");
    Button delete = new Button("Delete selected");
    add.setOnAction(event -> editTransaction(null, transactions));
    edit.setOnAction(
        event -> editTransaction(transactions.getSelectionModel().getSelectedItem(), transactions));
    delete.setOnAction(
        event ->
            deleteTransaction(transactions.getSelectionModel().getSelectedItem(), transactions));
    VBox content =
        new VBox(
            16,
            monthControls(),
            new Label("Transactions"),
            transactions,
            new HBox(10, add, edit, delete));
    content.setPadding(new Insets(28));
    VBox.setVgrow(transactions, Priority.ALWAYS);
    return content;
  }

  private VBox categoriesView() {
    ListView<Category> categories =
        new ListView<>(FXCollections.observableArrayList(service.categories()));
    TextField amount = new TextField();
    amount.setPromptText("Monthly amount");
    Button add = new Button("Add");
    Button rename = new Button("Rename");
    Button remove = new Button("Remove");
    Button saveBudget = new Button("Set selected-month budget");
    add.setOnAction(event -> addCategory(categories));
    rename.setOnAction(event -> renameCategory(categories));
    remove.setOnAction(event -> removeCategory(categories));
    saveBudget.setOnAction(
        event -> saveBudget(categories.getSelectionModel().getSelectedItem(), amount));
    VBox content =
        new VBox(
            16,
            monthControls(),
            new Label("Categories and budgets"),
            categories,
            new HBox(10, add, rename, remove),
            new HBox(10, amount, saveBudget));
    content.setPadding(new Insets(28));
    VBox.setVgrow(categories, Priority.ALWAYS);
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

  private void editTransaction(Transaction transaction, ListView<Transaction> list) {
    Transaction existing =
        transaction == null ? list.getSelectionModel().getSelectedItem() : transaction;
    Dialog<Transaction> dialog = new Dialog<>();
    dialog.setTitle(transaction == null ? "Add transaction" : "Edit transaction");
    ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
    ComboBox<TransactionType> type =
        new ComboBox<>(FXCollections.observableArrayList(TransactionType.values()));
    type.setValue(existing == null ? TransactionType.EXPENSE : existing.type());
    TextField amount = new TextField(existing == null ? "" : existing.amount().toPlainString());
    TextField date =
        new TextField(existing == null ? LocalDate.now().toString() : existing.date().toString());
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
    fields.addRow(1, new Label("Amount"), amount);
    fields.addRow(2, new Label("Date (YYYY-MM-DD)"), date);
    fields.addRow(3, new Label("Description"), description);
    fields.addRow(4, new Label("Category"), category);
    dialog.getDialogPane().setContent(fields);
    dialog.setResultConverter(
        button -> {
          if (button != saveType) {
            return null;
          }
          return new Transaction(
              existing == null ? 0 : existing.id(),
              type.getValue(),
              new BigDecimal(amount.getText().trim()),
              LocalDate.parse(date.getText().trim()),
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
                list.setItems(
                    FXCollections.observableArrayList(service.transactions(selectedMonth)));
              } catch (IllegalArgumentException | BudgetPersistenceException exception) {
                showError(exception);
              }
            });
  }

  private void deleteTransaction(Transaction transaction, ListView<Transaction> list) {
    if (transaction == null) {
      showInformation("Select a transaction", "Choose a transaction before deleting it.");
      return;
    }
    if (confirm("Delete transaction", "Delete this transaction?")) {
      service.deleteTransaction(transaction.id());
      list.setItems(FXCollections.observableArrayList(service.transactions(selectedMonth)));
    }
  }

  private void addCategory(ListView<Category> list) {
    textPrompt("Add category", "Category name")
        .ifPresent(
            name -> {
              try {
                service.addCategory(name);
                list.setItems(FXCollections.observableArrayList(service.categories()));
              } catch (IllegalArgumentException | BudgetPersistenceException exception) {
                showError(exception);
              }
            });
  }

  private void renameCategory(ListView<Category> list) {
    Category selected = list.getSelectionModel().getSelectedItem();
    if (selected == null) {
      showInformation("Select a category", "Choose a category before renaming it.");
      return;
    }
    textPrompt("Rename category", "New category name", selected.name())
        .ifPresent(
            name -> {
              try {
                service.renameCategory(selected.id(), name);
                list.setItems(FXCollections.observableArrayList(service.categories()));
              } catch (IllegalArgumentException | BudgetPersistenceException exception) {
                showError(exception);
              }
            });
  }

  private void removeCategory(ListView<Category> list) {
    Category selected = list.getSelectionModel().getSelectedItem();
    if (selected == null) {
      showInformation("Select a category", "Choose a category before removing it.");
      return;
    }
    ComboBox<Category> replacement =
        new ComboBox<>(FXCollections.observableArrayList(service.categories()));
    replacement.getItems().remove(selected);
    replacement.setCellFactory(ignored -> new CategoryCell());
    replacement.setButtonCell(new CategoryCell());
    Dialog<Category> dialog = new Dialog<>();
    dialog.setTitle("Remove " + selected.name());
    ButtonType remove = new ButtonType("Reassign and remove", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(remove, ButtonType.CANCEL);
    dialog.getDialogPane().setContent(new VBox(8, new Label("Reassign expenses to:"), replacement));
    dialog.setResultConverter(button -> button == remove ? replacement.getValue() : null);
    dialog
        .showAndWait()
        .ifPresent(
            target -> {
              try {
                service.removeCategory(selected.id(), target.id());
                list.setItems(FXCollections.observableArrayList(service.categories()));
              } catch (IllegalArgumentException | BudgetPersistenceException exception) {
                showError(exception);
              }
            });
  }

  private void saveBudget(Category category, TextField amount) {
    if (category == null) {
      showInformation("Select a category", "Choose a category before setting its budget.");
      return;
    }
    try {
      service.setMonthlyBudget(
          category.id(), selectedMonth, new BigDecimal(amount.getText().trim()));
      amount.clear();
      showInformation("Budget saved", "The selected month's base amount was saved.");
    } catch (IllegalArgumentException | BudgetPersistenceException exception) {
      showError(exception);
    }
  }

  private ListView<Transaction> transactionList(List<Transaction> transactions) {
    ListView<Transaction> list = new ListView<>(FXCollections.observableArrayList(transactions));
    list.setCellFactory(ignored -> new TransactionCell());
    return list;
  }

  private double progress(CategorySummary summary) {
    if (summary.available().signum() <= 0) {
      return summary.spent().signum() > 0 ? 1 : 0;
    }
    return Math.min(
        1,
        summary
            .spent()
            .divide(summary.available(), 4, java.math.RoundingMode.HALF_UP)
            .doubleValue());
  }

  private String stateText(CategorySummary summary) {
    return switch (summary.state()) {
      case NORMAL -> money(summary.remaining()) + " remaining";
      case WARNING -> "Warning: " + money(summary.remaining()) + " remaining";
      case OVER_BUDGET -> "Over budget by " + money(summary.remaining().abs());
    };
  }

  private String money(BigDecimal amount) {
    return "$" + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
  }

  private java.util.Optional<String> textPrompt(String title, String prompt) {
    return textPrompt(title, prompt, "");
  }

  private java.util.Optional<String> textPrompt(String title, String prompt, String value) {
    javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(value);
    dialog.setTitle(title);
    dialog.setHeaderText(null);
    dialog.setContentText(prompt);
    return dialog.showAndWait();
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

  private final class TransactionCell extends javafx.scene.control.ListCell<Transaction> {
    @Override
    protected void updateItem(Transaction item, boolean empty) {
      super.updateItem(item, empty);
      if (empty || item == null) {
        setText(null);
      } else {
        String sign = item.type() == TransactionType.INCOME ? "+" : "-";
        String description = item.description().isBlank() ? item.type().name() : item.description();
        setText(
            item.date().format(DATE_FORMAT)
                + "  "
                + description
                + "  "
                + sign
                + money(item.amount()));
      }
    }
  }
}
