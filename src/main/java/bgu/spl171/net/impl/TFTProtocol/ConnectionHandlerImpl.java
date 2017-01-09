package bgu.spl171.net.impl.TFTProtocol;

import bgu.spl171.net.srv.bidi.ConnectionHandler;

import java.io.IOException;

/**
 * Created by ron on 09/01/17.
 */
public class ConnectionHandlerImpl<T> implements ConnectionHandler<T> {
    @Override
    public void send(T msg) {

    }

    @Override
    public void close() throws IOException {

    }
}
