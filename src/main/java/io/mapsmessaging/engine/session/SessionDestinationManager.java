/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.engine.session;

import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.engine.destination.DestinationFactory;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationManager;
import io.mapsmessaging.engine.destination.DestinationManagerListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

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
  public DestinationImpl create(@NonNull @NotNull String name, @NonNull @NotNull DestinationType destinationType) throws IOException {
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
