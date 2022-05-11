package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import lombok.SneakyThrows;
import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.client.impl.MqttsnClient;
import org.slj.mqtt.sn.client.impl.MqttsnClientRuntimeRegistry;
import org.slj.mqtt.sn.client.impl.MqttsnClientUdpOptions;
import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.impl.AbstractTopicRegistry;
import org.slj.mqtt.sn.impl.ram.MqttsnInMemoryTopicRegistry;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.model.MqttsnWillData;
import org.slj.mqtt.sn.model.TopicInfo;
import org.slj.mqtt.sn.net.MqttsnUdpOptions;
import org.slj.mqtt.sn.net.MqttsnUdpTransport;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.spi.IMqttsnCodec;
import org.slj.mqtt.sn.spi.IMqttsnPublishFailureListener;
import org.slj.mqtt.sn.spi.IMqttsnPublishReceivedListener;
import org.slj.mqtt.sn.spi.IMqttsnPublishSentListener;
import org.slj.mqtt.sn.spi.MqttsnException;

public class MqttSnClient {

  private final MqttsnClient client;
  private final AbstractTopicRegistry topicRegistry;

  public MqttSnClient(String contextId, String host, int port, int version) throws MqttsnException {
    MqttsnUdpOptions udpOptions = new MqttsnClientUdpOptions().
        withHost(host).
        withPort(0);

    topicRegistry = new MqttsnInMemoryTopicRegistry();

    //-- runtimes options can be used to tune the behaviour of the client
    MqttsnOptions options = new MqttsnOptions().
        //-- specify the address of any static gateway nominating a context id for it
            withNetworkAddressEntry(contextId, NetworkAddress.localhost(port)).
            withMaxMessagesInflight(10).
        //-- configure your clientId
            withContextId(contextId);

    //-- using a default configuration for the controllers will just work out of the box, alternatively
    //-- you can supply your own implementations to change underlying storage or business logic as is required
    IMqttsnCodec codecs = MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2;
    if(version == 2) {
      codecs = MqttsnCodecs.MQTTSN_CODEC_VERSION_2_0;
    }

    AbstractMqttsnRuntimeRegistry registry = MqttsnClientRuntimeRegistry.defaultConfiguration(options).
        withTransport(new MqttsnUdpTransport(udpOptions)).
        withTopicRegistry(topicRegistry).
        withCodec(codecs);


    //-- the client is Closeable and so use a try with resource
    client = new MqttsnClient();
    //-- the client needs to be started using the configuration you constructed above
    client.start(registry);
  }

  public TopicInfo lookupRegistry(String topicName) throws MqttsnException {
    return topicRegistry.lookup(client.getSessionState().getContext(), topicName);
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

  @SneakyThrows
  public void publish(String topicName, int QoS, byte[] msg) throws MqttsnQueueAcceptException, MqttsnException {
    client.publish(topicName, QoS, msg);
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

  public void setWillData(MqttsnWillData details) throws MqttsnException {
    client.setWillData(details);
  }
}
