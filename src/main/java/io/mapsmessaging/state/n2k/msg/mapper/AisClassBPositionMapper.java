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

package io.mapsmessaging.state.n2k.msg.mapper;

import io.mapsmessaging.state.n2k.msg.AisClassBEmitterConfig;
import io.mapsmessaging.state.n2k.msg.AisClassBPositionReport;
import io.mapsmessaging.state.n2k.msg.AisMappingSupport;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.drone.DroneTwin;

import java.time.Instant;
import java.util.Optional;

public class AisClassBPositionMapper {

  private final AisClassBEmitterConfig config;

  public AisClassBPositionMapper(AisClassBEmitterConfig config) {
    this.config = config;
  }

  public Optional<AisClassBPositionReport> map(DroneTwin droneTwin) {
    if (!isEligible(droneTwin)) {
      return Optional.empty();
    }

    AisClassBPositionReport report = new AisClassBPositionReport();
    report.setMessageId(18L);
    report.setRepeatIndicator(config.getRepeatIndicator());
    report.setUserId(droneTwin.getMmsi());

    report.setLongitude(droneTwin.getGeoPosition().getLongitude());
    report.setLatitude(droneTwin.getGeoPosition().getLatitude());

    report.setPositionAccuracy(
        config.getPositionAccuracy() != null
            ? config.getPositionAccuracy()
            : (Boolean.TRUE.equals(droneTwin.getGpsValid()) ? 1L : 0L));
    report.setRaim(config.getRaim());
    report.setTimeStamp(toSecondOfMinute(droneTwin.getNavigationUpdatedAt()));

    report.setCog(toRadians(droneTwin.getCourseOverGroundDegrees()));
    report.setSog(droneTwin.getGroundSpeedMetersPerSecond());
    report.setCommunicationStateInformation(config.getCommunicationStateInformation());
    report.setAisTransceiverInformation(config.getAisTransceiverInformation());
    report.setHeading(toRadians(droneTwin.getHeadingDegrees()));

    report.setRegionalApplication(0L);
    report.setRegionalApplicationB(0L);

    report.setUnitType(config.getUnitType());
    report.setIntegratedDisplay(config.getIntegratedDisplay());
    report.setDsc(config.getDsc());
    report.setBand(config.getBand());
    report.setCanHandleMsg22(config.getCanHandleMsg22());
    report.setAisMode(config.getAisMode());
    report.setAisCommunicationState(config.getAisCommunicationState());

    return Optional.of(report);
  }

  private boolean isEligible(DroneTwin droneTwin) {
    if (droneTwin == null) {
      return false;
    }
    if (droneTwin.getLifecycleStatus() != TwinLifecycleStatus.ACTIVE) {
      return false;
    }
    if (droneTwin.getMmsi() == null) {
      return false;
    }
    if (droneTwin.getGeoPosition() == null) {
      return false;
    }
    if (droneTwin.getGeoPosition().getLatitude() == null) {
      return false;
    }
    if (droneTwin.getGeoPosition().getLongitude() == null) {
      return false;
    }
    if (!Boolean.TRUE.equals(droneTwin.getGpsValid())) {
      return false;
    }
    return droneTwin.getNavigationUpdatedAt() != null;
  }

  private static Long toSecondOfMinute(Instant instant) {
    return AisMappingSupport.toSecondOfMinute(instant);
  }

  private static Double toRadians(Double degrees) {
    if (degrees == null) {
      return null;
    }
    return Math.toRadians(normalizeDegrees(degrees));
  }

  private static double normalizeDegrees(double degrees) {
    return AisMappingSupport.normalizeDegrees(degrees);
  }
}