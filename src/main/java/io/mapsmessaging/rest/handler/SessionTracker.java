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

package io.mapsmessaging.rest.handler;

import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.dto.rest.endpoint.EndPointDetailsDTO;
import io.mapsmessaging.dto.rest.endpoint.EndPointSummaryDTO;
import io.mapsmessaging.dto.rest.protocol.impl.RestProtocolInformation;
import io.mapsmessaging.rest.api.impl.messaging.impl.HttpSessionState;
import io.mapsmessaging.rest.api.impl.messaging.impl.SessionState;
import io.mapsmessaging.rest.auth.BaseAuthenticationFilter;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@WebListener
public class SessionTracker implements HttpSessionListener {
  private static final ConcurrentHashMap<String, HttpSession> sessions = new ConcurrentHashMap<>();
  @Getter
  private static final HttpSessionState sessionStates = new HttpSessionState();

  @Override
  public void sessionCreated(HttpSessionEvent se) {
    HttpSession session = se.getSession();
    sessions.put(session.getId(), session);
  }

  @Override
  public void sessionDestroyed(HttpSessionEvent se) {
    clearSession( se.getSession());
    sessions.remove(se.getSession().getId());
  }

  public static void clearSession(HttpSession httpSession) {
    SessionState sessionState = sessionStates.removeSessionState(httpSession.getId());
    if(sessionState != null) {
      if(sessionState.getSession() != null){
        try {
          SessionManager.getInstance().close(sessionState.getSession(), false);
        } catch (IOException e) {
          // ignore
        }
      }
      if(sessionState.getRestMessageListener() != null){
        sessionState.getRestMessageListener().close();
      }
    }
  }

  public static void scan(){
    long start = System.currentTimeMillis() - BaseAuthenticationFilter.getMaxInactiveInterval();
    for(HttpSession session : sessions.values()){
      if(session.getLastAccessedTime() < start){
        session.invalidate();
      }
    }
  }

  public static List<EndPointSummaryDTO> getConnections(){
    List<EndPointSummaryDTO> connections = new ArrayList<>();
    for(HttpSession session : sessions.values()){
      connections.add(createSummary(session));
    }
    return connections;
  }

  public static EndPointDetailsDTO getConnection(long id){
    for(HttpSession session : sessions.values()){
      Object objId = session.getAttribute("connectionId");
      if(objId != null && objId.equals(id)){
        return createDetails(session);
      }
    }
    return null;
  }

  private static EndPointDetailsDTO createDetails(HttpSession session){
    if(session == null)return null;

    SessionState state = sessionStates.getSessionState(session.getId());


    RestProtocolInformation protocolInformation = new RestProtocolInformation();
    protocolInformation.setTimeout(BaseAuthenticationFilter.getMaxInactiveInterval());
    protocolInformation.setMessageTransformationName("");
    protocolInformation.setSelectorMapping(new LinkedHashMap<>());
    protocolInformation.setDestinationTransformationMapping(new LinkedHashMap<>());

    if(state != null && state.getSession() != null){
      protocolInformation.setSessionInfo(state.getSession().getSessionInformation());
    }

    EndPointDetailsDTO connection = new EndPointDetailsDTO();
    connection.setEndPointSummary(createSummary(session));
    connection.setProtocolInformation(protocolInformation);

    return connection;
  }


  private static EndPointSummaryDTO createSummary(HttpSession session){
    if(session == null)return null;
    EndPointSummaryDTO connection = new EndPointSummaryDTO();
    connection.setId((long) session.getAttribute("connectionId"));
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
