package dev.cwby.jasonify.reader;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JsonParserBackup {
  public static Object parse(String json) {
    if (json.startsWith("{")) {
      return parseJsonObject(json);
    } else if (json.startsWith("[")) {
      return parseJsonArray(json);
    }
    return null;
  }

  public static String stripEnclosing(String json, char openChar, char closeChar) {
    json = json.trim();
    if (json.charAt(0) == openChar && json.charAt(json.length() - 1) == closeChar) {
      json = json.substring(1, json.length() - 1).trim();
    }
    return json;
  }

  public static List<Object> parseJsonArray(String json) {
    json = stripEnclosing(json, '[', ']');
    var list = new LinkedList<>();
    int len = json.length();
    int pos = 0;

    while (pos < len) {
      char firstChar = json.charAt(pos);

      Object value;
      if (firstChar == '"') {
        int valueStart = pos + 1;
        int valueEnd = json.indexOf('"', valueStart);
        value = json.substring(valueStart, valueEnd);
        pos = valueEnd + 1;
      } else if (firstChar == '{') {
        int braces = 1;
        int endIndex = pos + 1;
        while (endIndex < len && braces > 0) {
          char c = json.charAt(endIndex);
          if (c == '{') braces++;
          if (c == '}') braces--;
          endIndex++;
        }
        value = parseJsonObject(json.substring(pos, endIndex));
        pos = endIndex;
      } else if (firstChar == '[') {
        int brackets = 1;
        int endIndex = pos + 1;
        while (endIndex < len && brackets > 0) {
          char c = json.charAt(endIndex);
          if (c == '[') brackets++;
          if (c == ']') brackets--;
          endIndex++;
        }
        value = parseJsonArray(json.substring(pos, endIndex));
        pos = endIndex;
      } else if (Character.isDigit(firstChar) || firstChar == '-') {
        int valueEnd = pos;
        while (valueEnd < len && (Character.isDigit(json.charAt(valueEnd)))) {
          valueEnd++;
        }

        String numberStr = json.substring(pos, valueEnd);
        if (numberStr.contains(".")) {
          value = Double.parseDouble(numberStr);
        } else {
          value = Integer.parseInt(numberStr);
        }
        pos = valueEnd;
      } else if (json.startsWith("true", pos) || json.startsWith("false", pos)) {
        value = json.startsWith("true", pos);
        pos += value.toString().length();
      } else if (json.startsWith("null", pos)) {
        value = null;
        pos += 4;
      } else {
        throw new IllegalArgumentException("Invalid JSON format");
      }

      list.add(value);

      pos = json.indexOf(',', pos);

      if (pos == -1) {
        break;
      }
      pos++;
    }
    return list;
  }

  public static Map<String, Object> parseJsonObject(String json) {
    json = stripEnclosing(json, '{', '}');

    var map = new LinkedHashMap<String, Object>();
    int len = json.length();
    int pos = 0;

    while (pos < len) {
      int keyStart = json.indexOf('"', pos) + 1;
      int keyEnd = json.indexOf('"', keyStart);
      String key = json.substring(keyStart, keyEnd);

      int separatorIndex = json.indexOf(':', keyEnd);
      pos = separatorIndex + 1;

      char firstChar = json.charAt(pos);

      Object value;
      if (firstChar == '"') {
        int valueStart = pos + 1;
        int valueEnd = json.indexOf('"', valueStart);
        value = json.substring(valueStart, valueEnd);
        pos = valueEnd + 1;
      } else if (firstChar == '{') {
        int braces = 1;
        int endIndex = pos + 1;
        while (endIndex < len && braces > 0) {
          char c = json.charAt(endIndex);
          if (c == '{') braces++;
          if (c == '}') braces--;
          endIndex++;
        }
        value = parseJsonObject(json.substring(pos, endIndex));
        pos = endIndex;
      } else if (firstChar == '[') {
        int brackets = 1;
        int endIndex = pos + 1;
        while (endIndex < len && brackets > 0) {
          char c = json.charAt(endIndex);
          if (c == '[') brackets++;
          if (c == ']') brackets--;
          endIndex++;
        }
        value = parseJsonArray(json.substring(pos, endIndex));
        pos = endIndex;
      } else if (Character.isDigit(firstChar) || firstChar == '-') {
        int valueEnd = pos;
        while (valueEnd < len && (Character.isDigit(json.charAt(valueEnd)))) {
          valueEnd++;
        }

        String numberStr = json.substring(pos, valueEnd);
        if (numberStr.contains(".")) {
          value = Double.parseDouble(numberStr);
        } else {
          value = Integer.parseInt(numberStr);
        }
        pos = valueEnd;
      } else if (json.startsWith("true", pos) || json.startsWith("false", pos)) {
        value = json.startsWith("true", pos);
        pos += value.toString().length();
      } else if (json.startsWith("null", pos)) {
        value = null;
        pos += 4;
      } else {
        throw new IllegalArgumentException("Invalid JSON format");
      }

      map.put(key, value);

      pos = json.indexOf(',', pos);

      if (pos == -1) {
        break;
      }
      pos++;
    }
    return map;
  }
}
