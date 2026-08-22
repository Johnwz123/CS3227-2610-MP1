package budgetbot.ui.dialogs;

import budgetbot.ui.MoneyInput;
import java.math.BigDecimal;
import java.util.Optional;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

/** Creates the validated dialog used to set a monthly category budget. */
public final class BudgetDialog {
  private static final double CONTENT_WIDTH = 320;

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
    input.setPrefWidth(CONTENT_WIDTH);
    ValidationMessage validation = new ValidationMessage();
    VBox content = new VBox(8, new Label(prompt), input, validation);
    content.setPrefWidth(CONTENT_WIDTH);
    dialog.getDialogPane().setContent(content);
    Button saveButton = (Button) dialog.getDialogPane().lookupButton(save);
    saveButton.addEventFilter(
        ActionEvent.ACTION,
        event -> {
          try {
            MoneyInput.parse(input.getText(), prompt, zeroAllowed);
            validation.setMessage("");
          } catch (IllegalArgumentException exception) {
            validation.setMessage(exception.getMessage());
            resizeToContent(dialog);
            event.consume();
          }
        });
    dialog.setResultConverter(
        button -> button == save ? MoneyInput.parse(input.getText(), prompt, zeroAllowed) : null);
    return dialog.showAndWait();
  }

  private static void resizeToContent(Dialog<?> dialog) {
    Platform.runLater(
        () -> {
          dialog.getDialogPane().requestLayout();
          ((Stage) dialog.getDialogPane().getScene().getWindow()).sizeToScene();
        });
  }

  private static final class ValidationMessage extends TextFlow {
    private final Text text = new Text();

    private ValidationMessage() {
      getChildren().add(text);
      text.getStyleClass().add("validation-message");
      setId("budget-validation-message");
      setMinWidth(0);
      setPrefWidth(CONTENT_WIDTH);
      setMaxWidth(CONTENT_WIDTH);
    }

    private void setMessage(String message) {
      text.setText(message);
    }
  }
}
