package io.github.subkek.customdiscs.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class JavaScheduler {
  private final ScheduledExecutorService scheduler;

  private static JavaScheduler instance;
  public static JavaScheduler getInstance() {
    if (instance == null) return instance = new JavaScheduler();
    return instance;
  }

  public JavaScheduler() {
    this.scheduler = Executors.newScheduledThreadPool(1);
  }

  public ScheduledFuture<?> runAtFixedRate(Runnable method, long initialDelay, long period, TimeUnit timeUnit) {
    return scheduler.scheduleAtFixedRate(method, initialDelay, period, timeUnit);
  }

  public void shutdown() {
    scheduler.shutdown();
  }
}
