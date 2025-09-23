package io.mapsmessaging.analytics.impl.stats;

import com.google.gson.JsonObject;
import io.mapsmessaging.utilities.service.Service;

public interface Statistics extends Service {

  void update(Object object);
  void reset();
  JsonObject toJson();

  Statistics create();
  void incrementMismatch();

}
