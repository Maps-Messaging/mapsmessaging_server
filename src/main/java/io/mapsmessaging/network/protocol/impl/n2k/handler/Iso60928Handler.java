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

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.canbus.j1939.n2k.codec.N2kMessageParser;
import io.mapsmessaging.network.protocol.impl.n2k.DroneEmissionState;
import io.mapsmessaging.network.protocol.impl.n2k.msg.source.IsoAddressClaimFieldValueSource;
import io.mapsmessaging.state.drone.drone.DroneTwin;

import java.util.Optional;

public class Iso60928Handler extends AbstractDronePgnHandler {

  private static final int PGN = 60928;
  private static final long EMIT_INTERVAL_MILLIS = 60_000L;

  public Iso60928Handler(N2kMessageParser n2kMessageParser) {
    super(PGN, n2kMessageParser);
  }

  @Override
  public Optional<PgnEmission> emit(DroneTwin droneTwin, DroneEmissionState droneEmissionState, long now) {
    PgnEmissionState pgnEmissionState = droneEmissionState.getOrCreateState(getPgn());
    if (!shouldEmit(pgnEmissionState, now)) {
      return Optional.empty();
    }

    byte[] payload = n2kMessageParser.encodeFromSource(getPgn(), new IsoAddressClaimFieldValueSource(MessageDaemon.getInstance().getId()));

    if (payload == null || payload.length == 0) {
      return Optional.empty();
    }

    pgnEmissionState.setLastEmitAt(now);
    pgnEmissionState.setEmitted(true);

    return Optional.of(new PgnEmission(getPgn(), payload));
  }

  private boolean shouldEmit(PgnEmissionState pgnEmissionState, long now) {
    if (!pgnEmissionState.isEmitted()) {
      return true;
    }
    return (now - pgnEmissionState.getLastEmitAt()) >= EMIT_INTERVAL_MILLIS;
  }

  @Override
  public String getName() {
    return "ISO address claim";
  }
}