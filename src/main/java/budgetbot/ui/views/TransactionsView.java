package budgetbot.ui.views;

import budgetbot.model.Transaction;
import budgetbot.persistence.BudgetPersistenceException;
import budgetbot.service.BudgetService;
import budgetbot.ui.UiAlerts;
import budgetbot.ui.dialogs.TransactionDialog;
import budgetbot.ui.tables.TransactionTableFactory;
import java.time.YearMonth;
import java.util.function.IntConsumer;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Builds the transaction-management view and handles its user actions. */
public final class TransactionsView {
  private final BudgetService service;
  private final TransactionTableFactory tables;
  private final TransactionDialog dialog;
  private final IntConsumer changeMonth;

  /**
   * Creates the transaction view builder.
   *
   * @param service service that supplies and changes transaction data
   * @param tables factory for transaction tables
   * @param dialog dialog used for transaction input
   * @param changeMonth receiver for month-navigation offsets
   */
  public TransactionsView(
      BudgetService service,
      TransactionTableFactory tables,
      TransactionDialog dialog,
      IntConsumer changeMonth) {
    this.service = service;
    this.tables = tables;
    this.dialog = dialog;
    this.changeMonth = changeMonth;
  }

  /**
   * Builds transaction-management content for a month.
   *
   * @param month selected budget month
   * @return transaction-management content
   */
  public VBox build(YearMonth month) {
    TableView<Transaction> transactions = tables.create(service.transactions(month));
    tables.addActions(
        transactions,
        transaction -> editTransaction(transaction, transactions, month),
        transaction -> deleteTransaction(transaction, transactions, month));
    Button add = new Button("Add transaction");
    add.setOnAction(event -> editTransaction(null, transactions, month));
    VBox content =
        new VBox(
            16,
            MonthControls.create(month, changeMonth),
            new Label("Transactions"),
            transactions,
            new HBox(10, add));
    content.setPadding(new javafx.geometry.Insets(28));
    VBox.setVgrow(transactions, Priority.ALWAYS);
    return content;
  }

  private void editTransaction(
      Transaction transaction, TableView<Transaction> table, YearMonth month) {
    dialog
        .show(transaction)
        .ifPresent(
            saved -> {
              try {
                if (saved.id() == 0) {
                  service.addTransaction(
                      saved.type(),
                      saved.amount(),
                      saved.date(),
                      saved.description(),
                      saved.categoryId());
                } else {
                  service.updateTransaction(saved);
                }
                refresh(table, month);
              } catch (IllegalArgumentException | BudgetPersistenceException exception) {
                UiAlerts.error(exception);
              }
            });
  }

  private void deleteTransaction(
      Transaction transaction, TableView<Transaction> table, YearMonth month) {
    if (UiAlerts.confirm("Delete transaction", "Delete this transaction?")) {
      service.deleteTransaction(transaction.id());
      refresh(table, month);
    }
  }

  private void refresh(TableView<Transaction> table, YearMonth month) {
    table.setItems(FXCollections.observableArrayList(service.transactions(month)));
  }
}
