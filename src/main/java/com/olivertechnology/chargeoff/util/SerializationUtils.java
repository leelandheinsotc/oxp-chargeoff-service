package com.olivertechnology.chargeoff.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SerializationUtils {

  public static <T> T deserialize(final String json, Class<T> clazz) {
    return new Gson().fromJson(json, clazz);
  }

  public static String serialize(Object object) {
    Gson gson = new GsonBuilder()
        .addSerializationExclusionStrategy(new GSONExposeExclusionStrategy()).create();
    return gson.toJson(object);
  }
}
