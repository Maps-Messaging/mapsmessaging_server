/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.consul;

public class Constants {

  public static final long PING_TIME = 30;
  public static final long HEALTH_TIME = 20;
  public static final int CONSUL_PORT = 8080;
  public static final String NAME = "mapsMessaging";
  public static final String VERSION = "1.0";
  public static final int RETRY_COUNT = 20;

  private Constants() {
    // hide the constructor
  }
}
