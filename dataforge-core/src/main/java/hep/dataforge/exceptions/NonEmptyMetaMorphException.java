package hep.dataforge.exceptions;

/**
 * Created by darksnake on 12-Nov-16.
 */
public class NonEmptyMetaMorphException extends IllegalStateException {
    private Class<?> type;

    public NonEmptyMetaMorphException(Class<?> type) {
        super(String.format("Can not update non-empty MetaMorph for class '%s'", type.getSimpleName()));
        this.type = type;
    }

}
