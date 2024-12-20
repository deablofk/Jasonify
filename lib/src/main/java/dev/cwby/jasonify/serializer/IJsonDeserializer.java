package dev.cwby.jasonify.serializer;

// Interface for generating code for deserializing Json String into a Object, the object itself must
// be Annotated with @Json or @JsonDeserializer
public interface IJsonDeserializer<T> {
  T parseJson(Object json);
}
