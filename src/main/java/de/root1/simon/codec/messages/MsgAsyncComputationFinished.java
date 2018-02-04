package de.root1.simon.codec.messages;

public class MsgAsyncComputationFinished extends AbstractMessage {

    private Object returnValue;
    private Throwable thrown;

    public MsgAsyncComputationFinished() { super(SimonMessageConstants.MSG_ASYNC_FINISHED); }

    // TODO: null has dual semantics here, it means unfinished ant it also means finished will null.
    // because this object is trying to act as a union-type, this is likely to result in a bug.
    // TODO: replace with some kind of sealed class hierarchy or at least explicit NULL and UNINITIALIZED values?
    // use JSR 305 `@Nonnull` after that?
    public Object getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }

    public Throwable getThrown() {
        return thrown;
    }

    public void setThrown(Throwable thrown) {
        this.thrown = thrown;
    }

    @Override
    public String toString() {
        // it is a good practice to create toString() method on message classes.
        return getSequence() + ":MsgAsyncComputationFinished(" + returnValue + ')';
    }
    //kotlin data classes would reduce the size of this repo by at least 500 lines....
}

