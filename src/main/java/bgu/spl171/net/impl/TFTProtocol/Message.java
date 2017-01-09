package bgu.spl171.net.impl.TFTProtocol;

/**
 * Created by ron on 09/01/17.
 */
public class Message {
    short opcode;

    private Message(short opcode) {
        this.opcode = opcode;
    }

    public class ReadMessage extends Message {
        String filename;
        private ReadMessage(short opcode, String filename) {
            super(opcode);
            this.filename = filename;
        }
    }

    public class WriteMessage extends Message {
        String filename;
        private WriteMessage(short opcode, String filename) {
            super(opcode);
            this.filename = filename;
        }
    }

    public class DataMessage extends Message {
        short size;
        short block;
        byte[] bArr;
        private DataMessage(short opcode, short size, short block, byte[] bArr) {
            super(opcode);
            this.size = size;
            this.block = block;
            this.bArr = bArr;
        }
    }

    public class AckMessage extends Message {
        short block;
        private AckMessage(short opcode, short block) {
            super(opcode);
            this.block = block;
        }
    }

    public class ErrMessage extends Message {
        short errCode;
        String msg;
        private ErrMessage(short opcode, short errCode, String msg) {
            super(opcode);
            this.errCode = errCode;
            this.msg = msg;
        }
    }

    public class DirqMessage extends Message {
        private DirqMessage(short opcode) {
            super(opcode);
        }
    }

    public class LoginMessage extends Message {
        String username;
        private LoginMessage(short opcode, String username) {
            super(opcode);
            this.username = username;
        }
    }

    public class DeleteMessage extends Message {
        String filename;
        private DeleteMessage(short opcode, String filename) {
            super(opcode);
            this.filename = filename;
        }
    }

    public class BcastMessage extends Message {
        String filename;
        boolean added;
        private BcastMessage(short opcode, boolean added, String filename) {
            super(opcode);
            this.filename = filename;
            this.added = added;
        }
    }

    public class DisconnectMessage extends Message {
        private DisconnectMessage(short opcode) {
            super(opcode);
        }
    }
}
