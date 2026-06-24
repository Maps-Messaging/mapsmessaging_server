package io.mapsmessaging.state.n2k.listener;

import com.google.gson.JsonObject;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;

import java.time.Instant;

public class N2kMotionJsonListener extends AbstractN2kJsonListener {

  public static final int LISTENER_ID = N2kPgns.COG_SOG_RAPID_UPDATE;

  @Override
  public int getPgn() {
    return LISTENER_ID;
  }

  @Override
  public void handle(DroneTwin droneTwin, JsonObject packet, TwinUpdateContext context) {
    Double courseOverGroundRadians = getDouble(packet, "courseOverGround");
    Double speedOverGroundMetersPerSecond = getDouble(packet, "speedOverGround");
    Double courseReference = getDouble(packet, "cogReference");

    if (courseOverGroundRadians == null && speedOverGroundMetersPerSecond == null && courseReference == null) {
      return;
    }

    Instant now = resolveTimestamp(context);

    if (courseOverGroundRadians != null) {
      droneTwin.setCourseOverGroundDegrees(normalizeDegrees(radiansToDegrees(courseOverGroundRadians)));
    }

    if (speedOverGroundMetersPerSecond != null) {
      droneTwin.setGroundSpeedMetersPerSecond(speedOverGroundMetersPerSecond);
    }

    if (courseReference != null) {
      droneTwin.getAttributes().put("n2k.motion.courseReference", String.valueOf(courseReference.longValue()));
    }

    droneTwin.setMotionUpdatedAt(now);
    droneTwin.setLastSeenAt(now);
  }
}