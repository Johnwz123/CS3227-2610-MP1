package edu.nus.cs3227.mp1;

/** Entry point for the CS3227 MP1 application. */
public final class HelloWorldApp {
  /** Creates the starter application. */
  public HelloWorldApp() {
    // Default construction is intentionally supported for the starter application.
  }

  /**
   * Returns the starter greeting.
   *
   * @return the greeting displayed by the command-line application
   */
  public static String greeting() {
    return "Hello, World!";
  }

  /**
   * Runs the command-line application.
   *
   * @param args command-line arguments, which are currently unused
   */
  public static void main(String[] args) {
    System.out.println(greeting()); // NOPMD - command-line application output
  }
}
