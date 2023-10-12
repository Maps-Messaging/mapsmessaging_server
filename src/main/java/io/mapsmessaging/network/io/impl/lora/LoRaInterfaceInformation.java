/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.io.impl.lora;

import io.mapsmessaging.network.io.InterfaceInformation;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.util.ArrayList;
import java.util.List;

public class LoRaInterfaceInformation implements InterfaceInformation {

  private final int mtu;
  private final InetAddress bcast;

  LoRaInterfaceInformation(int mtu, InetAddress bcast) {
    this.mtu = mtu;
    this.bcast = bcast;
  }

  @Override
  public boolean isLoopback() {
    return false;
  }

  @Override
  public boolean isUp() {
    return true;
  }

  @Override
  public boolean isLoRa() {
    return true;
  }

  @Override
  public List<InterfaceAddress> getInterfaces() {
    return new ArrayList<>();
  }

  @Override
  public int getMTU() {
    return mtu;
  }

  @Override
  public boolean isIPV4() {
    return false;
  }

  @Override
  public InetAddress getBroadcast() {
    return bcast;
  }
}
