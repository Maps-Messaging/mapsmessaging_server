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

package io.mapsmessaging.state.mavlink.model;

public interface UgvModel extends UxvModel {

    default UxvModelCommandSet orbit(UxvCommandContext context, OrbitRequest request) {
        throw unsupported(UxvOperation.ORBIT);
    }

    default UxvModelCommandSet loiter(UxvCommandContext context, LoiterRequest request) {
        throw unsupported(UxvOperation.LOITER);
    }

    default UxvModelCommandSet setSpeed(UxvCommandContext context, double speedMetersPerSecond) {
        throw unsupported(UxvOperation.SET_SPEED);
    }

    default UxvModelCommandSet setHeading(UxvCommandContext context, float headingDegrees) {
        throw unsupported(UxvOperation.SET_HEADING);
    }

    default UxvModelCommandSet setTurnRate(UxvCommandContext context, float turnRateDegreesPerSecond) {
        throw unsupported(UxvOperation.SET_TURN_RATE);
    }
}