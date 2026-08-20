package budgetbot.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/** Displays the shared information, confirmation, and error alerts used by BudgetBot. */
public final class UiAlerts {
  private UiAlerts() {
    throw new AssertionError("Utility class");
  }

  /**
   * Displays a confirmation alert.
   *
   * @param title alert title
   * @param message alert content
   * @return whether the user confirmed the action
   */
  public static boolean confirm(String title, String message) {
    Alert alert =
        new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
    alert.setTitle(title);
    return alert.showAndWait().filter(button -> button == ButtonType.OK).isPresent();
  }

  /**
   * Displays an informational alert.
   *
   * @param title alert title
   * @param message alert content
   */
  public static void information(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.showAndWait();
  }

  /**
   * Displays a persistence or validation error.
   *
   * @param exception error to display
   */
  public static void error(Exception exception) {
    Alert alert = new Alert(Alert.AlertType.ERROR, exception.getMessage(), ButtonType.OK);
    alert.setTitle("BudgetBot could not save your change");
    alert.setHeaderText(null);
    alert.showAndWait();
  }
}
