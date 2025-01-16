package dev.cwby.jasonify.reader;

import java.io.IOException;
import java.io.StringReader;

public class JsonParser {

  // TODO: create a specific exception for throwing

  private final StringReader reader;
  private JsonToken currentToken;
  private String currentValue;
  private int lookahead = -1;
  private int pos = 0;

  public JsonParser(String json) {
    this.reader = new StringReader(json.trim());
    this.currentToken = null;
    this.currentValue = null;
  }

  public void skipNulltoken() throws IOException {
    if (getCurrentToken() == null) {
      nextToken();
    }
  }

  public JsonToken nextToken() throws IOException {
    skipWhitespace();

    int ch = read();

    if (ch == -1) {
      currentToken = JsonToken.END_DOCUMENT;
      return currentToken;
    }

    switch (ch) {
      case '{':
        currentToken = JsonToken.START_OBJECT;
        break;
      case '}':
        currentToken = JsonToken.END_OBJECT;
        break;
      case '[':
        currentToken = JsonToken.START_ARRAY;
        break;
      case ']':
        currentToken = JsonToken.END_ARRAY;
        break;
      case ',':
        return nextToken();
      case ':':
        return nextToken();
      case '"':
        currentValue = parseString();
        currentToken =
            currentToken == JsonToken.FIELD_NAME ? JsonToken.VALUE_STRING : JsonToken.FIELD_NAME;
        break;
      case 'n':
        parseLiteral("null");
        currentToken = JsonToken.NULL;
        break;
      case 't':
        parseLiteral("true");
        currentValue = "true";
        currentToken = JsonToken.VALUE_BOOLEAN;
        break;
      case 'f':
        parseLiteral("false");
        currentValue = "false";
        currentToken = JsonToken.VALUE_BOOLEAN;
        break;
      default:
        if (Character.isDigit(ch) || ch == '-') {
          currentValue = parseNumber((char) ch);
          currentToken = JsonToken.VALUE_NUMBER;
        } else {
          throw new IllegalArgumentException("Unexpected Character: " + (char) ch);
        }
        break;
    }
    return currentToken;
  }

  public void skipOrSkipChildren(boolean debug) throws IOException {
    if (getCurrentToken() == JsonToken.START_OBJECT || getCurrentToken() == JsonToken.START_ARRAY) {
      skipChildren(debug);
    } else {
      nextToken();
    }
  }

  public void skipChildren(boolean debug) throws IOException {
    if (currentToken != JsonToken.START_OBJECT && currentToken != JsonToken.START_ARRAY) {
      throw new IllegalStateException(
          "skipChildren() can only be called on START_OBJECT or START_ARRAY");
    }

    StringBuilder debugBuffer = debug ? new StringBuilder("skiping: ") : null;
    if (debug) {
      appendToDebug(debugBuffer);
    }

    int depth = 1;
    while (depth > 0) {
      JsonToken token = nextToken();
      if (token == null || token == JsonToken.END_DOCUMENT) {
        throw new IllegalStateException("unexpected end of json while skiping");
      }

      if (debug) {
        appendToDebug(debugBuffer);
      }

      if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
        depth++;
      } else if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
        depth--;
      }
    }

    if (debug) {
      System.out.println(debugBuffer);
    }
  }

  public void appendToDebug(StringBuilder debugBuffer) {
    switch (getCurrentToken()) {
      case START_OBJECT -> debugBuffer.append("{");
      case END_OBJECT -> debugBuffer.append("}");
      case START_ARRAY -> debugBuffer.append("[");
      case END_ARRAY -> debugBuffer.append("]");
      case FIELD_NAME -> debugBuffer.append('"').append(getCurrentValue()).append("\":");
      case VALUE_STRING -> debugBuffer.append('"').append(getCurrentValue()).append("\" ");
      case VALUE_NUMBER, VALUE_BOOLEAN -> debugBuffer.append(getCurrentValue()).append(' ');
      case NULL -> debugBuffer.append("null");
      case END_DOCUMENT -> debugBuffer.append("EOF");
    }
  }

  public int countEntries() throws IOException {
    if (currentToken != JsonToken.START_OBJECT && currentToken != JsonToken.START_ARRAY) {
      throw new IllegalStateException(
          "countEntries() can only be called on START_OBJECT or START_ARRAY");
    }

    String remainingJson = reader.toString().substring(pos);
    JsonParser tempParser = new JsonParser(remainingJson);

    int count = 0;
    int depth = 1;

    while (depth > 0) {
      JsonToken token = tempParser.nextToken();
      if (token == null || token == JsonToken.END_DOCUMENT) {
        throw new IllegalStateException("Unexpected end of JSON while counting entries");
      }

      if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
        depth++;
      } else if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
        depth--;
      } else if (depth == 1
          && (token == JsonToken.FIELD_NAME
              || token == JsonToken.VALUE_STRING
              || token == JsonToken.VALUE_NUMBER
              || token == JsonToken.VALUE_BOOLEAN
              || token == JsonToken.NULL)) {
        count++;
      }
    }

    return count;
  }

  public String parseString() throws IOException {
    var sb = new StringBuilder();
    int ch;
    while ((ch = read()) != '"') {
      if (ch == '\\') {
        ch = read();
        switch (ch) {
          case '"' -> sb.append('"');
          case '\\' -> sb.append('\\');
          case '/' -> sb.append('/');
          case 'b' -> sb.append('\b');
          case 'f' -> sb.append('\f');
          case 'n' -> sb.append('\n');
          case 'r' -> sb.append('\r');
          case 't' -> sb.append('\t');
          default -> throw new IllegalArgumentException("invalid escape sequence");
        }
      } else {
        sb.append((char) ch);
      }
    }

    return sb.toString();
  }

  public void parseLiteral(String literal) throws IOException {
    for (int i = 1; i < literal.length(); i++) {
      if (read() != literal.charAt(i)) {
        throw new IllegalArgumentException("Invalid literal: expected " + literal);
      }
    }
  }

  public String parseNumber(char firstChar) throws IOException {
    var sb = new StringBuilder();
    sb.append(firstChar);

    int ch;

    while ((ch = read()) != -1) {
      if (Character.isDigit(ch) || ch == '.' || ch == 'e' || ch == 'E' || ch == '+' || ch == '-') {
        sb.append((char) ch);
      } else {
        pushBack(ch);
        break;
      }
    }

    return sb.toString();
  }

  public void skipWhitespace() throws IOException {
    int ch;
    while ((ch = read()) != -1) {
      if (!Character.isWhitespace(ch)) {
        pushBack(ch);
        break;
      }
    }
  }

  private void pushBack(int ch) {
    if (lookahead != -1) {
      throw new IllegalStateException("cant push multiple characters");
    }
    lookahead = ch;
  }

  private int read() throws IOException {
    if (lookahead != -1) {
      int tmp = lookahead;
      lookahead = -1;
      return tmp;
    }

    pos++;
    return reader.read();
  }

  public JsonToken getCurrentToken() {
    return currentToken;
  }

  public String getCurrentValue() {
    return currentValue;
  }

  public boolean getCurrentValueBoolean() {
    return Boolean.parseBoolean(currentValue);
  }

  public int getCurrentValueInteger() {
    return Integer.parseInt(currentValue);
  }

  public double getCurrentValueDouble() {
    return Double.parseDouble(currentValue);
  }

  public void expectOrSkip(JsonToken expected, Runnable callback) throws IOException {
    if (getCurrentToken() != expected) {
      skipChildren(true);
    } else {
      callback.run();
    }
  }

  @Override
  public String toString() {
    return "JsonParser{"
        + "currentToken="
        + currentToken
        + ", currentValue='"
        + currentValue
        + '\''
        + ", lookahead="
        + lookahead
        + ", pos="
        + pos
        + '}';
  }
}
