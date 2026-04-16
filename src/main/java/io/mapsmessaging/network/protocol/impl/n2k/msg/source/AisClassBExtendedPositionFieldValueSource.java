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


import io.mapsmessaging.network.protocol.impl.n2k.msg.AisClassBExtendedPositionReport;

public class AisClassBExtendedPositionFieldValueSource extends AbstractAisFieldValueSource {

  public AisClassBExtendedPositionFieldValueSource(AisClassBExtendedPositionReport report) {
    putLong("messageId", report.getMessageId());
    putLong("repeatIndicator", report.getRepeatIndicator());
    putLong("userId", report.getUserId());
    putDouble("longitude", report.getLongitude());
    putDouble("latitude", report.getLatitude());
    putLong("positionAccuracy", report.getPositionAccuracy());
    putLong("raim", report.getRaim());
    putLong("timeStamp", report.getTimeStamp());
    putDouble("cog", report.getCog());
    putDouble("sog", report.getSog());
    putLong("regionalApplication", report.getRegionalApplication());
    putLong("regionalApplicationB", report.getRegionalApplicationB());
    putLong("typeOfShip", report.getTypeOfShip());
    putDouble("trueHeading", report.getTrueHeading());
    putLong("gnssType", report.getGnssType());
    putDouble("length", report.getLength());
    putDouble("beam", report.getBeam());
    putDouble("positionReferenceFromStarboard", report.getPositionReferenceFromStarboard());
    putDouble("positionReferenceFromBow", report.getPositionReferenceFromBow());
    putString("name", report.getName());
    putLong("dte", report.getDte());
    putLong("aisMode", report.getAisMode());
    putLong("aisTransceiverInformation", report.getAisTransceiverInformation());
  }
}