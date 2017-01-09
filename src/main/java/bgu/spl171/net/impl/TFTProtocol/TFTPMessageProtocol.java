package bgu.spl171.net.impl.TFTProtocol;

import bgu.spl171.net.api.bidi.BidiMessagingProtocol;
import bgu.spl171.net.api.bidi.Connections;

/**
 * Created by ron on 09/01/17.
 */
public class TFTPMessageProtocol<T> implements BidiMessagingProtocol<T> {
    @Override
    public void start(int connectionId, Connections<T> connections) {

    }

    @Override
    public void process(T message) {

    }

    @Override
    public boolean shouldTerminate() {
        return false;
    }
}
