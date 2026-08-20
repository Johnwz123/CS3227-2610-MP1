package budgetbot.ui.dialogs;

import budgetbot.ui.MoneyInput;
import java.math.BigDecimal;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Creates the validated dialog used to set a monthly category budget. */
public final class BudgetDialog {
  /**
   * Opens a validated monetary input dialog.
   *
   * @param title dialog title
   * @param prompt input label and validation name
   * @param zeroAllowed whether zero is permitted
   * @return parsed amount when saved
   */
  public Optional<BigDecimal> show(String title, String prompt, boolean zeroAllowed) {
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
            MoneyInput.parse(input.getText(), prompt, zeroAllowed);
            validation.setText("");
          } catch (IllegalArgumentException exception) {
            validation.setText(exception.getMessage());
            event.consume();
          }
        });
    dialog.setResultConverter(
        button -> button == save ? MoneyInput.parse(input.getText(), prompt, zeroAllowed) : null);
    return dialog.showAndWait();
  }

  private static Label validationMessage() {
    Label validation = new Label();
    validation.getStyleClass().add("validation-message");
    validation.setWrapText(true);
    return validation;
  }
}
