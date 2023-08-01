package com.playmonumenta.networkrelay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class StandaloneLogger {
	private static class EventHandler implements Comparable<EventHandler>, Consumer<Object> {
		private final int mPriority;
		private final Consumer<Object> mConsumer;

		public EventHandler(int priority, Consumer<Object> consumer) {
			mPriority = priority;
			mConsumer = consumer;
		}

		@Override
		public int compareTo(@NotNull StandaloneLogger.EventHandler o) {
			return Integer.compare(o.mPriority, mPriority);
		}

		@Override
		public void accept(Object o) {
			mConsumer.accept(o);
		}
	}

	private static final TreeSet<EventHandler> mEventHandlers = new TreeSet<>();
	public static final BiConsumer<Integer, Consumer<Object>> mRegisterEventMethod =
		(integer, consumer) -> mEventHandlers.add(new EventHandler(integer, consumer));
	public static final Consumer<Object> mCallEventMethod = o -> {
		for (EventHandler eventHandler : mEventHandlers) {
			eventHandler.accept(o);
		}
	};

	public static void main(String[] args) {
		Logger rootLogger = Logger.getGlobal();
		rootLogger.info("Initializing...");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		File configFile = new File("config.yml");

		mRegisterEventMethod.accept(-1, o -> {
			if (!(o instanceof NetworkRelayMessageEventGeneric)) {
				return;
			}
			NetworkRelayMessageEventGeneric messageEvent = (NetworkRelayMessageEventGeneric) o;
			String logMessage = messageEvent.getSource() +
				": " +
				messageEvent.getChannel() +
				"\n" +
				gson.toJson(messageEvent.getData());
			rootLogger.info(logMessage);
		});

		NetworkRelayGeneric genericRelay = new NetworkRelayGeneric(rootLogger,
			configFile,
			StandaloneLogger.class,
			"standalone_logger.yml",
			"logger",
			mRegisterEventMethod,
			mCallEventMethod);

		rootLogger.info("End of config");

		genericRelay.setServerFinishedStarting();

		while (true) {
			try {
				TimeUnit.MINUTES.sleep(1L);
			} catch (InterruptedException ignored) {
			}
		}
	}
}
