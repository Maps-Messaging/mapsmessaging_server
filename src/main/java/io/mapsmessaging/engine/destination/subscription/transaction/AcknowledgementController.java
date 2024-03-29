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

package io.mapsmessaging.engine.destination.subscription.transaction;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.OutstandingEventDetails;

import java.util.List;

public interface AcknowledgementController {

  void close();

  void sent(Message key);

  int size();

  int getMaxOutstanding();

  boolean setMaxOutstanding(int count);

  String getType();

  void ack(long messageId);

  void rollback(long messageId);

  List<OutstandingEventDetails> getOutstanding();

  boolean canSend();

  long messageSent();

  void clear();
}
