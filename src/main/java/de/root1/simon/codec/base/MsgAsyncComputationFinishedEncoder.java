package de.root1.simon.codec.base;

import de.root1.simon.codec.messages.MsgAsyncComputationFinished;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MsgAsyncComputationFinishedEncoder<T extends MsgAsyncComputationFinished> extends AbstractMessageEncoder<T> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SerializerSet serializers = SerializerSet.INSTANCE;

    @Override
    protected void encodeBody(IoSession session, MsgAsyncComputationFinished message, IoBuffer out) {
        logger.trace("begin. message={}", message);

        UserObjectSerializer.writeUserObject(serializers, message.getThrown(), out);
        UserObjectSerializer.writeUserObject(serializers, message.getReturnValue(), out);

        /*
         * There is no need to write the message.getErrorMsg() string back to the client
         * If an error occurs while invoking the method, the exception is "transported" as
         * the method-result back to the client where it is thrown in SimonProxy
         */

        logger.trace("end");
    }

}
