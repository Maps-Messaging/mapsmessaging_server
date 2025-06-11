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

package io.mapsmessaging.network.protocol.transformation.internal;

import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;

import java.util.Map;

public class MessagePacker {

  private final Message message;


  public MessagePacker(Message message) {
    this.message = message;
  }

  public Map<String, String> getMeta(){
    return MetaRouteHandler.updateRoute(message.getMeta(), getCreation());
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


}
