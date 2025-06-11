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

package io.mapsmessaging.engine.destination.subscription.transaction;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.OutstandingEventDetails;

import java.util.ArrayList;
import java.util.List;

public class AutoAcknowledgementController implements AcknowledgementController {

  private final List<OutstandingEventDetails> standardReturn;
  private final CreditManager creditManager;

  public AutoAcknowledgementController(CreditManager creditManager) {
    this.creditManager = creditManager;
    standardReturn = new ArrayList<>();
  }

  @Override
  public void close() {
    standardReturn.clear();
    clear();
  }

  @Override
  public void rollback(long messageId) {
    ack(messageId);
  }

  @Override
  public List<OutstandingEventDetails> getOutstanding() {
    return standardReturn;
  }

  @Override
  public void sent(Message message) {
    standardReturn.add(new OutstandingEventDetails(message.getIdentifier(), message.getPriority().getValue()));
    creditManager.decrement();
  }

  // They are auto acknowledge on send
  public int size() {
    return standardReturn.size();
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
  public void ack(long messageId) {
    if (!standardReturn.isEmpty()) {
      standardReturn.remove(0);
    }
  }

  @Override
  public boolean canSend() {
    return standardReturn.size() < creditManager.getCurrentCredit();
  }

  @Override
  public long messageSent() {
    if (!standardReturn.isEmpty()) {
      OutstandingEventDetails outstanding = standardReturn.remove(0);
      if (outstanding != null) {
        creditManager.increment();
        return outstanding.getId();
      }
    }
    return -1;
  }

  @Override
  public void clear() {
    // no need to do anything here
  }

  public String getType() {
    return "auto";
  }
}
