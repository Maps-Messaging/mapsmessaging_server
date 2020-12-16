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

package org.maps.network;

import com.fazecast.jSerialComm.SerialPort;
import java.util.StringTokenizer;

public class SerialEndPointURL extends EndPointURL {

  private final String portName;
  private final int baudRate;
  private final int data;
  private final int parity;
  private final int stop;


  public SerialEndPointURL(String url) {
    protocol = parseProtocol(url);
    host = parseHost(url);
    parseParameterMap(url);
    file = "";
    port = 0;
    StringTokenizer st = new StringTokenizer(host, ",");
    portName = st.nextElement().toString();
    baudRate = Integer.parseInt(st.nextElement().toString());
    data = Integer.parseInt(st.nextElement().toString());
    String parityString = st.nextElement().toString().toLowerCase();
    String stopString = st.nextElement().toString();
    if (stopString.contains("/")) {
      stopString = stopString.substring(0, stopString.indexOf('/'));
    }
    switch (stopString) {
      case "2":
        stop = SerialPort.TWO_STOP_BITS;
        break;

      case "1.5":
        stop = SerialPort.ONE_POINT_FIVE_STOP_BITS;
        break;

      case "1":
      default:
        stop = SerialPort.ONE_STOP_BIT;
        break;

    }

    switch (parityString) {
      case "o":
        parity = SerialPort.ODD_PARITY;
        break;

      case "e":
        parity = SerialPort.EVEN_PARITY;
        break;

      case "m":
        parity = SerialPort.MARK_PARITY;
        break;

      case "s":
        parity = SerialPort.SPACE_PARITY;
        break;

      case "n":
      default:
        parity = SerialPort.NO_PARITY;
        break;
    }
  }

  @Override
  public String getJMXName(){
    return getProtocol() + "_" +portName;
  }

  @Override
  public String getHost() {
    return "";
  }

  @Override
  public int getPort() {
    return 0;
  }

  public String getPortName() {
    return portName;
  }

  public int getBaudRate() {
    return baudRate;
  }

  public int getData() {
    return data;
  }

  public int getParity() {
    return parity;
  }

  public int getStop() {
    return stop;
  }
}
