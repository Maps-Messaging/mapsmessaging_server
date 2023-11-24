/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.auth.registry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;
import java.util.UUID;

@Data
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class Quotas implements Serializer<Quotas> {

  public static Quotas createAdminQuota(String username) {
    return new Quotas(username);
  }

  private UUID uuid;
  private final String username;
  private final boolean isAdmin;
  private final boolean accessSystemTopics;
  private final boolean canPublishRetainedMessages;

  private final int publishPerMinute;
  private final int maxPublishSize;

  private final int maxQoS;
  private final int maxConcurrentPersistentSessions;
  private final int maxConcurrentSubscriptions;
  private final int maxConcurrentConnections;
  private final long maxSessionTimeout;

  public Quotas() {
    this("");
  }

  public Quotas(String username) {
    this.username = username;
    isAdmin = true;
    accessSystemTopics = true;
    canPublishRetainedMessages = true;
    publishPerMinute = 0;
    maxPublishSize = 0;
    maxQoS = 0;
    maxConcurrentPersistentSessions = 0;
    maxConcurrentSubscriptions = 0;
    maxConcurrentConnections = 0;
    maxSessionTimeout = 0;
  }

  @Override
  public void serialize(@NotNull DataOutput2 dataOutput2, @NotNull Quotas userDetails) throws IOException {
    dataOutput2.writeUTF(userDetails.username);
    dataOutput2.writeLong(userDetails.uuid.getMostSignificantBits());
    dataOutput2.writeLong(userDetails.uuid.getLeastSignificantBits());
    dataOutput2.writeBoolean(userDetails.isAdmin);
    dataOutput2.writeBoolean(userDetails.accessSystemTopics);
    dataOutput2.writeBoolean(userDetails.canPublishRetainedMessages);
    dataOutput2.writeInt(userDetails.publishPerMinute);
    dataOutput2.writeInt(userDetails.maxPublishSize);
    dataOutput2.writeInt(userDetails.maxQoS);
    dataOutput2.writeInt(userDetails.maxConcurrentConnections);
    dataOutput2.writeInt(userDetails.maxConcurrentSubscriptions);
    dataOutput2.writeInt(userDetails.maxConcurrentPersistentSessions);
    dataOutput2.writeLong(userDetails.maxSessionTimeout);
  }

  @Override
  public Quotas deserialize(@NotNull DataInput2 dataInput2, int i) throws IOException {
    String user = dataInput2.readUTF();
    java.util.UUID id = new UUID(dataInput2.readLong(), dataInput2.readLong());
    boolean admin = dataInput2.readBoolean();
    boolean system = dataInput2.readBoolean();
    boolean retain = dataInput2.readBoolean();
    int pub = dataInput2.readInt();
    int pubSize = dataInput2.readInt();
    int qos = dataInput2.readInt();
    int persistentSessions = dataInput2.readInt();
    int concurrentSub = dataInput2.readInt();
    int concurrentCon = dataInput2.readInt();
    long timeout = dataInput2.readLong();
    return new Quotas(id, user, admin, system, retain, pub, pubSize, qos, persistentSessions, concurrentSub, concurrentCon, timeout);
  }
}
