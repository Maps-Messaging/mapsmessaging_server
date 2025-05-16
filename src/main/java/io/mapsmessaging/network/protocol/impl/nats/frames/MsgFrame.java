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

package io.mapsmessaging.network.protocol.impl.nats.frames;

import lombok.Getter;
import lombok.ToString;

import java.nio.charset.StandardCharsets;

/**
 * Parses the incoming NATS MSG frame from server.
 */
@Getter
@ToString
public class MsgFrame extends PayloadFrame {

  public MsgFrame(int maxBufferSize) {
    super(maxBufferSize);
  }

  @Override
  public PayloadFrame duplicate() {
    return copy(new MsgFrame(maxBufferSize));
  }

  @Override
  public byte[] getCommand() {
    return "MSG".getBytes(StandardCharsets.US_ASCII);
  }

  @Override
  public NatsFrame instance() {
    return new MsgFrame(maxBufferSize);
  }

}
