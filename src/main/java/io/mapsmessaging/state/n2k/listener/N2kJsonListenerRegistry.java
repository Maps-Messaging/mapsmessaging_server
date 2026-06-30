package io.mapsmessaging.state.n2k.listener;

import java.util.HashMap;
import java.util.Map;

public class N2kJsonListenerRegistry {

  private final Map<Integer, N2kJsonListener> listeners;

  public N2kJsonListenerRegistry() {
    listeners = new HashMap<>();

    register(new N2kPositionJsonListener());
    register(new N2kGnssJsonListener());
    register(new N2kMotionJsonListener());
    register(new N2kHeadingJsonListener());
    register(new N2kAttitudeJsonListener());

    register(new N2kRateOfTurnJsonListener());
    register(new N2kGnssDopsJsonListener());
    register(new N2kBatteryStatusJsonListener());
    register(new N2kMagneticVariationJsonListener());
    register(new N2kWindJsonListener());
    register(new N2kEnvironmentalParametersJsonListener());
    register(new N2kInverterStatusJsonListener());
  }

  public N2kJsonListener getListener(int pgn) {
    return listeners.get(pgn);
  }

  public boolean hasListener(int pgn) {
    return listeners.containsKey(pgn);
  }

  private void register(N2kJsonListener listener) {
    listeners.put(listener.getPgn(), listener);
  }
}