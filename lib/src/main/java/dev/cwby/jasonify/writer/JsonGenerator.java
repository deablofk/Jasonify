package dev.cwby.jasonify.writer;

import dev.cwby.jasonify.exception.AppendJsonException;
import java.io.IOException;
import java.util.Base64;

public class JsonGenerator {
  private final Appendable appendable;
  private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
  private int depth = 0;
  private boolean isFirst = true;

  public JsonGenerator(final Appendable appendable) {
    this.appendable = appendable;
  }

  public JsonGenerator() {
    this.appendable = new StringBuilder();
  }

  private boolean isFirst() {
    return isFirst;
  }

  private void setFirst() {
    isFirst = false;
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
    depth++;
    isFirst = true;
    return this;
  }

  public JsonGenerator writeEndObject() throws AppendJsonException {
    append("}");
    depth--;
    isFirst = depth > 0;
    handleComma();
    return this;
  }

  public JsonGenerator writeStartArray() throws AppendJsonException {
    handleComma();
    append("[");
    depth++;
    isFirst = true;
    return this;
  }

  public JsonGenerator writeEndArray() throws AppendJsonException {
    append("]");
    return this;
  }

  public JsonGenerator writeField(String fieldName) throws AppendJsonException {
    handleComma();
    append("\"").append(fieldName).append("\":");
    depth++;
    isFirst = true;
    return this;
  }

  public JsonGenerator writeString(String value) throws AppendJsonException {
    handleComma();
    append("\"").append(escapeString(value)).append("\"");
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

  public JsonGenerator writeRaw(String raw) throws AppendJsonException {
    handleComma();
    append(raw);
    return this;
  }

  public JsonGenerator writeBase64String(byte[] bytes) throws AppendJsonException {
    return writeString(BASE64_ENCODER.encodeToString(bytes));
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

  public void reset() {
    this.depth = 0;
    this.isFirst = true;
    if (this.appendable instanceof StringBuilder) {
      ((StringBuilder) this.appendable).setLength(0);
    }
  }

  public String escapeString(String content) {
    var strBuilder = new StringBuilder(content.length());

    for (char c : content.toCharArray()) {
      switch (c) {
        case '\"' -> strBuilder.append("\\\"");
        case '\\' -> strBuilder.append("\\\\");
        case '\b' -> strBuilder.append("\\b");
        case '\f' -> strBuilder.append("\\f");
        case '\n' -> strBuilder.append("\\n");
        case '\r' -> strBuilder.append("\\r");
        case '\t' -> strBuilder.append("\\t");
        default -> strBuilder.append(c);
      }
    }

    return strBuilder.toString();
  }
}
