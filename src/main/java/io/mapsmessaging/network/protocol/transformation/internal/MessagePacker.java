/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.transformation.internal;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import java.util.LinkedHashMap;
import java.util.Map;

public class MessagePacker {

  private final Message message;


  public MessagePacker(Message message) {
    this.message = message;
  }

  public Map<String, String> getMeta(){
    Map<String, String> meta = message.getMeta();
    if(meta == null){
      meta = new LinkedHashMap<>();
    }
    Map<String, String> tmp = new LinkedHashMap<>(meta);
    String route = tmp.get("route");
    if(route == null){
      route = "[]";
    }
    tmp.put("route", updateRoute(route));
    return tmp;
  }

  public Map<String, TypedData> getDataMap(){

    return message.getDataMap();
  }

  public byte[] getOpaqueData(){
    return message.getOpaqueData();
  }

  public Object getCorrelationData(){
    return message.getCorrelationData();
  }

  public String getContentType(){
    return message.getContentType();
  }

  public String getSchemaId(){
    return message.getSchemaId();
  }
  public String getResponseTopic(){
    return message.getResponseTopic();
  }


  public long getIdentifier(){
    return message.getIdentifier();
  }

  public long getExpiry(){
    return message.getExpiry();
  }
  public long getDelayed(){
    return message.getDelayed();
  }
  public long getCreation(){
    return message.getCreation();
  }

  public Priority getPriority(){
    return message.getPriority();
  }

  public QualityOfService getQualityOfService(){
    return message.getQualityOfService();
  }

  public boolean isRetain(){
    return message.isRetain();
  }

  public boolean isStoreOffline(){
    return message.isStoreOffline();
  }
  public boolean isLastMessage(){
    return message.isLastMessage();
  }
  public boolean isUTF8(){
    return message.isUTF8();
  }
  public boolean isCorrelationDataByteArray(){
    return message.isCorrelationDataByteArray();
  }

  private String updateRoute(String route){
    if(route.startsWith("[")){
      route = route.substring(1, route.length()-1);
    }
    if(route.endsWith("]")){
      route = route.substring(0, route.length()-1);
    }
    route = route.trim();
    int originalLength = route.length();
    int newLength = route.replace("{", "").length();
    int count = (originalLength - newLength)+1;

    String server = MessageDaemon.getInstance().getId();
    String hostname = MessageDaemon.getInstance().getHostname();
    long age = System.currentTimeMillis() - getCreation();

    String entry =
        String.format(
            "{\"server\": \"%s\", \"host\": \"%s\", \"age\": %d, \"hop\": %d}",
            server, hostname, age, count);
    if(route.isEmpty()){
      route = entry;
    }
    else{
      route = route + "," + entry;
    }

    return "["+route+"]";
  }
}
