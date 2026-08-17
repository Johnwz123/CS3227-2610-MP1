package budgetbot.model;

import java.util.Objects;

/** An expense grouping chosen by the user. */
public record Category(long id, String name) {
  public Category {
    Objects.requireNonNull(name, "name");
  }
}
