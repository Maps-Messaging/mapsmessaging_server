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

import java.util.List;
import java.util.Objects;

public record PlanValidation(
        boolean valid,
        List<PlanValidationIssue> issues) {

    public PlanValidation {
        issues = List.copyOf(Objects.requireNonNull(issues, "issues must not be null"));
    }

    public static PlanValidation success() {
        return new PlanValidation(true, List.of());
    }

    public static PlanValidation failure(List<PlanValidationIssue> issues) {
        return new PlanValidation(false, issues);
    }
}