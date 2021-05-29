/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.control.measurements;

import java.time.Duration;

/**
 *
 * @author Alexander Nozik
 */
@Deprecated
public abstract class RegularMeasurement<T> extends SimpleMeasurement<T> {

    private boolean stopFlag = false;

    @Override
    protected void finishTask() {
        if (stopFlag || (stopOnError() && getMeasurementState() == MeasurementState.FAILED)) {
            afterStop();
        } else {
            startTask();
        }
    }

    @Override
    public boolean stop(boolean force) {
        if (isFinished()) {
            return false;
        } else if (force) {
            return interruptTask(force);
        } else {
            stopFlag = true;
            return true;
        }
    }

    protected boolean stopOnError() {
        return true;
    }

    protected abstract Duration getDelay();

}
