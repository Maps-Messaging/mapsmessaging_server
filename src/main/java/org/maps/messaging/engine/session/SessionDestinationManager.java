package org.maps.messaging.engine.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.maps.messaging.api.features.DestinationType;
import org.maps.messaging.engine.destination.DestinationFactory;
import org.maps.messaging.engine.destination.DestinationImpl;
import org.maps.messaging.engine.destination.DestinationManager;
import org.maps.messaging.engine.destination.DestinationManagerListener;

public class SessionDestinationManager implements DestinationFactory {

  private final SessionTenantManager sessionTenantManager;
  private final DestinationManager manager;

  public SessionDestinationManager(DestinationManager manager, SessionTenantManager sessionTenantManager){
    this.sessionTenantManager = sessionTenantManager;
    this.manager = manager;
  }

  public String getRoot(){
    return sessionTenantManager.getMapping();
  }

  @Override
  public List<DestinationImpl> getDestinations() {
    List<DestinationImpl> response = manager.getDestinations();
    List<DestinationImpl> filteredResponse = new ArrayList<>();
    for(DestinationImpl destination:response){
      if(destination.getName().startsWith(sessionTenantManager.getMapping())){
        filteredResponse.add(destination);
      }
    }
    return filteredResponse;
  }

  @Override
  public DestinationImpl find(String name) {
    return manager.find(adjustName(name));
  }

  @Override
  public DestinationImpl findOrCreate(String name) throws IOException {
    return manager.findOrCreate(adjustName(name));
  }

  @Override
  public DestinationImpl findOrCreate(String name, DestinationType destinationType) throws IOException {
    return manager.findOrCreate(adjustName(name), destinationType);
  }

  @Override
  public DestinationImpl create(@NotNull String name, @NotNull DestinationType destinationType) throws IOException {
    return manager.create(adjustName(name), destinationType);
  }

  @Override
  public DestinationImpl delete(DestinationImpl destinationImpl) {
    return manager.delete(destinationImpl);
  }

  @Override
  public Map<String, DestinationImpl> get() {
    return manager.get();
  }

  @Override
  public void addListener(DestinationManagerListener listener) {
    manager.addListener(listener);
  }

  @Override
  public void removeListener(DestinationManagerListener listener) {
    manager.removeListener(listener);
  }

  private String adjustName(String name){
    return sessionTenantManager.getAbsoluteName(name);
  }
}
