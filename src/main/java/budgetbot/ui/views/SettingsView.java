package budgetbot.ui.views;

import budgetbot.model.BudgetSettings;
import budgetbot.persistence.BudgetPersistenceException;
import budgetbot.service.BudgetService;
import budgetbot.ui.UiAlerts;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.VBox;

/** Builds the global settings view and saves its changes through the budget service. */
public final class SettingsView {
  private final BudgetService service;

  /**
   * Creates the settings view builder.
   *
   * @param service service that supplies and saves settings
   */
  public SettingsView(BudgetService service) {
    this.service = service;
  }

  /**
   * Builds the global settings content.
   *
   * @return settings content
   */
  public VBox build() {
    BudgetSettings current = service.settings();
    Spinner<Integer> threshold = new Spinner<>();
    threshold.setValueFactory(
        new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, current.warningThreshold()));
    Button save = new Button("Save settings");
    save.setOnAction(
        event -> {
          try {
            service.saveSettings(threshold.getValue());
            UiAlerts.information(
                "Settings saved",
                "New settings will apply to the next month started in BudgetBot.");
          } catch (IllegalArgumentException | BudgetPersistenceException exception) {
            UiAlerts.error(exception);
          }
        });
    VBox content =
        new VBox(16, new Label("Settings"), new Label("Warning threshold (%)"), threshold, save);
    content.setPadding(new javafx.geometry.Insets(28));
    return content;
  }
}
