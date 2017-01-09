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
                        readerArr.clear();
                    }
                    //new read request

                    break;
                case 2:
                    if(nextByte!='\0') {
                        readerArr.add(nextByte);
                    }
                    else {
                        String filename = new String(byteListToArray(readerArr), StandardCharsets.UTF_8);
                        readerArr.clear();
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
                    }
                    else { //end of file
                        // crate packet
                        readerArr.clear();
                    }

                    break;
                case 4:
                    if(counter<4) {
                        shortArray[counter-2] = nextByte;
                        if(counter==3) {
                            packetSize = bytesToShort(shortArray);
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

                        readerArr.clear();
                    }
                    break;
                case 6:
                    //execute dirc
                    break;
                case 7:
                    if(nextByte!='\0') {
                        readerArr.add(nextByte);
                    }
                    else { //if finished
                        String Username = new String(byteListToArray(readerArr), StandardCharsets.UTF_8);
                        ///log user
                        readerArr.clear();
                    }
                    break;
                case 8:
                    if(nextByte!='\0') {
                        readerArr.add(nextByte);
                    }
                    else {
                        String filename = new String(byteListToArray(readerArr), StandardCharsets.UTF_8);
                        readerArr.clear();
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

                        readerArr.clear();
                    }

                    break;
                case 10:
                    //disconnect
                    break;
                default:
                    //TODO ERROR
                    break;
            }
        }

        counter++;
        return null;
    }

    @Override
    public byte[] encode(Message message) {
        return new byte[0];
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
