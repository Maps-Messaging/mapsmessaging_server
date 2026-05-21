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

package io.mapsmessaging.network.protocol.impl.n2k.msg.mapper;


import io.mapsmessaging.network.protocol.impl.n2k.msg.AisClassBEmitterConfig;
import io.mapsmessaging.network.protocol.impl.n2k.msg.AisClassBExtendedPositionReport;
import io.mapsmessaging.network.protocol.impl.n2k.msg.AisMappingSupport;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.drone.DroneTwin;

import java.util.Optional;

public class AisClassBExtendedPositionMapper {

  private final AisClassBEmitterConfig config;

  public AisClassBExtendedPositionMapper(AisClassBEmitterConfig config) {
    this.config = config;
  }

  public Optional<AisClassBExtendedPositionReport> map(DroneTwin droneTwin) {
    if (!AisMappingSupport.hasCorePosition(droneTwin)) {
      return Optional.empty();
    }
    if (droneTwin.getLifecycleStatus() != TwinLifecycleStatus.ACTIVE) {
      return Optional.empty();
    }
    if (!Boolean.TRUE.equals(droneTwin.getGpsValid())) {
      return Optional.empty();
    }
    if (droneTwin.getNavigationUpdatedAt() == null) {
      return Optional.empty();
    }

    AisClassBExtendedPositionReport report = new AisClassBExtendedPositionReport();
    report.setMessageId(19L);
    report.setRepeatIndicator(config.getRepeatIndicator());
    report.setUserId(droneTwin.getMmsi());
    report.setLongitude(droneTwin.getGeoPosition().getLongitude());
    report.setLatitude(droneTwin.getGeoPosition().getLatitude());
    report.setPositionAccuracy(
        config.getPositionAccuracy() != null
            ? config.getPositionAccuracy()
            : (Boolean.TRUE.equals(droneTwin.getGpsValid()) ? 1L : 0L));
    report.setRaim(config.getRaim());
    report.setTimeStamp(AisMappingSupport.toSecondOfMinute(droneTwin.getNavigationUpdatedAt()));
    report.setCog(AisMappingSupport.toRadians(droneTwin.getCourseOverGroundDegrees()));
    report.setSog(droneTwin.getGroundSpeedMetersPerSecond());
    report.setRegionalApplication(0L);
    report.setRegionalApplicationB(0L);
    report.setTypeOfShip(config.getShipType());
    report.setTrueHeading(AisMappingSupport.toRadians(droneTwin.getHeadingDegrees()));
    report.setGnssType(config.getGnssType());
    report.setLength(config.getLengthMeters());
    report.setBeam(config.getBeamMeters());
    report.setPositionReferenceFromStarboard(config.getPositionReferenceFromStarboardMeters());
    report.setPositionReferenceFromBow(config.getPositionReferenceFromBowMeters());
    report.setName(AisMappingSupport.resolveName(droneTwin, config.getName()));
    report.setDte(config.getDte());
    report.setAisMode(config.getAisMode());
    report.setAisTransceiverInformation(config.getAisTransceiverInformation());
    return Optional.of(report);
  }
}