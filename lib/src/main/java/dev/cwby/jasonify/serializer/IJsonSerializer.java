package dev.cwby.jasonify.serializer;

public interface IJsonSerializer<T> {
  String toJson(T t);
}
