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
import io.mapsmessaging.network.protocol.impl.n2k.msg.AisClassBStaticDataPartAReport;
import io.mapsmessaging.network.protocol.impl.n2k.msg.mapper.AisClassBStaticDataPartAMapper;
import io.mapsmessaging.network.protocol.impl.n2k.msg.source.AisClassBStaticDataPartAFieldValueSource;
import io.mapsmessaging.state.drone.drone.DroneTwin;


import java.util.Objects;
import java.util.Optional;

public class Ais129809Handler extends AbstractDronePgnHandler {

  private static final int PGN = 129809;
  private final long intervalMillis;

  private final AisClassBStaticDataPartAMapper mapper;

  public Ais129809Handler(N2kMessageParser n2kMessageParser, AisClassBEmitterConfig config, long intervalMillis) {
    super(PGN, n2kMessageParser);
    this.intervalMillis = intervalMillis;
    this.mapper = new AisClassBStaticDataPartAMapper(config);
  }

  @Override
  public Optional<PgnEmission> emit(DroneTwin droneTwin, DroneEmissionState droneEmissionState, long now) {
    if (droneTwin == null || droneTwin.getMmsi() == null) {
      return Optional.empty();
    }

    PgnEmissionState pgnEmissionState = droneEmissionState.getOrCreateState(getPgn());

    Optional<AisClassBStaticDataPartAReport> optionalReport = mapper.map(droneTwin);
    if (optionalReport.isEmpty()) {
      return Optional.empty();
    }

    AisClassBStaticDataPartAReport report = optionalReport.get();
    String signature = buildSignature(report);

    if (!shouldEmit(pgnEmissionState, signature, now)) {
      return Optional.empty();
    }

    byte[] payload =
        n2kMessageParser.encodeFromSource(
            getPgn(),
            new AisClassBStaticDataPartAFieldValueSource(report));

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

    return (now - pgnEmissionState.getLastEmitAt()) >= intervalMillis;
  }

  private String buildSignature(AisClassBStaticDataPartAReport report) {
    StringBuilder builder = new StringBuilder();
    builder.append(report.getUserId()).append('|');
    builder.append(nullToEmpty(report.getName())).append('|');
    builder.append(report.getAisTransceiverInformation()).append('|');
    builder.append(report.getSequenceId());
    return builder.toString();
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  @Override
  public String getName() {
    return "AIS class B \"CS\" static data, part A";
  }
}