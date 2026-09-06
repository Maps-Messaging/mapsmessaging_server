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

import io.mapsmessaging.dto.rest.config.protocol.impl.CotProtocolConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.detection.MultiByteArrayDetection;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Accepts a raw, unframed stream of Cursor-on-Target (CoT) XML &lt;event&gt; documents and
 * republishes each one, byte for byte, onto a fixed topic. This is an inbound-only listener,
 * it does not initiate outbound connections.
 *
 * <p>Registered via {@code META-INF/services/io.mapsmessaging.network.protocol.ProtocolImplFactory};
 * the {@code cot} listener config is parsed by {@link io.mapsmessaging.config.protocol.impl.CotProtocolConfig}
 * ({@code EndPointConfigFactory} {@code case "cot"}).
 */
public class CotProtocolFactory extends ProtocolImplFactory {

  // Match the start of a CoT stream so the plain-tcp:// accept path (ProtocolFactory.detect)
  // recognises this protocol. NoOpDetection returns false and only works behind an ssl://
  // listener (where the TLS handshake stands in for byte detection); a raw tcp:// listener
  // needs a real Detection or every connection is rejected as "No known protocol detected".
  private static final byte[][] COT_STREAM_STARTS = {
    "<?xml".getBytes(StandardCharsets.US_ASCII),
    "<event".getBytes(StandardCharsets.US_ASCII),
  };

  public CotProtocolFactory() {
    super("cot", "Raw CoT XML passthrough ingest", new MultiByteArrayDetection(COT_STREAM_STARTS, 0));
  }

  @Override
  public Protocol connect(EndPoint endPoint, String sessionId, String username, String password, Map<String, String> topicMap) throws IOException {
    return null; // Inbound listener only, does not support initiating connections
  }

  @Override
  public void create(EndPoint endPoint, Packet packet) throws IOException {
    CotProtocolConfigDTO config = (CotProtocolConfigDTO) endPoint.getConfig().getProtocolConfig("cot");
    new CotProtocol(endPoint, packet, config);
  }

  @Override
  public String getTransportType() {
    return "tcp";
  }
}
