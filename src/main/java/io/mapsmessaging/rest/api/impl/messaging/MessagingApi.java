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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
      description = "Publishes a message to a specified topic")
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

  @Path("/subscribe")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Subscribe to a topic", description = "Subscribes to a specified topic")
  @POST
  public StatusResponse subscribeToTopic(@Valid SubscriptionRequestDTO subscriptionRequest) throws LoginException, IOException {
    hasAccess(RESOURCE);
    Session session = getAuthenticatedSession();
    String destinationName = subscriptionRequest.getDestinationName();
    SubscriptionContextBuilder contextBuilder = new SubscriptionContextBuilder(destinationName, ClientAcknowledgement.AUTO)
        .setReceiveMaximum(10)
        .setQos(QualityOfService.AT_MOST_ONCE)
        .setSelector(subscriptionRequest.getFilter())
        .setRetainHandler(RetainHandler.SEND_IF_NEW)
        .setNoLocalMessages(true);
    session.addSubscription(contextBuilder.build());

    return new StatusResponse("Successfully subscribed to "+destinationName);
  }

  @Path("/consume")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get messages",
      description = "Retrieves messages for a specified subscription")
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
      RestClientConnection restClientConnection = new RestClientConnection(httpSession);
      String username = (String) httpSession.getAttribute("username");
      if(username == null) {
        username = httpSession.getId();
        httpSession.setAttribute("username", username);
      }
      SessionContextBuilder sessionContextBuilder = new SessionContextBuilder(restClientConnection.getName(), restClientConnection)
        .isAuthorized(true)
        .setUsername(username)
        .setPersistentSession(false);
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
