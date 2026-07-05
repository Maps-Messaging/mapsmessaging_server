/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with
 *  the License.
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

import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;

import java.util.List;
import java.util.Objects;

public record UxvModelCommandSet(
        UxvOperation operation,
        String modelName,
        List<MavlinkMessage> messages) {

    public UxvModelCommandSet {
        operation = Objects.requireNonNull(operation, "operation must not be null");
        modelName = Objects.requireNonNull(modelName, "modelName must not be null");
        messages = List.copyOf(Objects.requireNonNull(messages, "messages must not be null"));
    }

    public static UxvModelCommandSet empty(UxvOperation operation, String modelName) {
        return new UxvModelCommandSet(operation, modelName, List.of());
    }

    public static UxvModelCommandSet of(UxvOperation operation, String modelName, MavlinkMessage message) {
        return new UxvModelCommandSet(operation, modelName, List.of(Objects.requireNonNull(message, "message must not be null")));
    }

    public static UxvModelCommandSet of(UxvOperation operation, String modelName, List<MavlinkMessage> messages) {
        return new UxvModelCommandSet(operation, modelName, messages);
    }
}