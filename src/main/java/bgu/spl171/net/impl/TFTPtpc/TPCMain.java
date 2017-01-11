package bgu.spl171.net.impl.TFTPtpc;

import bgu.spl171.net.api.bidi.BidiMessagingProtocol;
import bgu.spl171.net.impl.TFTProtocol.TFTPMessageProtocol;
import bgu.spl171.net.impl.TFTProtocol.TFTPencdec;
import bgu.spl171.net.srv.Server;

/**
 * Created by ron on 09/01/17.
 */
public class TPCMain {
    public static void main(String[] args) {
        Server.threadPerClient(Integer.parseInt(args[0]),
                ()-> new TFTPMessageProtocol(),
                ()-> new TFTPencdec()).
                serve();
    }
}
