package bgu.spl171.net.impl.TFTProtocol;

import bgu.spl171.net.api.bidi.BidiMessagingProtocol;
import bgu.spl171.net.api.bidi.Connections;

import java.io.File;

/**
 * Created by ron on 09/01/17.
 */
public class TFTPMessageProtocol implements BidiMessagingProtocol<Message> {
    File files = new File("Files");
    @Override
    public void start(int connectionId, Connections<Message> connections) {

    }

    @Override
    public void process(Message message) {
        short opCode = message.opcode;
        switch (opCode) {
            case 1:
                Message.ReadMessage m = (Message.ReadMessage) message;
                break;
        }
    }

    @Override
    public boolean shouldTerminate() {
        return false;
    }
}
