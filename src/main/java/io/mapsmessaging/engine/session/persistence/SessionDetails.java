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

package io.mapsmessaging.engine.session.persistence;

import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.security.access.Group;
import io.mapsmessaging.security.access.Identity;
import io.mapsmessaging.security.identity.GroupEntry;
import io.mapsmessaging.utilities.PersistentObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class SessionDetails extends PersistentObject {

  private int version;
  private String sessionName;
  private String uniqueId;
  private long internalUnqueId;
  private long expiryTime;
  private boolean needsUpdating;

  private Identity identity;

  private List<SubscriptionContext> subscriptionContextList = new ArrayList<>();

  public SessionDetails() {

  }

  public SessionDetails(String sessionName, String uniqueId, long internalUnqueId, long expiryTime) {
    this.sessionName = sessionName;
    this.uniqueId = uniqueId;
    this.internalUnqueId = internalUnqueId;
    this.expiryTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expiryTime);
    version = 4;
  }


  public SessionDetails(InputStream inputStream) throws IOException {
    version = readInt(inputStream);
    boolean hasInternalUnqueId = true;
    expiryTime = System.currentTimeMillis() + (60L * 60L * 1000L);
    needsUpdating = true;
    switch (version) {
      case 2 -> {
        sessionName = readString(inputStream);
        identity = null;
      }
      case 3 -> {
        sessionName = readString(inputStream);
        expiryTime = readLong(inputStream);
        uniqueId = readString(inputStream);
        identity = null;
        needsUpdating = false;
      }
      case 4 -> {
        sessionName = readString(inputStream);
        expiryTime = readLong(inputStream);
        uniqueId = readString(inputStream);
        identity = readIdentity(inputStream);
      }
      default -> {
        sessionName = new String(readFullBuffer(inputStream, version));
        uniqueId = readString(inputStream);
        hasInternalUnqueId = false;
        version = 4;
      }
    }
    if(hasInternalUnqueId){
      internalUnqueId = readLong(inputStream);
    }
    else{
      internalUnqueId = 0;
    }
    int subListSize = readInt(inputStream);
    for(int x=0;x<subListSize;x++){
      subscriptionContextList.add(new SubscriptionContext(inputStream, internalUnqueId));
    }
  }

  public Map<String, SubscriptionContext> getSubscriptionContextMap(){
    Map<String, SubscriptionContext> map = new LinkedHashMap<>();
    for(SubscriptionContext context:subscriptionContextList){
      map.put(context.getAlias(), context); // Pre-populate with persistent data
    }
    return map;
  }

  public void clearSubscriptions() {
    subscriptionContextList.clear();
  }

  public void save(OutputStream outputStream) throws IOException {
    writeInt(outputStream, version);
    writeString(outputStream, sessionName);
    writeLong(outputStream, expiryTime);
    writeString(outputStream, uniqueId);
    saveIdentity(outputStream, identity);
    writeLong(outputStream, internalUnqueId);
    writeInt(outputStream, subscriptionContextList.size());
    for(SubscriptionContext subscriptionContext:subscriptionContextList){
      subscriptionContext.save(outputStream);
    }
  }

  private Identity readIdentity(InputStream inputStream) throws IOException {
    int hasIdentity = inputStream.read();
    Identity identity = null;
    if(hasIdentity == 1){
      UUID id = UUID.fromString(readString(inputStream));
      String username = readString(inputStream);
      int attributeSize = readInt(inputStream);
      Map<String, String> attributes = new HashMap<>();
      for(int i=0;i<attributeSize;i++){
        String key =  readString(inputStream);
        String value = readString(inputStream);
        attributes.put(key, value);
      }
      int groupCount = readInt(inputStream);
      List<Group> groups = new ArrayList<>();
      for(int i=0;i<groupCount;i++){
        groups.add(readGroup(inputStream));
      }

      identity = new Identity(id, username, attributes, groups);
    }
    return identity;
  }

  private  void saveIdentity(OutputStream outputStream, Identity ident) throws IOException {
    if(ident == null){
      outputStream.write(0);
    }
    else{
      outputStream.write(1);
      writeString(outputStream, ident.getId().toString());
      writeString(outputStream, ident.getUsername());
      if(ident.getAttributes() != null && !ident.getAttributes().isEmpty()){
        writeInt(outputStream, ident.getAttributes().size());
        for(Map.Entry<String, String> attribute:ident.getAttributes().entrySet()){
          writeString(outputStream, attribute.getKey());
          writeString(outputStream, attribute.getValue());
        }
      }
      else{
        writeInt(outputStream, 0);
      }
      if(ident.getGroupList() != null && !ident.getGroupList().isEmpty()){
        writeInt(outputStream, ident.getGroupList().size());
        for(Group group:ident.getGroupList()){
          saveGroup(outputStream, group);
        }
      }
      else{
        writeInt(outputStream, 0);
      }
    }
  }

  private void saveGroup(OutputStream outputStream, Group group) throws IOException {
    writeString(outputStream, group.getId().toString());
    writeString(outputStream, group.getName());
  }

  private Group readGroup(InputStream inputStream) throws IOException {
    UUID id = UUID.fromString(readString(inputStream));
    String name = readString(inputStream);
    return new Group(id, new GroupEntry(name,new TreeSet<>() ));
  }

}
