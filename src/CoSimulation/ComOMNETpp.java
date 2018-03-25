package CoSimulation;

import static CoSimulation.TimeList.TIMELIST_TYPE_ONT;
import static CoSimulation.drv_ComAgent.*;
import static CoSimulation.drv_sstp.*;
import static CoSimulation.sysconfig.*;
import jade.core.AID;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ComOMNETpp extends ComBase{
    
    int OMNETppEn;
    
    drv_ComStream devCNT;
    Socket socket;
    InputStream in;
    OutputStream out;
    
    int[] cbuf = new int[128];
    
    int TM_GlobalTime,TM_LocalTime,TM_ProcessTime,TM_NextStep,TM_ProcessStep,TM_MiniStep;
    
    drv_ComAgent devGT,devAll;
    drv_ComAgent[] devComCIED;
    int devAll_cnt;
    TimeList devTimeList;
        
    protected void setup(){
        id_self = sysconfig.ID_COMOMNETPP;
        df_service = "COMOMNETPP";
        super.setup();
                
        OMNETppEn = 1;
            
        devCNT = new drv_ComStream();
        devCNT.init(DRV_SSTP_MASTER, id_self);
        
        devAll = new drv_ComAgent();
        devAll.init(DRV_SSTP_MASTER, id_self);
        
        String server = "127.0.0.1";      //Server IP
        int servPort = 6667;                    //port

        devTimeList = new TimeList();
        devTimeList.init(TIMELIST_TYPE_ONT);
        
        addBehaviour( new ComOMNETpp.myMSGReceive() );
    }  
    
    protected void takeDown(){
        try {
            socket.close();
        } catch (Exception e) {
            System.out.println(getLocalName()+"::net falure::"+e.getMessage());
        }
        super.takeDown();
    }

    public class myMSGReceive extends MSGReceive {        
        public int process(drv_sstp sstphead, int[] cmd, byte[] obuf, byte[] sbuf, ACLMessage reply) {
            
            int res = 0;
            switch(sstphead.GetType() & 0xff)
            {
                case DRV_SSTP_CMD_CSET:
                    switch(cmd[DRV_SSTP_CMD_POS])
                    {
                        case SSTP_DEF_CMD_INIT:
                            if(OMNETppEn == 1)
                            {
                                devCNT = new drv_ComStream();
                                String iptmp = TypeConvert.UIntToIPAdress(cmd[1]);
                                try {
                                    devCNT.init(DRV_SSTP_MASTER, id_self,iptmp ,cmd[2]);
                                } catch (IOException ex) {
                                    Logger.getLogger(ComPowerFactory.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                initCNT();
                            }
                            init_AgentCH();
                            res = ReturnCmd(0, 0, DRV_SSTP_RSP_OK);
                            break;
                        case SSTP_DEF_CMD_RST:
                            devTimeList.reset();
                            res = ReturnCmd(0, 0, DRV_SSTP_RSP_OK);
                            break;
                        case SSTP_DEF_CMD_NEXT:
                            devTimeList.TimeLapse();
                            int nexttime;
                            if(OMNETppEn == 1)
                                nexttime = nextCNT();
                            else
                                nexttime = 999999;
                            devTimeList.setLocalTime(nexttime);
                            int nexttmpint = devTimeList.getLocalTime();
                            if(nexttmpint<0)
                                res = ReturnCmd(0, 0, DRV_SSTP_RSP_ERR);
                            else
                                res = ReturnCmd(0, nexttmpint, DRV_SSTP_RSP_OK);
                            break;
                        case SSTP_DEF_CMD_STEP:
                            if(devTimeList.getGlobalTime() != cmd[DRV_AGENT_STEP_TIME_POS])
                            {
                                devTimeList.setGlobalTime(cmd[DRV_AGENT_STEP_TIME_POS]);
                                processCNT();
                            }
                            res = ReturnCmd(0, 0, DRV_SSTP_RSP_OK);                                   
                            break;
                        case SSTP_DEF_CMD_PROCESS:
                            res = CoreFunction(sstphead, cmd, obuf, sbuf, reply); 
                            break;
                        default:
                            res = ReturnCmd(0, 0, DRV_SSTP_RSP_ERR);                            
                    }
                    break;
                case DRV_SSTP_CMD_CSET_R:
                    int cmdtmp[] = {cmd[DRV_AGENT_CMD_POS],cmd[DRV_AGENT_LOCK_POS],cmd[DRV_AGENT_PAR1_POS],DRV_SSTP_RSP_OK};
                    devRTsynlock.setRcmd(cmdtmp);
                    devRTsynlock.openLock(cmd[DRV_AGENT_LOCK_POS]);
                    break;                    
            }
            return res;
        }

        void DFresult(DFAgentDescription[] dfds) {
        }
        
        public int CoreFunction(drv_sstp sstphead, int[] cmd, byte[] obuf, byte[] sbuf, ACLMessage reply){
            int res = 0;
            switch(sstphead.GetSor())
            {
                case ID_GLOBALTIMER:
                    int msgtmp = processCNT();
                    System.out.println(getLocalName()+"\tmsg cnt "+msgtmp+".");
                    if(msgtmp > 0)
                    {
                        devRTsynlock.setLock(cmd[DRV_AGENT_LOCK_POS], msgtmp, sstphead.GetSor(), reply);
                    
                        int[] msginfo = new int[2];
                        for(int i=0;i<msgtmp;i++)
                        {
                            byte[] clbuf = getMSG(i, msginfo);
                            System.out.println(getLocalName()+"\tget msg from "+msginfo[0]+" to "+msginfo[1]+".");
                            //int cmdtmp[] = {cmd[DRV_AGENT_CMD_POS],cmd[DRV_AGENT_LOCK_POS],0,0};
                            drv_ComAgent devTmp = new drv_ComAgent();
                            ACLMessage smsg = devComCIED[msginfo[1]-ID_COMIED].GetMsg();
                            smsg.setByteSequenceContent(clbuf);
                            send(smsg);
                            System.out.println(getLocalName()+"\tProcess::"+(cmd[DRV_AGENT_LOCK_POS])+"::Msg Rece."); 
                        }
                    }else
                    {
                        res = ReturnCmd(cmd[DRV_AGENT_LOCK_POS], 0, DRV_SSTP_RSP_OK);
                        System.out.println(getLocalName()+"\t::Process::"+(cmd[DRV_AGENT_LOCK_POS])+"::No Msg."); 
                    }
                    break;
                default:
                    if(OMNETppEn == 1)
                    {
                        setMSG(sstphead.GetSor(), sstphead.GetDes(), cpbuf, cpbuflen);
                        res = ReturnCmd(cmd[DRV_AGENT_LOCK_POS], 0, DRV_SSTP_RSP_OK);   
                        System.out.println(getLocalName()+"\tProcess::Msg Send::>"+(sstphead.GetSor())+">0.");
                    }else
                    {                      
                        byte[] clbuf = new byte[cpbuflen];
                        System.arraycopy(cpbuf, 0, clbuf, 0, cpbuflen);
                        drv_ComAgent devTmp = new drv_ComAgent();
                        ACLMessage smsg = devComCIED[sstphead.GetDes()-ID_COMIED].GetMsg();
                        smsg.setByteSequenceContent(clbuf);
                        send(smsg);
                        if(devRTsynlock.getStat()==0)
                        {
                            devRTsynlock.setLock(cmd[DRV_AGENT_LOCK_POS], 1, sstphead.GetSor(), reply);
                        }else
                        {
                            devRTsynlock.addLock();
                        }
                        System.out.println(getLocalName()+"\t::Process::Msg Send::>"+(sstphead.GetSor())+">0.");                        
                    }
            }                               
            return res;
        }        
    }   
    
    int lt_sim;
    int gt_sim;
    int msgcnt;
    int[] msgsor;
    int[] msgdes;
    int[] msgdatalen;
    byte[][] msgdata;
    int[] msgtime;
    
    protected void initCNT() {
        
        int res;
        System.out.println(getLocalName()+"\tSearching OMNet++...");
        int cmd[] = {SSTP_DEF_CMD_INIT,0,0,0};
        res = devCNT.commandSend(ID_OMNETPP, cmd);
        switch (res) {
            case drv_sstp.DRV_SSTP_RSP_OK:
                //System.out.println(getLocalName()+"::Init OK.");
                break;
            case drv_sstp.DRV_SSTP_RSP_ERR:
                System.out.println(getLocalName()+"::Init ERROR.");
                break;
            default:
                System.out.println(getLocalName()+"::Init FAILURE.");
                break;
        }
    }
    
    protected int nextCNT() {
        int res;
        System.out.println(getLocalName()+"\tnext...");
        int cmd[] = {SSTP_DEF_CMD_NEXT,0,0,0};
        res = devCNT.commandSend(ID_OMNETPP, cmd);
        switch (res) {
            case drv_sstp.DRV_SSTP_RSP_OK:
                //System.out.println(getLocalName()+"::next OK.");
                break;
            case drv_sstp.DRV_SSTP_RSP_ERR:
                System.out.println(getLocalName()+"::next ERROR.");
                break;
            default:
                System.out.println(getLocalName()+"::next FAILURE.");
                break;
        }    
        res = devCNT.getCMD(2);
        //if(res == 0)
        //    res = 999999;
        return res;
    }
    
    protected int processCNT() {
        int res;
        System.out.println(getLocalName()+"\tprocess OMNet++...");
        int cmd[] = {SSTP_DEF_CMD_PROCESS,0,devTimeList.getGlobalTime(),0};
        res = devCNT.commandSend(ID_OMNETPP, cmd);
        switch (res) {
            case drv_sstp.DRV_SSTP_RSP_OK:
                //System.out.println(getLocalName()+"::process OK.");
                break;
            case drv_sstp.DRV_SSTP_RSP_ERR:
                System.out.println(getLocalName()+"::process ERROR.");
                break;
            default:
                System.out.println(getLocalName()+"::process FAILURE.");
                break;
        }    
        return devCNT.getCMD(2);
    }
    
    protected byte[] getMSG(int i, int[] info) {
        System.out.println(getLocalName()+"\tGet Data...");
        int cmd[] = {0,0,0,0};
        devCNT.readSend(ID_POWERFACTORY,cmd);
        
        info[0] = devCNT.getCMD(0);
        info[1] = devCNT.getCMD(1);
        int datalen = devCNT.getCMD(2);
        byte[] msgdata = new byte[datalen];
        byte[] recedata = devCNT.getRecebuf();
        System.arraycopy(recedata, 16, msgdata, 0, datalen);
        
        return msgdata;   
    }    
       
    protected void setMSG(short sor, short des, byte[] Data, int len) {
        int res;
        System.out.println(getLocalName()+"\tSet Data..."+sor+":"+des+":"+len);
        int cmd[] = {sor,des,len,devTimeList.getGlobalTime()};
        res = devCNT.writeSend(ID_OMNETPP,cmd, Data, len);       
        switch (res) {
            case drv_sstp.DRV_SSTP_RSP_OK:
                //System.out.println(getLocalName()+"::OK.");
                break;
            case drv_sstp.DRV_SSTP_RSP_ERR:
                System.out.println(getLocalName()+"::ERROR.");
                break;
            default:
                System.out.println(getLocalName()+"::FAILURE.");
                break;
        }
    } 
    
    public void init_AgentCH() {
        //for GT
        AID[] gtaid = DFsearch("GLOBALTIMER");
        devGT = new drv_ComAgent();
        devGT.init(DRV_SSTP_MASTER, id_self);
        devGT.AddAgentReceiver(gtaid[0]);

        AID[] iedaid = DFsearch("COMIED");
        devAll_cnt = iedaid.length;
        devComCIED = new drv_ComAgent[devAll_cnt];
        String lnametmp;
        int postmp;
        for(int i=0;i<devAll_cnt;i++)
        {
            lnametmp = iedaid[i].getLocalName().substring(4);
            postmp = Integer.parseInt(lnametmp) - ID_COMIED;
            if(postmp<devAll_cnt)
            {
                devComCIED[postmp] = new drv_ComAgent();
                devComCIED[postmp].init(DRV_SSTP_MASTER, id_self);
                devComCIED[postmp].AddAgentReceiver(iedaid[i]);
                devAll.AddAgentReceiver(iedaid[i]);
            }
        }
    }      
}
