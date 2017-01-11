package bgu.spl171.net.impl.TFTProtocol;

import bgu.spl171.net.api.bidi.BidiMessagingProtocol;
import bgu.spl171.net.api.bidi.Connections;
import bgu.spl171.net.impl.ConnectionsImpl;
import bgu.spl171.net.srv.bidi.ConnectionHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

import java.nio.charset.StandardCharsets;

/**
 * Created by ron on 09/01/17.
 */
public class TFTPMessageProtocol implements BidiMessagingProtocol<Message> {
    int connectionId;
    ConnectionsImpl connections;
    File files = new File("Files");
    private enum state {Waiting, SendingFile, ReceivingFile};
    state currentState;
    byte[] writeBuffer;
    ArrayList<Byte> readBuffer = new ArrayList<Byte>();
    short bytesSent, bytesReceived, bytesRemaining, blockNum;
    boolean sendZeroBits;
    File writtenFile;
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
                        sendNextPacket();
                        ////TODO send file
                    } catch (IOException e) {
                        bytesRemaining=0; blockNum=0; sendZeroBits=false;
                        connections.send(connectionId,new Message.ErrMessage((short) 2,"Access violation – File cannot be written, read or deleted"));
                    }
                }
                else {
                    connections.send(connectionId,new Message.ErrMessage((short) 1,"File not found – RRQ of non-existing file"));
                }
                break;
            case 2:
                Message.WriteMessage wm = (Message.WriteMessage) message;
                File f1 = new File("Files/"+wm.filename);
                if(f1.exists()) {
                    connections.send(connectionId,new Message.ErrMessage((short) 5,"File already exists – File name exists on WRQ."));
                }
                else {
                    writtenFile = f1;
                    currentState = state.ReceivingFile;
                    bytesReceived = 0;
                    System.out.println("WRQ "+wm.filename);
                    connections.send(connectionId,new Message.AckMessage((short) 0));
                }
                break;
            case 3:
                Message.DataMessage dm = (Message.DataMessage) message;
                if(receiveNextPacket(dm)) {//if was the last packet
                    try {
                        FileOutputStream fos = new FileOutputStream(writtenFile.getAbsolutePath());
                        fos.write(ByteListToArray(readBuffer));
                        fos.close();
                        System.out.println("RRQ "+writtenFile.getName() +" complete");

                        connections.broadcast(new Message.BcastMessage(true,writtenFile.getName()));
                        writtenFile = null;
                        bytesReceived=0;
                        currentState = state.Waiting;
                    }
                    catch (IOException e) {

                    }
                }
                else {
                    connections.send(connectionId,new Message.AckMessage((short) (Math.floor(bytesReceived/512)+1)));
                }
                break;
            case 4:
                ackReceived((Message.AckMessage) message);
                break;
            case 5:

                break;
            case 6:
                currentState = state.SendingFile;
                File dir = new File("Files/");
                File[] directoryListing = dir.listFiles();
                String sendStr = "";
                if (directoryListing != null) {
                    for (File child : directoryListing) {
                        sendStr+=child.getName()+'\n';
                    }
                } else {
                    // Handle the case where dir is not really a directory.
                    // Checking dir.isDirectory() above would not be sufficient
                    // to avoid race conditions with another process that deletes
                    // directories.
                }
                writeBuffer = sendStr.getBytes(StandardCharsets.UTF_8);
                currentState = state.SendingFile;
                bytesSent=0; bytesRemaining =(short) writeBuffer.length;
                blockNum = 1;
                if(bytesRemaining%512 == 0 && bytesRemaining>0) sendZeroBits=true;
                else sendZeroBits = false;
                sendNextPacket();
                break;
            case 7:
                Message.LoginMessage lgm = (Message.LoginMessage) message;
                if(connections.checkIfNameExists(lgm.username)) {
                    connections.send(connectionId,new Message.ErrMessage((short) 7,"User already logged in – Login username already connected."));
                    break;
                }
                if(connections.setName(connectionId,lgm.username)) {
                    connections.send(connectionId,new Message.AckMessage((short) 0));
                }
                else {
                    connections.send(connectionId,new Message.ErrMessage((short) 7,"User already logged in – Login username already connected."));
                }
                break;
            case 8:
                Message.DeleteMessage delm = (Message.DeleteMessage) message;
                File toDelete = new File("Files/"+delm.filename);
                if(toDelete.exists()) {
                    try {
                        if (toDelete.delete()) {
                            connections.broadcast(new Message.BcastMessage(false,delm.filename));
                        } else {
                            connections.send(connectionId, new Message.ErrMessage((short) 1, "File not found – DELRQ of non-existing file"));
                        }
                    } catch (Exception e) {
                        connections.send(connectionId, new Message.ErrMessage((short) 2, "Access violation – File cannot be written, read or deleted."));
                    }
                }
                else { //if file doesn't exist
                    connections.send(connectionId,new Message.ErrMessage((short) 1,"File not found – DELRQ of non-existing file"));
                }
                break;
            case 9:
                break;
            case 10:
                Message.DisconnectMessage dscm = (Message.DisconnectMessage) message;
                if(connections.checkIfConnectionIdExists(connectionId)) {
                    connections.send(connectionId,new Message.AckMessage((short) 0));
                    connections.removeConnection(connectionId);
                }
                break;
        }
    }

    @Override
    public boolean shouldTerminate() {
        return false;
    }

    /**
     *sends a packrt
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

    /**
     * receives a packet
     * @return true if currentPacket was the last Packet.
     */
    public boolean receiveNextPacket(Message.DataMessage dm) {
        boolean ret = false;
        if(currentState == state.ReceivingFile) {
            if(dm.size != 0) {
                addBytesToList(readBuffer,dm.bArr);
            }
            if(dm.size < 512) {
                currentState = state.Waiting;
                ret = true;
            }
        }
        bytesReceived+=dm.size;
        return ret;
    }

    public boolean ackReceived(Message.AckMessage m) {
        System.out.println("ACK" + m.block);
        switch (currentState) {
            case SendingFile:
                if (!sendNextPacket()) {
                    currentState = state.Waiting;
                }
                break;
        }

        return  true;
    }

    public void addBytesToList(ArrayList<Byte> list, byte[] arr) {
        for (byte b:arr) {
            list.add(b);
        }
    }

    public byte[] ByteListToArray(ArrayList<Byte> al) {
        byte[] bArr = new byte[al.size()];
        for(int j=0; j<bArr.length; j++) {
            bArr[j] = al.get(j);
        }
        return bArr;
    }
}
