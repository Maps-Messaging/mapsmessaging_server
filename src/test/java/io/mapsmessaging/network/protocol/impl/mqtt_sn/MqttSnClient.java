package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import java.util.LinkedHashMap;
import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.client.impl.MqttsnClient;
import org.slj.mqtt.sn.client.impl.MqttsnClientRuntimeRegistry;
import org.slj.mqtt.sn.client.impl.MqttsnClientUdpOptions;
import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.model.MqttsnWillData;
import org.slj.mqtt.sn.net.MqttsnUdpOptions;
import org.slj.mqtt.sn.net.MqttsnUdpTransport;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.spi.IMqttsnCodec;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.IMqttsnWillRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;

public class MqttSnClient {

  private final MqttsnClient client;

  public MqttSnClient(String contextId, String host, int port, int version) throws MqttsnException {
    MqttsnUdpOptions udpOptions = new MqttsnClientUdpOptions().
        withHost(host).
        withPort(0);



    //-- runtimes options can be used to tune the behaviour of the client
    MqttsnOptions options = new MqttsnOptions().
        //-- specify the address of any static gateway nominating a context id for it
            withNetworkAddressEntry(contextId, NetworkAddress.localhost(port)).
        //-- configure your clientId
            withContextId(contextId);

    //-- using a default configuration for the controllers will just work out of the box, alternatively
    //-- you can supply your own implementations to change underlying storage or business logic as is required
    IMqttsnCodec codecs = MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2;
    if(version == 2) {
      codecs = MqttsnCodecs.MQTTSN_CODEC_VERSION_2_0;
    }

    IMqttsnWillRegistry willRegistry = new IMqttsnWillRegistry(){

      private LinkedHashMap<IMqttsnContext, MqttsnWillData> data = new LinkedHashMap<>();

      @Override
      public void start(IMqttsnRuntimeRegistry iMqttsnRuntimeRegistry) throws MqttsnException {

      }

      @Override
      public void stop() throws MqttsnException {

      }

      @Override
      public boolean running() {
        return true;
      }

      @Override
      public void setWillMessage(IMqttsnContext iMqttsnContext, MqttsnWillData mqttsnWillData) {
        data.put(iMqttsnContext, mqttsnWillData);
      }

      @Override
      public MqttsnWillData getWillMessage(IMqttsnContext iMqttsnContext) {
        return data.get(iMqttsnContext);
      }

      @Override
      public boolean hasWillMessage(IMqttsnContext iMqttsnContext) {
        return data.containsKey(iMqttsnContext);
      }

      @Override
      public void clear(IMqttsnContext iMqttsnContext) throws MqttsnException {
        data.remove(iMqttsnContext);
      }

      @Override
      public void clearAll() throws MqttsnException {
        data.clear();
      }
    };
    AbstractMqttsnRuntimeRegistry registry = MqttsnClientRuntimeRegistry.defaultConfiguration(options).
        withTransport(new MqttsnUdpTransport(udpOptions)).
        //-- select the codec you wish to use, support for SN 1.2 is standard or you can nominate your own
            withCodec(codecs)
        .withWillRegistry(willRegistry);


    //-- the client is Closeable and so use a try with resource
    client = new MqttsnClient();
    //-- the client needs to be started using the configuration you constructed above
    client.start(registry);
  }

  public void connect(int keepAlive, boolean cleanSession) throws MqttsnClientConnectException, MqttsnException {
    client.connect(keepAlive, cleanSession);
  }

  public void connect(int keepAlive, boolean cleanSession, String willTopic, int QoS, byte[] msg) throws MqttsnClientConnectException, MqttsnException {
    client.connect(keepAlive, cleanSession);
  }


  public boolean isConnected(){
    return client.isConnected();
  }

  public void publish(String topicName, int QoS, byte[] msg) throws MqttsnQueueAcceptException, MqttsnException {
    client.publish(topicName, QoS, msg);
  }

  public void sleep(long expiry) throws MqttsnException {
    client.sleep(expiry);
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

}
