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

package io.mapsmessaging;

import io.mapsmessaging.utilities.Agent;

/**
 * SPI allowing extension jars to contribute a lifecycle {@link Agent} to the server.
 * Implementations are discovered via {@link java.util.ServiceLoader} at startup and
 * started/stopped by {@link SubSystemManager} using the supplied ordering weights.
 */
public interface ServerAgentFactory {

  /** Create the agent instance to be managed by the server. */
  Agent create();

  /** Start ordering weight; lower values start earlier. */
  int startOrder();

  /** Stop ordering weight; lower values stop earlier. */
  int stopOrder();
}
