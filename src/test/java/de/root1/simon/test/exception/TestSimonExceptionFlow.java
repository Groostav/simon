/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.root1.simon.test.exception;

import de.root1.simon.InterfaceLookup;
import de.root1.simon.Lookup;
import de.root1.simon.Registry;
import de.root1.simon.Simon;
import de.root1.simon.exceptions.EstablishConnectionFailed;
import de.root1.simon.exceptions.LookupFailedException;
import de.root1.simon.exceptions.NameBindingException;
import de.root1.simon.test.PortNumberGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Geoff Groos
 */
public class TestSimonExceptionFlow {
    
    private final Logger logger = LoggerFactory.getLogger(TestSimonExceptionFlow.class);
//    private int PORT = 0;
    private int PORT = Simon.DEFAULT_PORT;


    private Lookup lookup;
    private ExceptionalRemoteObject eroiRemote;
    private Registry registry;

    public TestSimonExceptionFlow () {
    }

    //@BeforeClass
    //public static void setUpClass() throws Exception {
    //}

    //@AfterClass
    //public static void tearDownClass() throws Exception {
    //}

    @Before
    public void setUp() throws IOException, NameBindingException, EstablishConnectionFailed, LookupFailedException {
        logger.info("Begin interfaceLookupAndReleaseTwice...");
        ExceptionalRemoteObjectImpl roi = new ExceptionalRemoteObjectImpl();

        registry = Simon.createRegistry(PORT);
        registry.start();
        registry.bind("eroi", roi);

        lookup = Simon.createInterfaceLookup("127.0.0.1", PORT);
        eroiRemote = (ExceptionalRemoteObject) lookup.lookup(ExceptionalRemoteObject.class.getCanonicalName());

        logger.info("eroi registration done");

    }

    @After
    public void tearDown() {
        if(lookup != null ) {
            lookup.release(eroiRemote);
            logger.info("Awaiting network connections shutdown");
            ((InterfaceLookup) lookup).awaitCompleteShutdown(30000);
            logger.info("Awaiting network connections shutdown *done*");
            lookup = null;
        }

        if(registry != null) {
            registry.unbind("eroi");
            registry.stop();
            registry = null;
        }

        eroiRemote = null;
    }

    @Test public void callingRemoteThrowingCheckedException(){
        Exception result = null;
        try { eroiRemote.helloCheckedExceptions(); }
        catch (ExceptionalRemoteObject.CheckedException ex) { result = ex; }

        if (result == null)
            throw new RuntimeException("eroiRemove.helloCheckedExceptions was expected to throw, but it did not");
    }

    @Test public void callingRemoteThrowingUncheckedException(){
        Exception result = null;
        try { eroiRemote.helloCheckedExceptions(); }
        catch (ExceptionalRemoteObject.CheckedException ex) { result = ex; }

        if (result == null)
            throw new RuntimeException("eroiRemove.helloCheckedExceptions was expected to throw, but it did not");
    }

    @Test public void callingRemoteReturningException(){
        @SuppressWarnings("ThrowableNotThrown") Exception result
                = eroiRemote.helloExceptionProcessor(new RuntimeException("hello from client-side!"));

        assert result instanceof ExceptionalRemoteObject.ProcessedException;
        assert result.getCause() instanceof RuntimeException;
        assert result.getCause().getMessage().equals("hello from client-side!");
    }

}