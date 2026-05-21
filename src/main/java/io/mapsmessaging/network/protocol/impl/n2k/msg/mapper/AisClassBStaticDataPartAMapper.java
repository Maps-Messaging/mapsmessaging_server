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
import io.mapsmessaging.network.protocol.impl.n2k.msg.AisClassBStaticDataPartAReport;
import io.mapsmessaging.network.protocol.impl.n2k.msg.AisMappingSupport;
import io.mapsmessaging.state.drone.drone.DroneTwin;

import java.util.Optional;

public class AisClassBStaticDataPartAMapper {

  private final AisClassBEmitterConfig config;

  public AisClassBStaticDataPartAMapper(AisClassBEmitterConfig config) {
    this.config = config;
  }

  public Optional<AisClassBStaticDataPartAReport> map(DroneTwin droneTwin) {
    if (droneTwin == null || droneTwin.getMmsi() == null) {
      return Optional.empty();
    }

    AisClassBStaticDataPartAReport report = new AisClassBStaticDataPartAReport();
    report.setMessageId(24L);
    report.setRepeatIndicator(config.getRepeatIndicator());
    report.setUserId(droneTwin.getMmsi());
    report.setName(AisMappingSupport.resolveName(droneTwin, config.getName()));
    report.setAisTransceiverInformation(config.getAisTransceiverInformation());
    report.setSequenceId(AisMappingSupport.deriveSequenceId(droneTwin, config.getSequenceId()));
    return Optional.of(report);
  }
}