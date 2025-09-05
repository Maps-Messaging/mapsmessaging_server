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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet;

import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessageProperty;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessagePropertyFactory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

public class MessageProperties {

  private final List<MessageProperty> properties;
  private final List<MessageProperty> duplicates;
  private final BitSet duplicateCheck;

  public MessageProperties() {
    duplicateCheck = new BitSet(0xff);
    duplicateCheck.clear(); // ensure all is 0
    properties = new ArrayList<>();
    duplicates = new ArrayList<>();
  }

  public MessageProperties add(MessageProperty property) {
    if (property.getId() != MessagePropertyFactory.USER_PROPERTY
        && property.getId() != MessagePropertyFactory.SUBSCRIPTION_IDENTIFIER) {
      if (duplicateCheck.get(property.getId())) {
        duplicates.add(property);
      }
      duplicateCheck.set(property.getId());
    }
    properties.add(property);
    return this;
  }

  public MessageProperty get(int propertyId) {
    for (MessageProperty property : properties) {
      if (property.getId() == propertyId) {
        return property;
      }
    }
    return null;
  }

  public Collection<MessageProperty> values() {
    return properties;
  }

  public List<MessageProperty> duplicates() {
    return duplicates;
  }

  public void remove(MessageProperty property) {
    properties.removeIf(check -> check.getId() == property.getId());
  }

  public void remove(int propertyId) {
    for(MessageProperty property:properties){
      if(property.getId() == propertyId){
        properties.remove(property);
        break;
      }
    }
  }

  public String getDuplicateReport() {
    StringBuilder duplicateReport = new StringBuilder();
    if (!duplicates().isEmpty()) {
      for (MessageProperty duplicate : duplicates()) {
        duplicateReport.append(duplicate.toString());
      }
    }
    return duplicateReport.toString();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Properties > ");
    for (MessageProperty property : properties) {
      sb.append(property.toString()).append(", ");
    }
    return sb.toString();
  }
}
