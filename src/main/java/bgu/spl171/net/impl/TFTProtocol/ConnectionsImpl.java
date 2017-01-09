package bgu.spl171.net.impl.TFTProtocol;

import bgu.spl171.net.api.bidi.Connections;

/**
 * Created by ron on 09/01/17.
 */
public class ConnectionsImpl<T> implements Connections<T> {

    @Override
    public boolean send(int connectionId, T msg) {
        return false;
    }

    @Override
    public void broadcast(T msg) {

    }

    @Override
    public void disconnect(int connectionId) {

    }
}
