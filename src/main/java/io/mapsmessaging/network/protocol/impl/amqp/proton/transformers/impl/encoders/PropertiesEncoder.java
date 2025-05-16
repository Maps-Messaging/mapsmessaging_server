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

package io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl.encoders;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import lombok.NonNull;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.UnsignedInteger;
import org.apache.qpid.proton.amqp.messaging.Properties;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PropertiesEncoder {

  private static final List<JMSProperty> encodingList;
  protected static final List<String> propertyNames;

  static {
    encodingList = new ArrayList<>();
    encodingList.add(new JMSReplyToGroupId());
    encodingList.add(new JMSXUserID());
    encodingList.add(new JMSType());
    encodingList.add(new JMSDestination());
    encodingList.add(new JMSReplyTo());
    encodingList.add(new JMSMessageID());
    encodingList.add(new JMSXGroupSeq());
    encodingList.add(new JMSXGroupID());
    encodingList.add(new JMSCorrelationID());
    propertyNames = new ArrayList<>();
    for (JMSProperty jmsProperty : encodingList) {
      propertyNames.add(jmsProperty.getName());
    }
  }

  public static void unpackProperties(@NonNull @NotNull Properties properties, @NonNull @NotNull Map<String, TypedData> map, @NonNull @NotNull MessageBuilder messageBuilder) {
    if (properties.getContentType() != null) {
      messageBuilder.setContentType(properties.getContentType().toString());
    }
    if (properties.getAbsoluteExpiryTime() != null) {
      long expiry = properties.getAbsoluteExpiryTime().getTime();
      expiry = (expiry - System.currentTimeMillis());
      messageBuilder.setMessageExpiryInterval(expiry, TimeUnit.MILLISECONDS);
    }

    if (properties.getCreationTime() != null) {
      messageBuilder.setCreation(properties.getCreationTime().getTime());
    } else {
      messageBuilder.setCreation(System.currentTimeMillis());
    }

    for (JMSProperty jmsProperty : encodingList) {
      jmsProperty.unpack(messageBuilder, properties, map);
    }
  }

  public static void packProperties(@NonNull @NotNull Properties properties, @NonNull @NotNull Message message) {
    for (JMSProperty jmsProperty : encodingList) {
      jmsProperty.pack(message, properties);
    }
    properties.setCreationTime(new Date(message.getCreation()));
  }

  private PropertiesEncoder() {
  }

  static class JMSReplyToGroupId implements JMSProperty {

    private static final String PROPERTY = "JMS_AMQP_REPLY_TO_GROUP_ID";

    public JMSReplyToGroupId() {
      //static function
    }

    @Override
    public String getName() {
      return PROPERTY;
    }

    @Override
    public void unpack(@NonNull @NotNull MessageBuilder messageBuilder, @NonNull @NotNull Properties properties, @NonNull @NotNull Map<String, TypedData> map) {
      if (properties.getReplyToGroupId() != null) {
        map.put(PROPERTY, new TypedData(properties.getReplyToGroupId()));
      }
    }

    @Override
    public void pack(@NonNull @NotNull Message message, @NonNull @NotNull Properties properties) {
      Map<String, TypedData> map = message.getDataMap();
      if (map.containsKey(PROPERTY)) {
        properties.setReplyToGroupId((String) map.get(PROPERTY).getData());
      }
    }
  }

  static class JMSXUserID implements JMSProperty {

    private static final String PROPERTY = "JMSXUserID";

    public JMSXUserID() {
      //static function
    }

    @Override
    public String getName() {
      return PROPERTY;
    }

    @Override
    public void unpack(@NonNull @NotNull MessageBuilder messageBuilder, @NonNull @NotNull Properties properties, @NonNull @NotNull Map<String, TypedData> map) {
      if (properties.getUserId() != null) {
        map.put(PROPERTY, new TypedData(new String(properties.getUserId().getArray())));
      }
    }

    @Override
    public void pack(@NonNull @NotNull Message message, @NonNull @NotNull Properties properties) {
      Map<String, TypedData> map = message.getDataMap();
      if (map.containsKey(PROPERTY)) {
        properties.setUserId(new Binary(((String) map.get(PROPERTY).getData()).getBytes()));
      }
    }
  }

  static class JMSType implements JMSProperty {

    private static final String PROPERTY = "JMSType";

    public JMSType() {
      //static function
    }

    @Override
    public String getName() {
      return PROPERTY;
    }

    @Override
    public void unpack(@NonNull @NotNull MessageBuilder messageBuilder, @NonNull @NotNull Properties properties, @NonNull @NotNull Map<String, TypedData> map) {
      if (properties.getSubject() != null) {
        map.put(PROPERTY, new TypedData(properties.getSubject()));
      }
    }

    @Override
    public void pack(@NonNull @NotNull Message message, @NonNull @NotNull Properties properties) {
      Map<String, TypedData> map = message.getDataMap();
      if (map.containsKey(PROPERTY)) {
        properties.setSubject((String) map.get(PROPERTY).getData());
      }
    }
  }

  static class JMSDestination implements JMSProperty {

    private static final String PROPERTY = "JMSDestination";

    public JMSDestination() {
      //static function
    }

    @Override
    public String getName() {
      return PROPERTY;
    }

    @Override
    public void unpack(@NonNull @NotNull MessageBuilder messageBuilder, @NonNull @NotNull Properties properties, @NonNull @NotNull Map<String, TypedData> map) {
      if (properties.getTo() != null) {
        map.put(PROPERTY, new TypedData(properties.getTo()));
      }
    }

    @Override
    public void pack(@NonNull @NotNull Message message, @NonNull @NotNull Properties properties) {
      Map<String, TypedData> map = message.getDataMap();
      if (map.containsKey(PROPERTY)) {
        properties.setTo((String) map.get(PROPERTY).getData());
      }
    }
  }

  static class JMSReplyTo implements JMSProperty {

    private static final String PROPERTY = "JMSReplyTo";

    public JMSReplyTo() {
      //static function
    }

    @Override
    public String getName() {
      return PROPERTY;
    }

    @Override
    public void unpack(@NonNull @NotNull MessageBuilder messageBuilder, @NonNull @NotNull Properties properties, @NonNull @NotNull Map<String, TypedData> map) {
      if (properties.getReplyTo() != null) {
        map.put(PROPERTY, new TypedData(properties.getReplyTo()));
      }
    }

    @Override
    public void pack(@NonNull @NotNull Message message, @NonNull @NotNull Properties properties) {
      Map<String, TypedData> map = message.getDataMap();
      if (map.containsKey(PROPERTY)) {
        properties.setReplyTo((String) map.get(PROPERTY).getData());
      }
    }
  }

  static class JMSMessageID implements JMSProperty {

    private static final String PROPERTY = "JMSMessageID";

    public JMSMessageID() {
      //static function
    }

    @Override
    public String getName() {
      return PROPERTY;
    }

    @Override
    public void unpack(@NonNull @NotNull MessageBuilder messageBuilder, @NonNull @NotNull Properties properties, @NonNull @NotNull Map<String, TypedData> map) {
      if (properties.getMessageId() != null) {
        map.put(PROPERTY, new TypedData(properties.getMessageId()));
      }
    }

    @Override
    public void pack(@NonNull @NotNull Message message, @NonNull @NotNull Properties properties) {
      Map<String, TypedData> map = message.getDataMap();
      if (map.containsKey(PROPERTY)) {
        properties.setMessageId(map.get(PROPERTY).getData());
      }
    }
  }

  static class JMSXGroupSeq implements JMSProperty {

    private static final String PROPERTY = "JMSXGroupSeq";

    public JMSXGroupSeq() {
      //static function
    }

    @Override
    public String getName() {
      return PROPERTY;
    }

    @Override
    public void unpack(@NonNull @NotNull MessageBuilder messageBuilder, @NonNull @NotNull Properties properties, @NonNull @NotNull Map<String, TypedData> map) {
      if (properties.getGroupSequence() != null) {
        map.put(PROPERTY, new TypedData(properties.getGroupSequence().intValue()));
      }
    }

    @Override
    public void pack(@NonNull @NotNull Message message, @NonNull @NotNull Properties properties) {
      Map<String, TypedData> map = message.getDataMap();
      if (map.containsKey(PROPERTY)) {
        properties.setGroupSequence(new UnsignedInteger((Integer) map.get(PROPERTY).getData()));
      }
    }
  }

  static class JMSXGroupID implements JMSProperty {

    private static final String PROPERTY = "JMSXGroupID";

    public JMSXGroupID() {
      //static function
    }

    @Override
    public String getName() {
      return PROPERTY;
    }

    @Override
    public void unpack(@NonNull @NotNull MessageBuilder messageBuilder, @NonNull @NotNull Properties properties, @NonNull @NotNull Map<String, TypedData> map) {
      if (properties.getGroupId() != null) {
        map.put(PROPERTY, new TypedData(properties.getGroupId()));
      }
    }

    @Override
    public void pack(@NonNull @NotNull Message message, @NonNull @NotNull Properties properties) {
      Map<String, TypedData> map = message.getDataMap();
      if (map.containsKey(PROPERTY)) {
        properties.setGroupId(map.get(PROPERTY).getData().toString());
      }
    }
  }

  static class JMSCorrelationID implements JMSProperty {

    private static final String PROPERTY = "JMSCorrelationID";

    public JMSCorrelationID() {
      //static function
    }

    @Override
    public String getName() {
      return PROPERTY;
    }

    @Override
    public void unpack(@NonNull @NotNull MessageBuilder messageBuilder, @NonNull @NotNull Properties properties, @NonNull @NotNull Map<String, TypedData> map) {
      if (properties.getCorrelationId() != null) {
        Object obj = properties.getCorrelationId();
        if (obj instanceof Binary) {
          Binary binary = (Binary) obj;
          messageBuilder.setCorrelationData(binary.getArray());
        } else {
          messageBuilder.setCorrelationData(obj.toString());
        }
      }
    }

    @Override
    public void pack(@NonNull @NotNull Message message, @NonNull @NotNull Properties properties) {
      byte[] correlation = message.getCorrelationData();
      if (correlation != null) {
        if (message.isCorrelationDataByteArray()) {
          properties.setCorrelationId(new Binary(correlation));
        } else {
          properties.setCorrelationId(new String(correlation));
        }
      }
    }
  }

  interface JMSProperty {

    void unpack(@NonNull @NotNull MessageBuilder messageBuilder, @NonNull @NotNull Properties properties, @NonNull @NotNull Map<String, TypedData> map);

    void pack(@NonNull @NotNull Message message, @NonNull @NotNull Properties properties);

    String getName();
  }
}
