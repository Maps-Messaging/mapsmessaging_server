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

package io.mapsmessaging.network.protocol.impl.loragateway;

public class Constants {

  public static final byte FAILURE = 0x70;
  public static final byte SUCCESSFUL = 0x71;
  public static final byte DATA = 0x12; // Send the data to the supplied address
  public static final byte CONFIG = 0x13; // Set the Address, Power and encryption key to use
  public static final byte RESET = 0x14; // Hardware reset, use with caution, it may bounce the com port on windows OS
  public static final byte START = 0x15; // Starts transmitting and receiving on the radio if previously stopped
  public static final byte STOP = 0x16; // Stops transmitting and receiving on the radio if previously started
  public static final byte VERSION = 0x17; // Return the current version of this software
  public static final byte PING = 0x18; // something to say when nothing else to say
  public static final byte LOG = 0x19; // something to say when nothing else to say
  public static final byte START_RANGE = 0x12;
  public static final byte END_RANGE = 0x19;

  private Constants() {
    // nothing to do
  }

}
