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

package io.mapsmessaging.network.protocol.impl.satellite.modem.protocol;


import io.mapsmessaging.network.io.Packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

interface StreamBypass {
  /** Consume from serial input, emit control bytes via linkOut as needed (may block). */
  int parseInput(InputStream in, OutputStream linkOut) throws IOException;

  /** Optional: intercept app->modem output during bypass. Default: pass-through. */
  int parseOutput(OutputStream out, Packet packet) throws IOException;

  /** True when bypass has finished and result() is ready. */
  boolean isComplete();

  /** Final payload to inject into the Packet stream when complete. */
  byte[] result() throws IOException;

  /** Clear internal state so handler can be reused. */
  void reset();
}