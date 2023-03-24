package org.example;


import org.eclipse.jetty.http.*;
import org.eclipse.jetty.http3.api.Session;
import org.eclipse.jetty.http3.api.Stream;
import org.eclipse.jetty.http3.client.HTTP3Client;
import org.eclipse.jetty.http3.frames.DataFrame;
import org.eclipse.jetty.http3.frames.HeadersFrame;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Client {
    public static void main(String[] args) throws Exception {
        // Instantiate HTTP3Client.
        HTTP3Client http3Client = new HTTP3Client();

// Configure HTTP3Client, for example:
        http3Client.getHTTP3Configuration().setStreamIdleTimeout(15000);



        // Start HTTP3Client.
        http3Client.start();
        // Address of the server's port.
        SocketAddress serverAddress = new InetSocketAddress("localhost", 8080);

// Connect to the server, the CompletableFuture will be
// notified when the connection is succeeded (or failed).
        CompletableFuture<Session.Client> sessionCF = http3Client.connect(serverAddress, new Session.Client.Listener() {
            @Override
            public Map<Long, Long> onPreface(Session session) {
                Map<Long, Long> configuration = new HashMap<>();
                configuration.put(123123L, 123123123L);
                return configuration;
            }
        });

// Block to obtain the Session.
// Alternatively you can use the CompletableFuture APIs to avoid blocking.
        Session.Client session = sessionCF.get();

        HttpFields requestHeaders = HttpFields.build()
                .put(HttpHeader.CONTENT_TYPE, "application/json");

// The request metadata with method, URI and headers.
        MetaData.Request request = new MetaData.Request("POST", HttpURI.from("http://localhost:8444/path"), HttpVersion.HTTP_3, requestHeaders);

// The HTTP/3 HEADERS frame, with endStream=false to
// signal that there will be more frames in this stream.
        HeadersFrame headersFrame = new HeadersFrame(request, false);

// Open a Stream by sending the HEADERS frame.
        CompletableFuture<Stream> streamCF = session.newRequest(headersFrame, new Stream.Client.Listener() {});

// Block to obtain the Stream.
// Alternatively you can use the CompletableFuture APIs to avoid blocking.
        Stream stream = streamCF.get();

// The request content, in two chunks.
        String content1 = "{\"greet\": \"hello world\"}";
        ByteBuffer buffer1 = StandardCharsets.UTF_8.encode(content1);
        String content2 = "{\"user\": \"jetty\"}";
        ByteBuffer buffer2 = StandardCharsets.UTF_8.encode(content2);

// Send the first DATA frame on the stream, with endStream=false
// to signal that there are more frames in this stream.
        CompletableFuture<Stream> dataCF1 = stream.data(new DataFrame(buffer1, false));

// Only when the first chunk has been sent we can send the second,
// with endStream=true to signal that there are no more frames.
        dataCF1.thenCompose(s -> s.data(new DataFrame(buffer2, true)));



    }
}
