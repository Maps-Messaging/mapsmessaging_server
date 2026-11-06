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

package io.mapsmessaging.network.protocol.impl.n2k.msg;

import com.google.gson.JsonObject;
import io.mapsmessaging.canbus.j1939.n2k.codec.FieldValueSource;
import io.mapsmessaging.canbus.j1939.n2k.codec.N2kMessageParser;
import io.mapsmessaging.schemas.formatters.impl.CanbusFormatter;
import io.mapsmessaging.state.n2k.msg.AisClassBExtendedPositionReport;
import io.mapsmessaging.state.n2k.msg.AisClassBPositionReport;
import io.mapsmessaging.state.n2k.msg.AisClassBStaticDataPartBReport;
import io.mapsmessaging.state.n2k.msg.source.AisClassBExtendedPositionFieldValueSource;
import io.mapsmessaging.state.n2k.msg.source.AisClassBPositionFieldValueSource;
import io.mapsmessaging.state.n2k.msg.source.AisClassBStaticDataPartBFieldValueSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies server-generated payloads against the bundled canonical CANboat definitions. */
class CanboatFieldValueSourceRoundTripTest {

  private final N2kMessageParser parser;

  CanboatFieldValueSourceRoundTripTest() throws IOException {
    parser = new CanbusFormatter((String) null).getParser();
  }

  @Test
  void address_claim_preserves_manufacturer_and_device_identity() {
    JsonObject packet = roundTrip(60928, new IsoAddressClaimFieldValueSource("stable-device"));
    assertEquals(1850, packet.get("manufacturerCode").getAsInt());
    assertEquals(130, packet.get("deviceFunction").getAsInt());
    assertEquals(25, packet.get("deviceClass").getAsInt());
    assertEquals(4, packet.get("industryGroup").getAsInt());
    assertEquals(1, packet.get("arbitraryAddressCapable").getAsInt());
  }

  @Test
  void product_information_preserves_defaults_and_explicit_identity_on_wire() {
    ProductInformationFieldValueSource source =
        new ProductInformationFieldValueSource("Survey Bridge", "4.6", "2", "SN-42");
    byte[] payload = parser.encodeFromSource(126996, source);
    assertEquals(134, payload.length);
    assertEquals(2100, (payload[0] & 0xff) | ((payload[1] & 0xff) << 8));
    JsonObject packet = parser.decodeToJson(126996, payload).getAsJsonObject("packet");
    assertEquals(2.1, packet.get("nmea2000Version").getAsDouble(), 0.000001);
    assertEquals(1, packet.get("productCode").getAsInt());
    assertEquals("Survey Bridge", packet.get("modelId").getAsString());
    assertEquals("4.6", packet.get("softwareVersionCode").getAsString());
    assertEquals("2", packet.get("modelVersion").getAsString());
    assertEquals("SN-42", packet.get("modelSerialCode").getAsString());
    assertEquals(0, packet.get("certificationLevel").getAsInt());
    assertEquals(1, packet.get("loadEquivalency").getAsInt());
  }

  @Test
  void configuration_information_retains_all_three_variable_length_strings() {
    JsonObject packet = roundTrip(126998, new ConfigurationInformationFieldValueSource(
        "Bridge", "AIS Source", "MapsMessaging B.V."));
    assertEquals("Bridge", packet.get("installationDescription1").getAsString());
    assertEquals("AIS Source", packet.get("installationDescription2").getAsString());
    assertEquals("MapsMessaging B.V.", packet.get("manufacturerInformation").getAsString());
  }

  @Test
  void position_report_retains_heading_and_class_b_flags() {
    AisClassBPositionReport report = new AisClassBPositionReport();
    report.setMessageId(18L);
    report.setUserId(123456789L);
    report.setRaim(1L);
    report.setHeading(1.2345);
    report.setUnitType(1L);
    report.setIntegratedDisplay(1L);
    report.setDsc(1L);
    report.setBand(1L);
    report.setCanHandleMsg22(1L);
    report.setAisMode(1L);
    report.setAisCommunicationState(1L);
    JsonObject packet = roundTrip(129039, new AisClassBPositionFieldValueSource(report));
    assertEquals(1.2345, packet.get("heading").getAsDouble(), 0.0001);
    for (String field : new String[]{"raim", "unitType", "integratedDisplay", "dsc", "band",
        "canHandleMsg22", "aisMode", "aisCommunicationState"}) {
      assertEquals(1, packet.get(field).getAsInt(), field);
    }
  }

  @Test
  void extended_position_report_retains_dimensions_reference_and_dte() {
    AisClassBExtendedPositionReport report = new AisClassBExtendedPositionReport();
    report.setMessageId(19L);
    report.setUserId(123456789L);
    report.setTypeOfShip(55L);
    report.setRaim(1L);
    report.setLength(12.3);
    report.setBeam(4.5);
    report.setPositionReferenceFromStarboard(1.2);
    report.setPositionReferenceFromBow(3.4);
    report.setDte(0L);
    report.setAisMode(1L);
    JsonObject packet = roundTrip(129040, new AisClassBExtendedPositionFieldValueSource(report));
    assertEquals(55, packet.get("typeOfShip").getAsInt());
    assertEquals(1, packet.get("raim").getAsInt());
    assertEquals(12.3, packet.get("length").getAsDouble(), 0.1);
    assertEquals(4.5, packet.get("beam").getAsDouble(), 0.1);
    assertEquals(1.2, packet.get("positionReferenceFromStarboard").getAsDouble(), 0.1);
    assertEquals(3.4, packet.get("positionReferenceFromBow").getAsDouble(), 0.1);
    assertEquals(0, packet.get("dte").getAsInt());
    assertEquals(1, packet.get("aisMode").getAsInt());
  }

  @Test
  void static_part_b_retains_callsign_dimensions_and_mothership() {
    AisClassBStaticDataPartBReport report = new AisClassBStaticDataPartBReport();
    report.setMessageId(24L);
    report.setUserId(123456789L);
    report.setTypeOfShip(55L);
    report.setCallsign("MAPS123");
    report.setLength(12.3);
    report.setBeam(4.5);
    report.setPositionReferenceFromStarboard(1.2);
    report.setPositionReferenceFromBow(3.4);
    report.setMothershipUserId(987654321L);
    JsonObject packet = roundTrip(129810, new AisClassBStaticDataPartBFieldValueSource(report));
    assertEquals(55, packet.get("typeOfShip").getAsInt());
    assertEquals("MAPS123", packet.get("callsign").getAsString());
    assertEquals(12.3, packet.get("length").getAsDouble(), 0.1);
    assertEquals(4.5, packet.get("beam").getAsDouble(), 0.1);
    assertEquals(1.2, packet.get("positionReferenceFromStarboard").getAsDouble(), 0.1);
    assertEquals(3.4, packet.get("positionReferenceFromBow").getAsDouble(), 0.1);
    assertEquals(987654321L, packet.get("mothershipUserId").getAsLong());
  }

  private JsonObject roundTrip(int pgn, FieldValueSource source) {
    return parser.decodeToJson(pgn, parser.encodeFromSource(pgn, source)).getAsJsonObject("packet");
  }
}
