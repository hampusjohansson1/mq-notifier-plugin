/*
 *  The MIT License
 *
 *  Copyright 2015 Sony Mobile Communications Inc. All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package com.sonymobile.jenkins.plugins.mq.mqnotifier;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmCallback;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Creates an MQ connection.
 *
 * @author Örjan Percy &lt;orjan.percy@sonymobile.com&gt;
 */
public final class MQConnection implements ShutdownListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MQConnection.class);
    private static final int CONNECTION_WAIT = 10000;
    private static final int HEARTBEAT_INTERVAL = 30;
    private static final int MESSAGE_QUEUE_SIZE = 100000;
    private static final int SENDMESSAGE_TIMEOUT = 100;

    private String userName;
    private Secret userPassword;
    private String serverUri;
    private String virtualHost;
    private Connection connection = null;

    private volatile LinkedBlockingQueue messageQueue = new LinkedBlockingQueue(MESSAGE_QUEUE_SIZE);
    private volatile ConcurrentNavigableMap<Long, MessageData> outstandingConfirms = new ConcurrentSkipListMap<>();
    private Thread messageQueueThread;


    /**
     * Throw on exceptions when creating a channel
     */
    private static class ChannelCreationException extends IOException {

        public ChannelCreationException(String errorMessage) {
            super(errorMessage);
        }

        public ChannelCreationException(String errorMessage, Throwable cause) {
            super(errorMessage, cause);
        }
    }

    /**
     * Exception indicating an error delivering a message to MQ
     */
    private static class MessageDeliveryException extends IOException {
        public MessageDeliveryException(String errorMessage, Throwable cause) {
            super(errorMessage, cause);
        }
    }

    /**
     * Lazy-loaded singleton using the initialization-on-demand holder pattern.
     */
    private MQConnection() { }

    /**
     * Is only executed on {@link #getInstance()} invocation.
     */
    private static class LazyRabbit {
        private static final MQConnection INSTANCE = new MQConnection();
        private static final ConnectionFactory CF = new ConnectionFactory();
    }

    /**
     * Gets the instance.
     *
     * @return the instance
     */
    public static MQConnection getInstance() {
        return LazyRabbit.INSTANCE;
    }

    /**
     * Stores data for a RabbitMQ message.
     */
    private static final class MessageData {
        private String exchange;
        private String routingKey;
        private AMQP.BasicProperties props;
        private byte[] body;

        /**
         * Constructor.
         *
         * @param exchange the exchange to publish the message to
         * @param routingKey the routing key
         * @param props other properties for the message - routing headers etc
         * @param body the message body
         */
        private MessageData(String exchange, String routingKey, AMQP.BasicProperties props, byte[] body) {
            this.exchange = exchange;
            this.routingKey = routingKey;
            this.props = props;
            this.body = body;
        }

        /**
         * Gets the exchange name.
         *
         * @return the exchange name
         */
        private String getExchange() {
            return exchange;
        }

        /**
         * Gets the routing key.
         *
         * @return the routing key
         */
        private String getRoutingKey() {
            return routingKey;
        }

        /**
         * Gets the connection properties.
         *
         * @return the connection properties
         */
        private AMQP.BasicProperties getProps() {
            return props;
        }

        /**
         * Gets the message body.
         *
         * @return the message body
         */
        private byte[] getBody() {
            return body;
        }
    }

    /**
     * Get the number of currently outstanding confirms.
     *
     * @return the number of currently outstanding confirms
     */
    public int getSizeOutstandingConfirms() {
        return outstandingConfirms.size();
    }

    /**
     * Clear the outstanding confirms list, useful when testing.
     */
    public void clearOutstandingConfirms() {
        outstandingConfirms.clear();
    }

    /**
     * Puts a message in the message queue.
     *
     * @param exchange the exchange to publish the message to
     * @param routingKey the routing key
     * @param props other properties for the message - routing headers etc
     * @param body the message body
     */
    public void addMessageToQueue(String exchange, String routingKey, AMQP.BasicProperties props, byte[] body) {
        // If addMessageToQueue is called from multiple threads, make sure only one thread is started.
        synchronized (this) {
            if (messageQueueThread == null || !messageQueueThread.isAlive()) {
                messageQueueThread = new Thread(() -> sendMessages());
                messageQueueThread.start();
                LOGGER.info("messageQueueThread recreated since it was null or not alive.");
            }
        }
        MessageData messageData = new MessageData(exchange, routingKey, props, body);
        if (!messageQueue.offer(messageData)) {
            LOGGER.error("addMessageToQueue() failed, internal RabbitMQ queue is full!");
        }
    }

    /**
     * Publish json message on configured MQ server.
     *
     * @param json the message in json format
     */
    public void publish(JSONObject json) {
        MQNotifierConfig config = MQNotifierConfig.getInstance();
        if (config != null && config.isNotifierEnabled()) {
            AMQP.BasicProperties.Builder bob = new AMQP.BasicProperties.Builder();
            int dm = 1;
            if (config.getPersistentDelivery()) {
                dm = 2;
            }
            bob.appId(config.getAppId());
            bob.deliveryMode(dm);
            bob.contentType(Util.CONTENT_TYPE);
            bob.timestamp(Calendar.getInstance().getTime());

            addMessageToQueue(config.getExchangeName(), config.getRoutingKey(),
                    bob.build(), json.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Sends messages from the message queue.
     */
    private void sendMessages() {
        Channel channel = null;

        while (true) {
            try {
                if (channel == null || !channel.isOpen()) {
                    channel = createChannel();
                    channel.confirmSelect();
                    addMessageConfirmListener(channel);
                }
                MessageData messageData = (MessageData)messageQueue.poll(SENDMESSAGE_TIMEOUT,
                                                                         TimeUnit.MILLISECONDS);
                if (messageData != null) {
                    validateExchange(channel, messageData.getExchange());
                    getInstance().sendOnChannel(messageData, channel);
                }
            } catch (InterruptedException ie) {
                LOGGER.info("sendMessages() poll() was interrupted: ", ie);
            } catch (ChannelCreationException | MessageDeliveryException transientException) {
                LOGGER.error(transientException.getMessage(), transientException.getCause());
                try {
                    Thread.sleep(CONNECTION_WAIT);
                } catch (InterruptedException ie) {
                    LOGGER.error("Thread.sleep() was interrupted", ie);
                }
            } catch (IOException | IllegalArgumentException ioe) {
                LOGGER.error("error validating channel: ", ioe);
            }
        }
    }


    /**
     * Validate the exchange.
     *
     * @param channel a channel that must contain the given exchange
     * @param exchange the exchange to validate
     *
     * @throws IllegalArgumentException if the exchange is null
     * @throws IOException if the exchange exists, but is invalid for the channel
     */
    private void validateExchange(Channel channel, String exchange) throws IOException, IllegalArgumentException {
        if (exchange == null) {
            throw new IllegalArgumentException("Invalid configuration, exchange must not be null.");
        }
        channel.exchangeDeclarePassive(exchange);
    }

    /**
     * Try to create a channel using a connection.
     *
     * @return a Channel
     */
    private Channel createChannel() throws ChannelCreationException {
        try {
            connection = getConnection();
            if (connection != null) {
                LOGGER.debug("Channel successfully created");
                return connection.createChannel();
            }
            throw new ChannelCreationException("Cannot create channel, no connection found");
        } catch (IOException | ShutdownSignalException e) {
            throw new ChannelCreationException("Cannot create channel", e);
        }
    }

    /**
     * Add an async listener for ack/nack events and remove accordingly.
     *
     * @param channel the channel to configure a confirm listener for
     */
    private void addMessageConfirmListener(Channel channel) {
        ConfirmCallback cleanOutstandingConfirms = (sequenceNumber, multiple) -> {
            if (multiple) {
                ConcurrentNavigableMap<Long, MessageData> confirmed = this.outstandingConfirms.headMap(
                        sequenceNumber, true
                );
                confirmed.clear();
            } else {
                this.outstandingConfirms.remove(sequenceNumber);
            }
        };

        // Signature is addConfirmListener(successCallback, errorCallback)
        channel.addConfirmListener(cleanOutstandingConfirms, (sequenceNumber, multiple) -> {
            MessageData message = outstandingConfirms.get(sequenceNumber);
            messageQueue.offer(message);
            cleanOutstandingConfirms.handle(sequenceNumber, multiple);
        });
    }

    /**
     * Gets the connection factory that will enable a connection to the AMQP server.
     *
     * @return the connection factory
     */
    private ConnectionFactory getConnectionFactory() {
        if (LazyRabbit.CF != null) {
            try {
                // Try to recover the topology along with the connection.
                LazyRabbit.CF.setAutomaticRecoveryEnabled(true);
                // set requested heartbeat interval, in seconds
                LazyRabbit.CF.setRequestedHeartbeat(HEARTBEAT_INTERVAL);
                LazyRabbit.CF.setUri(serverUri);
                if (StringUtils.isNotEmpty(virtualHost)) {
                    LazyRabbit.CF.setVirtualHost(virtualHost);
                }
            } catch (KeyManagementException e) {
                LOGGER.error("KeyManagementException: ", e);
            } catch (NoSuchAlgorithmException e) {
                LOGGER.error("NoSuchAlgorithmException: ", e);
            } catch (URISyntaxException e) {
                LOGGER.error("URISyntaxException: ", e);
            }
            if (StringUtils.isNotEmpty(userName)) {
                LazyRabbit.CF.setUsername(userName);
                if (StringUtils.isNotEmpty(Secret.toString(userPassword))) {
                    LazyRabbit.CF.setPassword(Secret.toString(userPassword));
                }
            }
        }
        return LazyRabbit.CF;
    }

    /**
     * Gets the connection.
     *
     * @return the connection.
     */
    public Connection getConnection() {
        if (connection == null) {
            try {
                connection = getConnectionFactory().newConnection();
                connection.addShutdownListener(this);
            } catch (IOException e) {
                LOGGER.warn("Connection refused", e);
            } catch (TimeoutException te) {
                LOGGER.warn("Attempt to connect timed out: ", te);
            }
        }
        return connection;
    }

    /**
     * Initializes this instance with supplied values.
     *
     * @param name the user name
     * @param password the user password
     * @param uri the server uri
     * @param vh the virtual host
     */
    public void initialize(String name, Secret password, String uri, String vh) {
        userName = name;
        userPassword = password;
        serverUri = uri;
        virtualHost = vh;
        connection = null;
    }

    /**
     * Sends a message.
     * Keeps trying to get a connection indefinitely.
     *
     * @param messageData an object containing message data
     * @param channel a channel to publish the message on
     */
    private void sendOnChannel(MessageData messageData, Channel channel) throws MessageDeliveryException {
        try {
            outstandingConfirms.put(channel.getNextPublishSeqNo(), messageData);
            channel.basicPublish(
                    messageData.getExchange(),
                    messageData.getRoutingKey(),
                    messageData.getProps(),
                    messageData.getBody()
            );
        } catch (IOException e) {
            messageQueue.offer(messageData);
            throw new MessageDeliveryException("Cannot publish message", e);
        } catch (AlreadyClosedException e) {
            messageQueue.offer(messageData);
            throw new MessageDeliveryException("Connection is already closed", e);
        }
    }

    @Override
    public void shutdownCompleted(ShutdownSignalException cause) {
        if (cause.isHardError()) {
            if (!cause.isInitiatedByApplication()) {
                LOGGER.warn("MQ connection was suddenly disconnected.");
                try {
                    if (connection != null && connection.isOpen()) {
                        connection.close();
                    }
                } catch (IOException e) {
                    LOGGER.error("IOException: ", e);
                } catch (AlreadyClosedException e) {
                    LOGGER.error("AlreadyClosedException: ", e);
                } finally {
                    connection = null;
                }
            }
        } else {
            LOGGER.warn("MQ channel was suddenly disconnected.");
        }
    }
}
