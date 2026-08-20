package budgetbot.ui.dialogs;

import budgetbot.model.Category;
import budgetbot.model.Transaction;
import budgetbot.model.TransactionType;
import budgetbot.service.BudgetService;
import budgetbot.ui.MoneyInput;
import java.time.LocalDate;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** Creates the validated dialog used to add or edit a transaction. */
public final class TransactionDialog {
  private static final String AMOUNT_LABEL = "Amount";

  private final BudgetService service;

  /**
   * Creates a transaction dialog backed by the application's categories.
   *
   * @param service service supplying current categories
   */
  public TransactionDialog(BudgetService service) {
    this.service = service;
  }

  /**
   * Opens the transaction dialog.
   *
   * @param existing transaction to edit, or {@code null} for a new transaction
   * @return a validated transaction when saved
   */
  public Optional<Transaction> show(Transaction existing) {
    Dialog<Transaction> dialog = new Dialog<>();
    dialog.setTitle(existing == null ? "Add transaction" : "Edit transaction");
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
    configureCategoryPicker(category);
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
            MoneyInput.parse(amount.getText(), AMOUNT_LABEL, false);
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
              MoneyInput.parse(amount.getText(), AMOUNT_LABEL, false),
              date.getValue(),
              description.getText(),
              category.getValue() == null ? null : category.getValue().id());
        });
    return dialog.showAndWait();
  }

  private static void configureCategoryPicker(ComboBox<Category> category) {
    category.setCellFactory(ignored -> new CategoryCell());
    category.setButtonCell(new CategoryCell());
  }

  private static Label validationMessage() {
    Label validation = new Label();
    validation.getStyleClass().add("validation-message");
    validation.setWrapText(true);
    return validation;
  }

  private static final class CategoryCell extends ListCell<Category> {
    @Override
    protected void updateItem(Category item, boolean empty) {
      super.updateItem(item, empty);
      setText(empty || item == null ? null : item.name());
    }
  }
}
