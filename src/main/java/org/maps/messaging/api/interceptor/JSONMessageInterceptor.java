/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.messaging.api.interceptor;

import org.json.JSONException;
import org.json.JSONObject;
import org.maps.messaging.MessageDaemon;
import org.maps.messaging.api.message.Message;


public class JSONMessageInterceptor implements MessageInterceptor {

  @Override
  public String name() {
    return "JSON Interceptor";
  }

  @Override
  public Message process(String destinationName, Message original) {
    byte[] data = original.getOpaqueData();
    try {
      JSONObject jsonData = new JSONObject(new String(data));
      JSONObject jsonServer = new JSONObject();
      jsonServer.put("time", System.currentTimeMillis());
      jsonServer.put("id", MessageDaemon.getInstance().getId());
      JSONObject json = new JSONObject();
      json.put("data", jsonData);
      json.put("server", jsonServer);
      original.updateOpaqueData(json.toString().getBytes());
    } catch (JSONException e) {
      return original;
    }
    return original;
  }

  @Override
  public Message emit(String destinationName){
    return null;
  }
}
