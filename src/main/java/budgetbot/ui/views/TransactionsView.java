package budgetbot.ui.views;

import budgetbot.model.Transaction;
import budgetbot.model.TransactionQuery;
import budgetbot.model.TransactionType;
import budgetbot.persistence.BudgetPersistenceException;
import budgetbot.service.BudgetService;
import budgetbot.ui.MoneyInput;
import budgetbot.ui.UiAlerts;
import budgetbot.ui.dialogs.TransactionDialog;
import budgetbot.ui.tables.TransactionTableFactory;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.function.IntConsumer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Builds the transaction-management view and handles its user actions. */
public final class TransactionsView {
  private final BudgetService service;
  private final TransactionTableFactory tables;
  private final TransactionDialog dialog;
  private final IntConsumer changeMonth;
  private TransactionQuery appliedQuery = TransactionQuery.empty();

  /**
   * Creates the transaction view builder.
   *
   * @param service service that supplies and changes transaction data
   * @param tables factory for transaction tables
   * @param dialog dialog used for transaction input
   * @param changeMonth receiver for month-navigation offsets
   */
  public TransactionsView(
      BudgetService service,
      TransactionTableFactory tables,
      TransactionDialog dialog,
      IntConsumer changeMonth) {
    this.service = service;
    this.tables = tables;
    this.dialog = dialog;
    this.changeMonth = changeMonth;
  }

  /**
   * Builds transaction-management content for a month.
   *
   * @param month selected budget month
   * @return transaction-management content
   */
  public VBox build(YearMonth month) {
    TableView<Transaction> transactions = tables.create(service.transactions(month, appliedQuery));
    transactions.setPlaceholder(new Label("No matching transactions."));
    Label resultCount = new Label();
    resultCount.setId("transaction-result-count");
    updateResultCount(resultCount, transactions.getItems().size());
    VBox filters = createFilters(month, transactions, resultCount);
    tables.addActions(
        transactions,
        transaction -> editTransaction(transaction, transactions, resultCount, month),
        transaction -> deleteTransaction(transaction, transactions, resultCount, month));
    Button add = new Button("Add transaction");
    add.setOnAction(event -> editTransaction(null, transactions, resultCount, month));
    VBox content =
        new VBox(
            16,
            MonthControls.create(month, changeMonth),
            new Label("Transactions"),
            filters,
            resultCount,
            transactions,
            new HBox(10, add));
    content.setPadding(new Insets(28));
    VBox.setVgrow(transactions, Priority.ALWAYS);
    return content;
  }

  private void editTransaction(
      Transaction transaction, TableView<Transaction> table, Label resultCount, YearMonth month) {
    dialog
        .show(transaction)
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
                refresh(table, resultCount, month);
              } catch (IllegalArgumentException | BudgetPersistenceException exception) {
                UiAlerts.error(exception);
              }
            });
  }

  private void deleteTransaction(
      Transaction transaction, TableView<Transaction> table, Label resultCount, YearMonth month) {
    if (UiAlerts.confirm("Delete transaction", "Delete this transaction?")) {
      service.deleteTransaction(transaction.id());
      refresh(table, resultCount, month);
    }
  }

  private VBox createFilters(YearMonth month, TableView<Transaction> table, Label resultCount) {
    TextField search = textField("Search description", "transaction-search");
    DatePicker startDate = datePicker("Start date", "transaction-start-date");
    DatePicker endDate = datePicker("End date", "transaction-end-date");
    ComboBox<TransactionType> type = typeFilter();
    ComboBox<CategoryOption> category = categoryFilter();
    TextField minimumAmount = textField("Min amount", "transaction-minimum-amount");
    TextField maximumAmount = textField("Max amount", "transaction-maximum-amount");
    coordinateTypeAndCategory(type, category);
    restoreAppliedValues(search, startDate, endDate, category, type, minimumAmount, maximumAmount);
    Label error = new Label();
    error.setId("transaction-filter-error");
    error.getStyleClass().add("validation-message");
    Button apply = new Button("Apply filters");
    apply.setId("apply-transaction-filters");
    apply.setOnAction(
        event -> {
          try {
            TransactionQuery candidate =
                queryFrom(search, startDate, endDate, category, type, minimumAmount, maximumAmount);
            service.transactions(month, candidate);
            appliedQuery = candidate;
            error.setText("");
            refresh(table, resultCount, month);
          } catch (IllegalArgumentException exception) {
            error.setText(exception.getMessage());
          }
        });
    Button clear = new Button("Clear filters");
    clear.setId("clear-transaction-filters");
    clear.setOnAction(
        event -> {
          appliedQuery = TransactionQuery.empty();
          search.clear();
          startDate.setValue(null);
          endDate.setValue(null);
          category.setValue(null);
          type.setValue(null);
          minimumAmount.clear();
          maximumAmount.clear();
          error.setText("");
          refresh(table, resultCount, month);
        });
    HBox primaryCriteria = new HBox(8, search, startDate, endDate);
    primaryCriteria.setId("transaction-filter-primary-group");
    HBox secondaryCriteria = new HBox(8, type, category, minimumAmount, maximumAmount);
    secondaryCriteria.setId("transaction-filter-secondary-group");
    FlowPane criteria = new FlowPane(8, 8, primaryCriteria, secondaryCriteria);
    criteria.setId("transaction-filter-criteria");
    HBox actions = new HBox(8, apply, clear);
    VBox container = new VBox(8, new Label("Search and filters"), criteria, actions, error);
    container.getStyleClass().add("transaction-filters");
    return container;
  }

  private ComboBox<CategoryOption> categoryFilter() {
    ComboBox<CategoryOption> category = new ComboBox<>();
    category.setId("transaction-category-filter");
    category.setPromptText("Expense category");
    category.setMinWidth(170);
    category.setPrefWidth(190);
    category.getItems().addAll(service.categories().stream().map(CategoryOption::new).toList());
    category.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(CategoryOption value) {
            return value == null ? "" : value.name();
          }

          @Override
          public CategoryOption fromString(String text) {
            return null;
          }
        });
    return category;
  }

  private ComboBox<TransactionType> typeFilter() {
    ComboBox<TransactionType> type = new ComboBox<>();
    type.setId("transaction-type-filter");
    type.setPromptText("Income or expense");
    type.setMinWidth(165);
    type.setPrefWidth(175);
    type.getItems().addAll(TransactionType.values());
    type.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(TransactionType value) {
            if (value == null) {
              return "";
            }
            return value == TransactionType.INCOME ? "Income" : "Expense";
          }

          @Override
          public TransactionType fromString(String text) {
            return null;
          }
        });
    return type;
  }

  private void coordinateTypeAndCategory(
      ComboBox<TransactionType> type, ComboBox<CategoryOption> category) {
    type.valueProperty()
        .addListener(
            (ignored, previous, selected) -> {
              if (selected == TransactionType.INCOME) {
                category.setValue(null);
              }
              setCategoryVisibility(category, selected != TransactionType.INCOME);
            });
    category
        .valueProperty()
        .addListener(
            (ignored, previous, selected) -> {
              if (selected != null && type.getValue() != TransactionType.EXPENSE) {
                type.setValue(TransactionType.EXPENSE);
              }
            });
    setCategoryVisibility(category, type.getValue() != TransactionType.INCOME);
  }

  private void setCategoryVisibility(ComboBox<CategoryOption> category, boolean visible) {
    category.setVisible(visible);
    category.setManaged(visible);
  }

  private void restoreAppliedValues(
      TextField search,
      DatePicker startDate,
      DatePicker endDate,
      ComboBox<CategoryOption> category,
      ComboBox<TransactionType> type,
      TextField minimumAmount,
      TextField maximumAmount) {
    search.setText(appliedQuery.description() == null ? "" : appliedQuery.description());
    startDate.setValue(appliedQuery.startDate());
    endDate.setValue(appliedQuery.endDate());
    category.setValue(
        appliedQuery.categoryId() == null
            ? null
            : category.getItems().stream()
                .filter(item -> item.id() == appliedQuery.categoryId())
                .findFirst()
                .orElse(null));
    type.setValue(appliedQuery.type());
    minimumAmount.setText(amountText(appliedQuery.minimumAmount()));
    maximumAmount.setText(amountText(appliedQuery.maximumAmount()));
  }

  private TransactionQuery queryFrom(
      TextField search,
      DatePicker startDate,
      DatePicker endDate,
      ComboBox<CategoryOption> category,
      ComboBox<TransactionType> type,
      TextField minimumAmount,
      TextField maximumAmount) {
    return new TransactionQuery(
        search.getText(),
        startDate.getValue(),
        endDate.getValue(),
        category.getValue() == null ? null : category.getValue().id(),
        type.getValue(),
        optionalAmount(minimumAmount.getText(), "Minimum amount"),
        optionalAmount(maximumAmount.getText(), "Maximum amount"));
  }

  private BigDecimal optionalAmount(String input, String label) {
    String normalized = input == null ? "" : input.trim();
    if (normalized.isEmpty()) {
      return null;
    }
    return MoneyInput.parse(normalized, label, true);
  }

  private void refresh(TableView<Transaction> table, Label resultCount, YearMonth month) {
    table.setItems(FXCollections.observableArrayList(service.transactions(month, appliedQuery)));
    updateResultCount(resultCount, table.getItems().size());
  }

  private void updateResultCount(Label resultCount, int count) {
    resultCount.setText(count + (count == 1 ? " transaction" : " transactions"));
  }

  private TextField textField(String prompt, String id) {
    TextField field = new TextField();
    field.setPromptText(prompt);
    field.setId(id);
    return field;
  }

  private DatePicker datePicker(String prompt, String id) {
    DatePicker picker = new DatePicker();
    picker.setPromptText(prompt);
    picker.setId(id);
    return picker;
  }

  private String amountText(BigDecimal amount) {
    return amount == null ? "" : amount.toPlainString();
  }

  private record CategoryOption(long id, String name) {
    private CategoryOption(budgetbot.model.Category category) {
      this(category.id(), category.name());
    }
  }
}
