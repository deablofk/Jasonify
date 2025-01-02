package dev.cwby.jasonify;

import dev.cwby.jasonify.exception.AOTMapperInitializerException;
import dev.cwby.jasonify.reader.JsonParser;
import dev.cwby.jasonify.serializer.IJsonDeserializer;
import dev.cwby.jasonify.serializer.IJsonSerializer;
import dev.cwby.jasonify.writer.JsonGenerator;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SerializerManager {

  private static final Map<String, IJsonSerializer<?>> SERIALIZER_MAP = new HashMap<>();
  private static final Map<String, IJsonDeserializer<?>> DESERIALIZER_MAP = new HashMap<>();

  // TODO: jsonGenerator Pool, Object pool for json values
  // TODO: add new 2 Anontations, @JsonSerialize and @JsonDeserialize, @Json will be a wraper for
  // the two as @Data in lombok for getters,setters

  static {
    try {
      Class.forName("dev.cwby.jasonify.initializer.AOTMapperInitializer");
    } catch (ClassNotFoundException e) {
      try {
        throw new AOTMapperInitializerException("cant initialize the class: " + e.getMessage());
      } catch (AOTMapperInitializerException ignored) {
      }
    }
  }

  public static void registerSerializer(String qualifiedName, IJsonSerializer<?> serializer) {
    SERIALIZER_MAP.put(qualifiedName, serializer);
  }

  public static void registerDeserializer(String qualifiedName, IJsonDeserializer<?> deserializer) {
    DESERIALIZER_MAP.put(qualifiedName, deserializer);
  }

  @SuppressWarnings("unchecked")
  public static <T> IJsonSerializer<T> getSerializer(String qualifiedName) {
    return (IJsonSerializer<T>) SERIALIZER_MAP.get(qualifiedName);
  }

  @SuppressWarnings("unchecked")
  public static <T> IJsonDeserializer<T> getDeserializer(String qualifiedName) {
    return (IJsonDeserializer<T>) DESERIALIZER_MAP.get(qualifiedName);
  }

  public static <T> void appendToWriter(T type, JsonGenerator jsonGenerator) {
    String qualifiedName = type.getClass().getCanonicalName();
    getSerializer(qualifiedName).appendToWriter(type, jsonGenerator);
  }

  public static <T> String toJson(T type) {
    JsonGenerator jsonGenerator = new JsonGenerator();
    String qualifiedName = type.getClass().getCanonicalName();
    getSerializer(qualifiedName).appendToWriter(type, jsonGenerator);
    return jsonGenerator.getJson();
  }

  public static <T> T fromJson(String json, Class<T> clazz) throws IOException {
    var parser = new JsonParser(json);
    return fromJson(parser, clazz);
  }

  public static <T> T fromJson(JsonParser parser, Class<T> clazz) throws IOException {
    IJsonDeserializer<T> deserializer = getDeserializer(clazz.getCanonicalName());
    return deserializer.parseJson(parser);
  }
}
