package dev.cwby.jasonify.test;

import dev.cwby.jasonify.annotation.Json;

@Json
public class TestClass {
  public Teste2Class teste2Class = new Teste2Class();

  // JSON boolean values
  private boolean jsonBooleanTrue = true;
  private boolean jsonBooleanFalse = false;

  // JSON number values (integers, floating-point, scientific notation)
  private int jsonInteger = 42;
  private double jsonFloat = 3.14159;
  private double jsonScientific = 6.022e23;

  // JSON string values
  private String jsonString = "Hello, JSON!";
  private String jsonEmptyString = "";
  private String jsonEscapedString = "Line1\\nLine2";

  public boolean getJsonBooleanTrue() {
    return jsonBooleanTrue;
  }

  public boolean getJsonBooleanFalse() {
    return jsonBooleanFalse;
  }

  public int getJsonInteger() {
    return jsonInteger;
  }

  public double getJsonFloat() {
    return jsonFloat;
  }

  public double getJsonScientific() {
    return jsonScientific;
  }

  public String getJsonString() {
    return jsonString;
  }

  public String getJsonEmptyString() {
    return jsonEmptyString;
  }

  public String getJsonEscapedString() {
    return jsonEscapedString;
  }
}
