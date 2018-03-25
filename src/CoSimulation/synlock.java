package CoSimulation;

import static CoSimulation.drv_ComAgent.DRV_AGENT_CMD_POS;
import static CoSimulation.drv_ComAgent.DRV_AGENT_LOCK_POS;
import static CoSimulation.drv_ComAgent.DRV_AGENT_PAR1_POS;
import static CoSimulation.drv_sstp.DRV_SSTP_RSP_OK;
import jade.lang.acl.ACLMessage;

public class synlock {
    int cmdstat,cnt,now;
    ACLMessage reply;
    short DesID;
    int trigger;
    
    synlock(){
        cnt = 0;
        now = 0;
        cmdstat = 0;
        trigger = 0;
    }
    
    void setLock(int sta,int num){
        cmdstat = sta;
        cnt = num;
        now = 0;
        for(int i=0;i<4;i++)
            cmdbuf[i] = 0;
    }     
    
    void addLock(){
        trigger += 1;
    }  
        
    void setLock(int sta,int num, short id, ACLMessage re){
        cmdstat = sta;
        cnt = num;
        now = 0;
        DesID = id;
        reply = (ACLMessage) re.clone();
        for(int i=0;i<4;i++)
            cmdbuf[i] = 0;        
    }
    
    public ACLMessage getMSG(){
        return reply;
    }
        
    public short getID(){
        return DesID;
    }
            
    int openLock(int sta){
        int res = 0;
        
        if(cmdstat != 0) {//if((sta == cmdstat) && (cmdstat != 0)) {
            now++;
            if(trigger>0){
                trigger -= 1;
                finAction();
            }else
            {
                if(now>=cnt){
                    finAction();
                    cmdstat = 0;
                    res = 1;
                }
            }
        }
        return res;
    }
        
    void finAction()
    {
    }
    
    int getStat(){
        return cmdstat;
    }
    
    int[] cmdbuf = new int[4];
    
    void setRcmd(int[] cmd)
    {
        cmdbuf[0] = cmd[0];
        cmdbuf[1] = cmd[1];
        if(cmdbuf[2] < cmd[2])
            cmdbuf[2] = cmd[2];
        if(cmdbuf[3] < cmd[3])
            cmdbuf[3] = cmd[3];      
    }
}
