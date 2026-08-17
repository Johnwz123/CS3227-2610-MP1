package edu.nus.cs3227.mp1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class HelloWorldAppTest {
  @Test
  void greetingReturnsHelloWorld() {
    new HelloWorldApp();

    assertEquals("Hello, World!", HelloWorldApp.greeting());
  }

  @Test
  void mainWritesGreetingToStandardOutput() {
    PrintStream originalOutput = System.out;
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    try (PrintStream capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8)) {
      System.setOut(capturedOutput);
      HelloWorldApp.main(new String[0]);
    } finally {
      System.setOut(originalOutput);
    }

    assertEquals("Hello, World!" + System.lineSeparator(), output.toString(StandardCharsets.UTF_8));
  }
}
