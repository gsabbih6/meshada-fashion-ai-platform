package com.sabbih.meshadaaiservices.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonUtils {
  static ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public static <T> T deserialize(String json, Class<T> clazz) throws JsonProcessingException {
    T obj = mapper.readValue(json, clazz);
    return obj;
  }

  public static <T> String serialize(T obj) throws JsonProcessingException {
    String val = "";
    val = mapper.writeValueAsString(obj);
    return val;
  }
}
