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

    private HashMap<Integer,ConnectionHandler> connections = new HashMap<>();
    private HashMap<Integer,String> names = new HashMap<>();

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
            if(checkIfUserLogged(entry.getKey())) entry.getValue().send(msg);
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

    public boolean checkIfNameExists(String name) {
        return  names.containsValue(name);
    }

    public boolean checkIfConnectionIdExists(int connId) {
        return connections.containsKey(connId);
    }

    /**
     *
     * @param
     * @param name
     * @return if succeded returns true
     */
    public boolean setName(int connectionId,String name) {
        if(!checkIfNameExists(name)) {
            if(names.containsKey(connectionId)) {
                return false;
            }
            else {
                names.put(connectionId,name);
                return true;
            }
        }
        return false;
    }

    /**
     * removes a connectio
     * @param connectionId
     * @return true if succeded, false if not
     */
    public boolean removeUsername(int connectionId) {
        //boolean a1 = connections.remove(connectionId) != null;
        boolean a2 = names.remove(connectionId) != null;
        return /*a1||*/a2;
    }

    public boolean checkIfUserLogged(int connectionId) {
        return names.containsKey(connectionId);
    }

    public void killUser(int connId) {
        disconnect(connId);
        removeUsername(connId);
    }

}
