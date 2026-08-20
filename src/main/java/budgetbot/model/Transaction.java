package budgetbot.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A dated income or expense entry.
 *
 * @param id persistent transaction identifier, or zero before persistence
 * @param type whether the amount is income or an expense
 * @param amount positive monetary amount
 * @param date date on which the transaction occurred
 * @param description optional user-entered description
 * @param categoryId expense-category identifier, or {@code null} for income
 */
public record Transaction(
    long id,
    TransactionType type,
    BigDecimal amount,
    LocalDate date,
    String description,
    Long categoryId) {
  /**
   * Creates a transaction with required type, amount, and date values.
   *
   * @throws NullPointerException if {@code type}, {@code amount}, or {@code date} is null
   */
  public Transaction {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(date, "date");
    description = Objects.requireNonNullElse(description, "");
  }
}
