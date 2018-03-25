package CoSimulation;

import static CoSimulation.drv_sstp.*;

abstract class drv_ComChannel {
    
    drv_sstp sendhead;
    short id_self;
    int chtype;
    
    public static final short CHANNEL_AGENT  = 0;
    public static final short CHANNEL_SOCKET  = 1; 
    
    byte[] recebuf = new byte[512];
    byte[] protbuf = new byte[512];
    byte[] sendbuf = new byte[512];
        
    public int init(int setmod, short setaddr) {
        int res;
        sendhead = new drv_sstp();
        res = sendhead.init(setmod, setaddr);
        id_self = setaddr;
        return res;
    } 
    
    public void setaddr(short setaddr) {
        sendhead.setaddr(setaddr);
        id_self = setaddr;
    } 
    
    public int resolve(byte[] buf,int len, byte[] sbuf) {
        return sendhead.resolve(buf, len, sbuf);
    }
    
    public drv_sstp getHead()
    {
        return sendhead;
    }
    
    public int getRSP()
    {
        byte[] rep = new byte[4];
        System.arraycopy(protbuf, 12,rep, 0, 4);
        return TypeConvert.byteArrayToUInt(rep); 
    }
        
    public int getCMD(int i)
    {
        byte[] rep = new byte[4];
        System.arraycopy(protbuf, i*4,rep, 0, 4);
        return TypeConvert.byteArrayToUInt(rep); 
    }
    
    public int reqSend(short Des, short type, int len, byte[] inbuf, byte[] outbuf) {
        sendhead.SetSor(id_self);
        sendhead.SetDes(Des);
        sendhead.SetType((short)(type));
        sendhead.SetLen((short)len);
        return sendhead.fill(inbuf, outbuf);
    }    
    
    public int reqSend(short Des, short type, int len, int[] inbuf, byte[] outbuf) {
        sendhead.SetSor(id_self);
        sendhead.SetDes(Des);
        sendhead.SetType((short)(type));
        sendhead.SetLen((short)len);
        return sendhead.fill(inbuf, outbuf);
    } 
    
    public int replySend(short Des, short type, int len, byte[] inbuf, byte[] outbuf) {
        sendhead.SetSor(id_self);
        sendhead.SetDes(Des);
        sendhead.SetType((short)(type | 0x80));
        sendhead.SetLen((short)len);
        return sendhead.fill(inbuf, outbuf);
    }    
    
    public int replySend(short Des, short type, int len, int[] inbuf, byte[] outbuf) {
        sendhead.SetSor(id_self);
        sendhead.SetDes(Des);
        sendhead.SetType((short)(type | 0x80));
        sendhead.SetLen((short)len);
        return sendhead.fill(inbuf, outbuf);
    }
    
    public int commandSend(short Des, int[] cmd, byte[] buf) {        
        int ressend;
        ressend = reqSend(Des, DRV_SSTP_CMD_CSET, DRV_SSTP_IDXDL, cmd, buf);
        return ressend;
    } 
    
    public int RcmdSend(short Des, int[] cmd, byte[] buf) {        
        int ressend;
        ressend = replySend(Des, DRV_SSTP_CMD_CSET_R, DRV_SSTP_IDXDL, cmd, buf);
        return ressend;
    } 
        
    public int readSend(short Des, int[] cmd) {        
        int ressend,res;
        ressend = reqSend(Des, DRV_SSTP_CMD_RDATA, DRV_SSTP_IDXDL, cmd, sendbuf);
        res = DataSend(ressend);
        return res;
    }    
    
    public int writeSend(short Des, int[] cmd, int[] Data, int len) {        
        int ressend,res;
        int[] tbuf = new int[len+4];
        System.arraycopy(cmd, 0, tbuf, 0, 4);
        System.arraycopy(Data, 0, tbuf, 4, len);
        ressend = reqSend(Des, DRV_SSTP_CMD_WDATA, DRV_SSTP_IDXDL, tbuf, sendbuf);
        res = DataSend(ressend);
        return res;
    } 
    
    public int writeSend(short Des, int[] cmd, byte[] Data, int len) {        
        int ressend,res;
        byte[] tbuf = new byte[len+16];
        byte[] tmp = new byte[4];
        for(int i=0;i<4;i++)
        {
            tmp = TypeConvert.UIntToByteArray(cmd[i]);
            System.arraycopy(tmp, 0, tbuf, i*4, 4);
        }
        System.arraycopy(Data, 0, tbuf, 16, len);
        ressend = reqSend(Des, DRV_SSTP_CMD_WDATA, DRV_SSTP_IDXDL+len, tbuf, sendbuf);
        res = DataSend(ressend);
        return res;
    } 
        
    public int reply(byte[] inbuf, int len, byte[] outbuf) {
        return replySend(sendhead.GetSor(), sendhead.GetType(), len, inbuf, outbuf);      
    } 
        
    abstract int DataSend(int ressend);
}
