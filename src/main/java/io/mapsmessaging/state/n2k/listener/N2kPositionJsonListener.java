package io.mapsmessaging.state.n2k.listener;

import com.google.gson.JsonObject;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;

import java.time.Instant;

public class N2kPositionJsonListener extends AbstractN2kJsonListener {

  public static final int LISTENER_ID = N2kPgns.POSITION_RAPID_UPDATE;

  @Override
  public int getPgn() {
    return LISTENER_ID;
  }

  @Override
  public void handle(DroneTwin droneTwin, JsonObject packet, TwinUpdateContext context) {
    Double latitude = getDouble(packet, "latitude");
    Double longitude = getDouble(packet, "longitude");

    if (!isValidLatitude(latitude) || !isValidLongitude(longitude)) {
      return;
    }

    Instant now = resolveTimestamp(context);

    droneTwin.setGeoPosition(new GeoPosition(latitude, longitude, null, null));
    droneTwin.setGpsValid(true);
    droneTwin.setNavigationUpdatedAt(now);
    droneTwin.setLastSeenAt(now);
  }
}