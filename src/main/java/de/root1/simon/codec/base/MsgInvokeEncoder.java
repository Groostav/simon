/*
 * Copyright (C) 2008 Alexander Christian <alex(at)root1.de>. All rights reserved.
 * 
 * This file is part of SIMON.
 *
 *   SIMON is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   SIMON is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with SIMON.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.root1.simon.codec.base;

import de.root1.simon.Dispatcher;
import de.root1.simon.Statics;
import de.root1.simon.codec.messages.MsgInvoke;
import de.root1.simon.codec.messages.MsgInvokeReturn;
import de.root1.simon.exceptions.SimonRemoteException;
import de.root1.simon.utils.Utils;
import java.nio.charset.Charset;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.demux.MessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link MessageEncoder} that encodes {@link MsgInvoke}.
 *
 * @author ACHR
 */
public class MsgInvokeEncoder<T extends MsgInvoke> extends AbstractMessageEncoder<T> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private SerializerSet serializers;

    public MsgInvokeEncoder(SerializerSet serializers) {
        this.serializers = serializers;
    }

    @Override
    protected void encodeBody(IoSession session, T message, IoBuffer out) {

        logger.trace("begin. message={}", message);
        try {

            out.putPrefixedString(message.getRemoteObjectName(), Charset.forName("UTF-8").newEncoder());
            out.putLong(Utils.computeMethodHash(message.getMethod()));

            int argsLen = 0;

            if (message.getArguments() != null) {
                argsLen = message.getArguments().length;
            }

            logger.trace("argsLength={}", argsLen);

            out.putInt(argsLen);

            for (int i = 0; i < argsLen; i++) {
                Object argument = message.getArguments()[i];
                logger.trace("args[{}]={}", i, argument);
                UserObjectSerializer.writeUserObject(serializers, argument, out);
            }

        } catch (Exception e) {

            String errorMsg = "Failed to transfer invoke command to the server. error=" + e.getMessage();
            logger.warn(errorMsg);
            Dispatcher dispatcher = (Dispatcher) session.getAttribute(Statics.SESSION_ATTRIBUTE_DISPATCHER);

            MsgInvokeReturn mir = new MsgInvokeReturn();
            mir.setSequence(message.getSequence());
            mir.setReturnValue(new SimonRemoteException(errorMsg, e));

            try {
                dispatcher.messageReceived(session, mir);
            } catch (Exception e1) {
                // FIXME When will this Exception occur? API Doc shows no information
                logger.error("Got exception when calling 'dispatcher.messageReceived()'", e1);
            }
        }
        logger.trace("end");
    }

}
