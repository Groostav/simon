package de.root1.simon.codec.base;

import de.root1.simon.codec.messages.*;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MsgAsyncComputationFinishedDecoder extends AbstractMessageDecoder {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SerializerSet serializers;

    public MsgAsyncComputationFinishedDecoder(SerializerSet serializers) {
        super(SimonMessageConstants.MSG_ASYNC_FINISHED);
        this.serializers = serializers;
    }

    @Override
    protected AbstractMessage decodeBody(IoSession session, IoBuffer in) {

        MsgAsyncComputationFinished m = new MsgAsyncComputationFinished();
        try {
            Object exception = UserObjectSerializer.readUserObject(serializers, in);
            if(exception != null && !(exception instanceof Throwable)){
                MsgError error = new MsgError();
                error.setErrorMessage("Error while async result: thrown exception is not instance of Throwable");
                error.setRemoteObjectName(null);
                exception = new ClassCastException("cannot cast "+exception+" to java.lang.Throwable");
            }
            Object returnValue = UserObjectSerializer.readUserObject(serializers, in);
            m.setThrown((Throwable) exception);
            m.setReturnValue(returnValue);
        }
        catch (ClassNotFoundException e) {
            MsgError error = new MsgError();
            error.setErrorMessage("Error while async result: Not able to read result due to ClassNotFoundException");
            error.setRemoteObjectName(null);
            error.setThrowable(e);
            return error;
        }
        logger.trace("message={}", m);
        return m;

    }

    @Override
    public void finishDecode(IoSession session, ProtocolDecoderOutput out) throws Exception {
    }
}
