/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.goals;

import hep.dataforge.utils.ReferenceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * @author Alexander Nozik
 */
public abstract class AbstractGoal<T> implements Goal<T> {

    private final ReferenceRegistry<GoalListener<T>> listeners = new ReferenceRegistry<>();


    private final Executor executor;
    protected final GoalResult result = new GoalResult();
    private CompletableFuture<?> computation;
    private Thread thread;

    public AbstractGoal(Executor executor) {
        this.executor = executor;
    }

    public AbstractGoal() {
        this.executor = ForkJoinPool.commonPool();
    }

    protected Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

    @Override
    public synchronized void run() {
        if (!isRunning()) {
            //start all dependencies so they will occupy threads
            computation = CompletableFuture
                    .allOf(dependencies()
                            .map(dep -> {
                                dep.run();//starting all dependencies
                                return dep.asCompletableFuture();
                            })
                            .toArray(CompletableFuture[]::new))
                    .whenCompleteAsync((res, err) -> {
                        if (err != null) {
                            getLogger().error("One of goal dependencies failed with exception", err);
                            if (failOnError()) {
                                this.result.completeExceptionally(err);
                            }
                        }

                        try {
                            thread = Thread.currentThread();
                            //trigger start hooks
                            listeners.forEach(GoalListener::onGoalStart);
                            T r = compute();
                            //triggering result hooks
                            listeners.forEach(listener -> listener.onGoalComplete(r));
                            this.result.complete(r);
                        } catch (Exception ex) {
                            //trigger exception hooks
                            getLogger().error("Exception during goal execution", ex);
                            listeners.forEach(listener -> listener.onGoalFailed(ex));
                            this.result.completeExceptionally(ex);
                        } finally {
                            thread = null;
                        }
                    }, executor);
        }
    }

    public Executor getExecutor() {
        return executor;
    }

    protected abstract T compute() throws Exception;

    /**
     * If true the goal will result in error if any of dependencies throws exception.
     * Otherwise it will be calculated event if some of dependencies are failed.
     *
     * @return
     */
    protected boolean failOnError() {
        return true;
    }

    /**
     * Abort internal goals process without canceling result. Use with
     * care
     */
    protected void abort() {
        if (isRunning()) {
            if (this.computation != null) {
                this.computation.cancel(true);
            }
            if (thread != null) {
                thread.interrupt();
            }
        }
    }

    public boolean isRunning() {
        return this.result.isDone() || this.computation != null;
    }

    /**
     * Abort current goals if it is in progress and set result. Useful for
     * caching purposes.
     *
     * @param result
     */
    public final synchronized boolean complete(T result) {
        abort();
        return this.result.complete(result);
    }

    @Override
    public void registerListener(GoalListener<T> listener) {
        listeners.add(listener,true);
    }

    @Override
    public CompletableFuture<T> asCompletableFuture() {
        return result;
    }


    protected class GoalResult extends CompletableFuture<T> {

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (mayInterruptIfRunning) {
                abort();
            }
            return super.cancel(mayInterruptIfRunning);
        }
    }

}
