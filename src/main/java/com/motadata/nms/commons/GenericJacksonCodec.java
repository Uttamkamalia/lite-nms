package com.motadata.nms.commons;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class GenericJacksonCodec<T> implements MessageCodec<T, T> {

  private final Class<T> clazz;
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public GenericJacksonCodec(Class<T> clazz) {
    this.clazz = clazz;
  }

  @Override
  public void encodeToWire(Buffer buffer, T pojo) {
    try {
      String jsonStr = objectMapper.writeValueAsString(pojo);
      buffer.appendInt(jsonStr.length());
      buffer.appendString(jsonStr);
    } catch (Exception e) {
      throw new RuntimeException("Failed to encode object to wire", e);
    }
  }

  @Override
  public T decodeFromWire(int pos, Buffer buffer) {
    try {
      int length = buffer.getInt(pos);
      String jsonStr = buffer.getString(pos + 4, pos + 4 + length);
      return objectMapper.readValue(jsonStr, clazz);
    } catch (Exception e) {
      throw new RuntimeException("Failed to decode object from wire", e);
    }
  }

  @Override
  public T transform(T pojo) {
    // This is for local delivery (non-wire). Shallow copy is OK.
    return pojo;
  }

  @Override
  public String name() {
    return clazz.getName() + "Codec";
  }

  @Override
  public byte systemCodecID() {
    return -1; // custom codec
  }
}
