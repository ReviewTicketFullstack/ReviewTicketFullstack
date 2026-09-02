package com.reviewticket.sdk.imageverify.testing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * 추론 서버 흉내를 내는 최소 HTTP 스텁.
 *
 * <p>WireMock 같은 라이브러리를 쓰지 않는다. java-core 는 테스트까지 포함해
 * 외부 의존성이 0개여야 하고(SPEC 결정 D-2), JDK 에 이미 들어 있는
 * {@link HttpServer} 로 충분하다.
 */
public final class StubInferenceServer implements AutoCloseable {

    private final HttpServer server;
    private final AtomicInteger requestCount = new AtomicInteger();
    private final List<byte[]> receivedBodies = new ArrayList<>();

    private StubInferenceServer(HttpServer server) {
        this.server = server;
    }

    public static StubInferenceServer start(Responder responder) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        StubInferenceServer stub = new StubInferenceServer(server);

        // /embed 와 핸드셰이크(/)도 같은 스텁이 받는다 — 한 서버로 두 백엔드를
        // 모두 상대할 수 있어야 두 경로를 나란히 비교하는 테스트가 쉬워진다.
        server.createContext("/", exchange -> {
            stub.requestCount.incrementAndGet();
            try (InputStream in = exchange.getRequestBody()) {
                byte[] captured = in.readAllBytes();
                synchronized (stub.receivedBodies) {
                    stub.receivedBodies.add(captured);
                }
            }
            responder.respond(exchange);
        });
        server.createContext("/similarity", exchange -> {
            stub.requestCount.incrementAndGet();
            try (InputStream in = exchange.getRequestBody()) {
                synchronized (stub.receivedBodies) {
                    stub.receivedBodies.add(in.readAllBytes());
                }
            }
            responder.respond(exchange);
        });
        // 요청을 동시에 받아야 한다 — 기본(null)은 한 번에 하나씩 처리해
        // 동시성 테스트가 순차 실행처럼 보인다.
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();
        return stub;
    }

    /** 고정 응답. */
    public static StubInferenceServer respondingWith(int status, String body) throws IOException {
        return start(exchange -> send(exchange, status, body));
    }

    /** 응답 전에 지연시킨다. 타임아웃 확인용. */
    public static StubInferenceServer delayedBy(long millis) throws IOException {
        return start(exchange -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            send(exchange, 200, "{\"similarity\": 0.9}");
        });
    }

    public static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }

    public URI similarityUri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/similarity");
    }

    public int requestCount() {
        return requestCount.get();
    }

    public String lastBodyAsText() {
        synchronized (receivedBodies) {
            byte[] last = receivedBodies.get(receivedBodies.size() - 1);
            return new String(last, StandardCharsets.ISO_8859_1);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }

    @FunctionalInterface
    public interface Responder {
        void respond(HttpExchange exchange) throws IOException;
    }
}
