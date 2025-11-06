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

package io.mapsmessaging.rest.api.impl.messaging;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.dto.rest.messaging.AsyncMessageDTO;
import io.mapsmessaging.dto.rest.messaging.ConsumeRequestDTO;
import io.mapsmessaging.dto.rest.messaging.PublishRequestDTO;
import io.mapsmessaging.dto.rest.messaging.SubscriptionRequestDTO;
import io.mapsmessaging.engine.session.SessionContext;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.api.impl.messaging.impl.RestClientConnection;
import io.mapsmessaging.rest.api.impl.messaging.impl.RestMessageListener;
import io.mapsmessaging.rest.api.impl.messaging.impl.SessionState;
import io.mapsmessaging.rest.handler.SessionTracker;
import io.mapsmessaging.rest.responses.*;
import io.mapsmessaging.rest.token.TokenManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Messaging Interface")
@Path(URI_PATH + "/messaging")
public class MessagingApi extends BaseRestApi {

  private static final String RESOURCE = "messaging";

  @Path("/publish")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Publish a message",
      description = "Publishes a message to a specified topic with support for headers, properties, and delivery options",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  @POST
  public StatusResponse publishMessage(@Valid PublishRequestDTO publishRequest) throws LoginException, IOException {
    hasAccess(RESOURCE);
    Session session = getAuthenticatedSession();
    String destinationName = publishRequest.getDestinationName();
    Destination destination = session.findDestination(destinationName, DestinationType.TOPIC).join();
    MessageBuilder messageBuilder = new MessageBuilder(publishRequest.getMessage());
    
    if (publishRequest.getHeaders() != null && !publishRequest.getHeaders().isEmpty()) {
      for (Map.Entry<String, String> entry : publishRequest.getHeaders().entrySet()) {
        messageBuilder.addMetaData(entry.getKey(), entry.getValue());
      }
    }
    
    destination.storeMessage(messageBuilder.build());
    return new StatusResponse("Message published successfully");
  }


  @Path("/unsubscribe")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Unsubscribe from a topic", description = "Unsubscribes from a specified topic",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      })
  @POST
  public StatusResponse unsubscribeToTopic(@Valid SubscriptionRequestDTO subscriptionRequest) {
    hasAccess(RESOURCE);
    String destinationName = subscriptionRequest.getDestinationName();
    HttpSession httpSession = getSession();
    SessionState state = SessionTracker.getSessionStates().getSessionState(httpSession.getId());
    RestMessageListener restMessageListener = state.getRestMessageListener();
    restMessageListener.deregisterEventManager(destinationName);
    return new StatusResponse("Successfully unsubscribed to " + destinationName);
  }

  @Path("/subscribe")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Subscribe to a topic", description = "Subscribes to a specified topic",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      })
  @POST
  public StatusResponse subscribeToTopic(@Valid SubscriptionRequestDTO subscriptionRequest) throws LoginException, IOException {
    hasAccess(RESOURCE);
    Session session = getAuthenticatedSession();
    HttpSession httpSession = getSession();
    SubscribedEventManager eventManager = subscribeToTopic(session, subscriptionRequest);
    SessionState state = SessionTracker.getSessionStates().getSessionState(httpSession.getId());
    RestMessageListener restMessageListener = state.getRestMessageListener();
    restMessageListener.registerEventManager(subscriptionRequest.getDestinationName(), session, eventManager);
    return new StatusResponse("Successfully subscribed to " + subscriptionRequest.getDestinationName());
  }

  @GET
  @Path("/sse")
  @Produces(MediaType.SERVER_SENT_EVENTS)
  @Operation(
      summary = "Request a temporary token to access the listed destinations events",
      description = "Retrieve a temporary token that allows access to the destinations event stream",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "String token to use to access the log SSE",
              content = @Content(mediaType = "text")
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource")
      }
  )
  public String requestSseMessageToken(@QueryParam("destination") String destinationName) {
    hasAccess(RESOURCE);
    return TokenManager.getInstance().generateToken(getSession(), destinationName);
  }


  @GET
  @Path("/sse/stream/{token}")
  @Produces(MediaType.SERVER_SENT_EVENTS)
  @Operation(summary = "Expose AsyncMessageDTO in OpenAPI",
      description = "Delivers messages via Server Side Events, supports MQTT wild card plus JMS style filtering",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = AsyncMessageDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource")
      })
  public void subscribeSSE(
      @PathParam("token") String token,
      @Context SseEventSink eventSink,
      @Context Sse sse,
      @BeanParam SubscriptionRequestDTO subscriptionRequest
  ) throws LoginException, IOException {
    hasAccess(RESOURCE);

    if(!TokenManager.getInstance().useToken(getSession(), token, subscriptionRequest.getDestinationName())){
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    Session session = getAuthenticatedSession();
    HttpSession httpSession = getSession();
    SubscribedEventManager eventManager = subscribeToTopic(session, subscriptionRequest);
    SessionState state = SessionTracker.getSessionStates().getSessionState(httpSession.getId());
    state.getRestMessageListener().registerEventManager(subscriptionRequest.getDestinationName(), sse, eventSink, session, eventManager);
  }


  @Path("/commit")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Commit the message",
      description = "Commit the message specifed by the id and the destination name",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  @POST
  public StatusResponse commitMessages(@Valid TransactionData transactionData) {
    hasAccess(RESOURCE);
    SessionState state = SessionTracker.getSessionStates().getSessionState(getSession().getId());
    state.getRestMessageListener().ackReceived(transactionData.getDestinationName(), transactionData.getEventIds());
    return new StatusResponse("Successfully committed messages");
  }

  @Path("/abort")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Abort the message",
      description = "Abort the message specifed by the id and the destination name",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  @POST
  public StatusResponse abortMessages(@Valid TransactionData transactionData) {
    hasAccess(RESOURCE);
    SessionState state = SessionTracker.getSessionStates().getSessionState(getSession().getId());
    state.getRestMessageListener().nakReceived(transactionData.getDestinationName(), transactionData.getEventIds());
    return new StatusResponse("Successfully aborted messages");
  }

  @Path("/consume")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get messages",
      description = "Retrieves messages for a specified subscription",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConsumedResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  @POST
  public ConsumedResponse consumeMessages(@Valid ConsumeRequestDTO consumeRequestDTO) {
    hasAccess(RESOURCE);
    SessionState state = SessionTracker.getSessionStates().getSessionState(getSession().getId());
    RestMessageListener messageListener = state.getRestMessageListener();
    if (consumeRequestDTO.getDestination() == null || consumeRequestDTO.getDestination().isEmpty()) {
      List<ConsumedMessages> messages = new ArrayList<>();
      for (String destination : messageListener.getKnownDestinations()) {
        messages.add(new ConsumedMessages(destination, messageListener.getMessages(destination, consumeRequestDTO.getDepth())));
      }
      return new ConsumedResponse(messages);
    } else {
      ConsumedMessages messages = new ConsumedMessages(consumeRequestDTO.getDestination(), messageListener.getMessages(consumeRequestDTO.getDestination(), consumeRequestDTO.getDepth()));
      return new ConsumedResponse(List.of(messages));
    }
  }

  @POST
  @Path("/subscriptionDepth")
  @Operation(
      summary = "Get message depth",
      description = "Get the depth of the queue for a specified subscription",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubscriptionDepthResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public SubscriptionDepthResponse getSubscriptionDepth(@Valid ConsumeRequestDTO consumeRequestDTO) {
    hasAccess(RESOURCE);
    SessionState state = SessionTracker.getSessionStates().getSessionState(getSession().getId());

    RestMessageListener messageListener = state.getRestMessageListener();
    if (consumeRequestDTO.getDestination() == null || consumeRequestDTO.getDestination().isEmpty()) {
      Map<String, Integer> depth = messageListener.subscriptionDepth();
      List<SubscriptionDepth> depths = new ArrayList<>();
      for (Map.Entry<String, Integer> entry : depth.entrySet()) {
        depths.add(new SubscriptionDepth(entry.getValue(), entry.getKey()));
      }
      return new SubscriptionDepthResponse(depths);
    } else {

      int depth = messageListener.subscriptionDepth(consumeRequestDTO.getDestination());
      SubscriptionDepth subscriptionDepth = new SubscriptionDepth(depth, consumeRequestDTO.getDestination());
      return new SubscriptionDepthResponse(List.of(subscriptionDepth));
    }
  }

  private Session getAuthenticatedSession() throws LoginException, IOException {
    HttpSession httpSession = getSession();
    SessionState state = SessionTracker.getSessionStates().getSessionState(getSession().getId());
    if(state == null) {
      boolean persistentSession = false;
      Object obj = httpSession.getAttribute("persistentSession");
      if (obj instanceof Boolean bool) {
        persistentSession = bool;
      }
      RestClientConnection restClientConnection = new RestClientConnection(httpSession);

      Object id = httpSession.getAttribute("sessionId");
      String sessionId = id == null ? restClientConnection.getName() : id.toString();
      String username = (String) httpSession.getAttribute("username");
      if (username == null) {
        username = httpSession.getId();
        httpSession.setAttribute("username", username);
      }
      SessionContextBuilder sessionContextBuilder = new SessionContextBuilder(sessionId, restClientConnection)
          .setPersistentSession(persistentSession)
          .isAuthorized(true)
          .setUsername(username);

      SessionContext sessionContext = sessionContextBuilder.build();
      RestMessageListener restMessageListener = new RestMessageListener();
      Session session = SessionManager.getInstance().create(sessionContext, restMessageListener);
      SessionState sessionState = new SessionState(session, restMessageListener);
      SessionTracker.getSessionStates().setSessionState(getSession().getId(), sessionState);
      return session;
    }
    if(state.getSession() != null){
      return state.getSession();
    }
    throw new WebApplicationException("Access denied", Response.Status.FORBIDDEN);
  }

  private SubscribedEventManager subscribeToTopic(Session session, SubscriptionRequestDTO subscriptionRequest) throws IOException {
    return session.addSubscription(buildContext(subscriptionRequest).build());
  }

  private SubscriptionContextBuilder buildContext(SubscriptionRequestDTO subscriptionRequest) {
    String destinationName = subscriptionRequest.getDestinationName();
    QualityOfService qos = subscriptionRequest.isTransactional() ? QualityOfService.AT_LEAST_ONCE : QualityOfService.AT_MOST_ONCE;
    ClientAcknowledgement clientAcknowledgement = subscriptionRequest.isTransactional() ? ClientAcknowledgement.INDIVIDUAL : ClientAcknowledgement.AUTO;
    SubscriptionContextBuilder contextBuilder = new SubscriptionContextBuilder(destinationName, clientAcknowledgement)
        .setReceiveMaximum(subscriptionRequest.getMaxDepth())
        .setQos(qos)
        .setRetainHandler(RetainHandler.SEND_ALWAYS)
        .setNoLocalMessages(true);
    String sharedName = subscriptionRequest.getNamedSubscription();
    if (sharedName != null && !sharedName.isEmpty()) {
      contextBuilder.setSharedName(sharedName);
    }
    String filter = subscriptionRequest.getFilter();
    if (filter != null && !filter.trim().isEmpty()) {
      contextBuilder.setSelector(filter);
    }
    return contextBuilder;
  }
}
