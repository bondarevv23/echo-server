package com.github.bondarevv23;

import java.io.IOException;
import java.net.Socket;

public final class IOClientException extends SocketException {
    public IOClientException(Socket socket, IOException cause) {
        super(socket, cause);
    }
}
