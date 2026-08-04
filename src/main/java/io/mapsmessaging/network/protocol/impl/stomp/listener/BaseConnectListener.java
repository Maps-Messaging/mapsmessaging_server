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

package io.mapsmessaging.network.protocol.impl.stomp.listener;

import io.mapsmessaging.network.protocol.impl.stomp.frames.Error;
import io.mapsmessaging.network.protocol.impl.stomp.state.SessionState;

public abstract class BaseConnectListener implements FrameListener {

  private static final float MIN_VERSION = 1.0f;
  private static final float MAX_VERSION = 1.2f;

  protected static final String CONTENT_TYPE_TEXT = "text/plain";

  protected float processVersion(SessionState engine, String versionHeader) {
    if (versionHeader == null || versionHeader.isBlank()) {
      sendError(engine, "No version header supplied");
      return Float.NaN;
    }

    float version = calculateVersion(versionHeader);
    if (Float.isNaN(version)) {
      sendError(
          engine,
          "No suitable protocol version discovered, received "
              + versionHeader
              + " : Supported = 1.0, 1.1 and 1.2");
      return Float.NaN;
    }
    engine.getProtocol().setVersion(version);
    return version;
  }

  protected void handleFailedAuth(Exception failedAuth, SessionState engine) {
    sendError(engine, "Failed to authenticate: " + failedAuth.getMessage());
  }

  private float calculateVersion(String versionHeader) {
    float max = Float.NaN;
    String[] versions = versionHeader.split(",");
    for (String candidate : versions) {
      float value;
      try {
        value = Float.parseFloat(candidate.trim());
      } catch (NumberFormatException invalidVersion) {
        return Float.NaN;
      }
      if (value >= MIN_VERSION && value <= MAX_VERSION
          && (Float.isNaN(max) || value > max)) {
        max = value;
      }
    }
    return max;
  }

  private void sendError(SessionState engine, String message) {
    Error error = new Error();
    error.setContentType(CONTENT_TYPE_TEXT);
    error.setContent(message.getBytes());
    engine.send(error);
  }
}
