package com.olivertechnology.chargeoff.util;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.annotations.Expose;

public class GSONExposeExclusionStrategy implements ExclusionStrategy {

  @Override
  public boolean shouldSkipField(FieldAttributes field) {
    Expose expose = field.getAnnotation(Expose.class);
    return expose != null && !expose.serialize();
  }

  @Override
  public boolean shouldSkipClass(Class<?> clazz) {
    return false;
  }
}
