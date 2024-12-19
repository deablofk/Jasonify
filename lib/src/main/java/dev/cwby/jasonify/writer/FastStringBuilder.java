package dev.cwby.jasonify.writer;

public class FastStringBuilder {
  private static final int INITIAL_CAPACITY = 16;
  private char[] buffer;
  private int capacity;
  private int length;

  public FastStringBuilder() {
    this(INITIAL_CAPACITY);
  }

  public FastStringBuilder(int capacity) {
    this.capacity = capacity;
    this.buffer = new char[capacity];
    this.length = 0;
  }

  private void increaseCapacity(int newCapacity) {
    System.out.println("increasing the size of the buffer");
    char[] newBuffer = new char[newCapacity];
    System.arraycopy(buffer, 0, newBuffer, 0, length);
    this.buffer = newBuffer;
    this.capacity = newCapacity;
  }

  private void increaseCapacity() {
    increaseCapacity(capacity * 2);
  }

  public FastStringBuilder append(char c) {
    if (length == capacity) {
      increaseCapacity();
    }
    buffer[length++] = c;
    return this;
  }

  public FastStringBuilder append(String csq) {
    if (csq.length() + length > capacity) {
      increaseCapacity(capacity + csq.length());
    }

    csq.getChars(0, csq.length(), buffer, length);
    length += csq.length();
    return this;
  }

  public FastStringBuilder appendEscaped(char c) {
    switch (c) {
      case '\"':
        buffer[length++] = '\\';
        buffer[length++] = '\"';
        break;
      case '\\':
        buffer[length++] = '\\';
        buffer[length++] = '\\';
        break;
      case '\b':
        buffer[length++] = '\\';
        buffer[length++] = 'b';
        break;
      case '\f':
        buffer[length++] = '\\';
        buffer[length++] = 'f';
        break;
      case '\n':
        buffer[length++] = '\\';
        buffer[length++] = 'n';
        break;
      case '\r':
        buffer[length++] = '\\';
        buffer[length++] = 'r';
        break;
      case '\t':
        buffer[length++] = '\\';
        buffer[length++] = 't';
        break;
      default:
        buffer[length++] = c;
        break;
    }
    return this;
  }

  public FastStringBuilder appendEscaped(CharSequence csq) {
    for (int i = 0; i < csq.length(); i++) {
      appendEscaped(csq.charAt(i));
    }
    return this;
  }

  @Override
  public String toString() {
    return new String(buffer, 0, length);
  }

  public void setLength(int length) {
    this.length = length;
  }
}
