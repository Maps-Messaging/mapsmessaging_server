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

package io.mapsmessaging.network.io.security;

import lombok.Getter;

@Getter
public class VerificationResult {

  private final boolean valid;
  private final FailureReason reason;
  private final String algorithm;
  private final int signatureSize;
  private final int packetLength;
  private final int offset;
  private final int length;
  private final Throwable error;

  private VerificationResult(
      boolean valid,
      FailureReason reason,
      String algorithm,
      int signatureSize,
      int packetLength,
      int offset,
      int length,
      Throwable error
  ) {
    this.valid = valid;
    this.reason = reason;
    this.algorithm = algorithm;
    this.signatureSize = signatureSize;
    this.packetLength = packetLength;
    this.offset = offset;
    this.length = length;
    this.error = error;
  }

  public static VerificationResult ok(String algorithm, int signatureSize, int packetLength, int offset, int length) {
    return new VerificationResult(true, FailureReason.OK, algorithm, signatureSize, packetLength, offset, length, null);
  }

  public static VerificationResult fail(
      FailureReason reason,
      String algorithm,
      int signatureSize,
      int packetLength,
      int offset,
      int length
  ) {
    return new VerificationResult(false, reason, algorithm, signatureSize, packetLength, offset, length, null);
  }

  public static VerificationResult error(
      String algorithm,
      int signatureSize,
      int packetLength,
      int offset,
      int length,
      Throwable error
  ) {
    return new VerificationResult(false, FailureReason.INTERNAL_ERROR, algorithm, signatureSize, packetLength, offset, length, error);
  }
}
