/*
 * Copyright (C) 2013 Alexander Christian <alex(at)root1.de>. All rights reserved.
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
package de.root1.simon.utils;

import de.root1.simon.SimonProxy;
import de.root1.simon.SimonRemoteMarker;
import de.root1.simon.annotation.SimonRemote;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.*;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.root1.simon.codec.base.SimonProtocolCodecFactory;
import de.root1.simon.exceptions.IllegalRemoteObjectException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import org.xml.sax.SAXException;

/**
 *
 * A class with some static helper-methods
 *
 * @author ACHR
 */
public class Utils {

    private final static Logger logger = LoggerFactory.getLogger(Utils.class);
    /**
     * if this flag is set to TRUE, SIMON tries to load the java.util.logging
     * properties and enabled the debug-mode
     *
     * @deprecated use JVM argument
     * "java.util.logging.config.file=./log/mylogconfig.properties"
     */
    public static boolean DEBUG = false;
    /**
     * A map that memories some method hashes so that they need not to be
     * re-generated each time the hash is used. If memory is getting short, some
     * entries are gc'ed so that more memory is available. There is no need to
     * clear the map ourselves.
     */
    private static final WeakHashMap<Method, Long> methodHashes = new WeakHashMap<Method, Long>();
    
    private static final String SIMON_REMOTE_ANNOTATION_CLASSNAME = de.root1.simon.annotation.SimonRemote.class.getName();

    /**
     * Compute the "method hash" of a remote method. The method hash is a long
     * containing the first 64 bits of the SHA digest from the bytes
     * representing the complete method signature.
     *
     * @param m the method for which the hash has to be computed
     * @return the computed hash
     */
    public static long computeMethodHash(Method m) {

        Long hash = null;
        synchronized (methodHashes) {
            hash = methodHashes.get(m);
        }
        if (hash != null) {

            logger.trace("Got hash from map. map contains {} entries.", methodHashes.size());
            return hash.longValue();

        } else {

            long result = 0;
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream(127);

            try {
                MessageDigest md = MessageDigest.getInstance("SHA");

                DigestOutputStream out = new DigestOutputStream(byteArray, md);

                // use the complete method signature to generate the sha-digest
                out.write(m.toGenericString().getBytes());

                // use only the first 64 bits of the digest for the hash
                out.flush();
                byte hasharray[] = md.digest();
                for (int i = 0; i < Math.min(8, hasharray.length); i++) {
                    result += ((long) (hasharray[i] & 0xFF)) << (i * 8);
                }
            } catch (IOException ignore) {
                // can't really happen
                result = -1;
            } catch (NoSuchAlgorithmException complain) {
                throw new SecurityException(complain.getMessage());
            }

            synchronized (methodHashes) {
                methodHashes.put(m, result);
                logger.trace("computed new hash. map now contains {} entries.", methodHashes.size());
            }

            return result;
        }
    }

    /**
     * Loads a protocol codec factory by a given classname
     *
     * @param protocolFactory a class name like
     * "com.mydomain.myproject.codec.mySimonProtocolCodecFactory" which points
     * to a class, that extends {@link SimonProtocolCodecFactory}. <i>The
     * important thing is, that this class correctly overrides
     * {@link SimonProtocolCodecFactory#setup(boolean)}. For further details,
     * look at {@link SimonProtocolCodecFactory}!</i>
     * @return the protocolcodecfactory instance according to the given protocol
     * factory class name
     * @throws IllegalAccessException if the class or its nullary constructor is
     * not accessible.
     * @throws InstantiationException if this Class represents an abstract
     * class, an interface, an array class, a primitive type, or void; or if the
     * class has no nullary constructor; or if the instantiation fails for some
     * other reason.
     * @throws ClassNotFoundException if the class is not found by the
     * classloader. if so, please check your classpath.
     * @throws ClassCastException if the given class is no instance of
     * {@link SimonProtocolCodecFactory}
     */
    public static SimonProtocolCodecFactory getProtocolFactoryInstance(String protocolFactory)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        Class<?> clazz = Class.forName(protocolFactory);
        try {
            SimonProtocolCodecFactory instance = (SimonProtocolCodecFactory) clazz.newInstance();
            return instance;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    "The given class '"
                    + protocolFactory
                    + "' must extend '" + de.root1.simon.codec.base.SimonProtocolCodecFactory.class.getName() + "' !");
        }
    }

    /**
     * Converts a long value to a hex string, i.e. 0xF923
     *
     * @param l
     * @return return a string showing the hex value of parameter l
     */
    public static String longToHexString(long l) {

        StringBuilder id = new StringBuilder();
        id.append(Long.toHexString(l).toUpperCase());

        while (id.length() < 8) {
            id.insert(0, "0");
        }
        id.insert(0, "0x");

        return id.toString();
    }

    /**
     * Converts a boolean value to a byte value.
     *
     * @param bool
     * @return 0xFF if true, 0x00 if false
     */
    public static byte booleanToByte(boolean bool) {
        return (bool == true ? (byte) 0xFF : (byte) 0x00);
    }

    /**
     * Converts a byte value to a boolean value.
     *
     * @param b
     * @return 0xFF if true, 0x00 if false
     * @throws IllegalArgumentException if byte value not 0xFF or 0x00
     */
    public static boolean byteToBoolean(byte b) throws IllegalArgumentException {
        switch (b) {
            case (byte) 0xFF:
                return true;

            case (byte) 0x00:
                return false;

            default:
                throw new IllegalArgumentException("only 0xFF and 0x00 value allowed for 'byte-to-boolean' conversion!");
        }
    }

    /**
     * Method that returns an Class<?> array containing all remote interfaces of
     * a given class
     *
     * @param clazz the class to analyse for remote interfaces
     * @return the array with all known remote interfaces
     */
    public static Class<?>[] findAllRemoteInterfaces(Class<?> clazz) {

        Set<Class<?>> interfaceSet = doFindAllRemoteInterfaces(clazz);

        Class<?>[] interfaces = new Class[interfaceSet.size()];
        if (logger.isTraceEnabled()) {
            logger.trace("found interfaces: {}", Arrays.toString(interfaceSet.toArray(interfaces)));
        }
        return interfaceSet.toArray(interfaces);
    }

    /**
     * Internal helper method for finding remote interfaces
     *
     * @param clazz the class to analyse for remote interfaces
     * @return a set with remote interfaces
     */
    private static Set<Class<?>> doFindAllRemoteInterfaces(Class<?> clazz) {
        Set<Class<?>> interfaceSet = new HashSet<Class<?>>();

        if (clazz == null) {
            return interfaceSet;
        }

        String type = (clazz.isInterface() ? "interface" : "class");

        // check for annotation in clazz
        SimonRemote annotation = clazz.getAnnotation(SimonRemote.class);
        if (annotation != null) {

            logger.trace("SimonRemote annotation found for {} {}", type, clazz.getName());

            // check for remote interfaces specified in the SimonRemote annotation
            Class[] remoteInterfaces = annotation.value();

            if (remoteInterfaces != null && remoteInterfaces.length > 0) {
                /*
                 * found some specified remote interfaces in the annotation's value field. 
                 * Use them and return
                 */
                logger.trace("SimonRemote annotation has remote interfaces specified. Adding: {}", Arrays.toString(remoteInterfaces));
                for (Class<?> interfaze : remoteInterfaces) {
                    interfaceSet.add((Class<?>) interfaze);
                }

            } else {
                /*
                 * there is no interfaces specified with the annotation's value field. 
                 * Using all visible interfaces from the class as a remote interface.
                 * "All" means: All first-level interfaces, independant from any SimonRemote annotation or extension
                 * If class itself is an interface, just use this
                 */
                if (clazz.isInterface()) {
                    interfaceSet.add(clazz);
                } 
                
                for (Class<?> interfaze : clazz.getInterfaces()) {
                    interfaceSet.add((Class<?>) interfaze);
                }

            }

        } else { // deeper search

            logger.trace("No SimonRemote annotation found for {} {}. Searching for interfaces that extend SimonRemoteMarker or use SimonRemote Annotation.", type, clazz.getName());
            /*
             * There's no initial annotation
             * Need to search for a Interface in any superclass/superinterface that extends SimonRemote
             */

            // go through all interfaces
            for (Class<?> interfaze : clazz.getInterfaces()) {

                // check interfaces for remote
                if (interfaze.isAnnotationPresent(SimonRemote.class)) {
                    // interface is annotated
                    interfaceSet.add((Class<?>) interfaze);
                    
                    // Check for parent interfaces (made with 'interface extends anotherinterface')
                    for(Class<?> parentInterface : interfaze.getInterfaces()) {
                        interfaceSet.addAll(doFindAllRemoteInterfaces(parentInterface));
                    }
                } 
                else {
                    // no remote interface found
                    // checking for super interface
                    interfaceSet.addAll(doFindAllRemoteInterfaces(interfaze.getSuperclass()));
                }

            }

            // check also super classes
            if (clazz.getSuperclass() != null && !clazz.getSuperclass().equals(Object.class)) {
                interfaceSet.addAll(doFindAllRemoteInterfaces(clazz.getSuperclass()));
            }

        }
        return interfaceSet;
    }

    /**
     * Checks whether the object is annotated with
     * <code>SimonRemote</code> or not
     *
     * @return true, if object is annotated, false if not
     * @param annotatedElement
     */
    public static boolean isRemoteAnnotated(AnnotatedElement annotatedElement) {

        boolean isRemoteAnnotated = annotatedElement.isAnnotationPresent(de.root1.simon.annotation.SimonRemote.class);

        // if annotation is not found via current CL, try again with remoteobject's CL (but only if it's not the Bootstrap-CL (which means null)
        // see: http://dev.root1.de/issues/173
        if (!isRemoteAnnotated && annotatedElement instanceof Class) {

            // get CL only once, as getting CL requires a native call --> avoid too many JNI calls
            ClassLoader remoteObjectCL = ((Class) annotatedElement).getClassLoader();

            if (remoteObjectCL!=null) {
                try {
                    isRemoteAnnotated = annotatedElement.isAnnotationPresent(
                            (Class<? extends Annotation>) remoteObjectCL.loadClass(SIMON_REMOTE_ANNOTATION_CLASSNAME));
                } catch (ClassNotFoundException ex) {
                    // simply ignore
                }
            }
        }
        return isRemoteAnnotated;
    }

    /**
     * Returns the value of the
     * <code>SimonRemote</code> annotation.
     *
     * @param remoteObject the object to query
     * @return the annotation value
     * @throws IllegalArgumentException in case of remoteObject==null
     */
    public static Class<?>[] getRemoteAnnotationValue(Object remoteObject) {
        if (remoteObject == null) {
            throw new IllegalArgumentException("Cannot check a null-argument. You have to provide a proxy object instance ...");
        }
        return remoteObject.getClass().getAnnotation(de.root1.simon.annotation.SimonRemote.class).value();
    }

    /**
     * Checks if the given remote object is a valid remote object. Checks for:
     * <ul>
     * <li>SimonRemote annotation</li>
     * <li>SimonRemoteMarker proxy</li>
     * <li>implements SimonRemote</li>
     * </ul>
     *
     * @param remoteObject the object to check
     * @return true, if remote object is valid, false if not
     * @throws IllegalRemoteObjectException thrown in case of a faulty remote
     * object (ie. missing interfaces)
     */
    public static boolean isValidRemote(Object remoteObject) {

        if (remoteObject == null) {
            return false;
        }
        if (isRemoteAnnotated(remoteObject.getClass())) {

            if (remoteObject.getClass().getInterfaces().length > 0 || getRemoteAnnotationValue(remoteObject).length > 0) {
                return true;
            } else {
                throw new IllegalRemoteObjectException("There is no interface with the remote object of type '" + remoteObject.getClass().getName() + "' linked. Add a 'value' parameter with array of interfaces (at least one interface) to the SimonRemote annotation, or let the class implement an interface");
            }
        }
        if (getMarker(remoteObject) != null) {
            return true;
        }
        return false;
    }
    public static boolean isRemoteArgument(Parameter parameter) {
        return parameter.isAnnotationPresent(SimonRemote.class);
    }

    /**
     * Checks if given object is a simon proxy.
     *
     * @param o object to check
     * @return true, if object is a simon proxy, false if not
     */
    public static boolean isSimonProxy(Object o) {
        if (o instanceof Proxy) {
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(o);
            logger.trace("Got invocation handler ...");
            if (invocationHandler instanceof SimonProxy) {
                logger.trace("Yeeha. It's a SimonProxy ...");
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the related instance of {@link SimonRemoteMarker} of the given
     * object. if the specified object isn't marked, null is returned.
     *
     * @param o
     * @return the related instance of {@link SimonRemoteMarker}, or null if
     * given object is not marked
     */
    public static SimonRemoteMarker getMarker(Object o) {
        if (o instanceof Proxy) {
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(o);
            if (invocationHandler instanceof SimonRemoteMarker) {
                return (SimonRemoteMarker) invocationHandler;
            }
        }
        return null;
    }

    /**
     * Small helper method that pushes all interfaces of the specified class to
     * the specified stack
     *
     * @param stack
     * @param clazz
     */
    private static void putInterfacesToStack(Stack<Class> stack, Class clazz) {
        Class[] interfaces = clazz.getInterfaces();
        for (Class iClazz : interfaces) {
            stack.push(iClazz);
        }
    }

    /**
     * Reads all interfaces and subinterfaces of the given object and add the
     * names to the provided interface name list
     *
     * @param object the object to search for interfaces
     * @param interfaceNames the list to which found interfaces names are added
     */
    public static void putAllInterfaceNames(Object object, List<String> interfaceNames) {
        Stack<Class> stack = new Stack<Class>();
        Utils.putInterfacesToStack(stack, object.getClass());
        while (!stack.empty()) {
            Class iClazz = stack.pop();
            String iClazzName = iClazz.getName();
            logger.trace("Adding {} to the list of remote interfaces", iClazzName);
            if (!interfaceNames.contains(iClazzName)) {
                interfaceNames.add(iClazzName);
            }
            Utils.putInterfacesToStack(stack, iClazz);
        }
    }

    /**
     * Returns the stacktrace of the given throwable as a string. String will be
     * the same as "e.printStackTrace();" woulld print to console
     *
     * @param e
     * @return the exceptions stacktrace as a string
     */
    public static String getStackTraceAsString(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Looks up and returns the root cause of an exception. If none is found,
     * returns supplied Throwable object unchanged. If root is found,
     * recursively "unwraps" it, and returns the result to the caller.
     *
     * @param th
     * @return the exceptions root-cause, if available, otherwise th will be
     * returned unchanged
     */
    public static Throwable getRootCause(Throwable th) {
        if (th instanceof SAXException) {
            SAXException sax = (SAXException) th;
            if (sax.getException() != null) {
                return getRootCause(sax.getException());
            }
        } else if (th instanceof SQLException) {
            SQLException sql = (SQLException) th;
            if (sql.getNextException() != null) {
                return getRootCause(sql.getNextException());
            }
        } else if (th.getCause() != null) {
            return getRootCause(th.getCause());
        }

        return th;
    }

    /**
     * Retrieve object hash code and applies a supplemental hash function to the
     * result hash, which defends against poor quality hash functions. This is
     * critical because HashMap uses power-of-two length hash tables, that
     * otherwise encounter collisions for hashCodes that do not differ in lower
     * bits. Note: Null keys always map to hash 0, thus index 0.
     */
    public static final int hash(Object object) {

        if (object == null) {
            return 0;
        }

        int hash = 0;

        hash ^= object.hashCode();

        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        hash ^= (hash >>> 20) ^ (hash >>> 12);
        return hash ^ (hash >>> 7) ^ (hash >>> 4);
    }

    /**
     * Check whether the current VM is a Android DalvikVM or not
     *
     * @return true, is applications runs on Androids DalvikVM, false if not
     */
    private static boolean isDalvikVM() {

        // java.vm.specification.name=Dalvik Virtual Machine Specification
        // AND
        // java.vm.vendor=The Android Project

        if (System.getProperty("java.vm.specification.name", "").equals("Dalvik Virtual Machine Specification")
                && System.getProperty("java.vm.vendor", "").equals("The Android Project")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * see MBeanServer#registerMBean(Object, ObjectName) This is a workaround to
     * be able to run the code also on android, where the MBeanServer is not
     * available
     *
     * @return true, if registration succeeds, false if not
     */
    public static boolean registerMBean(Object o, String objectNameOfMBean) {

        if (isDalvikVM()) {
            logger.info("Running on Android. Skipping registration on MBeanServer for [{}]", objectNameOfMBean);
            return false;
        }

        try {
            // ManagementFactory#getPlatformMBeanServer()
            String cnManagementFactory = "java.lang.management.ManagementFactory";
            String mnGetPlatformMBeanServer = "getPlatformMBeanServer";
            Class<?> cManagementFactory = Class.forName(cnManagementFactory);
            Method mGetPlatformMBeanServer = cManagementFactory.getDeclaredMethod(mnGetPlatformMBeanServer);
            Object oMBeanServer = mGetPlatformMBeanServer.invoke(null);

            // create ObjectName object
            String cnObjectName = "javax.management.ObjectName";
            Class<?> cObjectName = Class.forName(cnObjectName);
            Constructor<?> constructor = cObjectName.getConstructor(new Class<?>[]{String.class});
            Object oObjectName = constructor.newInstance(objectNameOfMBean);

            // MBeanServer#registerMBean(this, ObjectName)
            String cnMBeanServer = "javax.management.MBeanServer";
            String mnRegisterMBean = "registerMBean";
            Class<?> cMBeanServer = Class.forName(cnMBeanServer);
            Method mRegisterMBean = cMBeanServer.getMethod(mnRegisterMBean, new Class<?>[]{Object.class, cObjectName});
            mRegisterMBean.invoke(oMBeanServer, new Object[]{o, oObjectName});
            return true;
        } catch (Throwable t) {
            logger.warn("Cannot register [" + objectNameOfMBean + "] on MBeanServer.", t);
            return false;
        }
    }


}
