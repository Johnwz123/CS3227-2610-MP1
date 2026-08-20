package budgetbot.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Formats and validates monetary values entered through the JavaFX user interface. */
public final class MoneyInput {
  private MoneyInput() {
    throw new AssertionError("Utility class");
  }

  /**
   * Formats an amount as a two-decimal-place dollar value.
   *
   * @param amount value to format
   * @return formatted dollar value
   */
  public static String format(BigDecimal amount) {
    return "$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  /**
   * Parses a non-negative or positive monetary value entered in the UI.
   *
   * @param input text entered by the user
   * @param label field name to include in validation messages
   * @param zeroAllowed whether zero is permitted
   * @return parsed amount with at most two decimal places
   * @throws IllegalArgumentException if the input is not a permitted monetary value
   */
  public static BigDecimal parse(String input, String label, boolean zeroAllowed) {
    String normalized = input == null ? "" : input.trim();
    if (!normalized.matches("\\d+(?:\\.\\d{1,2})?")) {
      throw new IllegalArgumentException(
          label + " must be a number with at most two decimal places.");
    }
    BigDecimal amount = new BigDecimal(normalized);
    if (zeroAllowed ? amount.signum() < 0 : amount.signum() <= 0) {
      throw new IllegalArgumentException(
          label + (zeroAllowed ? " must be zero or greater." : " must be greater than zero."));
    }
    return amount;
  }
}
