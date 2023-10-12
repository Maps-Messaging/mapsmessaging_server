/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.engine.session;

import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.engine.destination.*;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SessionDestinationManager implements DestinationFactory {

  private SessionTenantConfig sessionTenantConfig = new SessionTenantConfig("", null);
  private final DestinationManager manager;

  public SessionDestinationManager(DestinationManager manager) {
    this.manager = manager;
  }

  @Override
  public String calculateNamespace(String destinationName) {
    return sessionTenantConfig.calculateDestinationName(destinationName);
  }

  @Override
  public String calculateOriginalNamespace(String fqn) {
    return sessionTenantConfig.calculateOriginalName(fqn);
  }


  public void setSessionTenantConfig(SessionTenantConfig config) {
    sessionTenantConfig = config;
  }

  @Override
  public CompletableFuture<DestinationImpl> find(String name) {
    return manager.find(name);
  }

  @Override
  public CompletableFuture<DestinationImpl> findOrCreate(String name) throws IOException {
    return manager.findOrCreate(name);
  }

  @Override
  public CompletableFuture<DestinationImpl> findOrCreate(String name, DestinationType destinationType) throws IOException {
    return manager.findOrCreate(name, destinationType);
  }

  @Override
  public CompletableFuture<DestinationImpl> create(@NonNull @NotNull String name, @NonNull @NotNull DestinationType destinationType) throws IOException {
    return manager.create(name, destinationType);
  }

  @Override
  public CompletableFuture<DestinationImpl> delete(DestinationImpl destinationImpl) {
    return manager.delete(destinationImpl);
  }

  @Override
  public Map<String, DestinationImpl> get(DestinationFilter filter) {
    return manager.get(filter);
  }

  @Override
  public void addListener(DestinationManagerListener listener) {
    manager.addListener(listener);
  }

  @Override
  public void removeListener(DestinationManagerListener listener) {
    manager.removeListener(listener);
  }

}
