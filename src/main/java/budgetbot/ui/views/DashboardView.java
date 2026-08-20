package budgetbot.ui.views;

import budgetbot.model.CategorySummary;
import budgetbot.model.DashboardSnapshot;
import budgetbot.service.BudgetService;
import budgetbot.ui.MoneyInput;
import budgetbot.ui.tables.BudgetSummaryTableFactory;
import java.time.YearMonth;
import java.util.function.IntConsumer;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Builds the read-only dashboard for a selected month. */
public final class DashboardView {
  private final BudgetService service;
  private final BudgetSummaryTableFactory budgetTables;
  private final IntConsumer changeMonth;

  /**
   * Creates the dashboard builder.
   *
   * @param service service supplying dashboard data
   * @param budgetTables factory for category summary tables
   * @param changeMonth receiver for month-navigation offsets
   */
  public DashboardView(
      BudgetService service, BudgetSummaryTableFactory budgetTables, IntConsumer changeMonth) {
    this.service = service;
    this.budgetTables = budgetTables;
    this.changeMonth = changeMonth;
  }

  /**
   * Builds dashboard content for a month.
   *
   * @param month selected budget month
   * @return dashboard content
   */
  public VBox build(YearMonth month) {
    DashboardSnapshot snapshot = service.dashboard(month);
    Label balance = new Label("Net cash flow: " + MoneyInput.format(snapshot.netCashFlow()));
    balance.getStyleClass().add("balance");
    TableView<CategorySummary> budgets = budgetTables.create(snapshot.categorySummaries());
    VBox budgetSection = tableSection("Budget status", budgets);
    VBox content = new VBox(18, MonthControls.create(month, changeMonth), balance, budgetSection);
    content.setPadding(new javafx.geometry.Insets(28));
    VBox.setVgrow(budgetSection, Priority.ALWAYS);
    return content;
  }

  private static VBox tableSection(String title, TableView<?> table) {
    Label heading = new Label(title);
    heading.getStyleClass().add("section-title");
    VBox section = new VBox(8, heading, table);
    VBox.setVgrow(table, Priority.ALWAYS);
    return section;
  }
}
