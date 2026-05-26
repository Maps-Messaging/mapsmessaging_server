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

package io.mapsmessaging.state.n2k.msg.source;


import io.mapsmessaging.state.n2k.msg.AisClassBStaticDataPartBReport;

public class AisClassBStaticDataPartBFieldValueSource extends AbstractAisFieldValueSource {

  public AisClassBStaticDataPartBFieldValueSource(AisClassBStaticDataPartBReport report) {
    putLong("messageId", report.getMessageId());
    putLong("repeatIndicator", report.getRepeatIndicator());
    putLong("userId", report.getUserId());
    putLong("typeOfShipAndCargo", report.getTypeOfShip());
    putString("vendorId", report.getVendorId());
    putString("callSign", report.getCallsign());
    putDouble("shipLength", report.getLength());
    putDouble("shipBeam", report.getBeam());
    putDouble("referencePointPositionFromStarboard", report.getPositionReferenceFromStarboard());
    putDouble("referencePointPositionAftOfBow", report.getPositionReferenceFromBow());
    putLong("motherShipMmsi", report.getMothershipUserId());
    putLong("gnssType", report.getGnssType());
    putLong("aisTransceiverInformation", report.getAisTransceiverInformation());
    putLong("sequenceId", report.getSequenceId());
  }
}