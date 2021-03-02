/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
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

package org.maps.network.protocol.impl.mqtt_sn;

import org.eclipse.paho.mqttsn.udpclient.MqttsCallback;
import org.eclipse.paho.mqttsn.udpclient.MqttsClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.test.BaseTestConfig;

public class mqttSNConnectionTest extends BaseTestConfig implements MqttsCallback  {

  private volatile boolean connected;

  @Test
  public void connectWithOutFlags() {
    connected = false;
    MqttsClient client = new MqttsClient("localhost",1884 );
    client.registerHandler(this);
    client.connect("simpleConnection", true, (short)50);
    int loop = 20;
    while(!connected && loop > 0){
      loop--;
      delay(100);
    }
    Assertions.assertTrue(connected);
    client.disconnect();
    delay(500);
  }

  @Test
  public void connectWithFlags() {
    connected = false;
    MqttsClient client = new MqttsClient("localhost",1884 );
    client.registerHandler(this);
    client.connect("simpleConnection", true, (short)50, "willTopic", 0, "This is my last will and stuff", false);
    int loop = 20;
    while(!connected && loop > 0){
      loop--;
      delay(100);
    }
    Assertions.assertTrue(connected);
    client.disconnect();
    delay(500);
  }

  @Test
  public void connectWaitForKeepalive() {
    connected = false;
    MqttsClient client = new MqttsClient("localhost",1884 );
    client.registerHandler(this);
    client.connect("simpleConnection", true, (short)10);
    int loop = 20;
    while(!connected && loop > 0){
      loop--;
      delay(100);
    }
    Assertions.assertTrue(connected);
    //
    // OK we have a connection, lets wait for a Ping
    //
    delay(15000);

    client.disconnect();
    delay(500);
  }

  @Override
  public int publishArrived(boolean b, int i, int i1, byte[] bytes) {
    return 0;
  }

  @Override
  public void connected() {
    connected = true;
  }

  @Override
  public void disconnected(int i) {
    connected = false;
  }

  @Override
  public void unsubackReceived() {
  }

  @Override
  public void subackReceived(int i, int i1, int i2) {
  }

  @Override
  public void pubCompReceived() {
  }

  @Override
  public void pubAckReceived(int i, int i1) {
  }

  @Override
  public void regAckReceived(int topicId, int i1) {
  }

  @Override
  public void registerReceived(int i, String s) {
  }

  @Override
  public void connectSent() {
 }
}
