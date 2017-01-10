package bgu.spl171.net.impl;

import bgu.spl171.net.api.bidi.Connections;
import bgu.spl171.net.impl.TFTProtocol.Message;
import bgu.spl171.net.srv.bidi.ConnectionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ron on 09/01/17.
 */
public class ConnectionsImpl implements Connections<Message> {

    HashMap<Integer,ConnectionHandler> connections = new HashMap<>();

    @Override
    public boolean send(int connectionId, Message msg) {
        ConnectionHandler<Message> retVal =  connections.get(connectionId);
        if(retVal != null) {
            retVal.send(msg);
            return true;
        }
        return false;
    }

    @Override
    public void broadcast(Message msg) {
        for (Map.Entry<Integer,ConnectionHandler> entry : connections.entrySet()) {
            entry.getValue().send(msg);
        }
    }

    @Override
    public void disconnect(int connectionId) {
        connections.remove(connectionId);
    }

    public boolean addConnection(int id, ConnectionHandler ch) {
        connections.put(id,ch);
        return true;
    }

}
