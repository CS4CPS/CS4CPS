package CoSimulation;

import static CoSimulation.TimeList.TIMELIST_TYPE_CPF;
import static CoSimulation.TypeConvert.ByteArrayToFloat;
import static CoSimulation.TypeConvert.IntToByteArray;
import static CoSimulation.drv_ComAgent.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jade.domain.FIPAAgentManagement.*;

import static CoSimulation.drv_sstp.*;
import static CoSimulation.sysconfig.*;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

public class ComPowerFactory extends ComBase{
    
    drv_ComStream devPF;
    drv_ComAgent devGT,devAllCIED;
    drv_ComAgent[] devComCIED;
    int devAllCIED_cnt;
    TimeList devTimeList;
    synlock dev_synlock;
    int adpage;
    
    public static final int CPF_STEP_GETIEDS  = 1;
    
    //ModelConfig devConfig;
    
    byte[] rtdbbuf;
    
    protected void setup(){
        id_self = sysconfig.ID_COMPOWERFFACTORY;
        df_service = "COMPOWERFFACTORY";
        super.setup();
        
        devAllCIED = new drv_ComAgent();
        devAllCIED.init(DRV_SSTP_MASTER, id_self);
        
        //devConfig = new ModelConfig();
        //devConfig.configPF("D:/ModelConfig.xml");
        
        devTimeList = new TimeList();
        devTimeList.init(devConfig.SamplingPeriod, 0, 0, 0);
        
        dev_synlock = new synlock();
        
        rtdbbuf = new byte[1024*16];
        
        adpage = 0;
        
        addBehaviour( new ComPowerFactory.myMSGReceive() );
    }

    protected void takeDown(){
        try {
            devPF.fin();
        } catch (IOException ex) {
            Logger.getLogger(ComPowerFactory.class.getName()).log(Level.SEVERE, null, ex);
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
                            devPF = new drv_ComStream();
                            String iptmp = TypeConvert.UIntToIPAdress(cmd[1]);
                            try {
                                devPF.init(DRV_SSTP_MASTER, id_self,iptmp ,cmd[2]);
                            } catch (IOException ex) {
                                Logger.getLogger(ComPowerFactory.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            initPF();
                            init_AgentCH();
                            res = ReturnCmd(0, 0, DRV_SSTP_RSP_OK);
                            break;
                        case SSTP_DEF_CMD_RST:
                            devTimeList.reset();
                            resetPF(); 
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
                            //    resetPF();
                                stepPF(cmd[DRV_AGENT_STEP_TIME_POS]);
                            }
                            res = ReturnCmd(0, 0, DRV_SSTP_RSP_OK);                             
                            break;
                        case SSTP_DEF_CMD_PROCESS:
                            res = CoreFunction(sstphead, cmd, obuf, sbuf, reply); 
                            break;
                        case SSTP_DEF_CMD_CTRL:
                            //for(int i=0;i<(devAllCIED_cnt);i++) {
                            //    byte[] procbuf = new byte[devConfig.devIED[i].Ccount*4];
                            //    System.arraycopy(rtdbbuf, devConfig.devIED[i].Cbase,procbuf, 0, devConfig.devIED[i].Ccount*4);
                            //    setDataPF(devConfig.devIED[i].Cbase, devConfig.devIED[i].Ccount*4, procbuf);
                                System.out.println(getLocalName()+"::Process::"+(cmd[DRV_AGENT_LOCK_POS])+"::CTRL.");
                            //}                            
                            res = ReturnCmd(cmd[DRV_AGENT_LOCK_POS], 0, DRV_SSTP_RSP_OK);  
                            break;
                        default:
                            res = ReturnCmd(0, 0, DRV_SSTP_RSP_ERR);                           
                    }
                    break;        
                case DRV_SSTP_CMD_CSET_R:
                    devRTsynlock.openLock(cmd[DRV_AGENT_LOCK_POS]);
                    break;
            }
            return res;
        }

        public void DFresult(DFAgentDescription[] dfds) {
        }
        
        public int CoreFunction(drv_sstp sstphead, int[] cmd, byte[] obuf, byte[] sbuf, ACLMessage reply){
            int res = 0;
            switch(sstphead.GetSor())
            {
                case ID_GLOBALTIMER:     
                    //devTimeList.setGlobalTime(cmd[DRV_AGENT_STEP_TIME_POS]);
                    //resetPF();
                    //stepPF(cmd[DRV_AGENT_STEP_TIME_POS]);
                    int sendtarget = 0;
                    TimeList.Tmsg msgtmp = devTimeList.getCurrentTmsg();
                    while(msgtmp != null)
                    {
                        if(msgtmp.msg == null)//event for sampling
                        {
                            int lockagents = devAllCIED_cnt;
                            for(int i=0;i<(devAllCIED_cnt);i++) {
                                if(devConfig.devIED[i].Mcount != 0)
                                {
                                    byte[] procbuf = getDataPF(devConfig.devIED[i].Mbase*4, devConfig.devIED[i].Mcount*4);
                                    
                                    byte[] tmpb = IntToByteArray(adpage);
                                    System.arraycopy(tmpb, 0, procbuf, 16, 4);
                                    System.arraycopy(procbuf, 0, rtdbbuf, devConfig.devIED[i].Mbase, devConfig.devIED[i].Mcount*4);
                                    int proccmd[] = {SSTP_DEF_CMD_PROCESS,cmd[DRV_AGENT_LOCK_POS],0,0};
                            
                                    devComCIED[i].writeSend((short)(ID_COMIED+i),proccmd,procbuf,procbuf.length);
                                    send(devComCIED[i].GetMsg());
                                    System.out.println(getLocalName()+"\tProcess::"+(cmd[DRV_AGENT_LOCK_POS]+i)+"::Send AD Data to "+(ID_COMIED+i)+" at "+adpage+".");
                                }else
                                {
                                    lockagents = lockagents - 1;
                                }
                            }
                            adpage += 1;
                            sendtarget = sendtarget + lockagents;
                        }else
                        {
                            send(msgtmp.msg);
                            sendtarget++;
                        }
                        msgtmp = devTimeList.getCurrentTmsg();
                    }
                    if(sendtarget > 0)
                    {
                        devRTsynlock.setLock(cmd[DRV_AGENT_LOCK_POS], sendtarget, sstphead.GetSor(), reply);
                        System.out.println(getLocalName()+"\t::SetLock<"+sendtarget+">.");
                    }
                    break;
                default:
                    short addrtmp = (short) (sstphead.GetSor() - ID_COMIED);
                    byte[] procbuf = new byte[devConfig.devIED[addrtmp].Ccount*4];
                    System.arraycopy(obuf, 16, procbuf, 0, devConfig.devIED[addrtmp].Ccount*4);
                    setDataPF(devConfig.devIED[addrtmp].Cbase*4, devConfig.devIED[addrtmp].Ccount*4, procbuf);
                    res = ReturnCmd(cmd[DRV_AGENT_LOCK_POS], 0, DRV_SSTP_RSP_OK);   
                    System.out.println(getLocalName()+"\t::Process::UpData::>"+(sstphead.GetSor())+">1.");
                    System.out.println("\t\tSwitchChanged <"+devTimeList.getLocalTime()+">.");
            }                               
            return res;
        }
    }
            
    protected void initPF() {
        int res;
        System.out.println(getLocalName()+"\t::Searching PowerFactory...");
        int cmd[] = {SSTP_DEF_CMD_INIT,0,0,0};
        res = devPF.commandSend(ID_POWERFACTORY, cmd);
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
            
    protected void resetPF() {
        int res;
        System.out.println(getLocalName()+"\t::Initializing Conditions of PowerFactory...");
        int cmd[] = {SSTP_DEF_CMD_RST,0,0,0};
        res = devPF.commandSend(ID_POWERFACTORY,cmd);
        switch (res) {
            case drv_sstp.DRV_SSTP_RSP_OK:
                //System.out.println(getLocalName()+"::Reset OK.");
                break;
            case drv_sstp.DRV_SSTP_RSP_ERR:
                System.out.println(getLocalName()+"::Reset ERROR.");
                break;
            default:
                System.out.println(getLocalName()+"::Reset FAILURE.");
                break;
        } 
    }
    
    protected void stepPF(int second) {
        int res;
        //System.out.println(getLocalName()+"::Caculating Simulation to " 
        //        +(second/10000)+" milliseconds...");
        int cmd[] = {SSTP_DEF_CMD_STEP,second,0,0};
        res = devPF.commandSend(ID_POWERFACTORY,cmd);
        switch (res) {
            case drv_sstp.DRV_SSTP_RSP_OK:
                //System.out.println(getLocalName()+"::Step OK.");
                break;
            case drv_sstp.DRV_SSTP_RSP_ERR:
                System.out.println(getLocalName()+"::Step ERROR.");
                break;
            default:
                System.out.println(getLocalName()+"::Step FAILURE.");
                break;
        }
    }
    
    protected byte[] getDataPF(int addr, int len) {
        //System.out.println(getLocalName()+"::Get Data...");
        int cmd[] = {0,addr,len,0};
        devPF.readSend(ID_POWERFACTORY,cmd);
        return devPF.getRecebuf();
    }    
    
    protected void setDataPF(int addr, int len, byte[] Data) {
        int res;
        //System.out.println(getLocalName()+"::Set Data...");
        int cmd[] = {0,addr,len,0};
        res = devPF.writeSend(ID_POWERFACTORY,cmd, Data, len);       
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
        devAllCIED_cnt = iedaid.length;
        devComCIED = new drv_ComAgent[devAllCIED_cnt];
        String lnametmp;
        int postmp;
        for(int i=0;i<devAllCIED_cnt;i++)
        {
            lnametmp = iedaid[i].getLocalName().substring(4);
            postmp = Integer.parseInt(lnametmp) - ID_COMIED;
            if(postmp<devAllCIED_cnt)
            {
                devComCIED[postmp] = new drv_ComAgent();
                devComCIED[postmp].init(DRV_SSTP_MASTER, id_self);
                devComCIED[postmp].AddAgentReceiver(iedaid[i]);
                devAllCIED.AddAgentReceiver(iedaid[i]);
            }
        }
    }
}
