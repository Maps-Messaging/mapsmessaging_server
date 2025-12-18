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

package io.mapsmessaging.api.features;

import lombok.Getter;
import lombok.ToString;

/**
 * Enum of the currently supported destinations
 */
@ToString
public enum DestinationType {

  TOPIC(0, true, false, "Topic", "Reading of messages are not destructive"),
  QUEUE(1, false, false, "Queue", "Reading of events are destructive and only one consumer reads the event"),
  TEMPORARY_TOPIC(2, true, true, "TemporaryTopic", "Reading of messages are not destructive, this topic is deleted when the constructing connection disconnects"),
  TEMPORARY_QUEUE(3, false, true, "TemporaryQueue",
      "Reading of events are destructive and only one consumer reads the event, this topic is deleted when the constructing connection disconnects"),
  SCHEMA(4, true, false, "Schema", "Manages the destinations schema"),
  METRICS(5, true, false, "Metrics", "Manages the destinations metrics");


  // If we receive a destination type not of the following, there is nothing we can or should do
  // but throw a runtime exception
  @java.lang.SuppressWarnings("squid:S00112")
  public static DestinationType getType(String value) {
    if (value.equalsIgnoreCase("topic")) {
      return TOPIC;
    } else if (value.equalsIgnoreCase("queue")) {
      return QUEUE;
    } else if (value.equalsIgnoreCase("TemporaryTopic")) {
      return TEMPORARY_TOPIC;
    } else if (value.equalsIgnoreCase("TemporaryQueue")) {
      return TEMPORARY_QUEUE;
    } else if (value.equalsIgnoreCase("Schema")) {
      return SCHEMA;
    } else if (value.equalsIgnoreCase("Metrics")) {
      return METRICS;
    }
    throw new RuntimeException("No Such Resource Type");
  }

  @Getter
  private final int value;
  @Getter
  private final String descriptiveName;
  @Getter
  private final String description;
  @Getter
  private final boolean isTopic;
  @Getter
  private final boolean isTemporary;

  DestinationType(int value, boolean isTopic, boolean isTemporary, String name, String description) {
    this.value = value;
    this.isTopic = isTopic;
    this.isTemporary = isTemporary;
    this.descriptiveName = name;
    this.description = description;
  }

  public String getName(){
    return descriptiveName;
  }

  public boolean isQueue() {
    return !isTopic;
  }
}
