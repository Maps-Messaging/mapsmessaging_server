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

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.api.auth.DestinationAuthorisationCheck;
import io.mapsmessaging.api.features.DestinationType;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface DestinationFactory {

  default String calculateNamespace(String destinationName) {
    return destinationName;
  }

  default String calculateOriginalNamespace(String destinationName) {
    return destinationName;
  }

  CompletableFuture<DestinationImpl> find(String destinationName);

  CompletableFuture<DestinationImpl> findOrCreate(String destinationName, DestinationAuthorisationCheck authCheck) throws IOException;

  CompletableFuture<DestinationImpl> findOrCreate(String destinationName, DestinationType destinationType, DestinationAuthorisationCheck authCheck) throws IOException;

  CompletableFuture<DestinationImpl> create(@NonNull @NotNull String destinationName, @NonNull @NotNull DestinationType destinationType, DestinationAuthorisationCheck authCheck) throws IOException;

  CompletableFuture<DestinationImpl> delete(DestinationImpl destinationImpl);

  Map<String, DestinationImpl> get(DestinationFilter filter);

  void addListener(DestinationManagerListener subscriptionController);

  boolean removeListener(DestinationManagerListener subscriptionController);
}
