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

package io.mapsmessaging.state.n2k.msg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AisClassBExtendedPositionReport {

  private Long messageId;
  private Long repeatIndicator;
  private Long userId;
  private Double longitude;
  private Double latitude;
  private Long positionAccuracy;
  private Long raim;
  private Long timeStamp;
  private Double cog;
  private Double sog;
  private Long regionalApplication;
  private Long regionalApplicationB;
  private Long typeOfShip;
  private Double trueHeading;
  private Long gnssType;
  private Double length;
  private Double beam;
  private Double positionReferenceFromStarboard;
  private Double positionReferenceFromBow;
  private String name;
  private Long dte;
  private Long aisMode;
  private Long aisTransceiverInformation;
}