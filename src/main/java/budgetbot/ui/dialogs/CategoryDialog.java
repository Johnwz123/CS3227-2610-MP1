package budgetbot.ui.dialogs;

import budgetbot.model.Category;
import budgetbot.service.BudgetService;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Creates the dialogs used to add, rename, and remove categories. */
public final class CategoryDialog {
  private final BudgetService service;

  /**
   * Creates a category dialog backed by the application's categories.
   *
   * @param service service supplying current categories
   */
  public CategoryDialog(BudgetService service) {
    this.service = service;
  }

  /**
   * Opens a required-text category dialog.
   *
   * @param title dialog title
   * @param prompt input label and validation name
   * @param value initial text
   * @return entered text when saved
   */
  public Optional<String> text(String title, String prompt, String value) {
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

  /**
   * Opens the replacement-category dialog used before a category is removed.
   *
   * @param category category to remove
   * @return selected replacement category when saved
   */
  public Optional<Category> replacement(Category category) {
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
    return dialog.showAndWait();
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
