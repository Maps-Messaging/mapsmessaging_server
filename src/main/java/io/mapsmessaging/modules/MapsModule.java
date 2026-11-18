/*
 *
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

package io.mapsmessaging.modules;

import io.mapsmessaging.utilities.Agent;

import java.util.List;

/**
 * An optional subsystem discoverable using {@link java.util.ServiceLoader}.
 *
 * <p>The host may call {@link #getRestApiPackages()} and
 * {@link #getConfigProviders()} before {@link #initialize(ModuleContext)}.
 * Implementations must therefore make these methods independent of initialization.
 * Start and stop are managed by the server's existing agent lifecycle.
 * Lower priority values run first for both startup and shutdown.
 */
public interface MapsModule extends Agent {

  int getStartPriority();

  int getStopPriority();

  List<String> getRestApiPackages();

  List<ManagerConfigProvider> getConfigProviders();

  void initialize(ModuleContext context);
}
