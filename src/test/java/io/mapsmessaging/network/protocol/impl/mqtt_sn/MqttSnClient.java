package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.client.impl.MqttsnClient;
import org.slj.mqtt.sn.client.impl.MqttsnClientRuntimeRegistry;
import org.slj.mqtt.sn.client.impl.MqttsnClientUdpOptions;
import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.model.TopicInfo;
import org.slj.mqtt.sn.model.session.impl.MqttsnWillDataImpl;
import org.slj.mqtt.sn.net.MqttsnUdpOptions;
import org.slj.mqtt.sn.net.MqttsnUdpTransport;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.spi.IMqttsnCodec;
import org.slj.mqtt.sn.spi.IMqttsnPublishFailureListener;
import org.slj.mqtt.sn.spi.IMqttsnPublishReceivedListener;
import org.slj.mqtt.sn.spi.IMqttsnPublishSentListener;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.IMqttsnStorageService;
import org.slj.mqtt.sn.spi.MqttsnException;

public class MqttSnClient {

  private static final AtomicInteger counter = new AtomicInteger(0);
  private final MqttsnClient client;

  public MqttSnClient(String contextId, String host, int port, int version) throws MqttsnException {

    //-- using a default configuration for the controllers will just work out of the box, alternatively
    //-- you can supply your own implementations to change underlying storage or business logic as is required
    IMqttsnCodec codecs = (version == 2) ? MqttsnCodecs.MQTTSN_CODEC_VERSION_2_0 : MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2;

    //-- the client is Closeable and so use a try with resource
    client = new MqttsnClient();
    //-- the client needs to be started using the configuration you constructed above
    client.start(createClientRuntimeRegistry(contextId, codecs, host, port));
  }


  protected MqttsnClientRuntimeRegistry createClientRuntimeRegistry(String clientId, IMqttsnCodec codecs, String host, int port){
    IMqttsnStorageService storageService = new MemoryStorage();
    MqttsnUdpOptions udpOptions = new MqttsnClientUdpOptions().
        withHost(host).
        withPort(0);

    MqttsnOptions options = new MqttsnOptions().
        withNetworkAddressEntry("localhost",
            NetworkAddress.localhost(port)).
        withContextId(clientId + "-" + ThreadLocalRandom.current().nextLong()).
        withMaxMessagesInflight(1).
        withMaxWait(60000).
        withPredefinedTopic("predefined/topic", 1);
    return (MqttsnClientRuntimeRegistry) MqttsnClientRuntimeRegistry.defaultConfiguration(storageService, options).
        withTransport(new MqttsnUdpTransport(udpOptions)).
        withCodec(codecs);
  }

  public TopicInfo lookupRegistry(String topicName) throws MqttsnException {
    System.err.println("Looking for "+topicName);
    IMqttsnRuntimeRegistry registry = client.getRegistry();

    return null;
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

  public void setWillData(MqttsnWillDataImpl details) throws MqttsnException {
    client.setWillData(details);
  }
}
