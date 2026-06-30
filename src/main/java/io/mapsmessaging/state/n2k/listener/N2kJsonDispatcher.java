package io.mapsmessaging.state.n2k.listener;

import com.google.gson.JsonObject;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;

import java.util.Set;

public class N2kJsonDispatcher {

  private static final Set<Integer> IGNORED_PGNS = Set.of(
      N2kPgns.ISO_REQUEST,
      N2kPgns.ISO_ADDRESS_CLAIM,
      N2kPgns.SYSTEM_TIME,
      N2kPgns.TIME_DATE,
      N2kPgns.HEARTBEAT
  );

  private final N2kJsonListenerRegistry listeners;

  public N2kJsonDispatcher(N2kJsonListenerRegistry listeners) {
    this.listeners = listeners;
  }

  public void dispatch(DroneTwin droneTwin, int pgn, JsonObject packet, TwinUpdateContext context) {
    N2kJsonListener listener = listeners.getListener(pgn);
    if (listener == null) {
      if (!IGNORED_PGNS.contains(pgn)) {
        System.err.println("No listener for " + pgn + " with " + packet);
      }
      return;
    }

    listener.handle(droneTwin, packet, context);
  }
}