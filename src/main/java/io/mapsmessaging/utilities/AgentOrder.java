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

import lombok.Getter;

/**
 * This is the AgentOrder class.
 * It represents an order for starting and stopping an agent.
 * The startOrder and stopOrder properties define the order in which the agent should be started and stopped.
 * The agent property holds the reference to the agent object.
 */
public class AgentOrder {

  /**
   * Represents the start order of agents.
   */
  @Getter
  private final int startOrder;

  /**
   * Represents the stop order of agents.
   */
  @Getter
  private final int stopOrder;

  /**
   * Reference to the agent order.
   */
  @Getter
  private final Agent agent;

  /**
   * Constructor for the AgentOrder class.
   *
   * @param start the start order of agents
   * @param stop the stop order of agents
   * @param agent the agent object
   */
  public AgentOrder(int start, int stop, Agent agent) {
    stopOrder = stop;
    startOrder = start;
    this.agent = agent;
  }
}
