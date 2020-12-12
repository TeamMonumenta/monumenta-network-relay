package com.playmonumenta.networkrelay;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class RabbitMQManager {
	private static final String CONSUMER_TAG = "consumerTag";
	private static final String BROADCAST_EXCHANGE_NAME = "broadcast";

	private static RabbitMQManager INSTANCE = null;

	private final Gson mGson = new Gson();
	private final Logger mLogger;
	private final Channel mChannel;
	private final String mShardName;

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

	protected RabbitMQManager(Plugin plugin, String shardName, String rabbitURI) throws Exception {
		mLogger = plugin.getLogger();
		mShardName = shardName;

		ConnectionFactory factory = new ConnectionFactory();
		factory.setUri(rabbitURI);

		Connection connection = factory.newConnection();
		mChannel = connection.createChannel();

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

			/* Process the packet on the main thread */
			Bukkit.getScheduler().runTask(plugin, () -> {
				mLogger.fine("Processing message from=" + source + " channel=" + channel);
				mLogger.finer("data=" + mGson.toJson(data));

				try {
					NetworkRelayMessageEvent event = new NetworkRelayMessageEvent(channel, source, data);
					Bukkit.getPluginManager().callEvent(event);
				} catch (Exception ex) {
					mLogger.warning("Failed to handle rabbit message '" + message + "'");
					ex.printStackTrace();
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
				plugin.getLogger().info("RabbitMQ consumer has terminated");
			} else {
				plugin.getLogger().severe("RabbitMQ consumer has terminated unexpectedly - stopping the shard...");
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "stop");
			}
		});

		plugin.getLogger().info("Started RabbitMQ consumer");

		INSTANCE = this;
	}

	protected void stop() {
		mShutdown = true;
		if (mConsumerAlive) {
			try {
				mChannel.basicCancel(CONSUMER_TAG);
			} catch (Exception ex) {
				mLogger.info("Failed to cancel rabbit consumer");
			}
		}
	}

	protected static RabbitMQManager getInstance() {
		return INSTANCE;
	}

	protected void sendNetworkMessage(String destination, String channel, JsonObject data) throws Exception {
		/* Used in case the specific packet type overrides properties like expiration / time to live */
		// TODO: Hook this up?
		AMQP.BasicProperties properties = null;

		JsonObject json = new JsonObject();
		json.addProperty("source", mShardName);
		json.addProperty("dest", destination);
		json.addProperty("channel", channel);
		json.add("data", data);

		try {
			byte[] msg = mGson.toJson(json).getBytes(StandardCharsets.UTF_8);

			if (destination.equals("*")) {
				/* Broadcast message - send to the broadcast exchange to route to all queues */
				mChannel.basicPublish(BROADCAST_EXCHANGE_NAME, "", properties, msg);
			} else {
				/* Non-broadcast message - send to the default exchange, routing to the appropriate queue */
				mChannel.basicPublish("", destination, properties, msg);
			}

			mLogger.fine("Sent message destination=" + destination + " channel=" + channel);
			mLogger.finer("data=" + mGson.toJson(data));
		} catch (Exception e) {
			throw new Exception(String.format("Error sending message destination=" + destination + " channel=" + channel), e);
		}
	}
}
