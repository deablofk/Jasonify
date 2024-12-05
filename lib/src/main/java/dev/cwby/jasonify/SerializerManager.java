package dev.cwby.jasonify;

import dev.cwby.jasonify.exception.AOTMapperInitializerException;
import dev.cwby.jasonify.serializer.IJsonSerializer;

import java.util.HashMap;
import java.util.Map;

public class SerializerManager {

  private static final Map<String, IJsonSerializer<?>> SERIALIZER_MAP = new HashMap<>();

  // TODO: jsonGenerator Pool, Object pool for json values

  static {
    try {
      Class.forName("dev.cwby.jasonify.initializer.AOTMapperInitializer");
    } catch (ClassNotFoundException e) {
      try {
        throw new AOTMapperInitializerException("cant initialize the class: " + e.getMessage());
      } catch (AOTMapperInitializerException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  public static void registerSerializer(String qualifiedName, IJsonSerializer<?> serializer) {
    SERIALIZER_MAP.put(qualifiedName, serializer);
  }

  public static <T> IJsonSerializer<T> getSerializer(String qualifiedName) {
    return (IJsonSerializer<T>) SERIALIZER_MAP.get(qualifiedName);
  }

  public static <T> String toJson(T type) {
    String qualifiedName = type.getClass().getCanonicalName();
    return getSerializer(qualifiedName).toJson(type);
  }
}