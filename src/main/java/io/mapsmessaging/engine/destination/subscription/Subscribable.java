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


package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.api.message.Message;

import java.io.Closeable;
import java.util.Queue;

/**
 * This interface is the base for all subscriptions, for topics, queues and shared subscriptions. With or without filtering
 */
public interface Subscribable extends Closeable {

  int register(Message messageIdentifier);

  int register(long messageId);

  boolean hasMessage(long messageIdentifier);

  boolean expired(long messageIdentifier);

  int size();

  String getName();

  Queue<Long> getAll();

  Queue<Long> getAllAtRest();

  void pause();

  void resume();

}
