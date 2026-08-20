package budgetbot.ui.views;

import budgetbot.model.Category;
import budgetbot.model.CategorySummary;
import budgetbot.persistence.BudgetPersistenceException;
import budgetbot.service.BudgetService;
import budgetbot.ui.UiAlerts;
import budgetbot.ui.dialogs.BudgetDialog;
import budgetbot.ui.dialogs.CategoryDialog;
import budgetbot.ui.tables.BudgetSummaryTableFactory;
import java.time.YearMonth;
import java.util.function.IntConsumer;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Builds the category and budget-management view and handles its user actions. */
public final class BudgetsView {
  private final BudgetService service;
  private final BudgetSummaryTableFactory tables;
  private final CategoryDialog categoryDialog;
  private final BudgetDialog budgetDialog;
  private final IntConsumer changeMonth;

  /**
   * Creates the budget-management view builder.
   *
   * @param service service that supplies and changes category data
   * @param tables factory for category summary tables
   * @param categoryDialog dialog used for category input
   * @param budgetDialog dialog used for budget input
   * @param changeMonth receiver for month-navigation offsets
   */
  public BudgetsView(
      BudgetService service,
      BudgetSummaryTableFactory tables,
      CategoryDialog categoryDialog,
      BudgetDialog budgetDialog,
      IntConsumer changeMonth) {
    this.service = service;
    this.tables = tables;
    this.categoryDialog = categoryDialog;
    this.budgetDialog = budgetDialog;
    this.changeMonth = changeMonth;
  }

  /**
   * Builds budget-management content for a month.
   *
   * @param month selected budget month
   * @return budget-management content
   */
  public VBox build(YearMonth month) {
    TableView<CategorySummary> budgets =
        tables.create(service.dashboard(month).categorySummaries());
    tables.addActions(
        budgets,
        category -> saveBudget(category, budgets, month),
        category -> renameCategory(category, budgets, month),
        category -> removeCategory(category, budgets, month));
    Button add = new Button("Add");
    add.setOnAction(event -> addCategory(budgets, month));
    VBox content =
        new VBox(
            16,
            MonthControls.create(month, changeMonth),
            new Label("Budgets"),
            budgets,
            new HBox(10, add));
    content.setPadding(new javafx.geometry.Insets(28));
    VBox.setVgrow(budgets, Priority.ALWAYS);
    return content;
  }

  private void addCategory(TableView<CategorySummary> table, YearMonth month) {
    categoryDialog
        .text("Add category", "Category name", "")
        .ifPresent(
            name -> {
              try {
                service.addCategory(name);
                refresh(table, month);
              } catch (IllegalArgumentException | BudgetPersistenceException exception) {
                UiAlerts.error(exception);
              }
            });
  }

  private void renameCategory(
      Category category, TableView<CategorySummary> table, YearMonth month) {
    categoryDialog
        .text("Rename category", "New category name", category.name())
        .ifPresent(
            name -> {
              try {
                service.renameCategory(category.id(), name);
                refresh(table, month);
              } catch (IllegalArgumentException | BudgetPersistenceException exception) {
                UiAlerts.error(exception);
              }
            });
  }

  private void removeCategory(
      Category category, TableView<CategorySummary> table, YearMonth month) {
    categoryDialog
        .replacement(category)
        .ifPresent(
            replacement -> {
              try {
                service.removeCategory(category.id(), replacement.id());
                refresh(table, month);
              } catch (IllegalArgumentException | BudgetPersistenceException exception) {
                UiAlerts.error(exception);
              }
            });
  }

  private void saveBudget(Category category, TableView<CategorySummary> table, YearMonth month) {
    budgetDialog
        .show("Set " + category.name() + " budget", "Monthly base amount", true)
        .ifPresent(
            amount -> {
              try {
                service.setMonthlyBudget(category.id(), month, amount);
                refresh(table, month);
              } catch (IllegalArgumentException | BudgetPersistenceException exception) {
                UiAlerts.error(exception);
              }
            });
  }

  private void refresh(TableView<CategorySummary> table, YearMonth month) {
    table.setItems(FXCollections.observableArrayList(service.dashboard(month).categorySummaries()));
  }
}
