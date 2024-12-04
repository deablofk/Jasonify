package dev.cwby.jasonify.test;

import dev.cwby.jasonify.exception.AppendJsonException;
import dev.cwby.jasonify.serializer.IJsonSerializer;
import dev.cwby.jasonify.writer.JsonGenerator;

import java.io.StringWriter;

public class TestSerializer implements IJsonSerializer<TestClass> {

  @Override
  public String toJson(TestClass testClass) {
    JsonGenerator jsonGenerator = new JsonGenerator(new StringWriter());
    try {
      jsonGenerator
          .writeStartObject()
          .writeField("name")
          .writeString(testClass.getName())
          .writeField("age")
          .writeNumber(testClass.getAge())
          .writeEndObject();
    } catch (AppendJsonException e) {
      throw new RuntimeException(e);
    }
    return jsonGenerator.getJson();
  }
}
