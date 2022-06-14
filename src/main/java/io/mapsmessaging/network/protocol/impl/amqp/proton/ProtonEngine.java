/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.amqp.proton;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.listeners.EventListenerFactory;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.MessageTranslator;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.MessageTranslatorFactory;
import io.mapsmessaging.utilities.threads.tasks.SingleConcurrentTaskScheduler;
import io.mapsmessaging.utilities.threads.tasks.TaskScheduler;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.qpid.proton.codec.DroppingWritableBuffer;
import org.apache.qpid.proton.codec.WritableBuffer;
import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.engine.TransportResult;

public class ProtonEngine {

  private static final String PROTON_ENGINE_KEY = "ProtonEngineScheduler";

  private final AMQPProtocol protocol;
  private final Transport transport;
  private final Collector collector;
  private final Connection connection;
  private final EventListenerFactory eventListenerFactory;
  private final Map<String, Sender> subscriptions;
  private TaskScheduler engineScheduler;

  public ProtonEngine(AMQPProtocol protocol){
    engineScheduler = new SingleConcurrentTaskScheduler(PROTON_ENGINE_KEY);

    this.protocol = protocol;
    collector = Collector.Factory.create();
    connection = Connection.Factory.create();
    connection.collect(collector);
    transport = Transport.Factory.create();
    subscriptions = new LinkedHashMap<>();
    eventListenerFactory = new EventListenerFactory(protocol, this);

    //--------------------------------------------------------
    // ToDo implement SASL and integrate into server impl here
    //--------------------------------------------------------

    // Get any configured SASL mechanism and create an array to pass into the SASL mechanism
    String mechanismString = protocol.getEndPoint().getConfig().getProperties().getProperty("SaslMechanisms", "ANONYMOUS");
    List<String> mechanismList = new ArrayList<>();
    StringTokenizer st = new StringTokenizer(mechanismString, ",");
    while(st.hasMoreElements()){
      mechanismList.add(st.nextElement().toString().trim());
    }
    String[] mechanisms = new String[mechanismList.size()];
    mechanismList.toArray(mechanisms);

    Sasl sasl = transport.sasl();
    sasl.setMechanisms(mechanisms);
    sasl.server();

    // This will need to be moved into the correct SASL handler and ensure data is pushed into the SASL server till
    // auth is complete and then move to the normal event processing.
    sasl.done(Sasl.PN_SASL_OK);


    // Note: This should be a state engine and the state should be something like
    //       -> Init() -- Starting, initializing the session and the connection
    //       -> Sasl() -- IF the mechanisms supported is not ANONYMOUS, then move to Sasl state and any data pushed to the Sasl impl
    //       -> Conn() -- We have completed the startup and any authentication so all data is now pumped into the Proton engine
    //       -> Disc() -- We are processing a disconnect or have been disconnected so will be closing and winding up any resources
    //
    // State changes would be
    //
    //   Init -> Sasl() | Conn() | Disc() [if error during init]
    //   Sasl -> Conn() | Disc() [If error during auth]
    //   Conn -> Disc() [End of session]
    // No other state change would make sense
    //

    //--------------------------------------------------------
    transport.bind(connection);
  }

  public void close() {
    transport.close();
    connection.close();
    for(Entry<String, Sender> entry:subscriptions.entrySet()){
      Object sessionContext = entry.getValue().getSession().getContext();
      if(sessionContext != null){
        Session session = (Session)sessionContext;
        session.removeSubscription(entry.getKey());
      }
    }
    subscriptions.clear();
    engineScheduler = null;
  }

  public void processPacket(Packet packet) throws IOException {
    Future<Boolean> future = submit(new PacketTask(packet));
    try {
      if(!future.isDone()) {
        future.get(10000, TimeUnit.MILLISECONDS);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException | TimeoutException e) {
      throw new IOException(e);
    }
  }

   void processOutput() throws IOException {
    transport.process();
    while(transport.pending() > 0) {
      ByteBuffer buffer = transport.getOutputBuffer();
      Packet packet = new Packet(buffer);
      while (buffer.hasRemaining()) {
        protocol.getEndPoint().sendPacket(packet);
      }
      transport.outputConsumed();
      transport.process();
    }
  }

  /*
 Add the message ID as the tag for the delivery so when we get the delivery complete we can
 acknowledge the correct message Id
  */
  public void sendMessage(Message message, SubscribedEventManager manager){
    submit(new SendMessageTask(message, manager));
  }

  public byte[] packLong( long value) {
    byte[] buff = new byte[8];
    for(int x=0;x<buff.length;x++) {
      buff[x] =(byte) ((value >> (8*x)) & 0xff);
    }
    return buff;
  }

  public long unpackLong( byte[] buff) {
    long value =0;
    for(int x=0;x<buff.length;x++) {
      long val = buff[x];
      value ^= (val & 0xff) << (8 * x);
    }
    return value;
  }


  public void addSubscription(String alias, Sender sender) {
    synchronized (subscriptions) {
      subscriptions.put(alias, sender);
    }
  }

  public void removeSubscription(String alias) {
    synchronized (subscriptions) {
      subscriptions.remove(alias);
    }
  }

  private Future<Boolean> submit(Callable<Boolean> task){
    return engineScheduler.submit(task);
  }

  private class SendMessageTask implements Callable<Boolean> {

    private final Message message;
    private final SubscribedEventManager manager;

    public SendMessageTask(Message message, SubscribedEventManager manager) {
      this.message = message;
      this.manager = manager;
    }

    @Override
    public Boolean call() throws Exception {
      processMessage();
      return true;
    }

    private void processMessage() throws IOException {
      String alias = manager.getContext().getAlias();
      Sender sender = subscriptions.get(alias);
      if(sender != null){
        byte[] tag = packLong(message.getIdentifier());
        Delivery dlv = sender.delivery(tag);
        dlv.setContext(manager);
        MessageTranslator translator = MessageTranslatorFactory.getMessageTranslator(message);
        try {
          org.apache.qpid.proton.message.Message protonMessage = translator.encode(message);
          WritableBuffer sizingBuffer = new DroppingWritableBuffer();
          protonMessage.encode(sizingBuffer);
          byte[] data = new byte[sizingBuffer.position() + 10];
          int size = protonMessage.encode(data, 0, data.length);
          sender.send(data, 0, size);
          sender.advance();
          if (message.isLastMessage()) {
            sender.drained();
          }
        } catch (Exception e) {
          protocol.getLogger().log(ServerLogMessages.AMQP_ENGINE_TRANSPORT_EXCEPTION, e);
        }
      }
      processOutput();
    }
  }

  private class PacketTask implements Callable<Boolean>{

    private final Packet packet;

    public PacketTask(Packet packet){
      this.packet = packet;
    }

    @Override
    public Boolean call() throws Exception {
      pushDataIntoEngine();
      protocol.registerRead();
      return true;
    }

    private void processBuffers(){
      ByteBuffer buffer = transport.getInputBuffer();
      if (buffer.capacity() < packet.available()) {
        // Seems the buffer.put(ByteBuffer) will not just take what it can
        byte[] tmp = new byte[buffer.capacity()];
        packet.get(tmp);
        buffer.put(tmp);
      } else {
        buffer.put(packet.getRawBuffer());
      }
    }

    private void handleEvents(){
      Event ev = collector.peek();
      while (ev != null) {
        eventListenerFactory.handleEvent(ev);
        collector.pop();
        ev = collector.peek();
      }
    }

    private void pushDataIntoEngine() throws IOException {
      while (packet.hasRemaining()) {
        processBuffers();
        TransportResult result = transport.processInput();
        if (!result.isOk()) {
          if (result.getException() != null) {
            protocol.getLogger().log(ServerLogMessages.AMQP_ENGINE_TRANSPORT_EXCEPTION, result.getErrorDescription(), result.getException());
          }
        } else {
          handleEvents();
        }
      }
      processOutput();
      if (!packet.hasRemaining()) {
        packet.clear();
      }
    }
  }
}
