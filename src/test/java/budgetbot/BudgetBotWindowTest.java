package budgetbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import budgetbot.model.BudgetState;
import budgetbot.model.Category;
import budgetbot.model.CategorySummary;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BudgetBotWindowTest {
  @Test
  void parsesPositiveAmountsAndAllowsZeroOnlyWhenRequested() {
    assertEquals(new BigDecimal("12.50"), BudgetBotWindow.parseMoney(" 12.50 ", "Amount", false));
    assertEquals(BigDecimal.ZERO, BudgetBotWindow.parseMoney("0", "Budget", true));
  }

  @Test
  void rejectsInvalidOrDisallowedMonetaryInput() {
    assertThrows(
        IllegalArgumentException.class, () -> BudgetBotWindow.parseMoney("1.001", "Amount", false));
    assertThrows(
        IllegalArgumentException.class, () -> BudgetBotWindow.parseMoney("-1", "Amount", true));
    assertThrows(
        IllegalArgumentException.class, () -> BudgetBotWindow.parseMoney("0", "Amount", false));
    assertThrows(
        IllegalArgumentException.class, () -> BudgetBotWindow.parseMoney(null, "Amount", false));
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

    assertEquals("$12.35", BudgetBotWindow.money(new BigDecimal("12.345")));
    assertEquals("normal-remaining", BudgetBotWindow.remainingStyleClass(normal));
    assertEquals("warning-remaining", BudgetBotWindow.remainingStyleClass(warning));
    assertEquals("over-budget-remaining", BudgetBotWindow.remainingStyleClass(overBudget));
  }
}
