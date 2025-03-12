package io.mapsmessaging.license;

import io.mapsmessaging.license.features.Features;

import java.lang.reflect.Field;
import java.util.List;

public class FeatureManager {

  private final List<Features> featuresList;

  public FeatureManager(List<Features> featuresList) {
    this.featuresList = featuresList;
  }

  public boolean isEnabled(String featurePath) {
    for (Features features : featuresList) {
      Object value = getFieldValue(features, featurePath);
      if (value instanceof Boolean && ((boolean) value)) {
        return true;
      }
    }
    return false;
  }

  public int getMaxValue(String featurePath) {
    int maxValue = 0;
    for (Features features : featuresList) {
      Object value = getFieldValue(features, featurePath);
      if (value instanceof Integer) {
        maxValue = Math.max(maxValue, (Integer) value);
      }
    }
    return maxValue;
  }

  private Object getFieldValue(Object obj, String featurePath) {
    try {
      String[] parts = featurePath.split("\\.");
      for (String part : parts) {
        if (obj == null) return null;
        Field field = obj.getClass().getDeclaredField(part);
        obj = field.get(obj);
      }
      return obj;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      return null;
    }
  }

}
