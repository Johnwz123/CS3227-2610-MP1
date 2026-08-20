package budgetbot.ui.views;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.function.IntConsumer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** Creates the reusable previous/next controls for a selected budget month. */
final class MonthControls {
  private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM uuuu");

  private MonthControls() {
    throw new AssertionError("Utility class");
  }

  /**
   * Creates controls that report month offsets to their owner.
   *
   * @param selectedMonth month currently displayed
   * @param changeMonth receiver for a negative or positive one-month offset
   * @return configured controls
   */
  static HBox create(YearMonth selectedMonth, IntConsumer changeMonth) {
    Button previous = new Button("<");
    Button next = new Button(">");
    Label month = new Label(selectedMonth.format(MONTH_FORMAT));
    month.getStyleClass().add("section-title");
    previous.setOnAction(event -> changeMonth.accept(-1));
    next.setOnAction(event -> changeMonth.accept(1));
    HBox controls = new HBox(10, previous, month, next);
    controls.setAlignment(Pos.CENTER_LEFT);
    return controls;
  }
}
