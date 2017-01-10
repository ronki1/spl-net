package bgu.spl171.net.impl.TFTProtocol;

import bgu.spl171.net.api.bidi.BidiMessagingProtocol;
import bgu.spl171.net.api.bidi.Connections;
import bgu.spl171.net.impl.ConnectionsImpl;
import bgu.spl171.net.srv.bidi.ConnectionHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by ron on 09/01/17.
 */
public class TFTPMessageProtocol implements BidiMessagingProtocol<Message> {
    int connectionId;
    ConnectionsImpl connections;
    File files = new File("Files");
    private enum state {Waiting, SendingFile, ReceivingFile};
    state currentState;
    byte[] readBuffer, writeBuffer;
    short bytesSent, bytesRemaining, blockNum;
    boolean sendZeroBits;
    @Override
    public void start(int connectionId, Connections<Message> connections) {
        this.connectionId = connectionId;
        this.connections = (ConnectionsImpl) connections;
    }

    @Override
    public void process(Message message) {
        short opCode = message.opcode;
        switch (opCode) {
            case 1:
                Message.ReadMessage m = (Message.ReadMessage) message;
                File f = new File("Files/"+m.filename);
                if(f.exists()) {
                    try {
                        writeBuffer =Files.readAllBytes(f.toPath());
                        currentState = state.SendingFile;
                        bytesSent=0; bytesRemaining =(short) writeBuffer.length;
                        blockNum = 1;
                        if(bytesRemaining%512 == 0 && bytesRemaining>0) sendZeroBits=true;
                        else sendZeroBits = false;
                        if(sendNextPacket());
                        ////TODO send file
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    ///TODO send error
                }
                break;
            case 2:
                Message.WriteMessage wm = (Message.WriteMessage) message;
                File f1 = new File("Files/"+wm.filename);
                if(f1.exists()) {
                    //TODO SEMD ERROR
                }
                else {
                    //TODO create file
                }
                break;
            case 3:
                Message.DataMessage dm = (Message.DataMessage) message;

                break;
        }
    }

    @Override
    public boolean shouldTerminate() {
        return false;
    }

    /**
     *
     * @return if there were packets left. ie if a transmission happend
     */
    public boolean sendNextPacket() {
        boolean ret = false;
        if(bytesRemaining <= 512 && bytesRemaining!=0) {
            connections.send(this.connectionId,new Message.DataMessage(bytesRemaining,blockNum,Arrays.copyOfRange(writeBuffer,bytesSent,bytesSent+bytesRemaining)));
            bytesRemaining = 0;
            bytesSent += bytesRemaining;
            ret = true;
        }
        else if(bytesRemaining > 512) {
            connections.send(this.connectionId,new Message.DataMessage((short)512,blockNum,Arrays.copyOfRange(writeBuffer,bytesSent,bytesSent+512)));
            bytesRemaining-=512; bytesSent+=512;
            ret = true;
        }
        else if(bytesRemaining==0 && sendZeroBits) {
            connections.send(this.connectionId,new Message.DataMessage((short)0,blockNum,new byte[0]));
            sendZeroBits=false;
            ret = true;
        }
        blockNum++;
        return ret;
    }

    public boolean ackReceived(Message.AckMessage m) {
        switch (currentState) {
            case SendingFile:
                if (!sendNextPacket()) {
                    currentState = state.Waiting;
                }
                break;
        }

        return  true;
    }
}
