/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.cot;

import java.io.IOException;
import java.util.List;

/**
 * Converts between a CoT wire stream and the XML events used by the broker and twin pipeline.
 * Each connection owns its codec because decoding retains incomplete input between reads.
 */
public abstract class CotStreamCodec {

  /** Consumes the next wire chunk and returns any complete CoT events as XML. */
  public abstract List<byte[]> decode(byte[] chunk) throws IOException;

  /** Validates and encodes one CoT XML event for the wire. */
  public abstract byte[] encode(byte[] eventXml) throws IOException;

  public abstract String getVersion();
}

