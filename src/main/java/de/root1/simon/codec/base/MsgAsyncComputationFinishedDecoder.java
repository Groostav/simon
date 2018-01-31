package de.root1.simon.codec.base;

import de.root1.simon.codec.messages.AbstractMessage;
import de.root1.simon.codec.messages.MsgError;
import de.root1.simon.codec.messages.MsgInvokeReturn;
import de.root1.simon.codec.messages.SimonMessageConstants;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MsgAsyncComputationFinishedDecoder extends AbstractMessageDecoder {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public MsgAsyncComputationFinishedDecoder() {
        super(SimonMessageConstants.MSG_ASYNC_FINISHED);
    }

    @Override
    protected AbstractMessage decodeBody(IoSession session, IoBuffer in) {

        MsgInvokeReturn m = new MsgInvokeReturn();
        try {
            // oook! so my thinking is that we have a fast-path for primatives.
            // Serializable also fast-path'd?
            // when do we get dump'd into a general flow? how is that general flow configurable?
            // why does this message exist at all?
            Object returnValue = UserObjectSerializer.readUserObject(in);
            m.setReturnValue(returnValue);
        } catch (ClassNotFoundException e) {
            MsgError error = new MsgError();
            error.setErrorMessage("Error while decoding invoke return: Not able to read invoke result due to ClassNotFoundException");
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
