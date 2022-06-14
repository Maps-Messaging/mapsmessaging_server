/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.stomp.frames;

import java.io.IOException;

public abstract class ClientSubscriptionTransaction extends Frame {

  private String subscription;
  private String messageId;
  private String transaction;

  @Override
  public boolean isValid() {
    return subscription != null && messageId != null;
  }

  public String getSubscription() {
    return subscription;
  }

  public String getMessageId() {
    return messageId;
  }

  public String getTransaction() {
    return transaction;
  }

  @Override
  public void parseCompleted() throws IOException {
    subscription = getHeader("subscription");
    messageId = getHeader("message-id");
    transaction = getHeader("transaction");
    super.parseCompleted();
  }
}
