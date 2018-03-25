package CoSimulation;

import static CoSimulation.TimeList.TIMELIST_TYPE_GT;
import static CoSimulation.sysconfig.*;
import static CoSimulation.drv_sstp.*;
import static CoSimulation.TypeConvert.IPAdressToUInt;
import static CoSimulation.drv_ComAgent.*;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.util.leap.Iterator;
import java.text.DateFormat;
import java.util.Date;

public class GlobalTimer extends ComBase{
    
    private GlobalTimerGui myGui;
    
    drv_ComAgent devComPF, devComONT, devComAllCIED,devAll;
    drv_ComAgent[] devComCIED;
    int devCIED_cnt, devAll_cnt;
    
    TimeList devTimeList;
    
    TMsynlock dev_synlock;
    synlock[] proclock;
    int proclockCnt;
    int SimTime;
    
    protected void setup(){
        id_self = sysconfig.ID_GLOBALTIMER;
        df_service = "GLOBALTIMER";
        super.setup();

        DFsubscription("COMPOWERFFACTORY");
        DFsubscription("COMOMNETPP");
        
        // Create and show the GUI
        myGui = new GlobalTimerGuiImpl();
        myGui.setAgent(this);
        myGui.show();
 
        devComAllCIED = new drv_ComAgent();
        devComAllCIED.init(DRV_SSTP_MASTER, id_self);
        
        devAll = new drv_ComAgent();
        devAll.init(DRV_SSTP_MASTER, id_self);
        devCIED_cnt = 0;
        devAll_cnt = 0;
        processCount = 0;
        
        //ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        //ACLMessage msgtmp = new ACLMessage(ACLMessage.INFORM);
        //java.util.Iterator iter = msg.getAllReceiver();
        //while(iter.hasNext()){
        //    AID idinfo = (AID)iter.next();
        //    msgtmp.addReceiver(idinfo);
        //} 
        //msgtmp.setByteSequenceContent(msg.getByteSequenceContent());
        
        devTimeList = new TimeList();
        devTimeList.init(TIMELIST_TYPE_GT);
        
        dev_synlock = new TMsynlock();
        
        addBehaviour( new GlobalTimer.myMSGReceive(DRV_SSTP_MASTER, id_self) );
    }  
    
    protected void takeDown() {
        // Dispose the GUI if it is there
        if (myGui != null) {
            myGui.dispose();
        }
        super.takeDown();
    }
    
    String[] StrId = {"COSIMMANAGER","COMPOWERFFACTORY","COMOMNETPP","GLOBALTIMER"};
    String[] StrStepAct = {"HALT","INIT","RESET","NEXT","STEP","PROCESS","CTRL"};
    
    public class myMSGReceive extends MSGReceive {
        public myMSGReceive(int setmod, short setaddr) {
            devComCh = new drv_ComAgent();
            devComCh.init(setmod, setaddr);
        }        
        public int process(drv_sstp sstphead, int[] cmd, byte[] obuf, byte[] sbuf, ACLMessage reply) {
            
            int res = 0;
            String idtmp;
            String tmtmp;

            switch(dev_synlock.getStat()) {
                case GLOBALTIME_STEP_INIT:
                case GLOBALTIME_STEP_RST:
                case GLOBALTIME_STEP_RUN_STEP:  
                case GLOBALTIME_STEP_RUN_CTRL: 
                    idtmp = Integer.toString(sstphead.head_sor);
                    
                    if(cmd[DRV_AGENT_RSP_POS] == DRV_SSTP_RSP_OK)
                        myGui.notifyUser("    "+idtmp+":: "+StrStepAct[dev_synlock.getStat()]+" success.\n");
                    else
                        myGui.notifyUser("    "+idtmp+":: "+StrStepAct[dev_synlock.getStat()]+" failed.\n");
                    dev_synlock.openLock(dev_synlock.getStat());
                    break;
                case GLOBALTIME_STEP_RUN_NEXT:
                    idtmp = Integer.toString(sstphead.head_sor);
                    tmtmp = Integer.toString(cmd[DRV_AGENT_NEXT_TIME_POS]);
                    myGui.notifyUser("    "+idtmp+":: Next time is "+tmtmp+".\n");
                    System.out.println("\t\tNext Time of "+idtmp+" is "+tmtmp+".");
                    devTimeList.findShortTime(cmd[DRV_AGENT_NEXT_TIME_POS], sstphead.head_sor);
                    dev_synlock.openLock(dev_synlock.getStat());
                    break;  
                case GLOBALTIME_STEP_RUN_PROCESS:  
                    //for(int i=0;i<proclockCnt;i++)
                    //{
                        //int locktmp = proclock[i].openLock(cmd[DRV_AGENT_LOCK_POS]);
                        //if(locktmp == 1)
                        //{
                            idtmp = Integer.toString(sstphead.head_sor);
                            myGui.notifyUser("    "+idtmp+":: "+StrStepAct[dev_synlock.getStat()]+" success.\n");
                            System.out.println(getLocalName()+"\t::Process::fin.");  
                            if(cmd[DRV_AGENT_PAR1_POS] == 1)
                                isPFProcess = 1;
                            dev_synlock.openLock(dev_synlock.getStat());
                        //}
                    //}
                    break;
            }                   
            
            return res;
        }
        
        public void DFresult(DFAgentDescription[] dfds) {
            for(int i=0;i<dfds.length;i++)
            {
                Iterator it = dfds[i].getAllServices();
                ServiceDescription serv = (ServiceDescription)it.next();
                String s = serv.getType();
                if(s.equals("COMPOWERFFACTORY"))
                {
                    devComPF = new drv_ComAgent();
                    devComPF.init(DRV_SSTP_MASTER, id_self);
                    devComPF.AddAgentReceiver(dfds[0].getName());
                    myGui.notifyUser("    Find service COMPOWERFFACTORY.\n");
                    devAll.AddAgentReceiver(dfds[0].getName());
                    devAll_cnt++;
                }
                if(s.equals("COMOMNETPP"))
                {
                    devComONT = new drv_ComAgent();
                    devComONT.init(DRV_SSTP_MASTER, id_self);
                    devComONT.AddAgentReceiver(dfds[0].getName());
                    myGui.notifyUser("    Find service COMOMNETPP.\n");
                    devAll.AddAgentReceiver(dfds[0].getName());
                    devAll_cnt++;
                }
            }
        }
    }
           
    public static final int GLOBALTIME_STEP_INIT  = 1;
    public static final int GLOBALTIME_STEP_RST  = 2;
    public static final int GLOBALTIME_STEP_RUN_NEXT  = 3;
    public static final int GLOBALTIME_STEP_RUN_STEP  = 4;
    public static final int GLOBALTIME_STEP_RUN_PROCESS  = 5;
    public static final int GLOBALTIME_STEP_RUN_CTRL  = 6;      
    
    public void MAS_init(String servIP, int servPort) {
        dev_synlock.setLock(GLOBALTIME_STEP_INIT,3);
        myGui.notifyUser("Init: System initializing...\n");
        addBehaviour(new MAS_init_PF(this, servIP, servPort));     
        addBehaviour(new MAS_init_ONT(this, servIP, servPort+10));  
        addBehaviour(new MAS_init_CIED()); 
    }
    
    public void MAS_SimTime(int tmpSimTime) {
        SimTime = tmpSimTime;
        myGui.notifyUser("Init: SimeTime is "+SimTime+".\n");  
    }    

    private class MAS_init_PF extends OneShotBehaviour {
        int addr,port;
        private MAS_init_PF(Agent a, String servIP, int servPort) {
            myGui.notifyUser("    PF: IP:"+servIP+";"+Integer.toString(servPort)+".\n");
            addr = IPAdressToUInt(servIP);
            port = servPort;
        }
        public void action() {
            int cmd[] = {SSTP_DEF_CMD_INIT,addr,port,0};
            devComPF.commandSend(ID_COMPOWERFFACTORY,cmd);
            send(devComPF.GetMsg());
        }
    }  
    
    private class MAS_init_ONT extends OneShotBehaviour {
        int addr,port;
        private MAS_init_ONT(Agent a, String servIP, int servPort) {
            myGui.notifyUser("    ONT: IP:"+servIP+";"+Integer.toString(servPort)+".\n");
            addr = IPAdressToUInt(servIP);
            port = servPort;
        }
        public void action() {
            int cmd[] = {SSTP_DEF_CMD_INIT,addr,port,0};
            devComONT.commandSend(ID_COMOMNETPP,cmd);
            send(devComONT.GetMsg());
        }
    }      
    
    private class MAS_init_CIED extends OneShotBehaviour {
        public void action() {
            
            AID[] iedaid = DFsearch("COMIED");
            devCIED_cnt = iedaid.length;
            devComCIED = new drv_ComAgent[devCIED_cnt];
            String lnametmp;
            int postmp;
            for(int i=0;i<devCIED_cnt;i++)
            {
                lnametmp = iedaid[i].getLocalName().substring(4);
                postmp = Integer.parseInt(lnametmp) - ID_COMIED;

                devComCIED[postmp] = new drv_ComAgent();
                devComCIED[postmp].init(DRV_SSTP_MASTER, id_self);
                devComCIED[postmp].AddAgentReceiver(iedaid[i]);
                devComAllCIED.AddAgentReceiver(iedaid[i]);
                devAll.AddAgentReceiver(iedaid[i]);
                devAll_cnt++;
            }
            
            myGui.notifyUser("    Find "+Integer.toString(devCIED_cnt)+" IEDs.\n");
            dev_synlock.openLock(GLOBALTIME_STEP_INIT);         
        }
    }  
    
    public void MAS_reset() {
        dev_synlock.setLock(GLOBALTIME_STEP_RST,devAll_cnt);
        myGui.notifyUser("Reset: System reseting...\n");
        
        devTimeList.reset(devAll_cnt);
        //TM_ActiveAgent = new int[devAll_cnt];
        //TM_ActiveTime = new int[devAll_cnt];
        //TM_GlobalTime = 0;
        
        addBehaviour(new MAS_reset_PF());     
        addBehaviour(new MAS_reset_ONT());  
        addBehaviour(new MAS_reset_CIED());  
    }

    private class MAS_reset_PF extends OneShotBehaviour {
        public void action() {
            int cmd[] = {SSTP_DEF_CMD_RST,0,0,0};
            devComPF.commandSend(ID_COMPOWERFFACTORY,cmd);
            send(devComPF.GetMsg());
        }
    }  
    
    private class MAS_reset_ONT extends OneShotBehaviour {
        public void action() {
            int cmd[] = {SSTP_DEF_CMD_RST,0,0,0};
            devComONT.commandSend(ID_COMOMNETPP,cmd);
            send(devComONT.GetMsg());
        }
    }  
    
    private class MAS_reset_CIED extends OneShotBehaviour {
        public void action() {
            int cmd[] = {SSTP_DEF_CMD_RST,0,0,0};
            for(int i=0;i<(devCIED_cnt);i++) {
                //cmd[1] = ID_COMIED+i;
                //cmd[2] = 20+i;
                devComCIED[i].commandSend((short) (ID_COMIED+i), cmd);
                send(devComCIED[i].GetMsg());
            }
        }
    }  
    
    public void MAS_run(int tmpSimTime) {
        SimTime = tmpSimTime;
        MAS_run();
    }
    
    public void MAS_run() {
        Date sysdate = new Date();
        DateFormat df3 = DateFormat.getTimeInstance();
        int rttmpi = devTimeList.getGlobalTime();
        String rttmp = Float.toString(rttmpi);
        System.out.println("----------------------------------------------------");
        System.out.println(df3.format(sysdate)+" :: "+rttmp);
        System.out.println("----------------------------------------------------");
        dev_synlock.setLock(GLOBALTIME_STEP_RUN_NEXT,devAll_cnt);
        addBehaviour(new MAS_run_next());
    }    
    
    private class MAS_run_next extends OneShotBehaviour {
        public void action() {
            devTimeList.findSTRst();
            myGui.notifyUser("Next: Get next time...\n"); 
            System.out.println(getLocalName()+"\tGet Next Time...");
            int cmd[] = {SSTP_DEF_CMD_NEXT,0,0,0};
            devAll.commandSend((short)0,cmd);
            send(devAll.GetMsg());
        }
    }  
    
    private class MAS_run_step extends OneShotBehaviour {
        public void action() {
            dev_synlock.setLock(GLOBALTIME_STEP_RUN_STEP,devAll_cnt);
            float rttmpf = devTimeList.getGlobalTime();
            String rttmp = Float.toString(rttmpf/10);
            myGui.notifyUser("Step: Run system at "+rttmp+" ms ...\n"); 
            int cmd[] = {SSTP_DEF_CMD_STEP,0,devTimeList.getGlobalTime(),0};
            devAll.commandSend((short)0,cmd);
            send(devAll.GetMsg());
        }
    }  
    
    private class MAS_run_ctrl extends OneShotBehaviour {
        public void action() {
            dev_synlock.setLock(GLOBALTIME_STEP_RUN_CTRL,1);
            //int cmd[] = {SSTP_DEF_CMD_PROCESS,33,devTimeList.getGlobalTime(),0};
            int cmd[] = {SSTP_DEF_CMD_CTRL,0,0,0};
            devComPF.commandSend(ID_COMPOWERFFACTORY,cmd);
            send(devComPF.GetMsg());
        }
    }  
    
    int isPFProcess;
    int processCount;
    private class MAS_run_process extends OneShotBehaviour {
        public void action() {
            int idtmp;
            //int proccounttmp = 0;
            
            if(processCount == 0)
            {
                isPFProcess = 0;
                proclockCnt = devTimeList.getASTCount();
                System.out.println(getLocalName()+"\tProcess::proclockCnt="+proclockCnt+".");
            }
            
            //System.out.println(getLocalName()+"::Process::processCount="+processCount+".");
            dev_synlock.setLock(GLOBALTIME_STEP_RUN_PROCESS,1);
            proclock = new synlock[proclockCnt];
            
            int i=processCount;

            proclock[i] = new synlock();
            proclock[i].setLock(i+88, 1);

            idtmp = devTimeList.getASTAgent(i);
            String rttmp = Integer.toString(idtmp);
            myGui.notifyUser("Step process: Active model "+rttmp+" ...\n"); 
            System.out.println(getLocalName()+"\tProcess::"+(i+33)+"::Active model "+rttmp+".");
            if(idtmp < ID_COMIED)
            {
                if(idtmp == ID_COMPOWERFFACTORY)
                {
                    //isPFProcess = 1;
                    //proccounttmp--;
                    int cmd[] = {SSTP_DEF_CMD_PROCESS,i+33,devTimeList.getGlobalTime(),0};
                    devComPF.commandSend(ID_COMPOWERFFACTORY,cmd);
                    send(devComPF.GetMsg());
                }else if(idtmp == ID_COMOMNETPP)
                {
                    int cmd[] = {SSTP_DEF_CMD_PROCESS,i+33,devTimeList.getGlobalTime(),0};
                    devComONT.commandSend(ID_COMOMNETPP,cmd);
                    send(devComONT.GetMsg());
                }
            }else
            {
                int cmd[] = {SSTP_DEF_CMD_PROCESS,i+33,devTimeList.getGlobalTime(),0};
                devComCIED[idtmp-ID_COMIED].commandSend((short)0,cmd);
                send(devComCIED[idtmp-ID_COMIED].GetMsg());
            }
        }
    }  
    
    private class MAS_run_fin extends OneShotBehaviour {
        public void action() {
            
            float rttmpf = devTimeList.getGlobalTime();
            int rttmpi = devTimeList.getGlobalTime();
            String rttmp = Float.toString(rttmpf/10);
            
            myGui.notifyUser("------------------------------\nRun system at "+rttmp+" ms, Fin.\n\n"); 
            
            if(rttmpi < SimTime)
            {
                MAS_run();
            }else
            {
                Date sysdate = new Date();
                DateFormat df3 = DateFormat.getTimeInstance();
                rttmpi = devTimeList.getGlobalTime();
                rttmp = Float.toString(rttmpi);
                System.out.println("----------------------------------------------------");
                System.out.println(df3.format(sysdate)+" :: "+rttmp);
                System.out.println("----------------------------------------------------");
                System.out.println(getLocalName()+"\tCoSimulationFinish.");
            }
        }
    }  
        
    public class TMsynlock extends synlock {       
        int openLock(int sta){
            if((sta == cmdstat) && (cmdstat != 0)) {
                now++;
                if(now>=cnt){
                    switch(cmdstat) {
                        case GLOBALTIME_STEP_RUN_NEXT:
                            addBehaviour(new MAS_run_step()); 
                            break;
                        case GLOBALTIME_STEP_RUN_STEP:
                            addBehaviour(new MAS_run_process()); 
                            break;
                        case GLOBALTIME_STEP_RUN_PROCESS:
                            processCount++;
                            if(processCount < proclockCnt)
                                addBehaviour(new MAS_run_process()); 
                            else{
                                processCount = 0;
                                if(isPFProcess == 1)
                                    addBehaviour(new MAS_run_ctrl());
                                else
                                    addBehaviour(new MAS_run_fin()); 
                            }
                            break;      
                        case GLOBALTIME_STEP_RUN_CTRL:
                            addBehaviour(new MAS_run_fin()); 
                            break;                                  
                        default:
                            cmdstat = 0;
                    }
                }
            }
            return 0;
        }
    }
}
