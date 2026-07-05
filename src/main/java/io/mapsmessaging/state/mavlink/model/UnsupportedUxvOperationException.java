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

import java.util.Objects;

public class UnsupportedUxvOperationException extends RuntimeException {

    private final String modelName;
    private final UxvOperation operation;

    public UnsupportedUxvOperationException(String modelName, UxvOperation operation) {
        this(modelName, operation, "Operation is not supported by this vehicle model");
    }

    public UnsupportedUxvOperationException(String modelName, UxvOperation operation, String reason) {
        super("UxV model '" + modelName + "' does not support operation '" + operation + "': " + reason);
        this.modelName = Objects.requireNonNull(modelName, "modelName must not be null");
        this.operation = Objects.requireNonNull(operation, "operation must not be null");
    }

    public String getModelName() {
        return modelName;
    }

    public UxvOperation getOperation() {
        return operation;
    }
}