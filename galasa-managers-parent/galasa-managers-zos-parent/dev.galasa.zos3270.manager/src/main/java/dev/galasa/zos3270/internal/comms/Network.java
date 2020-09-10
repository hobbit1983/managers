/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.zos3270.internal.comms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dev.galasa.zos3270.spi.NetworkException;

public class Network {

    private final Log           logger          = LogFactory.getLog(getClass());

    private final String        host;
    private final int           port;
    private boolean             ssl;

    private Socket              socket;
    private OutputStream        outputStream;
    private InputStream         inputStream;

    private KeepAlive           keepAlive;
    private Instant             lastSend        = Instant.now();
    
    private Exception           errorException;

    public Network(String host, int port) {
        this(host, port, false);
    }

    public Network(String host, int port, boolean ssl) {
        this.host = host;
        this.port = port;
        this.ssl = ssl;
    }

    public boolean connectClient() throws NetworkException {
        if (socket != null) {
            if (socket.isConnected()) {
                return true;
            }

            close();
        }

        Socket newSocket = null;
        try {
            newSocket = createSocket();
            newSocket.setTcpNoDelay(true);
            newSocket.setKeepAlive(true);

            this.socket = newSocket;
            this.inputStream = this.socket.getInputStream();
            this.outputStream = this.socket.getOutputStream();
            newSocket = null;

            this.keepAlive = new KeepAlive();
            this.keepAlive.start();

            return true;
        } catch (Exception e) {
            throw new NetworkException("Unable to connect to Telnet server", e);
        } finally {
            if (newSocket != null) {
                try {
                    newSocket.close();
                } catch (IOException e) {
                    logger.error("Failed to close the socket", e);
                }
            }
        }
    }

    public boolean isConnected() {
        return (this.socket != null);
    }

    public Socket createSocket() throws IOException, NoSuchAlgorithmException, KeyManagementException {
        Socket newSocket = null;
        if (!ssl) {
            newSocket = new Socket(this.host, this.port);
        } else {
            boolean ibmJdk = System.getProperty("java.vendor").contains("IBM");
            SSLContext sslContext;
            if (ibmJdk) {
                sslContext = SSLContext.getInstance("SSL_TLSv2");
            } else {
                sslContext = SSLContext.getInstance("TLSv1.2");
            }
            sslContext.init(null, new TrustManager[] { new TrustAllCerts() }, new java.security.SecureRandom());
            newSocket = sslContext.getSocketFactory().createSocket(this.host, this.port);
            ((SSLSocket) newSocket).startHandshake();
        }
        newSocket.setTcpNoDelay(true);
        newSocket.setKeepAlive(true);

        return newSocket;
    }

    public void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("Failed to close the socket", e);
            }
            socket = null;
            inputStream = null;
            outputStream = null;

            this.keepAlive.shutdown = true;
            this.keepAlive.interrupt();
        }
    }

    public InputStream getInputStream() {
        return this.inputStream;
    }


    public Socket startTls() throws NetworkException {
        try {
            boolean ibmJdk = System.getProperty("java.vendor").contains("IBM");
            SSLContext sslContext;
            if (ibmJdk) {
                sslContext = SSLContext.getInstance("SSL_TLSv2");
            } else {
                sslContext = SSLContext.getInstance("TLSv1.2");
            }
            sslContext.init(null, new TrustManager[] { new TrustAllCerts() }, new java.security.SecureRandom());
            Socket tlsSocket = sslContext.getSocketFactory().createSocket(socket, this.host, this.port, false);
            ((SSLSocket) tlsSocket).startHandshake();
            tlsSocket.setTcpNoDelay(true);
            tlsSocket.setKeepAlive(true);

            this.socket = tlsSocket;
            this.inputStream = tlsSocket.getInputStream(); 
            this.outputStream = tlsSocket.getOutputStream();
            
            this.ssl = true;
            return tlsSocket;
        } catch(Exception e) {
            throw new NetworkException("Problem negotiating TLS on plain socket", e);
        }

    }


//    public static void expect(InputStream inputStream, byte... expected) throws IOException, NetworkException {
//        byte[] received = new byte[expected.length];
//
//        int length = inputStream.read(received);
//
//        if (length != expected.length) {
//            throw new NetworkException("Expected " + expected.length + " but received only " + length + " bytes");
//        }
//
//        if (!Arrays.equals(expected, received)) {
//            String expectedString = Hex.encodeHexString(expected);
//            String receivedString = Hex.encodeHexString(received);
//            throw new NetworkException("Expected " + expectedString + " but received " + receivedString);
//        }
//    }

    public void sendDatastream(byte[] outboundDatastream) throws NetworkException {
        if (this.errorException != null) {
            throw new NetworkException("Terminal network connection has gone into error state",this.errorException);
        }
        
        sendDatastream(outputStream, outboundDatastream);
    }

    public void sendDatastream(OutputStream outputStream, byte[] outboundDatastream) throws NetworkException {
        synchronized(outputStream) {
            try {
                byte[] header = new byte[] { 0, 0, 0, 0, 0 };
                byte[] trailer = new byte[] { (byte) 0xff, (byte) 0xef };

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                baos.write(header);
                baos.write(outboundDatastream);
                baos.write(trailer);
                outputStream.write(baos.toByteArray());
                outputStream.flush();

                this.lastSend = Instant.now();
            } catch (IOException e) {
                throw new NetworkException("Unable to write outbound datastream", e);
            }
        }
    }
    
    public void sendIac(byte[] outboundIac) throws NetworkException {
        synchronized(outputStream) {
            try {
                outputStream.write(outboundIac);
                outputStream.flush();

                this.lastSend = Instant.now();
            } catch (IOException e) {
                throw new NetworkException("Unable to write outbound iac", e);
            }
        }
    }
   
    public boolean isTls() {
        return this.ssl;
    }

    private void sendKeepAlive() {
        if (this.outputStream == null) {
            return;
        }

        if (this.lastSend.plus(10, ChronoUnit.MINUTES).isAfter(Instant.now())) {
            return;
        }

        synchronized(this.outputStream) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                baos.write(NetworkThread.IAC);
                baos.write(NetworkThread.DO);
                baos.write(NetworkThread.TIMING_MARK);
                outputStream.write(baos.toByteArray());
                outputStream.flush();
                this.lastSend = Instant.now();
            } catch(Exception e) {
                logger.error("Failed to write DO TIMING MARK",e);
            }
        }
    }

    public String getHostPort() {
        return this.host + ":" + Integer.toString(this.port);
    }

    private static class TrustAllCerts implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // TODO Add functionality for the Certificate management
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // TODO Add functionality for the Certificate management
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

    }

    private class KeepAlive extends Thread {

        private boolean shutdown = false;

        public KeepAlive() {
            setName("3270 keep alive");
        }

        @Override
        public void run() {
            while(!shutdown) {
                sendKeepAlive();

                try {
                    Thread.sleep(5000);
                } catch(Exception e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
