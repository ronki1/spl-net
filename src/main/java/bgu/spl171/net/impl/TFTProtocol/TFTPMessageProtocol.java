package bgu.spl171.net.impl.TFTProtocol;

import bgu.spl171.net.api.bidi.BidiMessagingProtocol;
import bgu.spl171.net.api.bidi.Connections;
import bgu.spl171.net.srv.bidi.ConnectionHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ron on 09/01/17.
 */
public class TFTPMessageProtocol implements BidiMessagingProtocol<Message> {
    int connectionId;
    Connections<Message> connections;
    File files = new File("Files");
    private enum state {Waiting, SendingFile, ReceivingFile};
    state currentState;
    byte[] writeBuffer;
    ArrayList<Byte> readBuffer = new ArrayList<Byte>();
    int bytesSent, bytesReceived, bytesRemaining;
    short blockNum;
    boolean sendZeroBits;
    private static HashMap<Integer,String> names = new HashMap<>();
    private static ArrayList<String> filesAwaiting = new ArrayList<>();
    File writtenFile;
    @Override
    public void start(int connectionId, Connections<Message> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
    }

    @Override
    public void process(Message message) {
        if(!checkIfUserLogged(connectionId) && message.opcode != 7) { //if user not logged
            connections.send(connectionId,new Message.ErrMessage((short) 6,"User not logged in – Any opcode received before Login completes."));
            return;
        }
        short opCode = message.opcode;
        switch (opCode) {
            case -1:
                connections.send(connectionId,new Message.ErrMessage((short)4,"Illegal TFTP operation – Unknown Opcode."));
                break;
            case 1:
                Message.ReadMessage m = (Message.ReadMessage) message;
                File f = new File("Files/"+m.filename);
                if(f.exists()) {
                    try {
                        writeBuffer =Files.readAllBytes(f.toPath());
                        currentState = state.SendingFile;
                        bytesSent=0; bytesRemaining = writeBuffer.length;
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
                if(f1.exists() || filesAwaiting.contains(wm.filename)) {
                    connections.send(connectionId,new Message.ErrMessage((short) 5,"File already exists – File name exists on WRQ."));
                }
                else {
                    writtenFile = f1;
                    filesAwaiting.add(wm.filename);
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
                        filesAwaiting.remove(writtenFile.getName());
                        connections.send(connectionId,new Message.AckMessage(dm.block));
                        broadcast(new Message.BcastMessage(true,writtenFile.getName()));
                        writtenFile = null;
                        bytesReceived=0;
                        currentState = state.Waiting;
                    }
                    catch (IOException e) {

                    }
                }
                else {
                    connections.send(connectionId,new Message.AckMessage(dm.block));
                }
                break;
            case 4:
                ackReceived((Message.AckMessage) message);
                break;
            case 5:
                System.out.println(((Message.ErrMessage) message).msg);
                zeroAll();
                break;
            case 6:
                currentState = state.SendingFile;
                File dir = new File("Files/");
                File[] directoryListing = dir.listFiles();
                String sendStr = "";
                if (directoryListing != null) {
                    if(directoryListing.length != 0) {
                        for (File child : directoryListing) {
                            sendStr += child.getName() + '\0';
                        }
                    }
                } else {
                    // Handle the case where dir is not really a directory.
                    // Checking dir.isDirectory() above would not be sufficient
                    // to avoid race conditions with another process that deletes
                    // directories.
                }
                if(!sendStr.equals("")) {
                    writeBuffer = sendStr.getBytes(StandardCharsets.UTF_8);
                    currentState = state.SendingFile;
                    bytesSent = 0;
                    bytesRemaining = (short) writeBuffer.length;
                    blockNum = 1;
                    if (bytesRemaining % 512 == 0 && bytesRemaining > 0) sendZeroBits = true;
                    else sendZeroBits = false;
                    sendNextPacket();
                } else {
                    blockNum=0; bytesRemaining=0; sendZeroBits=true;
                    writeBuffer = new byte[0];
                    bytesSent = 0;
                    bytesRemaining = (short) writeBuffer.length;
                    sendNextPacket();
                }
                break;
            case 7:
                Message.LoginMessage lgm = (Message.LoginMessage) message;
                System.out.println("LOGRQ "+lgm.username);
                /*if(connections.checkIfNameExists(lgm.username)) {
                    connections.send(connectionId,new Message.ErrMessage((short) 7,"User already logged in – Login username already connected."));
                    break;
                }*/
                if(setName(connectionId,lgm.username)) {
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
                            connections.send(connectionId,new Message.AckMessage((short) 0));
                            broadcast(new Message.BcastMessage(false,delm.filename));
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
                System.out.println("DISC");
                Message.DisconnectMessage dscm = (Message.DisconnectMessage) message;
                if(checkIfConnectionIdExists(connectionId)) {
                    connections.send(connectionId,new Message.AckMessage((short) 0));
                    removeUsername(connectionId);
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
            connections.send(this.connectionId,new Message.DataMessage((short) bytesRemaining,blockNum,Arrays.copyOfRange(writeBuffer,bytesSent,bytesSent+bytesRemaining)));
            bytesRemaining = 0;
            bytesSent += bytesRemaining;
            ret = true;
        }
        else if(bytesRemaining > 512) {
            connections.send(this.connectionId,new Message.DataMessage((short)512,blockNum,Arrays.copyOfRange(writeBuffer,bytesSent,bytesSent+512)));
            bytesRemaining-=512; bytesSent+=512;
            ret = true;
        }
        blockNum++;
        if(bytesRemaining==0 && sendZeroBits) {
            connections.send(this.connectionId,new Message.DataMessage((short)0,blockNum,new byte[0]));
            sendZeroBits=false;
            ret = true;
            blockNum++;
        }
        return ret;
    }

    /**
     * receives a packet
     * @return true if currentPacket was the last Packet.
     */
    public boolean receiveNextPacket(Message.DataMessage dm) {
        boolean ret = false;
        if(currentState == state.ReceivingFile) {
            if(dm.size>512 || dm.size<0) {
                connections.send(connectionId,new Message.ErrMessage((short) 0,"Size Wrong"));
                return false;
            }
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
        if(m.block!=0 && m.block != blockNum-1) {
            connections.send(connectionId,new Message.ErrMessage((short) 0, "WTF ACK"));
            zeroAll();
            return false;
        }
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

    public void killConn(int connid) {
        killUser(connid);
    }
    public boolean checkIfNameExists(String name) {
        return  names.containsValue(name);
    }

    public boolean checkIfConnectionIdExists(int connId) {
        return names.containsKey(connId);
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
        removeUsername(connId);
    }

    public void broadcast(Message msg) {
        for (Map.Entry<Integer,String> entry : names.entrySet()) {
            if(checkIfUserLogged(entry.getKey())) connections.send(entry.getKey(),msg);
        }
    }

    public void zeroAll() {
        currentState=state.Waiting;
        readBuffer.clear();
        bytesSent=0; bytesReceived=0; bytesRemaining=0;
        blockNum=0;
        sendZeroBits=false;
    }

}
