package dev.cwby.jasonify;

import dev.cwby.jasonify.exception.AOTMapperInitializerException;
import dev.cwby.jasonify.serializer.IJsonSerializer;

import java.util.HashMap;
import java.util.Map;

public class SerializerManager {

  private static final Map<String, IJsonSerializer<?>> SERIALIZER_MAP = new HashMap<>();

  public static void registerSerializer(String qualifiedName, IJsonSerializer<?> serializer) {
    SERIALIZER_MAP.put(qualifiedName, serializer);
  }

  public static <T> IJsonSerializer<T> getSerializer(String qualifiedName) {
    return (IJsonSerializer<T>) SERIALIZER_MAP.get(qualifiedName);
  }
}
