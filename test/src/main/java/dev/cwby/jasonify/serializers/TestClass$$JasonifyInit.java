package dev.cwby.jasonify.serializers;

import dev.cwby.jasonify.serializer.IJsonSerializer;
import dev.cwby.jasonify.test.TestClass;
import dev.cwby.jasonify.writer.JsonGenerator;
import java.io.StringWriter;
import java.lang.Override;
import java.lang.String;

public class TestClass$$JasonifyInit implements IJsonSerializer<TestClass> {
  @Override
  public String toJson(final TestClass testClass) {
    JsonGenerator jg = new JsonGenerator(new StringWriter());
    try {
      jg.writeStartObject();
      jg.writeField("name");
      jg.writeString(testClass.getName());
      jg.writeField("age");
      jg.writeNumber(testClass.getAge());
      jg.writeField("names");
      jg.writeStartArray();
      for(java.lang.String v : testClass.getNames()) {
        jg.writeString(v);
      }
      jg.writeEndArray();
      jg.writeEndObject();
    } catch (Exception e) {
      e.printStackTrace();}
    return jg.getJson();
  }
}
