package io.mapsmessaging.engine.destination;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DestinationUpdateManager {

  private final List<DestinationManagerListener> destinationManagerListeners;

  public DestinationUpdateManager() {
    destinationManagerListeners = new CopyOnWriteArrayList<>();
  }

  public List<DestinationManagerListener> get() {
    return new ArrayList<>(destinationManagerListeners);
  }

  public void created(DestinationImpl destination) {
    for (DestinationManagerListener listener : destinationManagerListeners) {
      listener.created(destination);
    }
  }

  public void deleted(DestinationImpl destination) {
    for (DestinationManagerListener listener : destinationManagerListeners) {
      listener.deleted(destination);
    }
  }

  public void add(DestinationManagerListener listener) {
    destinationManagerListeners.add(listener);
  }

  public void remove(DestinationManagerListener listener) {
    destinationManagerListeners.remove(listener);
  }

}
