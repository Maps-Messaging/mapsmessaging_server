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

package io.mapsmessaging.network.protocol.impl.n2k.msg.source;


import io.mapsmessaging.canbus.j1939.n2k.codec.FieldValueSource;
import io.mapsmessaging.network.protocol.impl.n2k.msg.AisClassBPositionReport;

import java.util.HashMap;
import java.util.Map;

public class AisClassBPositionFieldValueSource implements FieldValueSource {

  private static final String MESSAGE_ID = "messageId";
  private static final String REPEAT_INDICATOR = "repeatIndicator";
  private static final String USER_ID = "userId";
  private static final String LONGITUDE = "longitude";
  private static final String LATITUDE = "latitude";
  private static final String POSITION_ACCURACY = "positionAccuracy";
  private static final String RAIM = "raim";
  private static final String TIME_STAMP = "timeStamp";
  private static final String COG = "cog";
  private static final String SOG = "sog";
  private static final String COMMUNICATION_STATE_INFORMATION = "communicationStateInformation";
  private static final String AIS_TRANSCEIVER_INFORMATION = "aisTransceiverInformation";
  private static final String HEADING = "heading";
  private static final String REGIONAL_APPLICATION = "regionalApplication";
  private static final String REGIONAL_APPLICATION_B = "regionalApplicationB";
  private static final String UNIT_TYPE = "unitType";
  private static final String INTEGRATED_DISPLAY = "integratedDisplay";
  private static final String DSC = "dsc";
  private static final String BAND = "band";
  private static final String CAN_HANDLE_MSG_22 = "canHandleMsg22";
  private static final String AIS_MODE = "aisMode";
  private static final String AIS_COMMUNICATION_STATE = "aisCommunicationState";

  private final Map<String, Long> longValues;
  private final Map<String, Double> doubleValues;

  public AisClassBPositionFieldValueSource(AisClassBPositionReport report) {
    this.longValues = new HashMap<>();
    this.doubleValues = new HashMap<>();

    putLong(MESSAGE_ID, report.getMessageId());
    putLong(REPEAT_INDICATOR, report.getRepeatIndicator());
    putLong(USER_ID, report.getUserId());

    putDouble(LONGITUDE, report.getLongitude());
    putDouble(LATITUDE, report.getLatitude());

    putLong(POSITION_ACCURACY, report.getPositionAccuracy());
    putLong(RAIM, report.getRaim());
    putLong(TIME_STAMP, report.getTimeStamp());

    putDouble(COG, report.getCog());
    putDouble(SOG, report.getSog());
    putLong(COMMUNICATION_STATE_INFORMATION, report.getCommunicationStateInformation());
    putLong(AIS_TRANSCEIVER_INFORMATION, report.getAisTransceiverInformation());
    putDouble(HEADING, report.getHeading());

    putLong(REGIONAL_APPLICATION, report.getRegionalApplication());
    putLong(REGIONAL_APPLICATION_B, report.getRegionalApplicationB());

    putLong(UNIT_TYPE, report.getUnitType());
    putLong(INTEGRATED_DISPLAY, report.getIntegratedDisplay());
    putLong(DSC, report.getDsc());
    putLong(BAND, report.getBand());
    putLong(CAN_HANDLE_MSG_22, report.getCanHandleMsg22());
    putLong(AIS_MODE, report.getAisMode());
    putLong(AIS_COMMUNICATION_STATE, report.getAisCommunicationState());
  }

  @Override
  public boolean has(String fieldId) {
    return longValues.containsKey(fieldId) || doubleValues.containsKey(fieldId);
  }

  @Override
  public Long getLong(String fieldId) {
    return longValues.get(fieldId);
  }

  @Override
  public Double getDouble(String fieldId) {
    return doubleValues.get(fieldId);
  }

  @Override
  public String getString(String fieldId) {
    return null;
  }

  private void putLong(String key, Long value) {
    if (value != null) {
      longValues.put(key, value);
    }
  }

  private void putDouble(String key, Double value) {
    if (value != null) {
      doubleValues.put(key, value);
    }
  }
}