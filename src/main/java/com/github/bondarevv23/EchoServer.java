package com.github.bondarevv23;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;

public class EchoServer implements AutoCloseable {
    private static final int DEFAULT_PORT = 7;
    private static final int DEFAULT_TIMEOUT = 500;

    final Thread listener;
    final ExecutorService executor = Executors.newCachedThreadPool();
    private Exception listenerException = null;
    private final Queue<CanNotCloseSocketException> cantCloseSocketExceptions = new ConcurrentLinkedQueue<>();
    private final Queue<IOClientException> IOClientExceptions = new ConcurrentLinkedQueue<>();

    public EchoServer() {
        this(DEFAULT_PORT);
    }

    public EchoServer(int port) {
        this(port, DEFAULT_TIMEOUT);
    }

    public EchoServer(final int port, final int listenerSocketTimeout) {
        this.listener = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                serverSocket.setSoTimeout(listenerSocketTimeout);
                while (!Thread.interrupted()) {
                    try {
                        Socket acceptedSocket = serverSocket.accept();
                        addNewClientToPool(acceptedSocket);
                    } catch (SocketTimeoutException ignore) {

                    }
                }
                Thread.currentThread().interrupt();
            } catch (Exception exception) {
                listenerException = exception;
            }
        });
    }

    private void addNewClientToPool(final Socket client) {
        executor.execute(() -> {
            try {
                try (PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
                    while (!Thread.interrupted()) {
                        out.println(in.readLine());
                    }
                    Thread.currentThread().interrupt();
                } catch (IOException exception) {
                    IOClientExceptions.add(new IOClientException(client, exception));
                }
            } finally {
                try {
                    client.close();
                } catch (IOException exception) {
                    cantCloseSocketExceptions.add(new CanNotCloseSocketException(client, exception));
                }
            }
        });
    }

    public void start() {
        listener.start();
    }

    @Override
    public void close() {
        listener.interrupt();
        executor.shutdownNow();
    }

    public List<CanNotCloseSocketException> getCanNotCloseSocketExceptions() {
        return new ArrayList<>(cantCloseSocketExceptions);
    }

    public List<IOClientException> getIOClientExceptions() {
        return new ArrayList<>(IOClientExceptions);
    }

    public Exception getListenerException() {
        return listenerException;
    }

    private static String getRandomString() {
        return ThreadLocalRandom.current().ints('a', 'z' + 1)
                .limit(10)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public static void main(String[] args) throws Exception {
        int clientsCount = 3;
        EchoServer server = new EchoServer();
        server.start();
        List<Thread> clients = IntStream.range(0, clientsCount).mapToObj(i -> (Runnable) () -> {
            try (Socket socket = new Socket("127.0.0.1", DEFAULT_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                while (!Thread.currentThread().isInterrupted()) {
                    String randomString = getRandomString();
                    out.println(randomString);
                    System.out.println(
                            "Thread " + (i + 1) +
                                    "   request: \"" + randomString + "\" response: \"" + in.readLine() + "\""
                    );
                    SECONDS.sleep(1);
                }
            } catch (IOException | InterruptedException ignored) {

            }
        }).map(Thread::new).toList();
        clients.forEach(Thread::start);
        SECONDS.sleep(10);
        clients.forEach(Thread::interrupt);
        server.close();
    }
}
