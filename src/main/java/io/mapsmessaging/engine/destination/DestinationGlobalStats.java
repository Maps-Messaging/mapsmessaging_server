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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public class DestinationGlobalStats {
  private final String MESSAGES = "Messages";

  //<editor-fold desc="Global Statistic fields">
  private final LongAdder totalRetained = new LongAdder();
  private final LongAdder totalCurrentSubscriptions = new LongAdder();
  private final LongAdder totalPublishedMessages = new LongAdder();
  private final LongAdder totalSubscribedMessages = new LongAdder();
  private final LongAdder totalNoInterestMessages = new LongAdder();
  private final LongAdder totalRetrievedMessages = new LongAdder();
  private final LongAdder totalExpiredMessages = new LongAdder();
  private final LongAdder totalDeliveredMessages = new LongAdder();


  private final Stats totalPublishedMessagesAverages;
  private final Stats totalSubscribedMessagesAverages;
  private final Stats totalNoInterestMessagesAverages;
  private final Stats totalRetrievedMessagesAverages;
  private final Stats totalExpiredMessagesAverages;
  private final Stats totalDeliveredMessagesAverages;

  private final Statistics globalAverageStatistics;

  public DestinationGlobalStats(StatsType type) {
    globalAverageStatistics = new Statistics();
    totalPublishedMessagesAverages = globalAverageStatistics.create(type,MovingAverageFactory.ACCUMULATOR.ADD, "Published messages", MESSAGES);
    totalSubscribedMessagesAverages = globalAverageStatistics.create(type,MovingAverageFactory.ACCUMULATOR.ADD, "Subscribed messages", MESSAGES);
    totalNoInterestMessagesAverages = globalAverageStatistics.create(type,MovingAverageFactory.ACCUMULATOR.ADD, "No Interest", MESSAGES);
    totalRetrievedMessagesAverages = globalAverageStatistics.create(type,MovingAverageFactory.ACCUMULATOR.ADD, "Retrieved messages", MESSAGES);
    totalExpiredMessagesAverages = globalAverageStatistics.create(type,MovingAverageFactory.ACCUMULATOR.ADD, "Expired messages", MESSAGES);
    totalDeliveredMessagesAverages = globalAverageStatistics.create(type,MovingAverageFactory.ACCUMULATOR.ADD, "Delivered messages", MESSAGES);
  }

  public Map<String, LinkedMovingAverageRecordDTO> getGlobalStats(){
    return globalAverageStatistics.getAverageList()
        .stream()
        .filter(Stats::supportMovingAverage)
        .collect(
            Collectors.toMap(Stats::getName, averages -> ((LinkedMovingAverages) averages).getRecord(), (a, b) -> b, LinkedHashMap::new)
        );
  }

  public void subscriptionAdded() {
    totalCurrentSubscriptions.increment();
  }

  public void subscriptionRemoved() {
    totalCurrentSubscriptions.decrement();
  }

  public void messagePublished() {
    totalPublishedMessages.increment();
    totalPublishedMessagesAverages.increment();
  }

  public void messageSubscribed(int counter) {
    totalSubscribedMessages.add(counter);
    totalSubscribedMessagesAverages.add(counter);
  }

  public void noInterest() {
    totalNoInterestMessages.increment();
    totalNoInterestMessagesAverages.increment();
  }

  public void expiredMessage() {
    totalExpiredMessages.increment();
    totalExpiredMessagesAverages.increment();
  }

  public void retrievedMessage() {
    totalRetrievedMessages.increment();
    totalRetrievedMessagesAverages.increment();
  }

  public void deliveredMessage() {
    totalDeliveredMessages.increment();
    totalDeliveredMessagesAverages.increment();
  }

  public float getPublishedPerSecond(){
    return totalPublishedMessagesAverages.getPerSecond();
  }

  public float getSubscribedPerSecond(){
    return totalSubscribedMessagesAverages.getPerSecond();
  }

  public float getNoInterestPerSecond(){
    return totalNoInterestMessagesAverages.getPerSecond();
  }

  public float getDeliveredPerSecond(){
    return totalDeliveredMessagesAverages.getPerSecond();
  }

  public float getRetrievedPerSecond(){
    return totalRetrievedMessagesAverages.getPerSecond();
  }

  public long getTotalPublishedMessages() {
    return totalPublishedMessages.sum();
  }

  public long getTotalSubscribedMessages() {
    return totalSubscribedMessages.sum();
  }

  public long getTotalNoInterestMessages() {
    return totalNoInterestMessages.sum();
  }

  public long getTotalRetrievedMessages() {
    return totalRetrievedMessages.sum();
  }

  public long getTotalExpiredMessages() {
    return totalExpiredMessages.sum();
  }

  public long getTotalDeliveredMessages() {
    return totalDeliveredMessages.sum();
  }

  public long getTotalCurrentSubscriptions() {
    return totalCurrentSubscriptions.sum();
  }

  public long getTotalRetained() {
    return totalRetained.sum();
  }


  public List<Stats> getGlobalAverages() {
    return globalAverageStatistics.getAverageList();
  }

  //</editor-fold>
  
}
