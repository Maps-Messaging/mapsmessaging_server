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

package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import java.util.concurrent.ThreadLocalRandom;
import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.client.impl.MqttsnClient;
import org.slj.mqtt.sn.client.impl.MqttsnClientRuntimeRegistry;
import org.slj.mqtt.sn.client.impl.MqttsnClientUdpOptions;
import org.slj.mqtt.sn.client.spi.MqttsnClientOptions;
import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.model.IAuthHandler;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.model.MqttsnSecurityOptions;
import org.slj.mqtt.sn.model.session.impl.WillDataImpl;
import org.slj.mqtt.sn.net.MqttsnUdpOptions;
import org.slj.mqtt.sn.net.MqttsnUdpTransport;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.spi.*;

public class MqttSnClient {

  private final MqttsnClient client;

  public MqttSnClient(String host, int port, int version) throws MqttsnException {
    this( host, port, version, null);
  }

  public MqttSnClient(String host, int port, int version, IAuthHandler auth) throws MqttsnException {

    //-- using a default configuration for the controllers will just work out of the box, alternatively
    //-- you can supply your own implementations to change underlying storage or business logic as is required
    IMqttsnCodec codecs = (version == 2) ? MqttsnCodecs.MQTTSN_CODEC_VERSION_2_0 : MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2;

    //-- the client is Closeable and so use a try with resource
    client = new MqttsnClient();
    //-- the client needs to be started using the configuration you constructed above
    client.start(createClientRuntimeRegistry(codecs, host, port, auth));
  }


  protected MqttsnClientRuntimeRegistry createClientRuntimeRegistry(IMqttsnCodec codecs, String host, int port,  IAuthHandler auth){
    IMqttsnStorageService storageService = new MemoryStorage();
    MqttsnUdpOptions udpOptions = new MqttsnClientUdpOptions().
        withHost(host).
        withPort(0);

    MqttsnOptions options = new MqttsnClientOptions().
        withNetworkAddressEntry("localhost",
            NetworkAddress.localhost(port)).
        withContextId(""+ThreadLocalRandom.current().nextLong()).
        withMaxMessagesInflight(1).
        withMaxWait(60000).
        withPredefinedTopic("predefined/topic", 1);
    if(auth != null) {
      MqttsnSecurityOptions securityOptions = new MqttsnSecurityOptions().
          withAuthHandler(auth);
      options.setSecurityOptions(securityOptions);
    }

    return (MqttsnClientRuntimeRegistry) MqttsnClientRuntimeRegistry.defaultConfiguration(storageService, options).
        withTransport(new MqttsnUdpTransport(udpOptions)).
        withCodec(codecs);
  }

  public void ping() throws MqttsnException {
    client.ping();
  }

  public void connect(int keepAlive, boolean cleanSession) throws MqttsnClientConnectException, MqttsnException {
    client.connect(keepAlive, cleanSession);
  }

  public void connect(int keepAlive, boolean cleanSession, String willTopic, int QoS, byte[] msg) throws MqttsnClientConnectException, MqttsnException {
    client.connect(keepAlive, cleanSession);
  }

  public void registerPublishListener(IMqttsnPublishReceivedListener listener){
    client.registerPublishReceivedListener(listener);
  }

  public void registerSentListener(IMqttsnPublishSentListener listener){
    client.registerPublishSentListener(listener);
  }

  public void registerPublishFailedListener(IMqttsnPublishFailureListener listener){
    client.registerPublishFailedListener(listener);
  }

  public boolean isConnected(){
    return client.isConnected();
  }

  public void publish(String topicName, int QoS, byte[] msg) throws MqttsnQueueAcceptException, MqttsnException {
    client.publish(topicName, QoS, false,msg);
  }

  public void sleep(long expiry) throws MqttsnException {
    client.sleep(expiry);
  }

  public void subscribe(String topic, int qos) throws MqttsnException {
    client.subscribe(topic, qos);
  }

  public void unsubscribe(String topic) throws MqttsnException {
    client.unsubscribe(topic);
  }

  public void wake() throws MqttsnException {
    client.wake();
  }

  public void wake(int waitTime) throws MqttsnException {
    client.wake(waitTime);
  }


  public void disconnect() throws MqttsnException {
    client.disconnect();
  }

  public void setWillData(WillDataImpl details) throws MqttsnException {
    client.setWillData(details);
  }
}
