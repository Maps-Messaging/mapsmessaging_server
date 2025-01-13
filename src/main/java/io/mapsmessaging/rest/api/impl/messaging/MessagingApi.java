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

package io.mapsmessaging.rest.api.impl.messaging;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.dto.rest.messaging.ConsumeRequestDTO;
import io.mapsmessaging.dto.rest.messaging.PublishRequestDTO;
import io.mapsmessaging.dto.rest.messaging.SubscriptionRequestDTO;
import io.mapsmessaging.engine.session.SessionContext;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.api.impl.messaging.impl.RestClientConnection;
import io.mapsmessaging.rest.api.impl.messaging.impl.RestMessageListener;
import io.mapsmessaging.rest.responses.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.security.auth.login.LoginException;

@Tag(name = "Messaging Interface")
@Path(URI_PATH + "/messaging")
public class MessagingApi extends BaseRestApi {

  private static final String RESOURCE = "messaging";

  @Path("/publish")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Publish a message",
      description = "Publishes a message to a specified topic"
  )
  @POST
  public StatusResponse publishMessage(@Valid PublishRequestDTO publishRequest) throws LoginException, IOException {
    hasAccess(RESOURCE);
    Session session = getAuthenticatedSession();
    String destinationName = publishRequest.getDestinationName();
    Destination destination = session.findDestination(destinationName, DestinationType.TOPIC).join();
    MessageBuilder messageBuilder = new MessageBuilder(publishRequest.getMessage());
    destination.storeMessage(messageBuilder.build());
    return new StatusResponse("Message published successfully");
  }


  @Path("/unsubscribe")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Unsubscribe from a topic", description = "Unsubscribes from a specified topic")
  @POST
  public StatusResponse unsubscribeToTopic(@Valid SubscriptionRequestDTO subscriptionRequest) throws LoginException, IOException {
    hasAccess(RESOURCE);
    String destinationName = subscriptionRequest.getDestinationName();
    HttpSession httpSession = getSession();
    RestMessageListener restMessageListener = (RestMessageListener) httpSession.getAttribute("restListener");
    restMessageListener.deregisterEventManager(destinationName);
    return new StatusResponse("Successfully unsubscribed to "+destinationName);
  }

  @Path("/subscribe")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Subscribe to a topic", description = "Subscribes to a specified topic")
  @POST
  public StatusResponse subscribeToTopic(@Valid SubscriptionRequestDTO subscriptionRequest) throws LoginException, IOException {
    hasAccess(RESOURCE);
    Session session = getAuthenticatedSession();
    SubscribedEventManager eventManager = subscribeToTopic(session, subscriptionRequest);
    HttpSession httpSession = getSession();
    RestMessageListener restMessageListener = (RestMessageListener) httpSession.getAttribute("restListener");
    restMessageListener.registerEventManager(subscriptionRequest.getDestinationName(), session, eventManager);
    return new StatusResponse("Successfully subscribed to "+subscriptionRequest.getDestinationName());
  }

  @GET
  @Path("/sse")
  @Produces(MediaType.SERVER_SENT_EVENTS)
  public void subscribeSSE(
      @Context SseEventSink eventSink,
      @Context Sse sse,
      @QueryParam("destination") String destinationName,
      @QueryParam("transactional")boolean transactional,
      @QueryParam("filter") String filter
  ) throws LoginException, IOException {
    hasAccess(RESOURCE);
    Session session = getAuthenticatedSession();
    SubscriptionRequestDTO req = new SubscriptionRequestDTO();
    req.setDestinationName(destinationName);
    req.setNamedSubscription(destinationName);
    req.setTransactional(transactional);
    req.setFilter(filter != null? filter : "");
    SubscribedEventManager eventManager = subscribeToTopic(session, req);
    HttpSession httpSession = getSession();
    RestMessageListener restMessageListener = (RestMessageListener) httpSession.getAttribute("restListener");
    restMessageListener.registerEventManager(destinationName, sse, eventSink, session, eventManager);
  }

  private SubscribedEventManager subscribeToTopic(Session session,  SubscriptionRequestDTO subscriptionRequest) throws LoginException, IOException {
    String destinationName = subscriptionRequest.getDestinationName();
    QualityOfService qos = subscriptionRequest.isTransactional() ? QualityOfService.AT_LEAST_ONCE : QualityOfService.AT_MOST_ONCE;
    SubscriptionContextBuilder contextBuilder = new SubscriptionContextBuilder(destinationName, ClientAcknowledgement.AUTO)
        .setReceiveMaximum(subscriptionRequest.getMaxDepth())
        .setQos(qos)
        .setSelector(subscriptionRequest.getFilter())
        .setRetainHandler(RetainHandler.SEND_IF_NEW)
        .setNoLocalMessages(true);
    return session.addSubscription(contextBuilder.build());
  }

  @Path("/commit")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Commit the message",
      description = "Commit the message specifed by the id and the destination name"
  )
  @POST
  public StatusResponse commitMessages(@Valid TransactionData transactionData) {
    hasAccess(RESOURCE);
    RestMessageListener messageListener = (RestMessageListener) getSession().getAttribute("restListener");
    messageListener.ackReceived(transactionData.getDestinationName(), transactionData.getEventIds());
    return new StatusResponse("Successfully committed messages");
  }

  @Path("/abort")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Abort the message",
      description = "Abort the message specifed by the id and the destination name"
  )
  @POST
  public StatusResponse abortMessages(@Valid TransactionData transactionData) {
    hasAccess(RESOURCE);
    RestMessageListener messageListener = (RestMessageListener) getSession().getAttribute("restListener");
    messageListener.nakReceived(transactionData.getDestinationName(), transactionData.getEventIds());
    return new StatusResponse("Successfully aborted messages");
  }

  @Path("/consume")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get messages",
      description = "Retrieves messages for a specified subscription"
  )
  @POST
  public ConsumedResponse consumeMessages(
      @Valid ConsumeRequestDTO consumeRequestDTO
  ) {
    hasAccess(RESOURCE);
    RestMessageListener messageListener = (RestMessageListener) getSession().getAttribute("restListener");
    if(consumeRequestDTO.getDestination() == null || consumeRequestDTO.getDestination().isEmpty()) {
      List<ConsumedMessages> messages = new ArrayList<>();
      for(String destination : messageListener.getKnownDestinations()){
        messages.add(new ConsumedMessages(destination, messageListener.getMessages(destination, consumeRequestDTO.getDepth())));
      }
      return new ConsumedResponse(messages);
    }
    else{
      ConsumedMessages messages = new ConsumedMessages(consumeRequestDTO.getDestination(), messageListener.getMessages(consumeRequestDTO.getDestination(), consumeRequestDTO.getDepth()));
      return new ConsumedResponse(List.of(messages));
    }
  }

  @POST
  @Path("/subscriptionDepth")
  @Operation(
      summary = "Get message depth",
      description = "Get the depth of the queue for a specified subscription"
  )
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public SubscriptionDepthResponse getSubscriptionDepth(@Valid ConsumeRequestDTO consumeRequestDTO) {
    hasAccess(RESOURCE);
    RestMessageListener messageListener = (RestMessageListener) getSession().getAttribute("restListener");
    if(consumeRequestDTO.getDestination() == null || consumeRequestDTO.getDestination().isEmpty()) {
      Map<String, Integer> depth = messageListener.subscriptionDepth();
      List<SubscriptionDepth> depths = new ArrayList<>();
      for(Map.Entry<String, Integer> entry : depth.entrySet()) {
        depths.add(new SubscriptionDepth(entry.getValue(), entry.getKey()));
      }
      return new SubscriptionDepthResponse(depths);
    }
    else{

      int depth = messageListener.subscriptionDepth(consumeRequestDTO.getDestination());
      SubscriptionDepth subscriptionDepth = new SubscriptionDepth(depth, consumeRequestDTO.getDestination());
      return new SubscriptionDepthResponse(List.of(subscriptionDepth));
    }
  }

  private Session getAuthenticatedSession() throws LoginException, IOException {
    HttpSession httpSession = getSession();
    Object lookup = httpSession.getAttribute("authenticatedSession");
    if (lookup == null) {
      boolean persistentSession = false;
      Object obj  = httpSession.getAttribute("persistentSession");
      if(obj instanceof Boolean) {
        persistentSession = (Boolean) obj;
      }
      RestClientConnection restClientConnection = new RestClientConnection(httpSession);

      Object id = httpSession.getAttribute("sessionId");
      String sessionId = id == null ? restClientConnection.getName() : id.toString();
      String username = (String) httpSession.getAttribute("username");
      if(username == null) {
        username = httpSession.getId();
        httpSession.setAttribute("username", username);
      }
      SessionContextBuilder sessionContextBuilder = new SessionContextBuilder(sessionId, restClientConnection)
          .setPersistentSession(persistentSession)
          .isAuthorized(true)
          .setUsername(username);

      SessionContext sessionContext = sessionContextBuilder.build();
      RestMessageListener restMessageListener = new RestMessageListener();
      Session session = SessionManager.getInstance().create(sessionContext, restMessageListener );
      httpSession.setAttribute("authenticatedSession", session);
      httpSession.setAttribute("restListener", restMessageListener);
      return session;
    }
    if(lookup instanceof Session) {
      return (Session) lookup;
    }
    throw new WebApplicationException("Access denied", Response.Status.FORBIDDEN);
  }
}
