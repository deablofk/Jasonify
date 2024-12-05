package dev.cwby.jasonify;

import dev.cwby.jasonify.test.TestClass;

public class Main {

  public static void main(String[] args) {
    TestClass testClass = new TestClass();
    for (int i = 0; i < 100_000; i++) {
      SerializerManager.toJson(testClass);
    }
  }
}
