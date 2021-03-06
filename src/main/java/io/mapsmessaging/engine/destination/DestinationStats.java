/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.utilities.stats.LinkedMovingAverages;
import io.mapsmessaging.utilities.stats.MovingAverageFactory;
import io.mapsmessaging.utilities.stats.MovingAverageFactory.ACCUMULATOR;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class DestinationStats {
  private static final String MESSAGES = "Messages";
  private static final String MICRO_SECONDS = "μs";
  
  //<editor-fold desc="Global Statistic fields">
  private static final LongAdder totalPublishedMessages = new LongAdder();
  private static final LongAdder totalSubscribedMessages = new LongAdder();
  private static final LongAdder totalNoInterestMessages = new LongAdder();
  private static final LongAdder totalRetrievedMessages = new LongAdder();
  private static final LongAdder totalExpiredMessages = new LongAdder();
  private static final LongAdder totalDeliveredMessages = new LongAdder();

  public static long getTotalPublishedMessages(){
    return totalPublishedMessages.sum();
  }
  public static long getTotalSubscribedMessages(){
    return totalSubscribedMessages.sum();
  }
  public static long getTotalNoInterestMessages() {
    return totalNoInterestMessages.sum();
  }
  public static long getTotalRetrievedMessages() {
    return totalRetrievedMessages.sum();
  }
  public static long getTotalExpiredMessages() {
    return totalExpiredMessages.sum();
  }
  public static long getTotalDeliveredMessages() {
    return totalDeliveredMessages.sum();
  }
  //</editor-fold>

  //<editor-fold desc="Statistic fields">
  private final LinkedMovingAverages noInterestMessageAverages;
  private final LinkedMovingAverages publishedMessageAverages;
  private final LinkedMovingAverages subscribedMessageAverages;
  private final LinkedMovingAverages retrievedMessagesAverages;
  private final LinkedMovingAverages expiredMessagesAverages;
  private final LinkedMovingAverages deliveredMessagesAverages;
  private final LinkedMovingAverages subscribedClientAverages;
  private final LinkedMovingAverages storedMessageAverages;
  private final LinkedMovingAverages readTimeAverages;
  private final LinkedMovingAverages writeTimeAverages;
  private final LinkedMovingAverages deleteTimeAverages;
  private final LinkedMovingAverages delayedPublishedMessageAverages;
  private final LinkedMovingAverages transactedPublishedMessageAverages;

  private final List<LinkedMovingAverages> averageList;
  //</editor-fold>

  DestinationStats(){
    averageList = new ArrayList<>();
    noInterestMessageAverages = create(ACCUMULATOR.ADD, "No Interest", MESSAGES );
    publishedMessageAverages = create(ACCUMULATOR.ADD, "Published messages",  MESSAGES );
    subscribedMessageAverages = create(ACCUMULATOR.ADD, "Subscribed messages",  MESSAGES );
    retrievedMessagesAverages = create(ACCUMULATOR.ADD, "Retrieved messages",  MESSAGES );
    expiredMessagesAverages = create(ACCUMULATOR.ADD, "Expired messages",  MESSAGES );
    deliveredMessagesAverages = create(ACCUMULATOR.ADD, "Delivered messages",  MESSAGES );
    subscribedClientAverages = create(ACCUMULATOR.ADD, "Subscribed clients",  "Clients");
    storedMessageAverages = create(ACCUMULATOR.ADD, "Stored messages",  MESSAGES );
    delayedPublishedMessageAverages = create(ACCUMULATOR.ADD, "Delayed Publish messages",  MESSAGES );
    transactedPublishedMessageAverages = create(ACCUMULATOR.ADD, "Transacted Publish messages",  MESSAGES );
    readTimeAverages = create(ACCUMULATOR.AVE, "Time to read messages from resource",  MICRO_SECONDS);
    writeTimeAverages = create(ACCUMULATOR.AVE, "Time to write messages to resource",  MICRO_SECONDS);
    deleteTimeAverages = create(ACCUMULATOR.AVE, "Time to delete messages from resource",  MICRO_SECONDS);
  }

  public List<LinkedMovingAverages> getAverageList(){
    return new ArrayList<>(averageList);
  }

  public void subscriptionAdded(){
    subscribedClientAverages.increment();
  }

  public void subscriptionRemoved(){
    subscribedClientAverages.decrement();
  }

  public void messagePublished(){
    publishedMessageAverages.increment();
    totalPublishedMessages.increment();
  }

  public void messageSubscribed(int counter){
    storedMessageAverages.increment();
    totalSubscribedMessages.add(counter);
    subscribedMessageAverages.add(counter);
  }

  public void noInterest(){
    noInterestMessageAverages.increment();
    totalNoInterestMessages.increment();
  }

  public void expiredMessage(){
    totalExpiredMessages.increment();
    expiredMessagesAverages.add(1);
  }

  public void retrievedMessage(){
    retrievedMessagesAverages.increment();
    totalRetrievedMessages.increment();
  }

  public void removedMessage(){
    storedMessageAverages.decrement();
  }

  public void deliveredMessage(){
    totalDeliveredMessages.increment();
    deliveredMessagesAverages.increment();
  }

  public void messageWriteTime(long write){
    writeTimeAverages.add(write);
  }

  public void messageReadTime(long write){
    readTimeAverages.add(write);
  }

  public LinkedMovingAverages getDeleteTimeAverages() {
    return deleteTimeAverages;
  }

  public LinkedMovingAverages getReadTimeAverages() {
    return readTimeAverages;
  }

  public LinkedMovingAverages getWriteTimeAverages() {
    return writeTimeAverages;
  }

  public LinkedMovingAverages getNoInterestMessageAverages(){
    return noInterestMessageAverages;
  }

  public LinkedMovingAverages getPublishedMessageAverages(){
    return publishedMessageAverages;
  }

  public LinkedMovingAverages getSubscribedMessageAverages(){
    return subscribedMessageAverages;
  }

  public LinkedMovingAverages getRetrievedMessagesAverages(){
    return retrievedMessagesAverages;
  }

  public LinkedMovingAverages getExpiredMessagesAverages(){
    return expiredMessagesAverages;
  }

  public LinkedMovingAverages getDeliveredMessagesAverages(){
    return deliveredMessagesAverages;
  }

  public LinkedMovingAverages getSubscribedClientAverages() {
    return subscribedClientAverages;
  }

  public LinkedMovingAverages getStoredMessageAverages() {
    return storedMessageAverages;
  }

  public LinkedMovingAverages getDelayedPublishedMessageAverages() {
    return delayedPublishedMessageAverages;
  }

  public LinkedMovingAverages getTransactedPublishedMessageAverages() {
    return transactedPublishedMessageAverages;
  }

  private LinkedMovingAverages create(ACCUMULATOR accumulator, String name, String units){
    LinkedMovingAverages linkedMovingAverages = MovingAverageFactory.getInstance().createLinked(accumulator, name, 1, 5, 4, TimeUnit.MINUTES, units);
    averageList.add(linkedMovingAverages);
    return linkedMovingAverages;
  }

}
