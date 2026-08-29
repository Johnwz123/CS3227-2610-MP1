package budgetbot;

/**
 * Executable JAR entry point that delegates to the JavaFX application.
 *
 * <p>The Java launcher must start a class that does not extend {@link
 * javafx.application.Application} when JavaFX is bundled on the classpath.
 */
public final class BudgetBotLauncher {
  private BudgetBotLauncher() {
    throw new AssertionError("Utility class");
  }

  /**
   * Starts BudgetBot.
   *
   * @param arguments command-line arguments forwarded to the JavaFX application
   */
  public static void main(String[] arguments) {
    BudgetBotApp.main(arguments);
  }
}
