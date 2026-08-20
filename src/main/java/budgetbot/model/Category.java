package budgetbot.model;

import java.util.Objects;

/**
 * An expense grouping chosen by the user.
 *
 * @param id persistent category identifier
 * @param name display name of the category
 */
public record Category(long id, String name) {
  /**
   * Creates a category with a required display name.
   *
   * @throws NullPointerException if {@code name} is null
   */
  public Category {
    Objects.requireNonNull(name, "name");
  }
}
