package org.spockframework.runtime;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.logging.Logger;
import org.junit.runners.model.RunnerScheduler;

import static java.util.concurrent.ForkJoinTask.inForkJoinPool;

/**
 * Created by chetanc on 15/07/19.
 */
public class ParallelRunnerScheduler implements RunnerScheduler {
  Logger logger = Logger.getLogger(ParallelRunnerScheduler.class.toString());


  static ForkJoinPool forkJoinPool = setUpForkJoinPool();

  static ForkJoinPool setUpForkJoinPool() {
    int numThreads;
    try {
      String configuredNumThreads = System.getProperty("maxParallelTestThreads");
      numThreads = Math.max(2, Integer.parseInt(configuredNumThreads));
    } catch (Exception ignored) {
      Runtime runtime = Runtime.getRuntime();
      numThreads = Math.max(2, runtime.availableProcessors());
    }
    ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory = pool -> {
      if (pool.getPoolSize() >= pool.getParallelism()) {
        return null;
      } else {
        ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
        thread.setName("JUnit-" + thread.getName());
        return thread;
      }
    };
    return new ForkJoinPool(numThreads, threadFactory, null, false);
  }

  private final Deque<ForkJoinTask<?>> _asyncTasks = new LinkedList<>();
  private Runnable _lastScheduledChild;

  @Override
  public void schedule(Runnable childStatement) {

    logger.info("Scheule method called for child " + childStatement.toString());
    if (_lastScheduledChild != null) {
      // Execute previously scheduled child asynchronously ...
      if (inForkJoinPool()) {
        logger.info("inForkJoinPool if condition" + childStatement.toString());
        _asyncTasks.addFirst(ForkJoinTask.adapt(_lastScheduledChild).fork());
      } else {
        logger.info("inForkJoinPool else condition" + childStatement.toString());
        _asyncTasks.addFirst(forkJoinPool.submit(_lastScheduledChild));
      }
    }
    // Note: We don't schedule the childStatement immediately here,
    // but remember it, so that we can synchronously execute the
    // last scheduled child in the finished method() -- this way,
    // the current thread does not immediately call join() in the
    // finished() method, which might block it ...
    _lastScheduledChild = childStatement;
  }

  @Override
  public void finished() {
    logger.info("Running test :: in finished " );
    MultiException me = new MultiException();
    if (_lastScheduledChild != null) {
      if (inForkJoinPool()) {
        // Execute the last scheduled child in the current thread ...
        try {
          System.out.println("Running test :: " );
          _lastScheduledChild.run(); } catch (Throwable t) { me.add(t); }
      } else {
        // Submit the last scheduled child to the ForkJoinPool too,
        // because all tests should run in the worker threads ...
        _asyncTasks.addFirst(forkJoinPool.submit(_lastScheduledChild));
      }
      // Make sure all asynchronously executed children are done, before we return ...
      for (ForkJoinTask<?> task : _asyncTasks) {
        System.out.println("stealing task for test :: " + task);
        // Note: Because we have added all tasks via addFirst into _asyncTasks,
        // task.join() is able to steal tasks from other worker threads,
        // if there are tasks, which have not been started yet ...
        // from other worker threads ...
        try { task.join(); } catch (Throwable t) { me.add(t); }
      }
      me.throwIfNotEmpty();
    }
  }
}
