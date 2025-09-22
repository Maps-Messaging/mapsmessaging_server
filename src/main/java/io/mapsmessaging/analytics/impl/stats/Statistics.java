package io.mapsmessaging.analytics.impl.stats;

import com.google.gson.JsonObject;

public interface Statistics {
  void reset();
  JsonObject toJson();
}
