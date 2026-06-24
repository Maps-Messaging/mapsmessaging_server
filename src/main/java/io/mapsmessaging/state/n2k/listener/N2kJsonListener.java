package io.mapsmessaging.state.n2k.listener;

import com.google.gson.JsonObject;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;

public interface N2kJsonListener {

  int getPgn();

  void handle(DroneTwin droneTwin, JsonObject packet, TwinUpdateContext context);
}