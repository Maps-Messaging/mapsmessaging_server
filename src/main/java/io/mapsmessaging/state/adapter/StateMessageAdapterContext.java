/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.state.adapter;

import io.mapsmessaging.state.config.TwinManagerConfigDTO;
import io.mapsmessaging.state.drone.core.TwinManager;
import lombok.Getter;
import lombok.NonNull;
import io.mapsmessaging.state.task.CanonicalTaskRegistry;
import org.jetbrains.annotations.NotNull;

@Getter
public class StateMessageAdapterContext {

  @NonNull
  @NotNull
  private final TwinManager twinManager;

  @NonNull
  @NotNull
  private final TwinManagerConfigDTO config;

  @NonNull
  @NotNull
  private final CanonicalTaskRegistry taskRegistry;

  public StateMessageAdapterContext(TwinManager twinManager, TwinManagerConfigDTO config) {
    this(twinManager, config, new CanonicalTaskRegistry());
  }

  public StateMessageAdapterContext(
      TwinManager twinManager,
      TwinManagerConfigDTO config,
      CanonicalTaskRegistry taskRegistry) {
    this.twinManager = twinManager;
    this.config = config;
    this.taskRegistry = taskRegistry;
  }
}
