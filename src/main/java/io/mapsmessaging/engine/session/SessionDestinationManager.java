/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.engine.session;

import io.mapsmessaging.api.auth.DestinationAuthorisationCheck;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.engine.destination.*;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * This class represents a SessionDestinationManager that implements the DestinationFactory interface.
 * It is responsible for managing the session's destination configurations and operations.
 * The manager uses a DestinationManager instance to perform the actual operations.
 * The manager can find, create, delete, and get destinations based on the provided filters.
 * It also allows adding and removing listeners for destination manager events.
 * The manager uses a SessionTenantConfig instance to calculate the namespace and original namespace for destinations.
 */
public class SessionDestinationManager implements DestinationFactory {

  private SessionTenantConfig sessionTenantConfig = new SessionTenantConfig("", null);
  private final DestinationManager manager;

  /**
   * Constructor for SessionDestinationManager class.
   * Initializes the SessionDestinationManager with the given DestinationManager.
   *
   * @param manager The DestinationManager to be used by the SessionDestinationManager.
   */
  public SessionDestinationManager(DestinationManager manager) {
    this.manager = manager;
  }

  @Override
  /**
   * Calculates the namespace for a given destination name.
   *
   * This method delegates the calculation of the namespace to the {@link SessionTenantConfig} instance.
   * It calls the {@link SessionTenantConfig#calculateDestinationName(String)} method to calculate the namespace based on the provided destination name.
   *
   * @param destinationName the destination name for which the namespace needs to be calculated
   * @return the calculated namespace
   */
  public String calculateNamespace(String destinationName) {
    return sessionTenantConfig.calculateDestinationName(destinationName);
  }

  @Override
  /**
   * Calculates the original namespace based on the given fully qualified name (FQN).
   *
   * This method delegates the calculation of the original namespace to the {@link SessionTenantConfig} instance.
   * It calls the {@link SessionTenantConfig#calculateOriginalName(String)} method to calculate the original namespace based on the provided FQN.
   *
   * @param fqn the fully qualified name for which the original namespace needs to be calculated
   * @return the calculated original namespace
   */
  public String calculateOriginalNamespace(String fqn) {
    return sessionTenantConfig.calculateOriginalName(fqn);
  }


  /**
   * Sets the session tenant configuration.
   *
   * This method sets the session tenant configuration for the SessionDestinationManager.
   * It takes a SessionTenantConfig object as a parameter and assigns it to the sessionTenantConfig field.
   *
   * @param config the SessionTenantConfig object representing the session tenant configuration
   */
  public void setSessionTenantConfig(SessionTenantConfig config) {
    sessionTenantConfig = config;
  }

  @Override
  /**
   * Finds a destination with the given name.
   *
   * @param name the name of the destination to find
   * @return a CompletableFuture that resolves to the found DestinationImpl object
   */
  public CompletableFuture<DestinationImpl> find(String name) {
    return manager.find(name);
  }

  @Override
  /**
   * Finds or creates a destination with the given name.
   *
   * @param name the name of the destination
   * @return a CompletableFuture that resolves to the created or found DestinationImpl object
   * @throws IOException if an I/O error occurs
   */
  public CompletableFuture<DestinationImpl> findOrCreate(String name, DestinationAuthorisationCheck authCheck) throws IOException {
    return manager.findOrCreate(name, authCheck);
  }

  @Override
  /**
   * Finds or creates a destination with the given name and destination type.
   *
   * @param name             the name of the destination
   * @param destinationType  the type of the destination
   * @return                 a CompletableFuture that resolves to the created or found DestinationImpl object
   * @throws IOException     if an I/O error occurs while finding or creating the destination
   */
  public CompletableFuture<DestinationImpl> findOrCreate(String name, DestinationType destinationType, DestinationAuthorisationCheck authCheck) throws IOException {
    return manager.findOrCreate(name, destinationType, authCheck);
  }

  @Override
  /**
   * Creates a new destination with the given name and destination type.
   *
   * @param name The name of the destination.
   * @param destinationType The type of the destination.
   * @return A CompletableFuture that completes with the created destination.
   * @throws IOException If an I/O error occurs.
   */
  public CompletableFuture<DestinationImpl> create(@NonNull @NotNull String name, @NonNull @NotNull DestinationType destinationType, DestinationAuthorisationCheck authCheck) throws IOException {
    return manager.create(name, destinationType, authCheck);
  }

  @Override
  /**
   * Deletes a destination implementation.
   *
   * @param destinationImpl The destination implementation to be deleted.
   * @return A CompletableFuture representing the asynchronous operation of deleting the destination implementation.
   */
  public CompletableFuture<DestinationImpl> delete(DestinationImpl destinationImpl) {
    return manager.delete(destinationImpl);
  }

  @Override
  /**
   * Retrieves a map of DestinationImpl objects based on the provided DestinationFilter.
   *
   * @param filter The DestinationFilter used to filter the DestinationImpl objects.
   * @return A map of DestinationImpl objects that match the provided filter.
   */
  public Map<String, DestinationImpl> get(DestinationFilter filter) {
    return manager.get(filter);
  }

  @Override
  /**
   * Adds a listener to the DestinationManager.
   *
   * @param listener the DestinationManagerListener to be added
   */
  public void addListener(DestinationManagerListener listener) {
    manager.addListener(listener);
  }

  @Override
  /**
   * Removes a listener from the DestinationManager.
   *
   * @param listener the DestinationManagerListener to be removed
   */
  public boolean removeListener(DestinationManagerListener listener) {
    return manager.removeListener(listener);
  }

}
