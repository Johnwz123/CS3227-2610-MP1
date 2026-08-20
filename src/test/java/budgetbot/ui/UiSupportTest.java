package budgetbot.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import budgetbot.model.BudgetState;
import budgetbot.model.Category;
import budgetbot.model.CategorySummary;
import budgetbot.ui.tables.BudgetSummaryTableFactory;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class UiSupportTest {
  @Test
  void parsesPositiveAmountsAndAllowsZeroOnlyWhenRequested() {
    assertEquals(new BigDecimal("12.50"), MoneyInput.parse(" 12.50 ", "Amount", false));
    assertEquals(BigDecimal.ZERO, MoneyInput.parse("0", "Budget", true));
  }

  @Test
  void rejectsInvalidOrDisallowedMonetaryInput() {
    assertThrows(IllegalArgumentException.class, () -> MoneyInput.parse("1.001", "Amount", false));
    assertThrows(IllegalArgumentException.class, () -> MoneyInput.parse("-1", "Amount", true));
    assertThrows(IllegalArgumentException.class, () -> MoneyInput.parse("0", "Amount", false));
    assertThrows(IllegalArgumentException.class, () -> MoneyInput.parse(null, "Amount", false));
  }

  @Test
  void formatsMoneyAndSelectsStylesForEveryBudgetState() {
    CategorySummary normal =
        new CategorySummary(
            new Category(1, "Groceries"),
            BigDecimal.ZERO,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BudgetState.NORMAL);
    CategorySummary warning =
        new CategorySummary(
            new Category(1, "Groceries"),
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ZERO,
            BudgetState.WARNING);
    CategorySummary overBudget =
        new CategorySummary(
            new Category(1, "Groceries"),
            BigDecimal.TEN,
            BigDecimal.ONE,
            BigDecimal.ONE.negate(),
            BudgetState.OVER_BUDGET);

    assertEquals("$12.35", MoneyInput.format(new BigDecimal("12.345")));
    assertEquals("normal-remaining", BudgetSummaryTableFactory.remainingStyleClass(normal));
    assertEquals("warning-remaining", BudgetSummaryTableFactory.remainingStyleClass(warning));
    assertEquals(
        "over-budget-remaining", BudgetSummaryTableFactory.remainingStyleClass(overBudget));
  }
}
