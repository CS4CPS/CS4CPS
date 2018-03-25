package CoSimulation;

import static CoSimulation.drv_ComChannel.CHANNEL_AGENT;
import static CoSimulation.drv_sstp.DRV_SSTP_CMD_CSET;
import static CoSimulation.drv_sstp.DRV_SSTP_CMD_WDATA;
import static CoSimulation.drv_sstp.DRV_SSTP_IDXDL;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

public class drv_ComAgent extends drv_ComChannel {
    
    public static final short DRV_AGENT_CMD_POS  = 0;
    public static final short DRV_AGENT_PAR0_POS  = 1;
    public static final short DRV_AGENT_PAR1_POS  = 2;
    public static final short DRV_AGENT_PAR3_POS  = 3;
    
    public static final short DRV_AGENT_LOCK_POS = 1;
    public static final short DRV_AGENT_RSP_POS  = 3;
    
    public static final short DRV_AGENT_NEXT_TIME_POS = 2;
    public static final short DRV_AGENT_STEP_TIME_POS = 2;

        
    ACLMessage msginfo;
    
    public int init(int setmod, short setaddr) {
        int res = super.init(setmod, setaddr);
        chtype = CHANNEL_AGENT;
        msginfo = new ACLMessage(ACLMessage.INFORM);
        return res;
    } 
    
    public int init(int setmod, short setaddr, ACLMessage msg) {
        int res = super.init(setmod, setaddr);
        chtype = CHANNEL_AGENT;
        msginfo = msg;
        return res;
    }     
    
    public void AddAgentReceiver(AID id) {
        msginfo.addReceiver(id);
    }  
    
    public ACLMessage GetMsg() {  
        return msginfo;
    }
    /*
    public ACLMessage commandSend(int[] cmd) {        
        
        int ressend;
                
        ressend = commandSend((short)(0), cmd, sendbuf);
        if(ressend > 0) {
            byte[] sbuf = new byte[ressend];
            System.arraycopy(sendbuf, 0, sbuf, 0, ressend);
            msginfo.setByteSequenceContent(sbuf);
            
            //String hexString="";
            //for(int i=0;i<ressend;i++)
            //    hexString+= Integer.toHexString(0xff&sendbuf[i])+",";
            //System.out.println("Received: " + hexString);
            
            return msginfo;
        }else
            return null;
    }  *  
    
    public ACLMessage ReplyCmdSend(int[] cmd) {        
        
        int ressend;
                
        ressend = replySend((short)(0), DRV_SSTP_CMD_CSET, 4, cmd, sendbuf);
        if(ressend > 0) {
            byte[] sbuf = new byte[ressend];
            System.arraycopy(sendbuf, 0, sbuf, 0, ressend);
            msginfo.setByteSequenceContent(sbuf);
            
            //String hexString="";
            //for(int i=0;i<ressend;i++)
            //    hexString+= Integer.toHexString(0xff&sendbuf[i])+",";
            //System.out.println("Received: " + hexString);
            
            return msginfo;
        }else
            return null;
    }    
    */
    
    public int commandSend(short Des, int[] cmd) {
        int ressend,res;
        ressend = super.commandSend(Des, cmd, sendbuf);
        res = DataSend(ressend);
        return res;        
    } 
    
    public int RcmdSend(short Des, int[] cmd) {        
        int ressend,res;
        ressend = super.RcmdSend(Des, cmd, sendbuf);
        res = DataSend(ressend);
        return res;
    } 
    
    public int writeSend(short Des, int[] cmd, int[] Data, int len) {        
        int ressend,res;
        int[] tbuf = new int[len+4];
        System.arraycopy(cmd, 0, tbuf, 0, 4);
        System.arraycopy(Data, 0, tbuf, 4, len);
        ressend = reqSend(Des, DRV_SSTP_CMD_CSET, DRV_SSTP_IDXDL+len*4, tbuf, sendbuf);
        res = DataSend(ressend);
        return res;
    } 
        
    public int writeSend(short Des, int[] cmd, byte[] Data, int len) {        
        int ressend,res;
        byte[] tbuf = new byte[DRV_SSTP_IDXDL+len];
        byte[] tmp = new byte[4];
        for(int i=0;i<4;i++)
        {
            tmp = TypeConvert.UIntToByteArray(cmd[i]);
            System.arraycopy(tmp, 0, tbuf, i*4, 4);
        }
        System.arraycopy(Data, 0, tbuf, 16, len);
        ressend = reqSend(Des, DRV_SSTP_CMD_CSET, DRV_SSTP_IDXDL+len, tbuf, sendbuf);
        res = DataSend(ressend);
        return res;
    }
    
    public int DataSend(int ressend) {
        if(ressend > 0) {
            byte[] sbuf = new byte[ressend];
            System.arraycopy(sendbuf, 0, sbuf, 0, ressend);
            msginfo.setByteSequenceContent(sbuf);
            
            //String hexString="";
            //for(int i=0;i<ressend;i++)
            //    hexString+= Integer.toHexString(0xff&sendbuf[i])+",";
            //System.out.println("Received: " + hexString);
        }
        return ressend;            
    }     
}
