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

import io.mapsmessaging.MessageDaemon;

import java.util.LinkedHashMap;
import java.util.Map;

public class MetaRouteHandler {

  public static Map<String, String> updateRoute(Map<String, String> meta, long creationTime){
    return updateRoute(MessageDaemon.getInstance().getHostname(), MessageDaemon.getInstance().getId(), meta, creationTime);
  }

  public static Map<String, String> updateRoute( String remoteHost, String remoteId,  Map<String, String> meta,long creationTime){
    if(meta == null){
      meta = new LinkedHashMap<>();
    }
    Map<String, String> tmp = new LinkedHashMap<>(meta);
    String route = tmp.get("route");
    if(route == null){
      route = "[]";
    }
    tmp.put("route", updateRoute(remoteHost, remoteId, route, creationTime));
    return tmp;
  }

  private static String updateRoute( String hostname, String server, String route, long creation){
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

    long age = System.currentTimeMillis() - creation;

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

  private MetaRouteHandler(){}
}
