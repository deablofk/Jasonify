package dev.cwby.jasonify.writer;

import java.util.Base64;

public class JsonGenerator {
    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
    private final FastStringBuilder appendable;
    private int depth = 0;
    private boolean isFirst = true;

    public JsonGenerator(final FastStringBuilder appendable) {
        this.appendable = appendable;
    }

    public JsonGenerator() {
        this.appendable = new FastStringBuilder(1000);
    }

    private boolean isFirst() {
        return isFirst;
    }

    private void setFirst() {
        isFirst = false;
    }

    public JsonGenerator append(String str) {
        this.appendable.append(str);
        return this;
    }

    public JsonGenerator append(char c) {
        this.appendable.append(c);
        return this;
    }

    public JsonGenerator writeStartObject() {
        handleComma();
        append('{');
        depth++;
        isFirst = true;
        return this;
    }

    public JsonGenerator writeEndObject() {
        append('}');
        depth--;
        isFirst = depth > 0;
        handleComma();
        return this;
    }

    public JsonGenerator writeStartArray() {
        handleComma();
        append('[');
        depth++;
        isFirst = true;
        return this;
    }

    public JsonGenerator writeEndArray() {
        append(']');
        depth--;
        isFirst = false;
        return this;
    }

    public JsonGenerator writeField(String fieldName) {
        handleComma();
        append('\"').append(fieldName).append('\"').append(':');
        depth++;
        isFirst = true;
        return this;
    }

    public JsonGenerator writeString(String value) {
        handleComma();
        if (value != null) {
            appendable.append('\"').appendEscaped(value).append('\"');
        } else {
            appendable.append("null");
        }
        return this;
    }

    public JsonGenerator writeBoolean(boolean value) {
        handleComma();
        append(String.valueOf(value));
        return this;
    }

    public JsonGenerator writeNumber(Number value) {
        handleComma();
        append(value.toString());
        return this;
    }

    public JsonGenerator writeRaw(String raw) {
        handleComma();
        append(raw);
        return this;
    }

    public JsonGenerator writeBase64String(byte[] bytes) {
        return writeString(BASE64_ENCODER.encodeToString(bytes));
    }

    private void handleComma() {
        if (!isFirst()) {
            append(',');
        } else {
            setFirst();
        }
    }

    // TODO: pretty printing
    public String getJson() {
        return appendable.toString();
    }

    public void reset() {
        this.depth = 0;
        this.isFirst = true;
        if (this.appendable instanceof FastStringBuilder fastStringBuilder) {
            fastStringBuilder.setLength(0);
        }
    }
}
