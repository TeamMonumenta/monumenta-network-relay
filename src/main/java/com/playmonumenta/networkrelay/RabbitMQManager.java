package com.playmonumenta.networkrelay;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

public class RabbitMQManager {
	private static final String CONSUMER_TAG = "consumerTag";
	private static final String BROADCAST_EXCHANGE_NAME = "broadcast";

	private static @Nullable RabbitMQManager INSTANCE = null;

	private final Gson mGson = new Gson();
	private final Logger mLogger;
	private final Channel mChannel;
	private final Connection mConnection;
	private final RabbitMQManagerAbstractionInterface mAbstraction;
	private final String mShardName;
	private final int mHeartbeatInterval;
	private final int mDestinationTimeout;
	private final long mDefaultTTL;

	/*
	 * If mShutdown = false, this is expected to run normally
	 * If mShutdown = true, the server is already shutting down
	 */
	private boolean mShutdown = false;

	/*
	 * If mConsumerAlive = true, the consumer is running
	 * If mConsumerAlive = false, the consumer has terminated
	 */
	private boolean mConsumerAlive = false;

	/*
	 * Last time a message was sent.
	 * If this was more than mHeartbeatInterval seconds ago,
	 * send another heartbeat message.
	 */
	private Instant mLastHeartbeat = Instant.MIN;

	/*
	 * Last time a message was received from a destination.
	 * If this was more than mDestinationTimeout seconds ago,
	 * consider that destination offline.
	 * Offline destinations are removed from the map.
	 */
	private Map<String, Instant> mDestinationLastHeartbeat = new HashMap<>();

	/*
	 * Most recently received plugin data from a shard
	 * Updated each heartbeat, removed when shard is considered offline
	 * based on mDestinationLastHeartbeat
	 */
	private Map<String, JsonObject> mDestinationHeartbeatData = new HashMap<>();

	private static class QueuedMessage {
		final String mChannel;
		final JsonObject mData;

		private QueuedMessage(String channel, JsonObject data) {
			mChannel = channel;
			mData = data;
		}

		private String getChannel() {
			return mChannel;
		}

		private JsonObject getData() {
			return mData;
		}
	}

	private Map<String, ArrayDeque<QueuedMessage>> mDestinationQueuedMessages = new HashMap<>();

	protected RabbitMQManager(RabbitMQManagerAbstractionInterface abstraction, Logger logger, String shardName, String rabbitURI, int heartbeatInterval, int destinationTimeout, long defaultTTL) throws Exception {
		mAbstraction = abstraction;
		mLogger = logger;
		mShardName = shardName;
		mHeartbeatInterval = heartbeatInterval;
		mDestinationTimeout = destinationTimeout;
		mDefaultTTL = defaultTTL;

		mAbstraction.startHeartbeatRunnable(() -> {
			Instant now = Instant.now();
			if (now.minusSeconds(mHeartbeatInterval).compareTo(mLastHeartbeat) >= 0) {
				mLastHeartbeat = now;
				sendHeartbeat();
			}

			Instant timeoutThreshhold = now.minusSeconds(mDestinationTimeout);
			Iterator<Map.Entry<String, Instant>> iter = mDestinationLastHeartbeat.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, Instant> entry = iter.next();
				String dest = entry.getKey();
				Instant lastHeartbeat = entry.getValue();

				if (timeoutThreshhold.compareTo(lastHeartbeat) >= 0) {
					mDestinationHeartbeatData.remove(dest);
					sendDestOfflineEvent(dest);
					iter.remove();
				}
			}
		}, 2, 1);

		ConnectionFactory factory = new ConnectionFactory();
		factory.setUri(rabbitURI);

		mConnection = factory.newConnection();
		mChannel = mConnection.createChannel();

		/* Declare a broadcast exchange which routes messages to all attached queues */
		mChannel.exchangeDeclare(BROADCAST_EXCHANGE_NAME, "fanout");
		/* Declare the queue for this shard */
		mChannel.queueDeclare(shardName, false, false, false, null);
		/* Bind the queue to the exchange */
		mChannel.queueBind(shardName, BROADCAST_EXCHANGE_NAME, "");

		/* Consumer to receive messages */
		DeliverCallback deliverCallback = (consumerTag, delivery) -> {
			final String message;
			final JsonObject obj;

			try {
				message = new String(delivery.getBody(), "UTF-8");

				obj = mGson.fromJson(message, JsonObject.class);
				if (obj == null) {
					throw new Exception("Failed to parse rabbit message as json: " + message);
				}
				if (!obj.has("channel") || !obj.get("channel").isJsonPrimitive() || !obj.get("channel").getAsJsonPrimitive().isString()) {
					throw new Exception("Rabbit message missing 'channel': " + message);
				}
				if (!obj.has("source") || !obj.get("source").isJsonPrimitive() || !obj.get("source").getAsJsonPrimitive().isString()) {
					throw new Exception("Rabbit message missing 'source': " + message);
				}
				if (!obj.has("data") || !obj.get("data").isJsonObject()) {
					throw new Exception("Rabbit message missing 'data': " + message);
				}
			} catch (Exception ex) {
				mLogger.warning(ex.getMessage());
				/* Parsing this message failed - but ack it anyway, because it's not going to parse next time either */
				mChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
				return;
			}

			String channel = obj.get("channel").getAsString();
			String source = obj.get("source").getAsString();
			JsonObject data = obj.get("data").getAsJsonObject();
			JsonObject pluginData = null;

			/* Check for heartbeat data - pluginData */
			if (data.has("pluginData")) {
				JsonElement pluginDataElement = data.get("pluginData");
				if (pluginDataElement != null && pluginDataElement.isJsonObject()) {
					pluginData = pluginDataElement.getAsJsonObject();
				}
			}
			final JsonObject pluginDataFinal = pluginData;

			/* Process the packet on the main thread */
			mAbstraction.scheduleProcessPacket(() -> {
				mLogger.finer("Processing message from=" + source + " channel=" + channel);
				mLogger.finest(() -> "data=" + mGson.toJson(data));

				/* Check for heartbeat data - online status */
				boolean isDestShutdown = false;
				if (data.has("online")) {
					if (data.getAsJsonPrimitive("online").isBoolean() && data.getAsJsonPrimitive("online").getAsBoolean() == false) {
						isDestShutdown = true;
						mDestinationLastHeartbeat.remove(source);
						mDestinationHeartbeatData.remove(source);
						sendDestOfflineEvent(source);
					}
				}

				if (!isDestShutdown) {
					boolean heartbeatDataPresent = mDestinationLastHeartbeat.containsKey(source);

					if (pluginDataFinal != null) {
						/* This message contained heartbeat data - record it */
						mDestinationLastHeartbeat.put(source, Instant.now());
						mDestinationHeartbeatData.put(source, pluginDataFinal);
					}

					if (heartbeatDataPresent) {
						/* This shard was already marked online - deliver normally */
						mAbstraction.sendMessageEvent(channel, source, data);
					} else {
						/* This shard was not known to be online until this message */
						if (pluginDataFinal == null) {
							/*
							 * Got a message from this shard but unfortunately it doesn't contain any plugin data
							 * (i.e. it's not a heartbeat message)
							 * This can happen randomly when other traffic is happening and this receiving shard just started
							 * Can't send the online event yet - there's no heartbeat data which plugins might depend on while handling the online event
							 * Also can't deliver the message to plugins, since they may be doing state tracking based on online status
							 *
							 * Instead, queue the packet for later delivery, once we do receive a heartbeat message containing plugin data
							 */

							/* Get existing queue or create and insert a new one */
							ArrayDeque<QueuedMessage> queue = mDestinationQueuedMessages.computeIfAbsent(source, (unused) -> new ArrayDeque<>());
							queue.addLast(new QueuedMessage(channel, data));

							//TODO: change log level? Maybe warning if the queue is big or contains old entries?
							mLogger.warning("Queued packet from " + source + " as this shard has not received heartbeat data yet. Current queue size is " + queue.size());
						} else {
							/*
							 * Got a message from this shard and it has plugin data - great!
							 * Have everything needed to send online event and deliver the message
							 */

							mLogger.fine("Shard " + source + " is online");
							mAbstraction.sendDestOnlineEvent(source);

							/* Deliver this current message */
							mAbstraction.sendMessageEvent(channel, source, data);

							/* Check if there were any queued messages from before heartbeat data was available and deliver them */
							ArrayDeque<QueuedMessage> queue = mDestinationQueuedMessages.remove(source);
							if (queue != null) {
								for (QueuedMessage msg : queue) {
									mLogger.fine("Delivering queued message from " + source + " now that it is marked as online");
									mAbstraction.sendMessageEvent(msg.getChannel(), source, msg.getData());
								}
							}
						}
					}
				}

				/*
				 * Always acknowledge messages after attempting to handle them, even if there's an error
				 * Don't want a failing message to get stuck in an infinite loop
				 */

				try {
					mChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
				} catch (IOException ex) {
					/*
					 * If the channel disconnects, we just won't ack this message
					 * It will be redelivered later
					 */
					mLogger.warning("Failed to acknowledge rabbit message '" + message + "'");
				}
			});
		};

		mConsumerAlive = true;
		mChannel.basicConsume(shardName, false, CONSUMER_TAG, deliverCallback,
		                      consumerTag -> {
			mConsumerAlive = false;
			if (mShutdown) {
				mLogger.info("RabbitMQ consumer has terminated");
			} else {
				mLogger.severe("RabbitMQ consumer has terminated unexpectedly - stopping the shard...");
				mAbstraction.stopServer();
			}
		});

		mLogger.info("Started RabbitMQ consumer");

		INSTANCE = this;
	}

	protected void stop() {
		mShutdown = true;
		if (mConsumerAlive) {
			try {
				mAbstraction.stopHeartbeatRunnable();
			} catch (Exception ex) {
				mLogger.warning("Failed to cancel heartbeat runnable: " + ex.getMessage());
			}

			try {
				mChannel.basicCancel(CONSUMER_TAG);
			} catch (Exception ex) {
				mLogger.warning("Failed to cancel rabbit consumer: " + ex.getMessage());
			}
			try {
				JsonObject data = new JsonObject();
				data.addProperty("online", false);
				sendNetworkMessage("*", NetworkRelayAPI.HEARTBEAT_CHANNEL, data);
			} catch (Exception ex) {
				mLogger.warning("Failed to send shutdown heartbeat: " + ex.getMessage());
			}
			try {
				mConnection.close();
			} catch (Exception ex) {
				mLogger.warning("Failed to close rabbit channel: " + ex.getMessage());
			}
		}
	}

	protected static RabbitMQManager getInstance() {
		if (INSTANCE == null) {
			throw new RuntimeException("RabbitMQManager is not loaded");
		}
		return INSTANCE;
	}

	protected String getShardName() {
		return mShardName;
	}

	protected void sendNetworkMessage(String destination, String channel, JsonObject data) throws Exception {
		/* If no expiration set by caller, use the default */
		sendExpiringNetworkMessage(destination, channel, data, mDefaultTTL);
	}

	protected void sendNetworkMessage(String destination, String channel, JsonObject data, AMQP.BasicProperties properties) throws Exception {
		JsonObject json = new JsonObject();
		json.add("data", data);
		sendNetworkMessageInternal(destination, channel, json, properties);
	}

	/* Called after adding the data and (optionally) pluginData to a root json object */
	private void sendNetworkMessageInternal(String destination, String channel, JsonObject json, AMQP.BasicProperties properties) throws Exception {
		json.addProperty("source", mShardName);
		json.addProperty("dest", destination);
		json.addProperty("channel", channel);

		/* Broadcasting a non-heartbeat message - add heartbeat data to it if it's been more than half the normal heartbeat time since the last heartbeat */
		/* Note that heartbeats also go through this same method, so need to not add the same data to them twice */
		if (destination.equals("*") && !channel.equals(NetworkRelayAPI.HEARTBEAT_CHANNEL)) {
			Instant now = Instant.now();
			// * 500 because of converting seconds to milliseconds (*1000) divided by 2 (half the heartbeat interval as threshold to send early)
			if (now.minusMillis(mHeartbeatInterval * 500).compareTo(mLastHeartbeat) >= 0) {
				mLogger.finer("Adding heartbeat data to broadcast message instead of sending heartbeat");
				addHeartbeatDataToMessage(json);
				mLastHeartbeat = now;
			}
		}

		try {
			byte[] msg = mGson.toJson(json).getBytes(StandardCharsets.UTF_8);

			if (destination.equals("*")) {
				/* Broadcast message - send to the broadcast exchange to route to all queues */
				mChannel.basicPublish(BROADCAST_EXCHANGE_NAME, "", properties, msg);
			} else {
				/* Non-broadcast message - send to the default exchange, routing to the appropriate queue */
				mChannel.basicPublish("", destination, properties, msg);
			}

			mLogger.finer("Sent message destination=" + destination + " channel=" + channel);
			mLogger.finest(() -> "content=" + mGson.toJson(json));
		} catch (Exception e) {
			throw new Exception(String.format("Error sending message destination=" + destination + " channel=" + channel), e);
		}
	}

	protected void sendExpiringNetworkMessage(String destination, String channel, JsonObject data, long ttlSeconds) throws Exception {
		if (ttlSeconds <= 0) {
			throw new Exception("ttlSeconds must be a positive integer");
		}
		AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
			.expiration(Long.toString(ttlSeconds * 1000))
			.build();
		sendNetworkMessage(destination, channel, data, properties);
	}

	protected Set<String> getOnlineShardNames() {
		return new HashSet<String>(mDestinationLastHeartbeat.keySet());
	}

	protected Map<String, JsonObject> getOnlineShardHeartbeatData() {
		return mDestinationHeartbeatData;
	}

	private void sendHeartbeat() {
		try {
			JsonObject json = new JsonObject();
			addHeartbeatDataToMessage(json);
			json.add("data", new JsonObject()); /* A heartbeat message contains no data but this field is required */

			/* Heartbeat messages are only allowed to be retained by the exchange for 5x their interval */
			AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
				.expiration(Long.toString(mHeartbeatInterval * 1000 * 5))
				.build();

			sendNetworkMessageInternal("*", NetworkRelayAPI.HEARTBEAT_CHANNEL, json, properties);
		} catch (Exception ex) {
			mLogger.warning("Failed to send heartbeat: " + ex.getMessage());
		}
	}

	private void addHeartbeatDataToMessage(JsonObject json) {
		JsonObject eventPluginData = mAbstraction.gatherHeartbeatData();
		if (eventPluginData != null) {
			json.add("pluginData", eventPluginData);
		}
		json.addProperty("online", true);
	}

	private void sendDestOfflineEvent(String dest) {
		mLogger.fine("Shard " + dest + " is offline");
		mAbstraction.sendDestOfflineEvent(dest);
	}
}
