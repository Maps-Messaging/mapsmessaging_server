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

package io.mapsmessaging.ha;

import io.mapsmessaging.BuildInfo;
import lombok.Data;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.OffsetDateTime;

@Data
public class LockInfo {
  private final long pid;
  private final String hostname;
  private final String started;
  private final String version;
  private final String buildDate;
  private String lastHeartbeat;

  public LockInfo() {
    pid = ProcessHandle.current().pid();
    String thostname = null;
    try {
      thostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      thostname = "localhost";
    }
    hostname = thostname;
    started = OffsetDateTime.now().toString();
    version = BuildInfo.getBuildVersion();
    buildDate = BuildInfo.getBuildDate();
  }
}