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

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.api.features.DestinationType;
import java.io.IOException;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public interface DestinationFactory {

  default String calculateNamespace(String destinationName){
    return destinationName;
  }

  default String calculateOriginalNamespace(String destinationName){
    return destinationName;
  }

  DestinationImpl find(String name);

  DestinationImpl findOrCreate(String name) throws IOException;

  DestinationImpl findOrCreate(String name, DestinationType destinationType) throws IOException;

  DestinationImpl create(@NonNull @NotNull String name, @NonNull @NotNull DestinationType destinationType) throws IOException;

  DestinationImpl delete(DestinationImpl destinationImpl);

  Map<String, DestinationImpl> get(DestinationFilter filter);

  void addListener(DestinationManagerListener subscriptionController);

  void removeListener(DestinationManagerListener subscriptionController);
}
