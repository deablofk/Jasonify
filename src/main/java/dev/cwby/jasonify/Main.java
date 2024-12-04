package dev.cwby.jasonify;

import dev.cwby.jasonify.exception.AppendJsonException;
import dev.cwby.jasonify.writer.JsonGenerator;

import java.io.StringWriter;

public class Main {
  public static void main(String[] args) {
    JsonGenerator jsonGenerator = new JsonGenerator(new StringWriter());
    try {
      jsonGenerator
          .writeStartObject()
          .writeField("name")
          .writeString("josue")
          .writeField("age")
          .writeNumber(10)
          .writeField("childs")
          .writeBoolean(false)
          .writeField("sex")
          .writeString("man")
          .writeField("listTest")
          .writeStartArray()
          .writeStartObject()
          .writeField("teste")
          .writeString("value")
          .writeEndObject()
          .writeStartObject()
          .writeField("teste")
          .writeString("value2")
          .writeEndObject()
          .writeEndArray()
          .writeEndObject();

      System.out.println(jsonGenerator.getJson());
    } catch (AppendJsonException e) {
      System.out.println(e.getMessage());
    }
  }
}
