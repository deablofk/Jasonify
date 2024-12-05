package dev.cwby.jasonify.writer;

import dev.cwby.jasonify.exception.AppendJsonException;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;

public class JsonGenerator {
  private final Appendable appendable;
  private final Deque<Boolean> isFirstStack;

  public JsonGenerator(final Appendable appendable) {
    this.appendable = appendable;
    this.isFirstStack = new ArrayDeque<>();
    this.isFirstStack.push(true);
  }

  private boolean isFirst() {
    return Boolean.TRUE.equals(isFirstStack.peek());
  }

  private void setFirst() {
    isFirstStack.pop();
    isFirstStack.push(false);
  }

  public JsonGenerator append(String str) throws AppendJsonException {
    try {
      this.appendable.append(str);
    } catch (IOException e) {
      throw new AppendJsonException("Cant append value to the JsonGenerator: " + str);
    }
    return this;
  }

  public JsonGenerator writeStartObject() throws AppendJsonException {
    handleComma();
    append("{");
    isFirstStack.push(true);
    return this;
  }

  public JsonGenerator writeEndObject() throws AppendJsonException {
    append("}");
    isFirstStack.pop();
    return this;
  }

  public JsonGenerator writeStartArray() throws AppendJsonException {
    handleComma();
    append("[");
    isFirstStack.push(true);
    return this;
  }

  public JsonGenerator writeEndArray() throws AppendJsonException {
    append("]");
    isFirstStack.pop();
    return this;
  }

  public JsonGenerator writeField(String fieldName) throws AppendJsonException {
    handleComma();
    append("\"").append(fieldName).append("\":");
    isFirstStack.push(true);
    return this;
  }

  public JsonGenerator writeString(String value) throws AppendJsonException {
    handleComma();
    append("\"").append(value).append("\"");
    return this;
  }

  public JsonGenerator writeBoolean(boolean value) throws AppendJsonException {
    handleComma();
    append(String.valueOf(value));
    return this;
  }

  public JsonGenerator writeNumber(Number value) throws AppendJsonException {
    handleComma();
    append(value.toString());
    return this;
  }

  public JsonGenerator writeBase64String(byte[] bytes) throws AppendJsonException {
    return writeString(Base64.getEncoder().encodeToString(bytes));
  }

  private void handleComma() throws AppendJsonException {
    if (!isFirst()) {
      append(",");
    } else {
      setFirst();
    }
  }

  // TODO: pretty printing
  public String getJson() {
    return appendable.toString();
  }
}
