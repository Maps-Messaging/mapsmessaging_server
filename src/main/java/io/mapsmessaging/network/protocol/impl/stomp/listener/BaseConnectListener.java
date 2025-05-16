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

package io.mapsmessaging.network.protocol.impl.stomp.listener;

import io.mapsmessaging.network.protocol.impl.stomp.state.SessionState;

import java.util.ArrayList;
import java.util.StringTokenizer;

public abstract class BaseConnectListener implements FrameListener {

  private static final float MIN_VERSION = 1.0f;
  private static final float MAX_VERSION = 1.2f;

  protected static final String CONTENT_TYPE_TEXT = "text/plain";

  protected float processVersion(SessionState engine, String versionHeader) {
    if (versionHeader == null || versionHeader.isEmpty()) {
      io.mapsmessaging.network.protocol.impl.stomp.frames.Error error = new io.mapsmessaging.network.protocol.impl.stomp.frames.Error();
      error.setContentType(CONTENT_TYPE_TEXT);
      error.setContent("No version header supplied".getBytes());
      engine.send(error);
      return Float.NaN;
    }

    // Check to see if we support the version
    float version = calculateVersion(versionHeader);
    if (version < 0) {
      io.mapsmessaging.network.protocol.impl.stomp.frames.Error error = new io.mapsmessaging.network.protocol.impl.stomp.frames.Error();
      error.setContentType(CONTENT_TYPE_TEXT);
      error.setContent(("No suitable protocol version discovered, received " + versionHeader + " : Supported = 1.1 and 1.2").getBytes());
      engine.send(error);
      return Float.NaN;
    }
    engine.getProtocol().setVersion(version);
    return version;
  }

  protected void handleFailedAuth(Exception failedAuth, SessionState engine) {
    io.mapsmessaging.network.protocol.impl.stomp.frames.Error error = new io.mapsmessaging.network.protocol.impl.stomp.frames.Error();
    error.setContentType(CONTENT_TYPE_TEXT);
    error.setContent(("Failed to authenticate: " + failedAuth.getMessage()).getBytes());
    engine.send(error);
  }

  private float calculateVersion(String versionHeader) {
    ArrayList<Float> versions = new ArrayList<>();
    StringTokenizer versionList = new StringTokenizer(versionHeader, ",");
    while (versionList.hasMoreElements()) {
      versions.add(Float.parseFloat(versionList.nextElement().toString().trim()));
    }
    float max = -1.0f;
    for (Float test : versions) {
      if ((test >= MIN_VERSION && test <= MAX_VERSION) && max < test) {
        max = test;
      }
    }
    return max;
  }
}
