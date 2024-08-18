package com.playmonumenta.networkrelay;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

// Entire purpose of this class is to mimic a Bukkit/Bungee main "thread"
public class RabbitMQExecutor {
	private static @MonotonicNonNull RabbitMQExecutor INSTANCE;
	private final ScheduledExecutorService mExecutor;

	public RabbitMQExecutor(String threadName) {
		this.mExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(threadName).build());
	}

	public ScheduledExecutorService getExecutor() {
		return mExecutor;
	}

	public void stop() {
		if (mExecutor.isShutdown()) {
			return;
		}
		mExecutor.shutdown();
		try {
			if (mExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
				mExecutor.shutdownNow();
			}
		} catch (InterruptedException ex) {
			if (!mExecutor.isShutdown()) {
				mExecutor.shutdownNow();
			}
		}
	}

	public void schedule(Runnable runnable) {
		if (mExecutor.isShutdown()) {
			return;
		}
		schedule(new WrappedRunnable(runnable));
	}

	public void schedule(WrappedRunnable runnable) {
		if (mExecutor.isShutdown()) {
			return;
		}
		mExecutor.schedule(runnable, 0, TimeUnit.MILLISECONDS);
	}

	public void scheduleRepeatingTask(Runnable runnable, long delay, long period, TimeUnit unit) {
		if (mExecutor.isShutdown()) {
			return;
		}
		mExecutor.scheduleAtFixedRate(new WrappedRunnable(runnable), delay, period, unit);
	}

	public void scheduleRepeatingTask(WrappedRunnable runnable, long delay, long period, TimeUnit unit) {
		if (mExecutor.isShutdown()) {
			return;
		}
		mExecutor.scheduleAtFixedRate(runnable, delay, period, unit);
	}

	public static class WrappedRunnable implements Runnable {
		private final Runnable mTask;

		public WrappedRunnable(Runnable task) {
			this.mTask = task;
		}

		@Override
		public void run() {
			try {
				mTask.run();
			} catch (Exception ex) {
				MMLog.severe("Error executing task in RabbitMQ");
				ex.printStackTrace();
			}
		}
	}


}
