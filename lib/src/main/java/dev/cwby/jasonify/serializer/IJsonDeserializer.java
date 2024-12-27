package dev.cwby.jasonify.serializer;

import dev.cwby.jasonify.reader.JsonParser;

import java.io.IOException;

// Interface for generating code for deserializing Json String into a Object, the object itself must
// be Annotated with @Json or @JsonDeserializer
public interface IJsonDeserializer<T> {
  T parseJson(JsonParser parser) throws IOException;
}
