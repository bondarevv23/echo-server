package com.github.bondarevv23;

import java.net.Socket;

abstract class SocketException extends RuntimeException {
    private final Socket socket;

    public SocketException(Socket socket, Exception cause) {
        super(cause);
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }
}
