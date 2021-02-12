/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.network.protocol.impl.websockets.frames;

public class GetFrame extends Frame {

  public static final String HOST_HEADER = "host";
  public static final String UPGRADE_HEADER = "upgrade";
  public static final String CONNECTION_HEADER = "connection";
  public static final String SEC_WEBSOCKET_KEY_HEADER = "sec-websocket-key";
  public static final String SEC_WEBSOCKET_VERSION_HEADER = "sec-websocket-version";

  public GetFrame() {
    isComplete = false;
  }

}
