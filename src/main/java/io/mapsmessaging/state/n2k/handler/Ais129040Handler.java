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

package io.mapsmessaging.state.n2k.handler;

import io.mapsmessaging.canbus.j1939.n2k.codec.N2kMessageParser;
import io.mapsmessaging.state.n2k.DroneEmissionState;
import io.mapsmessaging.state.n2k.msg.AisClassBEmitterConfig;
import io.mapsmessaging.state.n2k.msg.AisClassBExtendedPositionReport;
import io.mapsmessaging.state.n2k.msg.mapper.AisClassBExtendedPositionMapper;
import io.mapsmessaging.state.n2k.msg.source.AisClassBExtendedPositionFieldValueSource;
import io.mapsmessaging.state.drone.drone.DroneTwin;


import java.util.Optional;

public class Ais129040Handler extends AbstractDronePgnHandler {

  private static final int PGN = 129040;
  private final long intervalMillis;

  private final AisClassBExtendedPositionMapper mapper;

  public Ais129040Handler(N2kMessageParser n2kMessageParser, AisClassBEmitterConfig config, long intervalMillis) {
    super(PGN, n2kMessageParser);
    this.intervalMillis = intervalMillis;
    this.mapper = new AisClassBExtendedPositionMapper(config);
  }



  @Override
  public Optional<PgnEmission> emit(DroneTwin droneTwin, DroneEmissionState droneEmissionState, long now) {
    if (!isActiveAndPositionValid(droneTwin)) {
      return Optional.empty();
    }

    PgnEmissionState pgnEmissionState = droneEmissionState.getOrCreateState(getPgn());

    if (!shouldEmit(droneTwin, pgnEmissionState, now)) {
      return Optional.empty();
    }

    Optional<AisClassBExtendedPositionReport> optionalReport = mapper.map(droneTwin);
    if (optionalReport.isEmpty()) {
      return Optional.empty();
    }

    byte[] payload =
        n2kMessageParser.encodeFromSource(
            getPgn(),
            new AisClassBExtendedPositionFieldValueSource(optionalReport.get()));

    if (payload == null || payload.length == 0) {
      return Optional.empty();
    }

    pgnEmissionState.setLastEmitAt(now);
    pgnEmissionState.setEmitted(true);
    updateMotionSnapshot(droneTwin, pgnEmissionState);

    return Optional.of(new PgnEmission(getPgn(), payload));
  }

  private boolean shouldEmit(DroneTwin droneTwin, PgnEmissionState pgnEmissionState, long now) {
    if (!pgnEmissionState.isEmitted()) {
      return true;
    }

    long elapsed = now - pgnEmissionState.getLastEmitAt();
    if (elapsed >= intervalMillis) {
      return true;
    }
    return hasMaterialMotionChange(droneTwin, pgnEmissionState);
  }

  @Override
  public String getName() {
    return "AIS class B extended position report";
  }
}