package budgetbot.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** A dated income or expense entry. */
public record Transaction(
    long id,
    TransactionType type,
    BigDecimal amount,
    LocalDate date,
    String description,
    Long categoryId) {
  public Transaction {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(date, "date");
    description = Objects.requireNonNullElse(description, "");
  }
}
