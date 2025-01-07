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

  @Path("/consume/{destinationName}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get messages",
      description = "Retrieves messages for a specified subscription")
  @POST
  public ConsumedMessages consumeMessages(
      @PathParam("destinationName") String destinationName
  ) {
    hasAccess(RESOURCE);
    RestMessageListener messageListener = (RestMessageListener) getSession().getAttribute("restListener");
    return new ConsumedMessages(destinationName, messageListener.getMessages(destinationName, 100));
  }

  @Path("/consume")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get all messages",
      description = "Retrieves messages for all subscriptions")
  @POST
  public AllConsumedMessages consumeAllMessages() {
    hasAccess(RESOURCE);
    RestMessageListener messageListener = (RestMessageListener) getSession().getAttribute("restListener");
    List<ConsumedMessages> messages = new ArrayList<>();
    for(String destination : messageListener.getKnownDestinations()){
      messages.add(new ConsumedMessages(destination, messageListener.getMessages(destination, 100)));
    }
    return new AllConsumedMessages(messages);
  }

  @GET
  @Path("/subscriptionDepth/{destinationName}")
  @Operation(
      summary = "Get message depth",
      description = "Get the depth of the queue for a specified subscription"
  )
  @Produces(MediaType.APPLICATION_JSON)
  public SubscriptionDepth getSubscriptionDepth(@PathParam("destinationName") String destinationName) {
    hasAccess(RESOURCE);
    RestMessageListener messageListener = (RestMessageListener) getSession().getAttribute("restListener");
    int depth = messageListener.subscriptionDepth(destinationName);
    return new SubscriptionDepth(depth, destinationName);
  }

  @GET
  @Path("/subscriptionDepth")
  @Operation(
      summary = "Get all message depth",
      description = "Get the depth of the queue for all subscriptions"
  )

  @Produces(MediaType.APPLICATION_JSON)
  public AllSubscriptionDepth getAllSubscriptionDepth() {
    hasAccess(RESOURCE);    // Implement the logic to get the depth of the subscription queue
    RestMessageListener messageListener = (RestMessageListener) getSession().getAttribute("restListener");
    Map<String, Integer> depth = messageListener.subscriptionDepth();
    List<SubscriptionDepth> depths = new ArrayList<>();
    for(Map.Entry<String, Integer> entry : depth.entrySet()) {
      depths.add(new SubscriptionDepth(entry.getValue(), entry.getKey()));
    }
    return new AllSubscriptionDepth(depths);
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
