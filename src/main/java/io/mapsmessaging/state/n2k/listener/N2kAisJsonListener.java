package io.mapsmessaging.state.n2k.listener;

import com.google.gson.JsonObject;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;

public class N2kAisJsonListener extends AbstractN2kJsonListener {

  private final int pgn;

  public N2kAisJsonListener(int pgn) {
    this.pgn = pgn;
  }

  @Override
  public int getPgn() {
    return pgn;
  }

  @Override
  public void handle(DroneTwin droneTwin, JsonObject packet, TwinUpdateContext context) {
    // AIS contacts are not local twin telemetry.
    // If enabled later, route these into a remote contact/object registry.
  }
}