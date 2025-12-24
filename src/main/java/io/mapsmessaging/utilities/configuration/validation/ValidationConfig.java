/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.utilities.configuration.validation;

import lombok.Data;

/**
 * Configuration for YAML validation behavior.
 * Controls when and how YAML configuration files are validated against JSON schemas.
 */
@Data
public class ValidationConfig {

  /**
   * Enable validation at startup when loading YAML files.
   * Default: true
   */
  private boolean validateAtStartup = true;

  /**
   * Enable validation at runtime when configs are updated.
   * Default: false
   */
  private boolean validateAtRuntime = false;

  /**
   * Validation failure mode.
   * FAIL_FAST: Block startup/update and throw exception
   * WARN: Log warning but continue with current/default values
   * SKIP: Skip invalid config and continue with others
   * Default: FAIL_FAST
   */
  private ValidationMode validationMode = ValidationMode.FAIL_FAST;

  /**
   * Cache generated JSON schemas to disk for reuse.
   * Default: true
   */
  private boolean cacheSchemas = true;

  /**
   * Directory to cache schemas (relative to MAPS_HOME).
   * Default: schemas/cache
   */
  private String schemaCacheDir = "schemas/cache";

  /**
   * Enable verbose validation logging.
   * Default: false
   */
  private boolean verboseLogging = false;

  public enum ValidationMode {
    /** Block startup/update and throw exception on validation failure */
    FAIL_FAST,
    /** Log warning but continue with current/default values */
    WARN,
    /** Skip invalid config and continue with others */
    SKIP
  }
}
