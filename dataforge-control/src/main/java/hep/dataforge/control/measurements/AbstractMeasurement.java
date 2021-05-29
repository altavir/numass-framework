/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.control.measurements;

import hep.dataforge.exceptions.MeasurementException;
import hep.dataforge.utils.DateTimeUtils;
import kotlin.Pair;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * A boilerplate code for measurements
 *
 * @author Alexander Nozik
 */
@Deprecated
public abstract class AbstractMeasurement<T> implements Measurement<T> {

    //    protected final ReferenceRegistry<MeasurementListener<T>> listeners = new ReferenceRegistry<>();
    protected Pair<T, Instant> lastResult;
    protected Throwable exception;
    private MeasurementState state;

    protected MeasurementState getMeasurementState() {
        return state;
    }

    protected void setMeasurementState(MeasurementState state) {
        this.state = state;
    }

    /**
     * Call after measurement started
     */
    protected void afterStart() {
        setMeasurementState(MeasurementState.PENDING);
        notifyListeners(it -> it.onMeasurementStarted(this));
    }

    /**
     * Call after measurement stopped
     */
    protected void afterStop() {
        setMeasurementState(MeasurementState.FINISHED);
        notifyListeners(it -> it.onMeasurementFinished(this));
    }

    /**
     * Reset measurement to initial state
     */
    protected void afterPause() {
        setMeasurementState(MeasurementState.OK);
        notifyListeners(it -> it.onMeasurementFinished(this));
    }

    protected synchronized void onError(String message, Throwable error) {
        LoggerFactory.getLogger(getClass()).error("Measurement failed with error: " + message, error);
        setMeasurementState(MeasurementState.FAILED);
        this.exception = error;
        notify();
        notifyListeners(it -> it.onMeasurementFailed(this, error));
    }

    /**
     * Internal method to notify measurement complete. Uses current system time
     *
     * @param result
     */
    protected final void result(T result) {
        result(result, DateTimeUtils.now());
    }

    /**
     * Internal method to notify measurement complete
     *
     * @param result
     */
    protected synchronized void result(T result, Instant time) {
        this.lastResult = new Pair<>(result, time);
        setMeasurementState(MeasurementState.OK);
        notify();
        notifyListeners(it -> it.onMeasurementResult(this, result, time));
    }

    protected void updateProgress(double progress) {
        notifyListeners(it -> it.onMeasurementProgress(this, progress));
    }

    protected void updateMessage(String message) {
        notifyListeners(it -> it.onMeasurementProgress(this, message));
    }

    protected final void notifyListeners(Consumer<MeasurementListener> consumer) {
        getDevice().forEachConnection(MeasurementListener.class, consumer);
    }

    @Override
    public boolean isFinished() {
        return state == MeasurementState.FINISHED;
    }

    @Override
    public boolean isStarted() {
        return state == MeasurementState.PENDING || state == MeasurementState.OK;
    }

    @Override
    public Throwable getError() {
        return this.exception;
    }

    protected synchronized Pair<T, Instant> get() throws MeasurementException {
        if (getMeasurementState() == MeasurementState.INIT) {
            start();
            LoggerFactory.getLogger(getClass()).debug("Measurement not started. Starting");
        }
        while (state == MeasurementState.PENDING) {
            try {
                //Wait for result could cause deadlock if called in main thread
                wait();
            } catch (InterruptedException ex) {
                throw new MeasurementException(ex);
            }
        }
        if (this.lastResult != null) {
            return this.lastResult;
        } else if (state == MeasurementState.FAILED) {
            throw new MeasurementException(getError());
        } else {
            throw new MeasurementException("Measurement failed for unknown reason");
        }
    }

    @Override
    public Instant getTime() throws MeasurementException {
        return get().getSecond();
    }

    @Override
    public T getResult() throws MeasurementException {
        return get().getFirst();
    }

    protected enum MeasurementState {
        INIT, //Measurement not started
        PENDING, // Measurement in process
        OK, // Last measurement complete, next is planned
        FAILED, // Last measurement failed
        FINISHED, // Measurement finished or stopped
    }

}
