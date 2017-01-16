package bgu.spl171.net.impl.TFTProtocol;

import bgu.spl171.net.api.MessageEncoderDecoder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Created by ron on 09/01/17.
 */
public class TFTPencdec implements MessageEncoderDecoder<Message> {
    int counter=0;
    byte[] shortArray = new byte[2];
    short opCode;
    ArrayList<Byte> readerArr = new ArrayList<>();
    short packetSize,blockNum;
    short errorCode;
    boolean written;

    @Override
    public Message decodeNextByte(byte nextByte) {
        if(counter<2) {
            shortArray[counter]=nextByte;
            if(counter==1) {
                opCode = bytesToShort(shortArray);
                if(opCode == 6) {//DIRQ
                    //execute dirc
                    Message m = new Message.DirqMessage();
                    counter=0;
                    return m;
                }
                else if(opCode==10) {
                    //disconnect
                    Message m2 = new Message.DisconnectMessage();
                    counter=0;
                    return m2;
                }
            }
        }
        else {
            switch (opCode) {
                case 1:
                    if(nextByte!='\0') {
                        readerArr.add(nextByte);
                    }
                    else {
                        String filename = new String(byteListToArray(readerArr), StandardCharsets.UTF_8);
                        Message m = new Message.ReadMessage(filename);
                        readerArr.clear();
                        counter=0;
                        return m;
                    }
                    //new read request

                    break;
                case 2:
                    if(nextByte!='\0') {
                        readerArr.add(nextByte);
                    }
                    else {
                        String filename = new String(byteListToArray(readerArr), StandardCharsets.UTF_8);
                        Message m = new Message.WriteMessage(filename);
                        readerArr.clear();
                        counter=0;
                        return m;
                    }
                    //new write request
                    break;
                case 3:
                    if(counter<4) {
                        shortArray[counter-2] = nextByte;
                        if(counter==3) {
                            packetSize = bytesToShort(shortArray);
                        }
                    }
                    else if (counter<6) {
                        shortArray[counter-4] = nextByte;
                        if(counter==5) {
                            blockNum = bytesToShort(shortArray);
                        }
                    }
                    else if(counter<6+packetSize) { //if reading data
                        readerArr.add(nextByte);
                        if(counter == 6+packetSize-1) {//end of file
                            Message m = new Message.DataMessage(packetSize,blockNum,byteListToArray(readerArr));
                            readerArr.clear();
                            counter=0;
                            return m;
                        }
                    }
                    else { //end of file
                        // crate packet
                        Message m = new Message.DataMessage(packetSize,blockNum,byteListToArray(readerArr));
                        readerArr.clear();
                        counter=0;
                        return m;
                    }

                    break;
                case 4:
                    if(counter<4) {
                        shortArray[counter-2] = nextByte;
                        if(counter==3) {
                            packetSize = bytesToShort(shortArray);
                            Message m = new Message.AckMessage(packetSize);
                            counter=0;
                            return m;
                        }
                    }

                    //sendACK
                    break;
                case 5:
                    if(counter<4){
                        shortArray[counter-2] = nextByte;
                        if(counter==3) {
                            errorCode = bytesToShort(shortArray);
                        }
                    }
                    else if(nextByte!='\0') {
                        readerArr.add(nextByte);
                    }
                    else { //if finished
                        String ErrMsg = new String(byteListToArray(readerArr), StandardCharsets.UTF_8);
                        //add error
                        Message m = new Message.ErrMessage(errorCode,ErrMsg);
                        readerArr.clear();
                        counter=0;
                        return m;
                    }
                    break;
                case 6:

                case 7:
                    if(nextByte!='\0') {
                        readerArr.add(nextByte);
                    }
                    else { //if finished
                        String username = new String(byteListToArray(readerArr), StandardCharsets.UTF_8);
                        ///log user
                        Message m1 = new Message.LoginMessage(username);
                        readerArr.clear();
                        counter=0;
                        return m1;
                    }
                    break;
                case 8:
                    if(nextByte!='\0') {
                        readerArr.add(nextByte);
                    }
                    else {
                        String filename = new String(byteListToArray(readerArr), StandardCharsets.UTF_8);
                        Message m2 = new Message.DeleteMessage(filename);
                        readerArr.clear();
                        counter=0;
                        return m2;
                    }
                    //new delete request
                    break;
                case 9:
                    if(counter==2) {
                        written = nextByte == 1;
                    }
                    else if(counter>2 && nextByte!='\0') {
                        readerArr.add(nextByte);
                    }
                    else {
                        String filename = new String(byteListToArray(readerArr), StandardCharsets.UTF_8);
                        //bcast
                        Message m3 = new Message.BcastMessage(written,filename);
                        readerArr.clear();
                        counter=0;
                        return m3;
                    }

                    break;
                case 10:

                default:
                    counter = 0;
                    return new Message.ErrMessage((short) 4,"Illegal TFTP operation – Unknown Opcode.");
            }
        }

        counter++;
        return null;
    }

    @Override
    public byte[] encode(Message message) {
        byte[] retArr = new byte[2];
        byte[] opcodeArr = shortToBytes(message.opcode);
        switch (message.opcode) {
            case 3: //case data
                Message.DataMessage dm = (Message.DataMessage) message;
                retArr = new byte[6+dm.bArr.length];
                byte[] ps =shortToBytes(dm.size);
                retArr[2] = ps[0];
                retArr[3] = ps[1];
                byte[] blockNum = shortToBytes(dm.block);
                retArr[4] = blockNum[0];
                retArr[5] = blockNum[1];
                for (int j=6; j<retArr.length; j++){
                    retArr[j] = dm.bArr[j-6];
                }
                break;
            case 4: //case ACK
                Message.AckMessage ackm = (Message.AckMessage) message;
                retArr = new byte[4];
                byte[] block = shortToBytes(ackm.block);
                retArr[2] = block[0];
                retArr[3] = block[1];
                break;
            case 5://case error
                Message.ErrMessage errm = (Message.ErrMessage) message;
                byte[] errContent = errm.msg.getBytes();
                retArr = new byte[5+errContent.length];
                byte[] errCode = shortToBytes(errm.errCode);
                retArr[2] = errCode[0];
                retArr[3] = errCode[1];
                for (int j=4; j<retArr.length-1; j++) {
                    retArr[j] = errContent[j-4];
                }
                retArr[retArr.length-1] = '\0';
                break;
            case 6://case DIRQ
                Message.DirqMessage dirqm = (Message.DirqMessage) message;
                break;
            case 9://case BCAST
                Message.BcastMessage bcastm = (Message.BcastMessage) message;
                byte[] bcastFilename = bcastm.filename.getBytes();
                retArr = new byte[4+bcastFilename.length];
                retArr[2] = (byte) (bcastm.added ? 1:0);
                for (int j=3; j<retArr.length-1; j++) {
                    retArr[j] = bcastFilename[j-3];
                }
                retArr[retArr.length-1] = '\0';
                break;
        }

        retArr[0] = opcodeArr[0];
        retArr[1] = opcodeArr[1];
            return retArr;
    }

    public short bytesToShort(byte[] byteArr)
    {
        short result = (short)((byteArr[0] & 0xff) << 8);
        result += (short)(byteArr[1] & 0xff);
        return result;
    }

    public byte[] shortToBytes(short num)
    {
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte)((num >> 8) & 0xFF);
        bytesArr[1] = (byte)(num & 0xFF);
        return bytesArr;
    }

    public byte[] byteListToArray(ArrayList<Byte> a) {
        byte[] btarr = new byte[a.size()];
        for(int j=0; j<btarr.length; j++) {
            btarr[j] = readerArr.get(j);
        }
        return btarr;
    }
}
