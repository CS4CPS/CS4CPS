package CoSimulation;

import static CoSimulation.TimeList.TIMELIST_TYPE_IED;
import static CoSimulation.drv_ComAgent.*;
import static CoSimulation.drv_sstp.*;
import static CoSimulation.sysconfig.*;
import MultiAgentSystem.ModelIED;
import MultiAgentSystem.ModelServer;
import MultiAgentSystem.ModelNode;
import MultiAgentSystem.ModelIEDdata;
import MultiAgentSystem.Nodeinfo;
import jade.core.AID;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class ComIED extends ComBase {
    
    Nodeinfo dev_nodeinfo;
    ModelIEDdata[] devIEDdata;
    int devIEDdatalen;
    
    drv_ComAgent devPF,devOT,devGT;
    drv_ComAgent[] devIED;
    drv_ComAgent devAll;
    
    int[] cbuf = new int[128];
    
    ModelIED dev_ied;
    
    TimeList devTimeList;
    
    public static final int COMIED_EVENT_AD  = 1;
    public static final int COMIED_EVENT_GETMSG  = 2;
    
    protected void setup(){
        df_service = "COMIED";
        super.setup();
        
        dev_nodeinfo = new Nodeinfo();        
        switch(devConfig.devIED[id_self-ID_COMIED].asType)
        {
            case 0:
                dev_ied = new ModelNode();
                break;
            case 1:
                dev_ied = new ModelServer();
                break;
        }

        dev_ied.init(id_self, devConfig, dev_nodeinfo);
        
        devAllCIED = new drv_ComAgent();
        devAllCIED.init(DRV_SSTP_MASTER, id_self);
        devCIEDZA = new drv_ComAgent();
        devCIEDZA.init(DRV_SSTP_MASTER, id_self);
        
        //ServiceDescription[] sd  = new ServiceDescription[2];
        //sd[0] = new ServiceDescription();
        //sd[0].setType(df_service);
        //sd[0].setName(getLocalName());
        //sd[1] = new ServiceDescription();
        //sd[1].setType(dev_nodeinfo.getZName());
        //sd[1].setName(getLocalName());
        //DFregisterMult(sd,2);
        
        devTimeList = new TimeList();
        devTimeList.init(TIMELIST_TYPE_IED);
        devTimeList.init(0, devConfig.devIED[id_self - ID_COMIED].asDelayProcess,devConfig.devIED[id_self - ID_COMIED].asDelaySampling, 0);
        
        devIEDdata = new ModelIEDdata[64];
        for(int i=0;i<64;i++)
            devIEDdata[i] = new ModelIEDdata();
        
        addBehaviour( new ComIED.myMSGReceive() );
    }

    protected void takeDown(){
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
                        case SSTP_DEF_CMD_RST:
                            init_AgentCH();
                            devTimeList.reset();
                            res = ReturnCmd(0, 0, DRV_SSTP_RSP_OK);                         
                            break;
                        case SSTP_DEF_CMD_NEXT:
                            devTimeList.TimeLapse();
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
            int ctrlsta = 0;
                                
            switch(sstphead.GetSor())
            {
                case ID_GLOBALTIMER:                   
                    int sendtarget = 0;
                    TimeList.Tmsg msgtmp = devTimeList.getCurrentTmsg();
                    while(msgtmp != null)
                    {
                        send(msgtmp.msg);
                        sendtarget++;
                        msgtmp = devTimeList.getCurrentTmsg();
                    }
                    if(sendtarget > 0)
                    {
                        devRTsynlock.setLock(cmd[DRV_AGENT_LOCK_POS], sendtarget, sstphead.GetSor(), reply); 
                        System.out.println(getLocalName()+"\t::Process::SetLock<"+sendtarget+">.");
                    }
                    break;
                case ID_COMPOWERFFACTORY:
                    System.out.println(getLocalName()+"\tProcess::"+(cmd[DRV_AGENT_LOCK_POS])+"::AD event.");
                    res = IEDFunction(COMIED_EVENT_AD, sstphead, cmd, obuf, sbuf, reply);
                    break;
                default:
                    System.out.println(getLocalName()+"\t::Process::"+(cmd[DRV_AGENT_LOCK_POS])+"::getMsg event.");
                    res = IEDFunction(COMIED_EVENT_GETMSG, sstphead, cmd, obuf, sbuf, reply);                    
            }                               
            return res;
        }  
        
      public int IEDFunction(int intr, drv_sstp sstphead, int[] cmd, byte[] obuf, byte[] sbuf, ACLMessage reply){
            int res = 0;  
            int ctrlsta = 0;
                                
            //AgentsMan();
            dev_nodeinfo.setGTime(devTimeList.getGlobalTime());
            if(intr == COMIED_EVENT_AD)
            {
                devIEDdatalen = dev_ied.intr_ADSample(dev_nodeinfo, sstphead, cmd, obuf, devIEDdata);
                devTimeList.actSamplingDelay();
            }else
                devIEDdatalen = dev_ied.intr_GetMSG(dev_nodeinfo, sstphead, cmd, obuf, devIEDdata);
            
            for(int i=0;i<devIEDdatalen;i++)
            {
                int cmdtmp[] = {SSTP_DEF_CMD_PROCESS,cmd[DRV_AGENT_LOCK_POS],devTimeList.guessNextTime(),0};
                drv_ComAgent devTmp = new drv_ComAgent();
                devTmp.init(DRV_SSTP_MASTER, id_self, findIED(devIEDdata[i].getDes()));
                devTmp.writeSend(devIEDdata[i].getDes(),cmdtmp,devIEDdata[i].getData(),devIEDdata[i].getDatalen());                     
                devTimeList.setSendTmsg(devTmp.GetMsg());
                System.out.println(getLocalName()+"\t::Process::"+(cmd[DRV_AGENT_LOCK_POS])+"::Add Msg.");
            }
            if(ctrlsta != 1)
            {
                res = ReturnCmd(cmd[DRV_AGENT_LOCK_POS], 0, DRV_SSTP_RSP_OK);
                System.out.println(getLocalName()+"\t::Process::finIntrrupt::>"+(sstphead.GetSor())+">"+(ctrlsta)+".");
            }
            return res;
        }        
    }
    
    private void init_AgentCH() {
        //for PF
        AID[] pfaid = DFsearch("COMPOWERFFACTORY");
        devPF = new drv_ComAgent();
        devPF.init(DRV_SSTP_MASTER, id_self);
        devPF.AddAgentReceiver(pfaid[0]);

        //for OT
        AID[] otaid = DFsearch("COMOMNETPP");
        devOT = new drv_ComAgent();
        devOT.init(DRV_SSTP_MASTER, id_self);
        devOT.AddAgentReceiver(otaid[0]);

        //for GT
        AID[] gtaid = DFsearch("GLOBALTIMER");
        devGT = new drv_ComAgent();
        devGT.init(DRV_SSTP_MASTER, id_self);
        devGT.AddAgentReceiver(gtaid[0]);
    }      
    
    drv_ComAgent devAllCIED, devCIEDZA;
    drv_ComAgent[] devComCIED;
    int devAllCIED_cnt;
    
    public void AgentsMan() {
        
        String zntmp = dev_nodeinfo.getZName().substring(4);
        dev_nodeinfo.setAZA(Integer.parseInt(zntmp));
        
        AID[] iedaid = DFsearch(dev_nodeinfo.getZName());
        devAllCIED_cnt = iedaid.length;
        devComCIED = new drv_ComAgent[devAllCIED_cnt];
        int[] sortbuf = new int[devAllCIED_cnt];
        int[] sortorder = new int[devAllCIED_cnt];
        
        String lnametmp;
        int postmp;
        for(int i=0;i<devAllCIED_cnt;i++)
        {
            lnametmp = iedaid[i].getLocalName().substring(4);
            postmp = Integer.parseInt(lnametmp);
            sortbuf[i] = postmp;
        }
        SortStoL(sortbuf, sortorder);

        dev_nodeinfo.setALen(devAllCIED_cnt);
        
        int idtmp;
        for(int i=0;i<devAllCIED_cnt;i++)
        {
            dev_nodeinfo.setAgents(i,sortbuf[i]);
            devComCIED[i] = new drv_ComAgent();
            devComCIED[i].init(DRV_SSTP_MASTER, id_self);
            idtmp = sortorder[i];
            devComCIED[i].AddAgentReceiver(iedaid[idtmp]);
            devAllCIED.AddAgentReceiver(iedaid[idtmp]);
            if(dev_nodeinfo.getAZA() == sortbuf[i])
            {
                devCIEDZA.AddAgentReceiver(iedaid[idtmp]);
            }
        }
        dev_nodeinfo.setAgentCnt(devAllCIED_cnt);
    }    
    
    public void SortStoL(int[] inbuf, int[] order)
    {
        int tmp;
        
        for(int i=0;i<inbuf.length;i++)
            order[i] = i;
        
        for(int i=0;i<inbuf.length;i++)
        {
            for(int j=i;j<inbuf.length;j++)
            {
                if(inbuf[j]<inbuf[i])
                {
                    tmp = inbuf[i];
                    inbuf[i] = inbuf[j];
                    order[i] = j;
                    inbuf[j] = tmp;
                    order[j] = i;
                }
            }
        }
    }
    
    public ACLMessage findIED(int num)
    {
        if(num < 20)
        {
            switch(num)
            {
                case ID_COMPOWERFFACTORY:
                    return devPF.GetMsg();
                case ID_COMOMNETPP:
                    return devOT.GetMsg();
            }
        }else{
            return devOT.GetMsg();
        }
        return null;
    }
}
