package io.mapsmessaging.state.n2k.listener;

import com.google.gson.JsonObject;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;

import java.time.Instant;

public class N2kHeadingJsonListener extends AbstractN2kJsonListener {

  public static final int LISTENER_ID = N2kPgns.VESSEL_HEADING;

  @Override
  public int getPgn() {
    return LISTENER_ID;
  }

  @Override
  public void handle(DroneTwin droneTwin, JsonObject packet, TwinUpdateContext context) {
    Double headingRadians = getDouble(packet, "headingSensorReading");
    Double variationRadians = getDouble(packet, "variation");
    Double deviationRadians = getDouble(packet, "deviation");
    Double headingSensorReference = getDouble(packet, "headingSensorReference");

    if (headingRadians == null && variationRadians == null && deviationRadians == null
        && headingSensorReference == null) {
      return;
    }

    Instant now = resolveTimestamp(context);

    if (headingRadians != null) {
      droneTwin.setHeadingDegrees(normalizeDegrees(radiansToDegrees(headingRadians)));
    }

    if (variationRadians != null) {
      droneTwin.getAttributes().put("n2k.heading.variationDegrees", String.valueOf(radiansToDegrees(variationRadians)));
    }

    if (deviationRadians != null) {
      droneTwin.getAttributes().put("n2k.heading.deviationDegrees", String.valueOf(radiansToDegrees(deviationRadians)));
    }

    if (headingSensorReference != null) {
      droneTwin.getAttributes().put("n2k.heading.sensorReference", String.valueOf(headingSensorReference.longValue()));
    }

    droneTwin.setNavigationUpdatedAt(now);
    droneTwin.setLastSeenAt(now);
  }
}