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

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.dto.rest.stats.LinkedMovingAverageRecordDTO;
import io.mapsmessaging.utilities.stats.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DestinationStats extends Statistics {


  private static final String MESSAGES = "Messages";
  private static final String MICRO_SECONDS = "μs";

  //<editor-fold desc="Statistic fields">
  private final Stats noInterestMessageAverages;
  private final Stats publishedMessageAverages;
  private final Stats subscribedMessageAverages;
  private final Stats retrievedMessagesAverages;
  private final Stats expiredMessagesAverages;
  private final Stats deliveredMessagesAverages;
  private final Stats subscribedClientAverages;
  private final Stats storedMessageAverages;
  private final Stats readTimeAverages;
  private final Stats writeTimeAverages;
  private final Stats deleteTimeAverages;
  private final Stats transactedPublishedMessageAverages;
  private final Stats storedMessageCountAverages;
  private final Stats totalRetained;

  //</editor-fold>

  public DestinationStats(StatsType type) {
    totalRetained = create(type, MovingAverageFactory.ACCUMULATOR.ADD, "Retained messages", MESSAGES);
    noInterestMessageAverages = create(type, MovingAverageFactory.ACCUMULATOR.ADD, "No Interest", MESSAGES);
    publishedMessageAverages = create(type, MovingAverageFactory.ACCUMULATOR.ADD, "Published messages", MESSAGES);
    subscribedMessageAverages = create(type, MovingAverageFactory.ACCUMULATOR.ADD, "Subscribed messages", MESSAGES);
    retrievedMessagesAverages = create(type, MovingAverageFactory.ACCUMULATOR.ADD, "Retrieved messages", MESSAGES);
    expiredMessagesAverages = create(type, MovingAverageFactory.ACCUMULATOR.ADD, "Expired messages", MESSAGES);
    deliveredMessagesAverages = create(type, MovingAverageFactory.ACCUMULATOR.ADD, "Delivered messages", MESSAGES);
    subscribedClientAverages = create(type, MovingAverageFactory.ACCUMULATOR.ADD, "Subscribed clients", "Clients");
    storedMessageAverages = create(type, MovingAverageFactory.ACCUMULATOR.ADD, "Stored messages", MESSAGES);
    storedMessageCountAverages= create(type, MovingAverageFactory.ACCUMULATOR.ADD, "Stored messages", MESSAGES);
    transactedPublishedMessageAverages = create(type, MovingAverageFactory.ACCUMULATOR.ADD, "Transacted Publish messages", MESSAGES);
    readTimeAverages = create(type, MovingAverageFactory.ACCUMULATOR.AVE, "Time to read messages from resource", MICRO_SECONDS);
    writeTimeAverages = create(type, MovingAverageFactory.ACCUMULATOR.AVE, "Time to write messages to resource", MICRO_SECONDS);
    deleteTimeAverages = create(type, MovingAverageFactory.ACCUMULATOR.AVE, "Time to delete messages from resource", MICRO_SECONDS);
  }


  public Map<String, LinkedMovingAverageRecordDTO> getStatistics(){
    return getAverageList()
        .stream()
        .filter(Stats::supportMovingAverage)
        .collect(
            Collectors.toMap(Stats::getName, averages -> ((LinkedMovingAverages) averages).getRecord(), (a, b) -> b, LinkedHashMap::new)
        );
  }

  public void subscriptionAdded() {
    subscribedClientAverages.increment();
    DestinationImpl.getGlobalStats().subscriptionAdded();
  }

  public void subscriptionRemoved() {
    subscribedClientAverages.decrement();
    DestinationImpl.getGlobalStats().subscriptionRemoved();
  }

  public void messagePublished() {
    publishedMessageAverages.increment();
    DestinationImpl.getGlobalStats().messagePublished();
  }

  public void messageSubscribed(int counter) {
    storedMessageAverages.increment();
    subscribedMessageAverages.add(counter);
    DestinationImpl.getGlobalStats().messageSubscribed(counter);
  }

  public void noInterest() {
    noInterestMessageAverages.increment();
    DestinationImpl.getGlobalStats().noInterest();
  }

  public void expiredMessage() {
    expiredMessagesAverages.increment();
    DestinationImpl.getGlobalStats().expiredMessage();
  }

  public void retrievedMessage() {
    retrievedMessagesAverages.increment();
    DestinationImpl.getGlobalStats().retrievedMessage();
  }


  public void storedMessages(int count) {
    storedMessageCountAverages.add(count);
  }


  public void deliveredMessage() {
    deliveredMessagesAverages.increment();
    DestinationImpl.getGlobalStats().deliveredMessage();
  }


  public void messageWriteTime(long write) {
    writeTimeAverages.add(write);
  }


  public void messageReadTime(long write) {
    readTimeAverages.add(write);
  }


  public long getNoInterest() {
    return noInterestMessageAverages.getTotal();
  }


  public long getExpiredMessage() {
    return expiredMessagesAverages.getTotal();
  }


  public long getRetrievedMessage() {
    return retrievedMessagesAverages.getTotal();
  }


  public long getStoredMessages() {
    return storedMessageAverages.getTotal();
  }


  public long getDeliveredMessages() {
    return deliveredMessagesAverages.getTotal();
  }


  public long getMessagePublished() {
    return publishedMessageAverages.getTotal();
  }


  public long getMessageSubscribed() {
    return subscribedMessageAverages.getTotal();
  }


  public long getMessageWriteTime() {
    return writeTimeAverages.getTotal();
  }


  public long getMessageReadTime() {
    return readTimeAverages.getTotal();
  }


  public long getMessageRemovedTime() {
    return deleteTimeAverages.getTotal();
  }


  public long getTransactionalPublished() {
    return transactedPublishedMessageAverages.getTotal();
  }


  public void transactionalPublish() {
    transactedPublishedMessageAverages.increment();
  }


  public void retainedMessages(int count) {
    totalRetained.add(count);
  }


  public void messageDeleteTime(long nano) {
    deleteTimeAverages.add(nano);
  }



}