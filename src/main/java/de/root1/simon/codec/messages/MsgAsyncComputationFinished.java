package de.root1.simon.codec.messages;

public class MsgAsyncComputationFinished extends AbstractMessage {

    private Object returnValue;

    public MsgAsyncComputationFinished() { super(SimonMessageConstants.MSG_ASYNC_FINISHED); }

    public Object getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }

    @Override
    public String toString() {
        // it is a good practice to create toString() method on message classes.
        return getSequence() + ":MsgAsyncComputationFinished(" + returnValue + ')';
    }

    //kotlin data classes would reduce the size of this repo by at least 500 lines....
}
