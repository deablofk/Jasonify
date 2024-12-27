package dev.cwby.jasonify.reader;

public enum JsonToken {
  START_OBJECT,
  END_OBJECT,
  START_ARRAY,
  END_ARRAY,
  FIELD_NAME,
  VALUE_STRING,
  VALUE_NUMBER,
  VALUE_BOOLEAN,
  NULL,
  END_DOCUMENT
}
