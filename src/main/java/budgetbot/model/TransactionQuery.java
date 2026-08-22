package budgetbot.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Optional criteria used to retrieve a subset of transaction history.
 *
 * @param description case-insensitive description substring, or {@code null} when absent
 * @param startDate inclusive earliest transaction date, or {@code null} when unbounded
 * @param endDate inclusive latest transaction date, or {@code null} when unbounded
 * @param categoryId expense category identifier, or {@code null} when absent
 * @param type transaction type, or {@code null} when absent
 * @param minimumAmount inclusive lowest transaction amount, or {@code null} when unbounded
 * @param maximumAmount inclusive highest transaction amount, or {@code null} when unbounded
 */
public record TransactionQuery(
    String description,
    LocalDate startDate,
    LocalDate endDate,
    Long categoryId,
    TransactionType type,
    BigDecimal minimumAmount,
    BigDecimal maximumAmount) {
  /**
   * Returns a query without active criteria.
   *
   * @return an unfiltered transaction query
   */
  public static TransactionQuery empty() {
    return new TransactionQuery(null, null, null, null, null, null, null);
  }
}
