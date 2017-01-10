package bgu.spl171.net.impl.TFTProtocol;

/**
 * Created by ron on 09/01/17.
 */
public class Message {
    short opcode;

    private Message(short opcode) {
        this.opcode = opcode;
    }

    public static class ReadMessage extends Message {
        String filename;
        public ReadMessage(String filename) {
            super((short) 1);
            this.filename = filename;
        }
    }

    public static class WriteMessage extends Message {
        String filename;
        public WriteMessage(String filename) {
            super((short) 2);
            this.filename = filename;
        }
    }

    public static class DataMessage extends Message {
        short size;
        short block;
        byte[] bArr;
        public DataMessage(short size, short block, byte[] bArr) {
            super((short) 3);
            this.size = size;
            this.block = block;
            this.bArr = bArr;
        }
    }

    public static class AckMessage extends Message {
        short block;
        public AckMessage(short block) {
            super((short) 4);
            this.block = block;
        }
    }

    public static class ErrMessage extends Message {
        short errCode;
        String msg;
        public ErrMessage(short errCode, String msg) {
            super((short) 5);
            this.errCode = errCode;
            this.msg = msg;
        }
    }

    public static class DirqMessage extends Message {
        public DirqMessage() {
            super((short) 6);
        }
    }

    public static class LoginMessage extends Message {
        String username;
        public LoginMessage(String username) {
            super((short) 7);
            this.username = username;
        }
    }

    public static class DeleteMessage extends Message {
        String filename;
        public DeleteMessage(String filename) {
            super((short) 8);
            this.filename = filename;
        }
    }

    public static class BcastMessage extends Message {
        String filename;
        boolean added;
        public BcastMessage(boolean added, String filename) {
            super((short) 9);
            this.filename = filename;
            this.added = added;
        }
    }

    public static class DisconnectMessage extends Message {
        public DisconnectMessage() {
            super((short) 10);
        }
    }
}
