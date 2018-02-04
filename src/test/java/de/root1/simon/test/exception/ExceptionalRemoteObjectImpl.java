/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.root1.simon.test.exception;

import de.root1.simon.annotation.SimonRemote;
import de.root1.simon.exceptions.SimonRemoteException;

/**
 *
 * @author achristian
 */
@SimonRemote
public class ExceptionalRemoteObjectImpl implements ExceptionalRemoteObject {

    @Override
    public void helloCheckedExceptions() throws CheckedException {
        throw new CheckedException("Checked blam!");
    }

    @Override
    public void helloUncheckedExceptions() {
        throw new UncheckedException("unchecked blam!");
    }

    @Override
    public Exception helloExceptionProcessor(Exception arg) {
        return new ProcessedException("Simons-stack!", arg);
    }
}
