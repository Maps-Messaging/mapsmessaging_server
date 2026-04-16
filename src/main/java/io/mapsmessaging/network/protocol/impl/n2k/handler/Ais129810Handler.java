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

package io.mapsmessaging.network.protocol.impl.n2k.handler;

import io.mapsmessaging.canbus.j1939.n2k.codec.N2kMessageParser;
import io.mapsmessaging.network.protocol.impl.n2k.DroneEmissionState;
import io.mapsmessaging.network.protocol.impl.n2k.msg.AisClassBEmitterConfig;
import io.mapsmessaging.network.protocol.impl.n2k.msg.AisClassBStaticDataPartBReport;
import io.mapsmessaging.network.protocol.impl.n2k.msg.mapper.AisClassBStaticDataPartBMapper;
import io.mapsmessaging.network.protocol.impl.n2k.msg.source.AisClassBStaticDataPartBFieldValueSource;
import io.mapsmessaging.state.drone.drone.DroneTwin;


import java.util.Objects;
import java.util.Optional;

public class Ais129810Handler extends AbstractDronePgnHandler {

  private static final int PGN = 129810;
  private static final long HEARTBEAT_INTERVAL_MILLIS = 60_000L;

  private final AisClassBStaticDataPartBMapper mapper;

  public Ais129810Handler(N2kMessageParser n2kMessageParser, AisClassBEmitterConfig config) {
    super(PGN, n2kMessageParser);
    this.mapper = new AisClassBStaticDataPartBMapper(config);
  }

  @Override
  public Optional<PgnEmission> emit(DroneTwin droneTwin, DroneEmissionState droneEmissionState, long now) {
    if (droneTwin == null || droneTwin.getMmsi() == null) {
      return Optional.empty();
    }

    PgnEmissionState pgnEmissionState = droneEmissionState.getOrCreateState(getPgn());

    Optional<AisClassBStaticDataPartBReport> optionalReport = mapper.map(droneTwin);
    if (optionalReport.isEmpty()) {
      return Optional.empty();
    }

    AisClassBStaticDataPartBReport report = optionalReport.get();
    String signature = buildSignature(report);

    if (!shouldEmit(pgnEmissionState, signature, now)) {
      return Optional.empty();
    }

    byte[] payload =
        n2kMessageParser.encodeFromSource(
            getPgn(),
            new AisClassBStaticDataPartBFieldValueSource(report));

    if (payload == null || payload.length == 0) {
      return Optional.empty();
    }

    pgnEmissionState.setLastEmitAt(now);
    pgnEmissionState.setEmitted(true);
    pgnEmissionState.setSignature(signature);

    return Optional.of(new PgnEmission(getPgn(), payload));
  }

  private boolean shouldEmit(PgnEmissionState pgnEmissionState, String signature, long now) {
    if (!pgnEmissionState.isEmitted()) {
      return true;
    }

    if (!Objects.equals(pgnEmissionState.getSignature(), signature)) {
      return true;
    }

    return (now - pgnEmissionState.getLastEmitAt()) >= HEARTBEAT_INTERVAL_MILLIS;
  }

  private String buildSignature(AisClassBStaticDataPartBReport report) {
    StringBuilder builder = new StringBuilder();
    builder.append(report.getUserId()).append('|');
    builder.append(report.getTypeOfShip()).append('|');
    builder.append(nullToEmpty(report.getVendorId())).append('|');
    builder.append(nullToEmpty(report.getCallsign())).append('|');
    builder.append(report.getLength()).append('|');
    builder.append(report.getBeam()).append('|');
    builder.append(report.getPositionReferenceFromStarboard()).append('|');
    builder.append(report.getPositionReferenceFromBow()).append('|');
    builder.append(report.getMothershipUserId()).append('|');
    builder.append(report.getGnssType()).append('|');
    builder.append(report.getAisTransceiverInformation()).append('|');
    builder.append(report.getSequenceId());
    return builder.toString();
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  @Override
  public String getName() {
    return "AIS class B \"CS\" static data, part B ";
  }
}