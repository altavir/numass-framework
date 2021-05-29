package hep.dataforge.exceptions;

import hep.dataforge.Named;

public class ContextLockException extends RuntimeException {
    private final Object locker;

    public ContextLockException(Object locker) {
        this.locker = locker;
    }

    public ContextLockException() {
        this.locker = null;
    }

    private String getObjectName() {
        if (locker instanceof Named) {
            return locker.getClass().getSimpleName() + ":" + ((Named) locker).getName();
        } else {
            return locker.getClass().getSimpleName();
        }
    }

    @Override
    public String getMessage() {
        if (locker == null) {
            return "Context is locked";
        } else {
            return "Context is locked by " + getObjectName();
        }
    }
}
