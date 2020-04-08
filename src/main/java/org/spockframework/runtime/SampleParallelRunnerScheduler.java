package org.spockframework.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.runners.model.RunnerScheduler;

/**
 * Created by chetanc on 16/07/19.
 */
public class SampleParallelRunnerScheduler implements RunnerScheduler {

  private final ExecutorService fService = Executors.newCachedThreadPool();

  public void schedule(Runnable childStatement) {
    fService.submit(childStatement);
  }

  public void finished() {
    try {
      fService.shutdown();
      fService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace(System.err);
    }
  }
}

