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

package io.mapsmessaging.network.protocol.impl.coap.packet.options;

public class Constants {
  public static final int IF_MATCH		= 1;
  public static final int URI_HOST		= 3;
  public static final int ETAG			= 4;
  public static final int IF_NONE_MATCH	= 5;
  public static final int OBSERVE			= 6;
  public static final int URI_PORT		= 7;
  public static final int LOCATION_PATH	= 8;
  public static final int URI_PATH		= 11;
  public static final int CONTENT_FORMAT	= 12;
  public static final int MAX_AGE			= 14;
  public static final int URI_QUERY		= 15;
  public static final int ACCEPT			= 17;
  public static final int LOCATION_QUERY	= 20;
  public static final int PROXY_URI		= 35;
  public static final int PROXY_SCHEME	= 39;
  public static final int BLOCK2 =     23;
  public static final int BLOCK1 =     27;
  public static final int SIZE1			= 60;

  private Constants(){}
}
