/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.network.io;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.SocketException;
import java.util.List;

public interface InterfaceInformation {

  boolean isLoopback() throws SocketException;

  boolean isUp() throws SocketException;

  boolean isLoRa();

  List<InterfaceAddress> getInterfaces();

  int getMTU() throws SocketException;

  boolean isIPV4();

  InetAddress getBroadcast();

}
