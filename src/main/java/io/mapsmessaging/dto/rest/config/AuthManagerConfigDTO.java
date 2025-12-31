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

package io.mapsmessaging.dto.rest.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Auth Manager Configuration DTO")
public class AuthManagerConfigDTO extends BaseConfigDTO {

  @Schema(description = "Indicates if authentication is enabled", example = "true")
  protected boolean authenticationEnabled;

  @Schema(description = "Indicates if authorization is enabled", example = "true")
  protected boolean authorisationEnabled;

  @Schema(description = "Configuration properties for authentication", implementation = Map.class)
  protected Map<String, Object> authConfig;

  @Schema(description = "Minimum password length.", example = "12", minimum = "1")
  protected int minimumPasswordLength = 12;

  @Schema(description = "Maximum password length.", example = "128", minimum = "6")
  protected int maximumPasswordLength = 128;

  @Schema(description = "Minimum number of lowercase letters required.", example = "1", minimum = "0")
  protected int minimumLowercase = 1;

  @Schema(description = "Minimum number of uppercase letters required.", example = "1", minimum = "0")
  protected int minimumUppercase = 1;

  @Schema(description = "Minimum number of digits required.", example = "1", minimum = "0")
  protected int minimumDigits = 1;

  @Schema(description = "Minimum number of special characters required.", example = "1", minimum = "0")
  protected int minimumSpecial = 1;

  @Schema(
      description = "Allowed special characters set. If empty/null, any non-alphanumeric character may be treated as special (implementation-defined).",
      example = "!@#$%^&*()-_=+[]{};:,.?/\\|"
  )
  protected String allowedSpecialCharacters = "!@#$%^&*()-_=+[]{};:,.?/\\|";

  @Schema(
      description = "If true, whitespace characters are rejected in passwords.",
      example = "true"
  )
  protected boolean rejectWhitespace = true;

  @Schema(
      description = "If true, passwords containing the username (case-insensitive) are rejected.",
      example = "true"
  )
  protected boolean rejectContainsUsername = true;

  @Schema(
      description = "Maximum number of identical consecutive characters allowed (e.g., 'aaa'). Use 0 to disable.",
      example = "3",
      minimum = "0"
  )
  protected int maximumConsecutiveIdenticalCharacters = 3;

  @Schema(
      description = "If set, overrides composition rules. Java regex pattern the password must match. Leave null to use the composition settings.",
      example = "^(?=.{12,128}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()\\-_=+\\[\\]{};:,.?/\\\\|]).*$"
  )
  protected String passwordRegex;

  @Schema(
      description = "Number of previous passwords that cannot be reused. Use 0 to disable.",
      example = "5",
      minimum = "0"
  )
  protected int passwordHistoryCount = 0;

  @Schema(
      description = "Maximum password age in days before forcing a reset. Use 0 to disable.",
      example = "90",
      minimum = "0"
  )
  protected int passwordMaxAgeDays = 0;

  @Schema(
      description = "Number of consecutive authentication failures required before an account is locked.",
      example = "5",
      minimum = "1"
  )
  protected int maxFailuresBeforeLock = 5;

  @Schema(
      description = "Initial lock duration in seconds once the failure threshold is exceeded. Subsequent locks may increase up to the configured maximum.",
      example = "30",
      minimum = "1"
  )
  protected int initialLockSeconds = 30;

  @Schema(
      description = "Maximum lock duration in seconds. Lock times will not grow beyond this value regardless of repeated failures.",
      example = "900",
      minimum = "1"
  )
  protected int maxLockSeconds = 900;

  @Schema(
      description = "Time in seconds after which recorded authentication failures decay if no new failures occur. This allows accounts to recover naturally over time.",
      example = "900",
      minimum = "1"
  )
  protected int failureDecaySeconds = 900;

  @Schema(
      description = "Enable progressive response delays before lockout is triggered. When enabled, each failed attempt adds a short delay before authentication is processed.",
      example = "true"
  )
  protected boolean enableSoftDelay = true;

  @Schema(
      description = "Additional delay in milliseconds applied per authentication failure when soft delay is enabled.",
      example = "200",
      minimum = "0"
  )
  protected int softDelayMillisPerFailure = 200;

  @Schema(
      description = "Maximum cumulative soft delay in milliseconds that can be applied before authentication processing. Prevents unbounded delays.",
      example = "2000",
      minimum = "0"
  )
  protected int maxSoftDelayMillis = 2000;


  public AuthManagerConfigDTO(AuthManagerConfigDTO source) {
    this.authenticationEnabled = source.authenticationEnabled;
    this.authorisationEnabled = source.authorisationEnabled;
    this.authConfig = source.authConfig == null ? null : Map.copyOf(source.authConfig);

    this.minimumPasswordLength = source.minimumPasswordLength;
    this.maximumPasswordLength = source.maximumPasswordLength;
    this.minimumLowercase = source.minimumLowercase;
    this.minimumUppercase = source.minimumUppercase;
    this.minimumDigits = source.minimumDigits;
    this.minimumSpecial = source.minimumSpecial;
    this.allowedSpecialCharacters = source.allowedSpecialCharacters;
    this.rejectWhitespace = source.rejectWhitespace;
    this.rejectContainsUsername = source.rejectContainsUsername;
    this.maximumConsecutiveIdenticalCharacters = source.maximumConsecutiveIdenticalCharacters;
    this.passwordRegex = source.passwordRegex;
    this.passwordHistoryCount = source.passwordHistoryCount;
    this.passwordMaxAgeDays = source.passwordMaxAgeDays;

    this.maxFailuresBeforeLock = source.maxFailuresBeforeLock;
    this.initialLockSeconds = source.initialLockSeconds;
    this.maxLockSeconds = source.maxLockSeconds;
    this.failureDecaySeconds = source.failureDecaySeconds;
    this.enableSoftDelay = source.enableSoftDelay;
    this.softDelayMillisPerFailure = source.softDelayMillisPerFailure;
    this.maxSoftDelayMillis = source.maxSoftDelayMillis;
  }


}
