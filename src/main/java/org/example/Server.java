package org.example;

import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http3.api.Session;
import org.eclipse.jetty.http3.api.Stream;
import org.eclipse.jetty.http3.frames.HeadersFrame;
import org.eclipse.jetty.http3.server.HTTP3ServerConnector;
import org.eclipse.jetty.http3.server.RawHTTP3ServerConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

import static java.lang.System.Logger.Level.INFO;

public class Server {
    public static void main(String[] args) throws Exception {
        // Create a Server instance.
        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server();

        // HTTP/3 is always secure, so it always need a SslContextFactory.
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath("src\\main\\resources\\server.jks");
        sslContextFactory.setKeyStorePassword("storepass");

        // The listener for session events.
        Session.Server.Listener sessionListener = new Session.Server.Listener() {
            @Override
            public void onAccept(Session session) {
                SocketAddress remoteAddress = session.getRemoteSocketAddress();
                System.out.println("Somebody's connected!!");
                System.getLogger("http3").log(INFO, "Connection from {0}", remoteAddress);
            }

            @Override
            public Stream.Server.Listener onRequest(Stream.Server stream, HeadersFrame frame) {
                MetaData.Request request = (MetaData.Request) frame.getMetaData();

                // Demand to be called back when data is available.
                stream.demand();

                // Return a Stream.Server.Listener to handle the request content.
                return new Stream.Server.Listener() {
                    @Override
                    public void onDataAvailable(Stream.Server stream) {
                        // Read a chunk of the request content.
                        Stream.Data data = stream.readData();

                        if (data == null) {
                            // No data available now, demand to be called back.
                            stream.demand();
                        } else {
                            // Get the content buffer.
                            ByteBuffer buffer = data.getByteBuffer();

                            // Consume the buffer, here - as an example - just log it.
                            System.getLogger("http3").log(INFO, "Consuming buffer {0}", buffer);
                            // Tell the implementation that the buffer has been consumed.
                            data.complete();
                            if (!data.isLast()) {
                                // Demand to be called back.
                                stream.demand();
                            }
                        }
                    }
                };
            }

        };

        // Create and configure the RawHTTP3ServerConnectionFactory.
        RawHTTP3ServerConnectionFactory http3 = new RawHTTP3ServerConnectionFactory(sessionListener);
        http3.getHTTP3Configuration().setStreamIdleTimeout(15000);

        // Create and configure the HTTP3ServerConnector.
        HTTP3ServerConnector connector = new HTTP3ServerConnector(server, sslContextFactory, http3);
        connector.setHost("localhost");
        connector.setPort(8080);

        // Configure the max number of requests per QUIC connection.
        connector.getQuicConfiguration().setMaxBidirectionalRemoteStreams(1024);

        // Add the Connector to the Server.
        server.addConnector(connector);

        server.start();
    }
}
