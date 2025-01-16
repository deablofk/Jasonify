package dev.cwby.jasonify.serializer;

import dev.cwby.jasonify.writer.JsonGenerator;

public interface IJsonSerializer<T> {
    void appendToWriter(T t, JsonGenerator jg);
}
