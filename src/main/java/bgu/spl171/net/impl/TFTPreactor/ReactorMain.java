package bgu.spl171.net.impl.TFTPreactor;

import bgu.spl171.net.impl.echo.EchoProtocol;
import bgu.spl171.net.impl.echo.LineMessageEncoderDecoder;
import bgu.spl171.net.srv.Server;

/**
 * Created by ron on 09/01/17.
 */
public class ReactorMain {
    public static void main(String[] args) {
        Server.reactor(5,7777, EchoProtocol::new, LineMessageEncoderDecoder::new).serve();
    }
}
