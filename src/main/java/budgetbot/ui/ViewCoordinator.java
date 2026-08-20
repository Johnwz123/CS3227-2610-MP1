package budgetbot.ui;

import budgetbot.service.BudgetService;
import budgetbot.ui.dialogs.BudgetDialog;
import budgetbot.ui.dialogs.CategoryDialog;
import budgetbot.ui.dialogs.TransactionDialog;
import budgetbot.ui.tables.BudgetSummaryTableFactory;
import budgetbot.ui.tables.TransactionTableFactory;
import budgetbot.ui.views.BudgetsView;
import budgetbot.ui.views.DashboardView;
import budgetbot.ui.views.SettingsView;
import budgetbot.ui.views.TransactionsView;
import java.time.YearMonth;
import javafx.scene.layout.BorderPane;

/** Owns the selected month, active view, and content rerendering for the main window. */
final class ViewCoordinator {
  private final BorderPane layout;
  private final DashboardView dashboardView;
  private final TransactionsView transactionsView;
  private final BudgetsView budgetsView;
  private final SettingsView settingsView;
  private View activeView = View.DASHBOARD;
  private YearMonth selectedMonth = YearMonth.now();

  /**
   * Creates the coordinator and its focused view collaborators.
   *
   * @param layout layout whose center displays the active view
   * @param service service that supplies and changes budget data
   */
  ViewCoordinator(BorderPane layout, BudgetService service) {
    this.layout = layout;
    TransactionTableFactory transactionTables = new TransactionTableFactory(service);
    BudgetSummaryTableFactory budgetTables = new BudgetSummaryTableFactory();
    TransactionDialog transactionDialog = new TransactionDialog(service);
    CategoryDialog categoryDialog = new CategoryDialog(service);
    BudgetDialog budgetDialog = new BudgetDialog();
    dashboardView = new DashboardView(service, budgetTables, this::changeMonth);
    transactionsView =
        new TransactionsView(service, transactionTables, transactionDialog, this::changeMonth);
    budgetsView =
        new BudgetsView(service, budgetTables, categoryDialog, budgetDialog, this::changeMonth);
    settingsView = new SettingsView(service);
  }

  void showDashboard() {
    show(View.DASHBOARD);
  }

  void showTransactions() {
    show(View.TRANSACTIONS);
  }

  void showBudgets() {
    show(View.BUDGETS);
  }

  void showSettings() {
    show(View.SETTINGS);
  }

  private void show(View view) {
    activeView = view;
    renderActiveView();
  }

  private void changeMonth(int offset) {
    selectedMonth = selectedMonth.plusMonths(offset);
    renderActiveView();
  }

  private void renderActiveView() {
    layout.setCenter(
        switch (activeView) {
          case DASHBOARD -> dashboardView.build(selectedMonth);
          case TRANSACTIONS -> transactionsView.build(selectedMonth);
          case BUDGETS -> budgetsView.build(selectedMonth);
          case SETTINGS -> settingsView.build();
        });
  }

  private enum View {
    DASHBOARD,
    TRANSACTIONS,
    BUDGETS,
    SETTINGS
  }
}
