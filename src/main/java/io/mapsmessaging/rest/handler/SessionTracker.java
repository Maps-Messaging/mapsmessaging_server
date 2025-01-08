/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.rest.handler;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.dto.rest.endpoint.EndPointDetailsDTO;
import io.mapsmessaging.dto.rest.endpoint.EndPointSummaryDTO;
import io.mapsmessaging.dto.rest.protocol.impl.RestProtocolInformation;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@WebListener
public class SessionTracker implements HttpSessionListener {
  private static final ConcurrentHashMap<String, HttpSession> sessions = new ConcurrentHashMap<>();

  @Override
  public void sessionCreated(HttpSessionEvent se) {
    HttpSession session = se.getSession();
    sessions.put(session.getId(), session);
  }

  @Override
  public void sessionDestroyed(HttpSessionEvent se) {
    sessions.remove(se.getSession().getId());
  }

  public static List<EndPointSummaryDTO> getConnections(){
    List<EndPointSummaryDTO> connections = new ArrayList<>();
    for(HttpSession session : sessions.values()){
      connections.add(createSummary(session));
    }
    return connections;
  }

  public static EndPointDetailsDTO getConnection(String id){
    return createDetails(sessions.values().stream().filter(session -> session.getAttribute("name").toString().equals(id)).findFirst().orElse(null));
  }

  private static EndPointDetailsDTO createDetails(HttpSession session){
    if(session == null)return null;
    Session mapsSession = (Session) session.getAttribute("authenticatedSession");


    RestProtocolInformation protocolInformation = new RestProtocolInformation();
    protocolInformation.setTimeout(180_000);
    protocolInformation.setMessageTransformationName("");
    protocolInformation.setSelectorMapping(new LinkedHashMap<>());
    protocolInformation.setDestinationTransformationMapping(new LinkedHashMap<>());

    if(mapsSession != null){
      protocolInformation.setSessionInfo(mapsSession.getSessionInformation());
    }

    EndPointDetailsDTO connection = new EndPointDetailsDTO();
    connection.setEndPointSummary(createSummary(session));
    connection.setProtocolInformation(protocolInformation);

    return connection;
  }


  private static EndPointSummaryDTO createSummary(HttpSession session){
    if(session == null)return null;
    EndPointSummaryDTO connection = new EndPointSummaryDTO();
    connection.setName((String)session.getAttribute("name"));
    connection.setUser((String)session.getAttribute("username"));
    connection.setLastWrite(session.getLastAccessedTime());
    connection.setLastRead(session.getLastAccessedTime());
    connection.setConnectedTimeMs(System.currentTimeMillis() - session.getCreationTime());
    connection.setProtocolName("REST");
    connection.setProtocolVersion("1.1");
    connection.setAdapter("rest-server");
    return connection;
  }
}
