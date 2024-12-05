package dev.cwby.jasonify;

import dev.cwby.jasonify.exception.AppendJsonException;
import dev.cwby.jasonify.test.TestClass;
import dev.cwby.jasonify.writer.JsonGenerator;

public class Main {

  public static void main(String[] args) throws AppendJsonException {
    TestClass testClass = new TestClass();
    JsonGenerator jsonGenerator = new JsonGenerator(new StringBuilder());
    jsonGenerator.writeStartObject();

    jsonGenerator.writeField("jsonBooleanTrue").writeBoolean(true);
    jsonGenerator.writeField("jsonBooleanFalse").writeBoolean(false);

    jsonGenerator.writeField("jsonInteger").writeNumber(42);
    jsonGenerator.writeField("jsonFloat").writeNumber(3.14159);
    jsonGenerator.writeField("jsonScientific").writeNumber(6.022e23);

    jsonGenerator.writeField("jsonString").writeString("Hello, JSON!");
    jsonGenerator.writeField("jsonEmptyString").writeString("");
    jsonGenerator.writeField("jsonEscapedString").writeString("Line\\nLine2");

    String[] names = new String[] {"teste", "test2", "teste3"};
    jsonGenerator.writeField("names");
    jsonGenerator.writeStartArray();
    for (String v : names) {
      jsonGenerator.writeString(v);
    }
    jsonGenerator.writeEndArray();
    jsonGenerator.writeField("test").writeString("Hello, JSON!");

    jsonGenerator.writeField("testeObject");
    jsonGenerator.writeStartObject();
    jsonGenerator.writeEndObject();

    jsonGenerator.writeField("teste2Class");
    jsonGenerator.writeStartObject();
    jsonGenerator.writeField("teste");
    jsonGenerator.writeString("limão");
    jsonGenerator.writeField("htehut");
    jsonGenerator.writeString("testte");
    jsonGenerator.writeField("age");
    jsonGenerator.writeNumber(10);
    jsonGenerator.writeEndObject();

    jsonGenerator.writeEndObject();
    System.out.println(jsonGenerator.getJson());
  }
}
