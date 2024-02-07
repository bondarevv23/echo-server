package com.github.bondarevv23;

import java.net.Socket;

public final class CanNotCloseSocketException extends SocketException {
    public CanNotCloseSocketException(Socket socket, Exception cause) {
        super(socket, cause);
    }
}
