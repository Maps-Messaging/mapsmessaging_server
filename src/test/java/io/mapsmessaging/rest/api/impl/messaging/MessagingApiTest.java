/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

import io.mapsmessaging.dto.rest.messaging.ConsumeRequestDTO;
import io.mapsmessaging.dto.rest.messaging.MessageDTO;
import io.mapsmessaging.dto.rest.messaging.PublishRequestDTO;
import io.mapsmessaging.dto.rest.messaging.SubscriptionRequestDTO;
import io.mapsmessaging.rest.ApiTestBase;
import io.mapsmessaging.rest.responses.ConsumedResponse;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.rest.responses.SubscriptionDepthResponse;
import io.mapsmessaging.rest.responses.TransactionData;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

public class MessagingApiTest extends ApiTestBase {

  private static final String BASE_PATH = "/api/v1/messaging";

  @Test
  void publish_subscribe_consume_unsubscribe_happyPath() {
    String topic = "/it_messaging_" + System.currentTimeMillis();
    MessageDTO message = new MessageDTO();
    message.setPayload(Base64.getEncoder().encodeToString ("hello".getBytes()));
    Response publishResponse = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body(buildPublishRequest(topic, message))
        .when()
        .post(BASE_PATH + "/publish")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(publishResponse));

    Response subscribeResponse = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body(buildSubscriptionRequest(topic))
        .when()
        .post(BASE_PATH + "/subscribe")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(subscribeResponse));

    Response consumeResponse = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body(buildConsumeRequest(topic, 10))
        .when()
        .post(BASE_PATH + "/consume")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    ConsumedResponse consumedResponse = consumeResponse.as(ConsumedResponse.class);
    Assertions.assertNotNull(consumedResponse);

    Response depthResponse = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body(buildConsumeRequest(topic, 10))
        .when()
        .post(BASE_PATH + "/subscriptionDepth")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    SubscriptionDepthResponse subscriptionDepthResponse = depthResponse.as(SubscriptionDepthResponse.class);
    Assertions.assertNotNull(subscriptionDepthResponse);

    Response unsubscribeResponse = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body(buildSubscriptionRequest(topic))
        .when()
        .post(BASE_PATH + "/unsubscribe")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(unsubscribeResponse));
  }

  @Test
  void publish_blankDestination_returns400() {
    MessageDTO message = new MessageDTO();
    message.setPayload("hello");

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body(buildPublishRequest("   ", message))
        .when()
        .post(BASE_PATH + "/publish")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void subscribe_blankDestination_returns400() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body(buildSubscriptionRequest("   "))
        .when()
        .post(BASE_PATH + "/subscribe")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void commit_emptyEventIds_returns400() {
    TransactionData transactionData = new TransactionData();
    transactionData.setDestinationName("/it_messaging_" + System.currentTimeMillis());
    transactionData.setEventIds(List.of());

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body(transactionData)
        .when()
        .post(BASE_PATH + "/commit")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void abort_emptyEventIds_returns400() {
    TransactionData transactionData = new TransactionData();
    transactionData.setDestinationName("/it_messaging_" + System.currentTimeMillis());
    transactionData.setEventIds(List.of());

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body(transactionData)
        .when()
        .post(BASE_PATH + "/abort")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  private Object buildPublishRequest(String destinationName, MessageDTO message) {
    PublishRequestDTO publishRequestDTO = new PublishRequestDTO();
    publishRequestDTO.setDestinationName(destinationName);
    publishRequestDTO.setMessage(message);
    return publishRequestDTO;
  }

  private Object buildSubscriptionRequest(String destinationName) {
    SubscriptionRequestDTO subscriptionRequestDTO = new SubscriptionRequestDTO();
    subscriptionRequestDTO.setDestinationName(destinationName);
    subscriptionRequestDTO.setTransactional(false);
    subscriptionRequestDTO.setMaxDepth(100);
    subscriptionRequestDTO.setNamedSubscription(null);
    subscriptionRequestDTO.setFilter(null);
    return subscriptionRequestDTO;
  }

  private Object buildConsumeRequest(String destinationName, int depth) {
    ConsumeRequestDTO consumeRequestDTO = new ConsumeRequestDTO();
    consumeRequestDTO.setDestination(destinationName);
    consumeRequestDTO.setDepth(depth);
    return consumeRequestDTO;
  }

  private boolean hasNonBlankStatusMessage(Response response) {
    StatusResponse statusResponse = response.as(StatusResponse.class);
    if (statusResponse == null) {
      return false;
    }
    if (statusResponse.getStatus() == null) {
      return false;
    }
    return !statusResponse.getStatus().trim().isEmpty();
  }
}
