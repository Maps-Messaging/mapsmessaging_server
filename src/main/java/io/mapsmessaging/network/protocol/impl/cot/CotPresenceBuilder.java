/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.network.protocol.impl.cot;

import io.mapsmessaging.dto.rest.config.protocol.impl.CotPresenceConfigDTO;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

final class CotPresenceBuilder {

  private CotPresenceBuilder() {
  }

  static byte[] build(CotPresenceConfigDTO config, String interfaceName, Instant now)
      throws XMLStreamException {
    validate(config);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    XMLStreamWriter writer = XMLOutputFactory.newFactory()
        .createXMLStreamWriter(output, StandardCharsets.UTF_8.name());
    try {
      writer.writeStartElement("event");
      attribute(writer, "version", "2.0");
      attribute(writer, "uid", resolve(config.getUid(), interfaceName));
      attribute(writer, "type", config.getCotType());
      attribute(writer, "how", config.getHow());
      attribute(writer, "time", now.toString());
      attribute(writer, "start", now.toString());
      attribute(writer, "stale", now.plusSeconds(config.getStaleSeconds()).toString());

      writer.writeEmptyElement("point");
      attribute(writer, "lat", Double.toString(config.getLatitude()));
      attribute(writer, "lon", Double.toString(config.getLongitude()));
      attribute(writer, "hae", Double.toString(config.getHae()));
      attribute(writer, "ce", Double.toString(config.getCe()));
      attribute(writer, "le", Double.toString(config.getLe()));

      writer.writeStartElement("detail");
      writer.writeEmptyElement("contact");
      attribute(writer, "callsign", resolve(config.getCallsign(), interfaceName));
      writeOptionalGroup(writer, config);
      writeOptionalTakv(writer, config);
      writer.writeEndElement();
      writer.writeEndElement();
      writer.flush();
      return output.toByteArray();
    } finally {
      writer.close();
    }
  }

  static void validate(CotPresenceConfigDTO config) {
    requireText(config.getUid(), "presence.uid");
    requireText(config.getCallsign(), "presence.callsign");
    requireText(config.getCotType(), "presence.cotType");
    requireText(config.getHow(), "presence.how");
    requireFinite(config.getLatitude(), "presence.latitude");
    requireFinite(config.getLongitude(), "presence.longitude");
    requireFinite(config.getHae(), "presence.hae");
    requireFinite(config.getCe(), "presence.ce");
    requireFinite(config.getLe(), "presence.le");
    if (config.getLatitude() < -90 || config.getLatitude() > 90) {
      throw new IllegalArgumentException("presence.latitude must be between -90 and 90");
    }
    if (config.getLongitude() < -180 || config.getLongitude() > 180) {
      throw new IllegalArgumentException("presence.longitude must be between -180 and 180");
    }
    if (config.getIntervalSeconds() < 1) {
      throw new IllegalArgumentException("presence.intervalSeconds must be at least 1");
    }
    if (config.getStaleSeconds() <= config.getIntervalSeconds()) {
      throw new IllegalArgumentException("presence.staleSeconds must be greater than presence.intervalSeconds");
    }
  }

  private static void writeOptionalGroup(XMLStreamWriter writer, CotPresenceConfigDTO config)
      throws XMLStreamException {
    if (isBlank(config.getGroupName()) && isBlank(config.getGroupRole())) {
      return;
    }
    writer.writeEmptyElement("__group");
    optionalAttribute(writer, "name", config.getGroupName());
    optionalAttribute(writer, "role", config.getGroupRole());
  }

  private static void writeOptionalTakv(XMLStreamWriter writer, CotPresenceConfigDTO config)
      throws XMLStreamException {
    if (isBlank(config.getDevice())
        && isBlank(config.getPlatform())
        && isBlank(config.getOperatingSystem())
        && isBlank(config.getSoftwareVersion())) {
      return;
    }
    writer.writeEmptyElement("takv");
    optionalAttribute(writer, "device", config.getDevice());
    optionalAttribute(writer, "platform", config.getPlatform());
    optionalAttribute(writer, "os", config.getOperatingSystem());
    optionalAttribute(writer, "version", config.getSoftwareVersion());
  }

  private static void attribute(XMLStreamWriter writer, String name, String value)
      throws XMLStreamException {
    writer.writeAttribute(name, value);
  }

  private static void optionalAttribute(XMLStreamWriter writer, String name, String value)
      throws XMLStreamException {
    if (!isBlank(value)) {
      writer.writeAttribute(name, value);
    }
  }

  private static String resolve(String value, String interfaceName) {
    return value.replace("{interfaceName}", interfaceName);
  }

  private static void requireText(String value, String name) {
    if (isBlank(value)) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
  }

  private static void requireFinite(double value, String name) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be finite");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
