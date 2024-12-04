package dev.cwby.jasonify.test;

import dev.cwby.jasonify.annotation.Json;

@Json
public class TestClass {

  private String name;
  private int age;
  private String[] names = new String[]{"teste", "macaco", "pelado"};

  public TestClass(String name, int age) {
    this.name = name;
    this.age = age;
  }

  public String getName() {
    return name;
  }

  public int getAge() {
    return age;
  }

  public String[] getNames() {
    return names;
  }
}
