package org.eclipse.paho.mqttv5.client.internal;

/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    https://www.eclipse.org/legal/epl-2.0
 * and the Eclipse Distribution License is available at
 *   https://www.eclipse.org/org/documents/edl-v10.php
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 *    Ian Craggs - per subscription message handlers (bug 466579)
 *    Ian Craggs - ack control (bug 472172)
 *    James Sutton - checkForActivity Token (bug 473928)
 *    James Sutton - Automatic Reconnect & Offline Buffering.
 */
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ExecutorService;

import org.eclipse.paho.mqttv5.client.BufferedMessage;
import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClientException;
import org.eclipse.paho.mqttv5.client.MqttClientInterface;
import org.eclipse.paho.mqttv5.client.MqttClientPersistence;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttPingSender;
import org.eclipse.paho.mqttv5.client.MqttToken;
import org.eclipse.paho.mqttv5.client.MqttTopic;
import org.eclipse.paho.mqttv5.client.TimerPingSender;
import org.eclipse.paho.mqttv5.client.logging.Logger;
import org.eclipse.paho.mqttv5.client.logging.LoggerFactory;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttPersistenceException;
import org.eclipse.paho.mqttv5.common.packet.*;

/**
 * Handles client communications with the server. Sends and receives MQTT V5
 * messages.
 */
public class ClientComms {
    public static String VERSION = "${project.version}";
    public static String BUILD_LEVEL = "L${build.level}";
    private static final String CLASS_NAME = ClientComms.class.getName();
    private Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);

    private static final byte CONNECTED = 0;
    private static final byte CONNECTING = 1;
    private static final byte DISCONNECTING = 2;
    private static final byte DISCONNECTED = 3;
    private static final byte CLOSED = 4;

    private MqttClientInterface client;
    private int networkModuleIndex;
    private NetworkModule[] networkModules;
    private CommsReceiver receiver;
    private CommsSender sender;
    private CommsCallback callback;
    private ClientState clientState;
    private MqttConnectionOptions conOptions;
    private MqttClientPersistence persistence;
    private MqttPingSender pingSender;
    private CommsTokenStore tokenStore;
    private boolean stoppingComms = false;

    private byte conState = DISCONNECTED;
    private final Object conLock = new Object(); // Used to synchronize connection state
    private boolean closePending = false;
    private boolean resting = false;
    private DisconnectedMessageBuffer disconnectedMessageBuffer;
    private ExecutorService executorService;
    private MqttConnectionState mqttConnection;

    /**
     * Creates a new ClientComms object, using the specified module to handle the
     * network calls.
     *
     * @param client
     *            The {@link MqttClientInterface}
     * @param persistence
     *            the {@link MqttClientPersistence} layer.
     * @param pingSender
     *            the {@link TimerPingSender}
     * @param executorService
     *            the {@link ExecutorService}
     * @param mqttSession
     *            the {@link MqttSessionState}
     * @param mqttConnection
     *            the {@link MqttConnectionState}
     * @throws MqttException
     *             if an exception occurs whilst communicating with the server
     */
    public ClientComms(MqttClientInterface client, MqttClientPersistence persistence, MqttPingSender pingSender,
                       ExecutorService executorService, MqttSessionState mqttSession, MqttConnectionState mqttConnection) throws MqttException {
        this.conState = DISCONNECTED;
        this.client = client;
        this.persistence = persistence;
        this.pingSender = pingSender;
        this.pingSender.init(this);
        this.executorService = executorService;
        this.mqttConnection = mqttConnection;

        this.tokenStore = new CommsTokenStore(getClient().getClientId());
        this.callback = new CommsCallback(this);
        this.clientState = new ClientState(persistence, tokenStore, this.callback, this, pingSender, this.mqttConnection);

        callback.setClientState(clientState);
        log.setResourceName(getClient().getClientId());
    }

    CommsReceiver getReceiver() {
        return receiver;
    }

    /**
     * Sends a message to the server. Does not check if connected this validation
     * must be done by invoking routines.
     *
     * @param message
     * @param token
     * @throws MqttException
     */
    void internalSend(MqttWireMessage message, MqttToken token) throws MqttException {
        final String methodName = "internalSend";
        // @TRACE 200=internalSend key={0} message={1} token={2}
        log.fine(CLASS_NAME, methodName, "200", new Object[] { message.getKey(), message, token });

        if (token.getClient() == null) {
            // Associate the client with the token - also marks it as in use.
            token.internalTok.setClient(getClient());
        } else {
            // Token is already in use - cannot reuse
            // @TRACE 213=fail: token in use: key={0} message={1} token={2}
            log.fine(CLASS_NAME, methodName, "213", new Object[] { message.getKey(), message, token });

            throw new MqttException(MqttClientException.REASON_CODE_TOKEN_INUSE);
        }

        try {
            // Persist if needed and send the message
            this.clientState.send(message, token);
        } catch (MqttException e) {
            token.internalTok.setClient(null); // undo client setting on error
            if (message instanceof MqttPublish) {
                this.clientState.undo((MqttPublish) message);
            }
            throw e;
        }
    }

    /**
     * Sends a message to the broker if in connected state, but only waits for the
     * message to be stored, before returning.
     *
     * @param message
     *            The {@link MqttWireMessage} to send
     * @param token
     *            The {@link MqttToken} to send.
     * @throws MqttException
     *             if an error occurs sending the message
     */
    public void sendNoWait(MqttWireMessage message, MqttToken token) throws MqttException {
        final String methodName = "sendNoWait";

        if (isConnected() || (!isConnected() && (message instanceof MqttConnect || message instanceof MqttAuth))
                || (isDisconnecting() && message instanceof MqttDisconnect)) {

            if (disconnectedMessageBuffer != null && disconnectedMessageBuffer.getMessageCount() != 0) {
                // @TRACE 507=Client Connected, Offline Buffer available, but not empty. Adding
                // message to buffer. message={0}
                log.fine(CLASS_NAME, methodName, "507", new Object[] { message.getKey() });
                // If the message is a publish, strip the topic alias:
                if(message instanceof MqttPublish && message.getProperties().getTopicAlias()!= null) {
                    MqttProperties messageProps = message.getProperties();
                    messageProps.setTopicAlias(null);
                    message.setProperties(messageProps);
                }
                if (disconnectedMessageBuffer.isPersistBuffer()) {
                    this.clientState.persistBufferedMessage(message);
                }
                disconnectedMessageBuffer.putMessage(message, token);

            } else {

                if (message instanceof MqttPublish) {
                    // Override the QoS if the server has set a maximum
                    if (this.mqttConnection.getMaximumQoS() != null
                            && ((MqttPublish) message).getMessage().getQos() > this.mqttConnection.getMaximumQoS()) {
                        MqttMessage mqttMessage = ((MqttPublish) message).getMessage();
                        mqttMessage.setQos(this.mqttConnection.getMaximumQoS());
                        ((MqttPublish) message).setMessage(mqttMessage);
                    }

                    // Override the Retain flag if the server has disabled it
                    if (this.mqttConnection.isRetainAvailable() != null
                            && ((MqttPublish) message).getMessage().isRetained()
                            && (this.mqttConnection.isRetainAvailable() == false)) {
                        MqttMessage mqttMessage = ((MqttPublish) message).getMessage();
                        mqttMessage.setRetained(false);
                        ((MqttPublish) message).setMessage(mqttMessage);
                    }

                }
                this.internalSend(message, token);
            }
        } else if (disconnectedMessageBuffer != null && isResting()) {
            // @TRACE 508=Client Resting, Offline Buffer available. Adding message to
            // buffer. message={0}
            log.fine(CLASS_NAME, methodName, "508", new Object[] { message.getKey() });
            if (disconnectedMessageBuffer.isPersistBuffer()) {
                this.clientState.persistBufferedMessage(message);
            }
            disconnectedMessageBuffer.putMessage(message, token);
        } else {
            // @TRACE 208=failed: not connected
            log.fine(CLASS_NAME, methodName, "208");
            throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_CLIENT_NOT_CONNECTED);
        }
    }

    /**
     * Close and tidy up.
     *
     * Call each main class and let it tidy up e.g. releasing the token store which
     * normally survives a disconnect.
     *
     * @param force
     *            force disconnection
     * @throws MqttException
     *             if not disconnected
     */
    public void close(boolean force) throws MqttException {
        final String methodName = "close";
        synchronized (conLock) {
            if (!isClosed()) {
                // Must be disconnected before close can take place or if we are being forced
                if (!isDisconnected() || force) {
                    // @TRACE 224=failed: not disconnected
                    log.fine(CLASS_NAME, methodName, "224");

                    if (isConnecting()) {
                        throw new MqttException(MqttClientException.REASON_CODE_CONNECT_IN_PROGRESS);
                    } else if (isConnected()) {
                        throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_CLIENT_CONNECTED);
                    } else if (isDisconnecting()) {
                        closePending = true;
                        return;
                    }
                }

                conState = CLOSED;
                // Don't shut down an externally supplied executor service
                //shutdownExecutorService();
                // ShutdownConnection has already cleaned most things
                clientState.close();
                clientState = null;
                callback = null;
                persistence = null;
                sender = null;
                pingSender = null;
                receiver = null;
                networkModules = null;
                conOptions = null;
                tokenStore = null;
            }
        }
    }

    /**
     * Sends a connect message and waits for an ACK or NACK. Connecting is a special
     * case which will also start up the network connection, receive thread, and
     * keep alive thread.
     *
     * @param options
     *            The {@link MqttConnectionOptions} for the connection
     * @param token
     *            The {@link MqttToken} to track the connection
     * @throws MqttException
     *             if an error occurs when connecting
     */
    public void connect(MqttConnectionOptions options, MqttToken token) throws MqttException {
        final String methodName = "connect";
        synchronized (conLock) {
            if (isDisconnected() && !closePending) {
                // @TRACE 214=state=CONNECTING
                log.fine(CLASS_NAME, methodName, "214");

                conState = CONNECTING;

                conOptions = options;

                MqttConnect connect = new MqttConnect(client.getClientId(), conOptions.getMqttVersion(),
                        conOptions.isCleanStart(), conOptions.getKeepAliveInterval(),
                        conOptions.getConnectionProperties(), conOptions.getWillMessageProperties());

                if (conOptions.getWillDestination() != null) {
                    connect.setWillDestination(conOptions.getWillDestination());
                }

                if (conOptions.getWillMessage() != null) {
                    connect.setWillMessage(conOptions.getWillMessage());
                }

                if (conOptions.getUserName() != null) {
                    connect.setUserName(conOptions.getUserName());
                }
                if (conOptions.getPassword() != null) {
                    connect.setPassword(conOptions.getPassword());
                }

                /*
                 * conOptions.getUserName(), conOptions.getPassword(),
                 * conOptions.getWillMessage(), conOptions.getWillDestination()
                 */
                this.mqttConnection.setKeepAliveSeconds(conOptions.getKeepAliveInterval());
                this.clientState.setCleanStart(conOptions.isCleanStart());

                tokenStore.open();
                ConnectBG conbg = new ConnectBG(this, token, connect, executorService);
                conbg.start();
            } else {
                // @TRACE 207=connect failed: not disconnected {0}
                log.fine(CLASS_NAME, methodName, "207", new Object[] { Byte.valueOf(conState) });
                if (isClosed() || closePending) {
                    throw new MqttException(MqttClientException.REASON_CODE_CLIENT_CLOSED);
                } else if (isConnecting()) {
                    throw new MqttException(MqttClientException.REASON_CODE_CONNECT_IN_PROGRESS);
                } else if (isDisconnecting()) {
                    throw new MqttException(MqttClientException.REASON_CODE_CLIENT_DISCONNECTING);
                } else {
                    throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_CLIENT_CONNECTED);
                }
            }
        }
    }

    public void connectComplete(MqttConnAck cack, MqttException mex) throws MqttException {
        final String methodName = "connectComplete";
        int rc = cack.getReturnCode();
        synchronized (conLock) {
            if (rc == 0) {
                // We've successfully connected
                // @TRACE 215=state=CONNECTED
                log.fine(CLASS_NAME, methodName, "215");

                conState = CONNECTED;
                return;
            }
        }

        // @TRACE 204=connect failed: rc={0}
        log.fine(CLASS_NAME, methodName, "204", new Object[] { Integer.valueOf(rc) });
        throw mex;
    }

    /**
     * Shuts down the connection to the server. This may have been invoked as a
     * result of a user calling disconnect or an abnormal disconnection. The method
     * may be invoked multiple times in parallel as each thread when it receives an
     * error uses this method to ensure that shutdown completes successfully.
     *
     * @param token
     *            the {@link MqttToken} To track closing the connection
     * @param reason
     *            the {@link MqttException} thrown requiring the connection to be
     *            shut down.
     * @param message
     *            the {@link MqttDisconnect} that triggered the connection to be
     *            shut down.
     */
    public void shutdownConnection(MqttToken token, MqttException reason, MqttDisconnect message) {
        final String methodName = "shutdownConnection";

        boolean wasConnected;
        MqttToken endToken = null; // Token to notify after disconnect completes

        // This method could concurrently be invoked from many places only allow it
        // to run once.
        synchronized (conLock) {
            if (stoppingComms || closePending || isClosed()) {
                return;
            }
            stoppingComms = true;

            // @TRACE 216=state=DISCONNECTING
            log.fine(CLASS_NAME, methodName, "216");

            wasConnected = (isConnected() || isDisconnecting());
            conState = DISCONNECTING;
        }

        // Update the token with the reason for shutdown if it
        // is not already complete.
        if (token != null && !token.isComplete()) {
            token.internalTok.setException(reason);
        }

        // Stop the thread that is used to call the user back
        // when actions complete
        if (callback != null) {
            callback.stop();
        }

        // Stop the thread that handles inbound work from the network
        if (receiver != null) {
            receiver.stop();
        }

        // Stop the network module, send and receive now not possible
        try {
            if (networkModules != null) {
                NetworkModule networkModule = networkModules[networkModuleIndex];
                if (networkModule != null) {
                    networkModule.stop();
                }
            }
        } catch (Exception ioe) {
            // Ignore as we are shutting down
        }

        // Stop any new tokens being saved by app and throwing an exception if they do
        tokenStore.quiesce(new MqttException(MqttClientException.REASON_CODE_CLIENT_DISCONNECTING));

        // Notify any outstanding tokens with the exception of
        // con or discon which may be returned and will be notified at
        // the end
        endToken = handleOldTokens(token, reason);

        try {
            // Clean session handling and tidy up
            clientState.disconnected(reason);
            if (clientState.getCleanStart())
                callback.removeMessageListeners();
        } catch (Exception ex) {
            // Ignore as we are shutting down
        }

        if (sender != null) {
            sender.stop();
        }

        if (pingSender != null) {
            pingSender.stop();
        }

        try {
            if (disconnectedMessageBuffer == null && persistence != null) {
                persistence.close();
            }

        } catch (Exception ex) {
            // Ignore as we are shutting down
        }
        // All disconnect logic has been completed allowing the
        // client to be marked as disconnected.
        synchronized (conLock) {
            // @TRACE 217=state=DISCONNECTED
            log.fine(CLASS_NAME, methodName, "217");

            conState = DISCONNECTED;
            stoppingComms = false;
        }

        // Internal disconnect processing has completed. If there
        // is a disconnect token or a connect in error notify
        // it now. This is done at the end to allow a new connect
        // to be processed and now throw a currently disconnecting error.
        // any outstanding tokens and unblock any waiters
        if (endToken != null && callback != null) {
            callback.asyncOperationComplete(endToken);
        }
        if (wasConnected && callback != null) {
            // Let the user know client has disconnected either normally or abnormally
            callback.connectionLost(reason, message);
        }

        // While disconnecting, close may have been requested - try it now
        synchronized (conLock) {
            if (closePending) {
                try {
                    close(true);
                } catch (Exception e) { // ignore any errors as closing
                }
            }
        }
    }

    // Tidy up. There may be tokens outstanding as the client was
    // not disconnected/quiseced cleanly! Work out what tokens still
    // need to be notified and waiters unblocked. Store the
    // disconnect or connect token to notify after disconnect is
    // complete.
    private MqttToken handleOldTokens(MqttToken token, MqttException reason) {
        final String methodName = "handleOldTokens";
        // @TRACE 222=>
        log.fine(CLASS_NAME, methodName, "222");

        MqttToken tokToNotifyLater = null;
        try {
            // First the token that was related to the disconnect / shutdown may
            // not be in the token table - temporarily add it if not
            if (token != null) {
                if (tokenStore.getToken(token.internalTok.getKey()) == null) {
                    tokenStore.saveToken(token, token.internalTok.getKey());
                }
            }

            Vector<MqttToken> toksToNot = clientState.resolveOldTokens(reason);
            Enumeration<MqttToken> toksToNotE = toksToNot.elements();
            while (toksToNotE.hasMoreElements()) {
                MqttToken tok = (MqttToken) toksToNotE.nextElement();

                if (tok.internalTok.getKey().equals(MqttDisconnect.KEY)
                        || tok.internalTok.getKey().equals(MqttConnect.KEY)) {
                    // Its con or discon so remember and notify @ end of disc routine
                    tokToNotifyLater = tok;
                } else {
                    // notify waiters and callbacks of outstanding tokens
                    // that a problem has occurred and disconnect is in
                    // progress
                    callback.asyncOperationComplete(tok);
                }
            }
        } catch (Exception ex) {
            // Ignore as we are shutting down
        }
        return tokToNotifyLater;
    }

    public void disconnect(MqttDisconnect disconnect, long quiesceTimeout, MqttToken token) throws MqttException {
        final String methodName = "disconnect";
        synchronized (conLock) {
            if (isClosed()) {
                // @TRACE 223=failed: in closed state
                log.fine(CLASS_NAME, methodName, "223");
                throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_CLIENT_CLOSED);
            } else if (isDisconnected()) {
                // @TRACE 211=failed: already disconnected
                log.fine(CLASS_NAME, methodName, "211");
                throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED);
            } else if (isDisconnecting()) {
                // @TRACE 219=failed: already disconnecting
                log.fine(CLASS_NAME, methodName, "219");
                throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_CLIENT_DISCONNECTING);
            } else if (Thread.currentThread() == callback.getThread()) {
                // @TRACE 210=failed: called on callback thread
                log.fine(CLASS_NAME, methodName, "210");
                // Not allowed to call disconnect() from the callback, as it will deadlock.
                throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_CLIENT_DISCONNECT_PROHIBITED);
            }

            // @TRACE 218=state=DISCONNECTING
            log.fine(CLASS_NAME, methodName, "218");
            conState = DISCONNECTING;
            DisconnectBG discbg = new DisconnectBG(disconnect, quiesceTimeout, token, executorService);
            discbg.start();
        }
    }

    public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout, int reasonCode,
                                   MqttProperties disconnectProperties) throws MqttException {
        disconnectForcibly(quiesceTimeout, disconnectTimeout, true, reasonCode, disconnectProperties);
    }

    /**
     * Disconnect the connection and reset all the states.
     *
     * @param quiesceTimeout
     *            How long to wait whilst quiesing before messages are deleted.
     * @param disconnectTimeout
     *            How long to wait whilst disconnecting
     * @param sendDisconnectPacket
     *            If true, will send a disconnect packet
     * @param reasonCode
     *            the disconnection reason code.
     * @param disconnectProperties
     *            the {@link MqttProperties} to send in the Disconnect packet.
     * @throws MqttException
     *             if an error occurs whilst disconnecting
     */
    public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout, boolean sendDisconnectPacket,
                                   int reasonCode, MqttProperties disconnectProperties) throws MqttException {
        conState = DISCONNECTING;
        // Allow current inbound and outbound work to complete
        if (clientState != null) {
            clientState.quiesce(quiesceTimeout);
        }
        MqttToken token = new MqttToken(client.getClientId());
        try {
            // Send disconnect packet
            if (sendDisconnectPacket) {
                internalSend(new MqttDisconnect(reasonCode, disconnectProperties), token);

                // Wait util the disconnect packet sent with timeout
                token.waitForCompletion(disconnectTimeout);
            }
        } catch (Exception ex) {
            // ignore, probably means we failed to send the disconnect packet.
        } finally {
            token.internalTok.markComplete(null, null);
            shutdownConnection(token, null, null);
        }
    }

    public boolean isConnected() {
        synchronized (conLock) {
            return conState == CONNECTED;
        }
    }

    public boolean isConnecting() {
        synchronized (conLock) {
            return conState == CONNECTING;
        }
    }

    public boolean isDisconnected() {
        synchronized (conLock) {
            return conState == DISCONNECTED;
        }
    }

    public boolean isDisconnecting() {
        synchronized (conLock) {
            return conState == DISCONNECTING;
        }
    }

    public boolean isClosed() {
        synchronized (conLock) {
            return conState == CLOSED;
        }
    }

    public boolean isResting() {
        synchronized (conLock) {
            return resting;
        }
    }

    public void setCallback(MqttCallback mqttCallback) {
        this.callback.setCallback(mqttCallback);
    }

    public void setReconnectCallback(MqttCallback callback) {
        this.callback.setReconnectCallback(callback);
    }

    public void setManualAcks(boolean manualAcks) {
        this.callback.setManualAcks(manualAcks);
    }

    public void messageArrivedComplete(int messageId, int qos) throws MqttException {
        this.callback.messageArrivedComplete(messageId, qos);
    }

    public void setMessageListener(Integer subscriptionId, String topicFilter, IMqttMessageListener messageListener) {
        this.callback.setMessageListener(subscriptionId, topicFilter, messageListener);
    }

    public void removeMessageListener(String topicFilter) {
        this.callback.removeMessageListener(topicFilter);
    }

    protected MqttTopic getTopic(String topic) {
        return new MqttTopic(topic, this);
    }

    public void setNetworkModuleIndex(int index) {
        this.networkModuleIndex = index;
    }

    public int getNetworkModuleIndex() {
        return networkModuleIndex;
    }

    public NetworkModule[] getNetworkModules() {
        return networkModules;
    }

    public void setNetworkModules(NetworkModule[] networkModules) {
        this.networkModules = networkModules;
    }

    public MqttToken[] getPendingTokens() {
        return tokenStore.getOutstandingDelTokens();
    }

    protected void deliveryComplete(MqttPublish msg) throws MqttPersistenceException {
        this.clientState.deliveryComplete(msg);
    }

    protected void deliveryComplete(int messageId) throws MqttPersistenceException {
        this.clientState.deliveryComplete(messageId);
    }

    public MqttClientInterface getClient() {
        return client;
    }

    public long getKeepAlive() {
        return this.mqttConnection.getKeepAlive();
    }

    public MqttState getClientState() {
        return clientState;
    }

    public MqttConnectionOptions getConOptions() {
        return conOptions;
    }

    public Properties getDebug() {
        Properties props = new Properties();
        props.put("conState", Integer.valueOf(conState));
        props.put("serverURI", getClient().getServerURI());
        props.put("callback", callback);
        props.put("stoppingComms", Boolean.valueOf(stoppingComms));
        return props;
    }

    // Kick off the connect processing in the background so that it does not block.
    // For instance
    // the socket could take time to create.
    private class ConnectBG implements Runnable {
        ClientComms clientComms = null;
        MqttToken conToken;
        MqttConnect conPacket;
        private String threadName;

        ConnectBG(ClientComms cc, MqttToken cToken, MqttConnect cPacket, ExecutorService executorService) {
            clientComms = cc;
            conToken = cToken;
            conPacket = cPacket;
            threadName = "MQTT Con: " + getClient().getClientId();
        }

        void start() {
            if (executorService == null) {
                new Thread(this).start();
            } else {
                executorService.execute(this);
            }
        }

        public void run() {
            Thread.currentThread().setName(threadName);
            final String methodName = "connectBG:run";
            MqttException mqttEx = null;
            // @TRACE 220=>
            log.fine(CLASS_NAME, methodName, "220");

            try {
                // Reset an exception on existing delivery tokens.
                // This will have been set if disconnect occurred before delivery was
                // fully processed.
                MqttToken[] toks = tokenStore.getOutstandingDelTokens();
                for (MqttToken tok : toks) {
                    tok.internalTok.setException(null);
                }

                // Save the connect token in tokenStore as failure can occur before send
                tokenStore.saveToken(conToken, conPacket);

                // Connect to the server at the network level e.g. TCP socket and then
                // start the background processing threads before sending the connect
                // packet.
                NetworkModule networkModule = networkModules[networkModuleIndex];
                networkModule.start();
                receiver = new CommsReceiver(clientComms, clientState, tokenStore, networkModule.getInputStream());
                receiver.start("MQTT Rec: " + getClient().getClientId(), executorService);
                sender = new CommsSender(clientComms, clientState, tokenStore, networkModule.getOutputStream());
                sender.start("MQTT Snd: " + getClient().getClientId(), executorService);
                callback.start("MQTT Call: " + getClient().getClientId(), executorService);
                internalSend(conPacket, conToken);
            } catch (MqttException ex) {
                // @TRACE 212=connect failed: unexpected exception
                log.fine(CLASS_NAME, methodName, "212", null, ex);
                mqttEx = ex;
            } catch (Exception ex) {
                // @TRACE 209=connect failed: unexpected exception
                log.fine(CLASS_NAME, methodName, "209", null, ex);
                mqttEx = ExceptionHelper.createMqttException(ex);
            }

            if (mqttEx != null) {
                shutdownConnection(conToken, mqttEx, null);
            }
        }
    }

    // Kick off the disconnect processing in the background so that it does not
    // block. For instance
    // the quiesce
    private class DisconnectBG implements Runnable {
        MqttDisconnect disconnect;
        long quiesceTimeout;
        MqttToken token;
        private String threadName;

        DisconnectBG(MqttDisconnect disconnect, long quiesceTimeout, MqttToken token, ExecutorService executorService) {
            this.disconnect = disconnect;
            this.quiesceTimeout = quiesceTimeout;
            this.token = token;
        }

        void start() {
            threadName = "MQTT Disc: "+getClient().getClientId();
            if (executorService == null) {
                new Thread(this).start();
            } else {
                executorService.execute(this);
            }
        }

        public void run() {
            Thread.currentThread().setName(threadName);
            final String methodName = "disconnectBG:run";
            // @TRACE 221=>
            log.fine(CLASS_NAME, methodName, "221");

            // Allow current inbound and outbound work to complete
            clientState.quiesce(quiesceTimeout);
            try {
                internalSend(disconnect, token);
                // do not wait if the sender process is not running
                if (sender != null && sender.isRunning()) {
                    token.internalTok.waitUntilSent();
                }
            }
            catch (MqttException ex) {
            }
            finally {
                token.internalTok.markComplete(null, null);
                if (sender == null || !sender.isRunning()) {
                    // if the sender process is not running
                    token.internalTok.notifyComplete();
                }
                shutdownConnection(token, null, null);
            }
        }
    }

    /*
     * Check and send a ping if needed and check for ping timeout. Need to send a
     * ping if nothing has been sent or received in the last keepalive interval.
     */
    public MqttToken checkForActivity() {
        return this.checkForActivity(null);
    }

    /*
     * Check and send a ping if needed and check for ping timeout. Need to send a
     * ping if nothing has been sent or received in the last keepalive interval.
     * Passes an IMqttActionListener to ClientState.checkForActivity so that the
     * callbacks are attached as soon as the token is created (Bug 473928)
     */
    public MqttToken checkForActivity(MqttActionListener pingCallback) {
        MqttToken token = null;
        try {
            token = clientState.checkForActivity(pingCallback);
        } catch (MqttException e) {
            handleRunException(e);
        } catch (Exception e) {
            handleRunException(e);
        }
        return token;
    }

    private void handleRunException(Exception ex) {
        final String methodName = "handleRunException";
        // @TRACE 804=exception
        log.fine(CLASS_NAME, methodName, "804", null, ex);
        MqttException mex;
        if (!(ex instanceof MqttException)) {
            mex = new MqttException(MqttClientException.REASON_CODE_CONNECTION_LOST, ex);
        } else {
            mex = (MqttException) ex;
        }

        shutdownConnection(null, mex, null);
    }

    /**
     * When Automatic reconnect is enabled, we want ClientComs to enter the
     * 'resting' state if disconnected. This will allow us to publish messages
     *
     * @param resting
     *            if true, resting is enabled
     */
    public void setRestingState(boolean resting) {
        this.resting = resting;
    }

    public void setDisconnectedMessageBuffer(DisconnectedMessageBuffer disconnectedMessageBuffer) {
        this.disconnectedMessageBuffer = disconnectedMessageBuffer;
    }

    public int getBufferedMessageCount() {
        return this.disconnectedMessageBuffer.getMessageCount();
    }

    public MqttMessage getBufferedMessage(int bufferIndex) {
        MqttPublish send = (MqttPublish) this.disconnectedMessageBuffer.getMessage(bufferIndex).getMessage();
        return send.getMessage();
    }

    public void deleteBufferedMessage(int bufferIndex) {
        this.disconnectedMessageBuffer.deleteMessage(bufferIndex);
    }

    /**
     * When the client automatically reconnects, we want to send all messages from
     * the buffer first before allowing the user to send any messages
     */
    public void notifyReconnect() {
        final String methodName = "notifyReconnect";
        if (disconnectedMessageBuffer != null) {
            // @TRACE 509=Client Reconnected, Offline Buffer Available. Sending Buffered
            // Messages.
            log.fine(CLASS_NAME, methodName, "509");

            disconnectedMessageBuffer.setPublishCallback(new ReconnectDisconnectedBufferCallback(methodName));
            if (executorService == null) {
                new Thread(disconnectedMessageBuffer).start();
            } else {
                executorService.execute(disconnectedMessageBuffer);
            }
        }
    }

    class ReconnectDisconnectedBufferCallback implements IDisconnectedBufferCallback {

        final String methodName;

        ReconnectDisconnectedBufferCallback(String methodName) {
            this.methodName = methodName;
        }

        public void publishBufferedMessage(BufferedMessage bufferedMessage) throws MqttException {
            if (isConnected()) {
                // @TRACE 510=Publising Buffered message message={0}
                log.fine(CLASS_NAME, methodName, "510", new Object[] { bufferedMessage.getMessage().getKey() });
                internalSend(bufferedMessage.getMessage(), bufferedMessage.getToken());
                // Delete from persistence if in there
                clientState.unPersistBufferedMessage(bufferedMessage.getMessage());
            } else {
                // @TRACE 208=failed: not connected
                log.fine(CLASS_NAME, methodName, "208");
                throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_CLIENT_NOT_CONNECTED);
            }
        }
    }

    public int getActualInFlight() {
        return this.clientState.getActualInFlight();
    }

    public boolean doesSubscriptionIdentifierExist(int subscriptionIdentifier) {
        return this.callback.doesSubscriptionIdentifierExist(subscriptionIdentifier);

    }

}