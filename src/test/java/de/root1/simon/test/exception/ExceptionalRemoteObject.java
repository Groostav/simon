/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.root1.simon.test.exception;

/**
 * @author Geoff Groos
 */
public interface ExceptionalRemoteObject {

    public void helloCheckedExceptions() throws CheckedException;
    public void helloUncheckedExceptions();

    public Exception helloExceptionProcessor(Exception arg);

    class CheckedException extends Exception {
        public CheckedException(String message) { super(message); }
    }

    class UncheckedException extends RuntimeException {
        public UncheckedException(String message) { super(message); }
    }

    class ProcessedException extends RuntimeException {
        public ProcessedException(String message, Throwable cause) { super(message, cause); }
    }
}
