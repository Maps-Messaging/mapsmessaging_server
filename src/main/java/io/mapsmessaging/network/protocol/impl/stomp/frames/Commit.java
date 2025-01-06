/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.network.protocol.impl.stomp.frames;

/**
 * Implements the STOMP Connect frame as per https://stomp.github.io/stomp-specification-1.2.html#COMMIT
 */
public class Commit extends ClientTransaction {

  @Override
  byte[] getCommand() {
    return "COMMIT".getBytes();
  }

  @Override
  public Frame instance() {
    return new Commit();
  }

  @Override
  public String toString() {
    return "STOMP Commit[ Transaction:"
        + getTransaction()
        + ", Receipt:"
        + receipt
        + ", Header:"
        + getHeaderAsString()
        + "]";
  }
}
