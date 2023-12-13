/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.data.messaging.PublishRequest;
import io.mapsmessaging.rest.data.messaging.SubscriptionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Messaging Interface")
@Path(URI_PATH + "/messaging")
public class MessagingApi extends BaseRestApi {

  @Path("/publish")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Publish a message", description = "Publishes a message to a specified topic")
  @ApiResponse(responseCode = "200",
      description = "Message published successfully",
      content = @Content(schema = @Schema(implementation = String.class)))
  @POST
  public Response publishMessage(@Valid PublishRequest publishRequest) {

    // Implement the logic to publish a message
    // messagingService.publish(publishRequest.getTopic(), publishRequest.getMessage());
    return Response.ok().entity("Message published successfully").build();
  }

  @Path("/subscribe")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Subscribe to a topic", description = "Subscribes to a specified topic")
  @ApiResponse(responseCode = "200", description = "Subscribed to topic successfully",
      content = @Content(schema = @Schema(implementation = String.class)))
  @POST
  public Response subscribeToTopic(@Valid SubscriptionRequest subscriptionRequest) {
    // Implement the logic to subscribe to a topic
    // messagingService.subscribe(subscriptionRequest.getTopic());
    return Response.ok().entity("Subscribed to topic successfully").build();
  }

  @Path("/consume/{subscriptionName}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get messages", description = "Retrieves messages for a specified subscription")
  @ApiResponse(responseCode = "200",
      description = "Messages retrieved successfully",
      content = @Content(schema = @Schema(implementation = String.class)))
  @POST
  public Response consumeMessages(@PathParam("subscriptionName") String subscriptionName) {
    // Implement the logic to retrieve messages
    // List<Message> messages = messagingService.getMessages(topic);
    return Response.ok().entity("Messages retrieved successfully").build();
  }

  @Path("/consume")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get all messages", description = "Retrieves messages for a specified subscription")
  @ApiResponse(responseCode = "200",
      description = "Messages retrieved successfully",
      content = @Content(schema = @Schema(implementation = String.class)))
  @POST
  public Response consumeAllMessages() {
    // Implement the logic to retrieve messages
    // List<Message> messages = messagingService.getMessages(topic);
    return Response.ok().entity("Messages retrieved successfully").build();
  }

  @GET
  @Path("/subscriptionDepth/{subscriptionName}")
  @Operation(summary = "Get message depth", description = "Get the depth of the queue for a specified subscription")
  @ApiResponse(responseCode = "200",
      description = "Message depth retrieved successfully",
      content = @Content(schema = @Schema(implementation = String.class)))
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSubscriptionDepth(@PathParam("subscriptionName") String subscriptionName) {
    // Implement the logic to get the depth of the subscription queue
    // int depth = messagingService.getSubscriptionDepth(subscriptionName);
    return Response.ok().entity("Queue depth for subscription '" + subscriptionName + "' is: [depth]").build();
  }

  @GET
  @Path("/subscriptionDepth")
  @Operation(summary = "Get all message depth", description = "Get the depth of the queue for all subscriptions")
  @ApiResponse(responseCode = "200",
      description = "Message depths retrieved successfully",
      content = @Content(schema = @Schema(implementation = String.class)))

  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllSubscriptionDepth() {
    // Implement the logic to get the depth of the subscription queue
    // int depth = messagingService.getSubscriptionDepth(subscriptionName);
    return Response.ok().entity("Queue depth for subscription ' is: [depth]").build();
  }
}
