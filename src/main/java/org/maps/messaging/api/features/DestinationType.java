/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.messaging.api.features;

/**
 * Enum of the currently supported destinations
 */
public enum DestinationType {

  TOPIC(0, true, false,"Topic", "Reading of messages are not destructive"),
  QUEUE(1, false, false,"Queue", "Reading of events are destructive and only one consumer reads the event"),
  TEMPORARY_TOPIC(2, true, true,"TemporaryTopic", "Reading of messages are not destructive, this topic is deleted when the constructing connection disconnects"),
  TEMPORARY_QUEUE(3, false, true, "TemporaryQueue", "Reading of events are destructive and only one consumer reads the event, this topic is deleted when the constructing connection disconnects");

  // If we receive a destination type not of the following, there is nothing we can or should do
  // but throw a runtime exception
  @java.lang.SuppressWarnings("squid:S00112")
  public static DestinationType getType(String value){
    if(value.equalsIgnoreCase("topic")){
      return TOPIC;
    }
    else if(value.equalsIgnoreCase("queue")){
      return QUEUE;
    }
    else if(value.equalsIgnoreCase("TemporaryTopic")){
      return TEMPORARY_TOPIC;
    }
    else if(value.equalsIgnoreCase("TemporaryQueue")){
      return TEMPORARY_QUEUE;
    }
    throw new RuntimeException("No Such Resource Type");
  }

  private final int value;
  private final String name;
  private final String description;
  private final boolean isTopic;
  private final boolean isTemporary;

  DestinationType(int value, boolean isTopic, boolean isTemporary, String name, String description){
    this.value = value;
    this.isTopic = isTopic;
    this.isTemporary = isTemporary;
    this.name = name;
    this.description = description;
  }

  public boolean isTopic(){
    return isTopic;
  }

  public boolean isQueue(){
    return !isTopic;
  }

  public boolean isTemporary(){
    return isTemporary;
  }

  public int getValue() {
    return value;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public String toString(){
    return name;
  }
}
