/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.messaging.engine.destination.subscription.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.destination.subscription.OutstandingEventDetails;

public class ClientAcknowledgementController implements AcknowledgementController {

  protected final TreeMap<Long, OutstandingEventDetails> outstanding;
  protected final CreditManager creditManager;

  public ClientAcknowledgementController(CreditManager creditManager) {
    this.creditManager =creditManager;
    outstanding = new TreeMap<>();
  }

  @Override
  public void close() {
    clear();
  }

  @Override
  public void sent(Message message) {
    if (!message.getQualityOfService().equals(QualityOfService.AT_MOST_ONCE)) {
      outstanding.put(message.getIdentifier(), new OutstandingEventDetails(message.getIdentifier(), message.getPriority().getValue()));
    }
    creditManager.decrement();
  }

  @Override
  public long messageSent() {
    return -1;
  }

  @Override
  public void ack(long messageId) {
    removeMessageId(messageId);
  }

  @Override
  public void rollback(long messageId) {
    removeMessageId(messageId);
  }

  @Override
  public boolean canSend() {
    return outstanding.size() < creditManager.getCurrentCredit();
  }

  @Override
  public void clear() {
    outstanding.clear();
  }

  @Override
  public int size() {
    return outstanding.size();
  }

  @Override
  public int getMaxOutstanding() {
    return creditManager.getCurrentCredit();
  }

  @Override
  public boolean setMaxOutstanding(int count) {
    creditManager.setCurrentCredit(count);
    return canSend();
  }

  @Override
  public List<OutstandingEventDetails> getOutstanding() {
    return new ArrayList<>(outstanding.values());
  }

  protected List<OutstandingEventDetails> removeMessageId(long messageId) {
    List<OutstandingEventDetails> response = new ArrayList<>();
    Map.Entry<Long, OutstandingEventDetails> entry = outstanding.firstEntry();
    while (entry != null && entry.getKey() <= messageId) {
      outstanding.remove(entry.getKey());
      response.add(entry.getValue());
      entry = outstanding.firstEntry();
      creditManager.increment();
    }
    return response;
  }

  @Override
  public String getType() {
    return "client";
  }
}
