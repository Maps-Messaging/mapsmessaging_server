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

import io.mapsmessaging.cot.CotStreamDecoder;
import io.mapsmessaging.cot.CotStreamEncoder;
import io.mapsmessaging.cot.CotValidator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/** Codec for traditional CoT XML streaming connections. */
public final class CotXmlStreamCodec extends CotStreamCodec {

  private static final byte[] XML_DECL_START = "<?xml".getBytes(StandardCharsets.US_ASCII);

  private final CotStreamDecoder streamDecoder;
  private final CotStreamEncoder streamEncoder = new CotStreamEncoder();
  private final CotValidator cotValidator = new CotValidator();

  public CotXmlStreamCodec(int maximumEventSize) {
    streamDecoder = new CotStreamDecoder(maximumEventSize);
  }

  @Override
  public List<byte[]> decode(byte[] chunk) throws IOException {
    return streamDecoder.accept(chunk);
  }

  @Override
  public byte[] encode(byte[] eventXml) throws IOException {
    cotValidator.validate(eventXml);
    return hasXmlDeclaration(eventXml) ? eventXml : streamEncoder.encode(eventXml);
  }

  @Override
  public String getVersion() {
    return "CoT 2.0 XML Streaming";
  }

  private boolean hasXmlDeclaration(byte[] xml) {
    int index = 0;
    while (index < xml.length && Character.isWhitespace(xml[index] & 0xff)) {
      index++;
    }
    return index + XML_DECL_START.length <= xml.length
        && Arrays.equals(
            Arrays.copyOfRange(xml, index, index + XML_DECL_START.length),
            XML_DECL_START);
  }
}

