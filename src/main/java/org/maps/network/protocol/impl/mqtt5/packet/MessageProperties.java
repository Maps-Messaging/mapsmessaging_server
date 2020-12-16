/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.mqtt5.packet;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import org.maps.network.protocol.impl.mqtt5.packet.properties.MessageProperty;
import org.maps.network.protocol.impl.mqtt5.packet.properties.MessagePropertyFactory;

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

  public MessageProperty get(int propertyId){
    for(MessageProperty property:properties){
      if(property.getId() == propertyId){
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
    properties.remove(propertyId);
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
}
