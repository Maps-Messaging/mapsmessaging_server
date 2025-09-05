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

package io.mapsmessaging.utilities;


import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;

/**
 * This is the Agent interface.
 * It defines the methods and properties that an agent should have.
 */
public interface Agent {

  /**
   * Returns the name of the agent.
   *
   * @return the name of the agent
   */
  String getName();

  /**
   * Returns the description of the agent.
   *
   * @return the description of the agent
   */
  String getDescription();

  /**
   * Starts the agent.
   */
  void start();

  /**
   * Stops the agent.
   */
  void stop();

  /**
   * Get the current status of the sub-system
   * @return Status
   */
  SubSystemStatusDTO getStatus();
}
