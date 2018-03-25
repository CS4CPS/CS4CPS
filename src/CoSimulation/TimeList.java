package CoSimulation;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TimeList {
    
    public class Tmsg {
        int time;
        ACLMessage msg;
        byte[] content;
        
        public Tmsg(){
        }
                
        public Tmsg(int tm, ACLMessage m){
            time = tm;
            msg = m;
        }
    }
    
    public static final short TIMELIST_TYPE_GT  = 1; 
    public static final short TIMELIST_TYPE_CPF  = 2; 
    public static final short TIMELIST_TYPE_ONT  = 3; 
    public static final short TIMELIST_TYPE_IED  = 4; 

    private final  List<Tmsg> SendList;
    private final  List<Tmsg> ReceList;
    int TM_TargetTime;
    int TM_GlobalTime,TM_LocalTime,TM_StepTime,TM_ProcessTime;
    int TM_NextStep,TM_ProcessStep,TM_MiniStep;
    int TM_ActiveCount;
    int[] TM_ActiveTime,TM_ActiveAgent;
    int tltype;
    int SamplingDelay, SamplingAct;
    
    public TimeList() {
        this.SendList = new  ArrayList<>();
        this.ReceList = new  ArrayList<>();
        TM_GlobalTime = 0;
        TM_LocalTime = 0;
        TM_StepTime = 0;
        TM_ProcessTime = 0;
        tltype = 0;
        SamplingAct = 0;
    }

    public void init(int type)
    {
        tltype = type;
        switch(type)
        {
            case TIMELIST_TYPE_CPF:
                TM_NextStep = 500;
                TM_ProcessStep = 0;                
                TM_MiniStep = 0;                
                break;
            case TIMELIST_TYPE_ONT:
                TM_NextStep = 0;
                TM_ProcessStep = 100;                
                TM_MiniStep = 0;                
                break;                
            case TIMELIST_TYPE_IED:
                TM_NextStep = 0;
                TM_ProcessStep = 100;
                TM_MiniStep = 0;                
                break;
        }
    }
    
    public void init(int NStep, int PStep, int SStep, int MStep)
    {
        TM_NextStep = NStep;
        TM_ProcessStep = PStep;                
        TM_MiniStep = MStep;
        SamplingDelay = SStep;
    }
    
    //for GT
    public void reset(int cnt)
    {
        TM_GlobalTime = 0;    
        TM_ActiveAgent = new int[cnt];
        TM_ActiveTime = new int[cnt];
    }
    //for GT
    public void findSTRst()
    {
        TM_ActiveCount = 0;
        TM_ActiveTime[0] = 999999;        
    }
    //for GT
    public void findShortTime(int tm, short sor)
    {
        if(tm < TM_ActiveTime[0]){
            TM_ActiveCount = 1; 
            TM_ActiveTime[0] = tm;
            TM_ActiveAgent[0] = sor;
            TM_GlobalTime = TM_ActiveTime[0];
        }else if(tm==TM_ActiveTime[0])
        {
            TM_ActiveTime[TM_ActiveCount] = tm;
            TM_ActiveAgent[TM_ActiveCount] = sor;
            TM_ActiveCount++;
        }                
    }
    //for GT    
    public int getASTCount()
    {        
        return TM_ActiveCount;
    } 
    //for GT
    public int getASTAgent(int i)
    {        
        return TM_ActiveAgent[i];
    } 
        
    public int getSamplingDelay()
    {        
        return SamplingDelay;
    } 
    //for GT
    public void setSamplingDelay(int i)
    {        
        SamplingDelay = i;
    } 
    
    public void actSamplingDelay()
    {        
        SamplingAct = 1;
    } 
        
    public void reset()
    {
        TM_GlobalTime = 0;
        TM_LocalTime = 0;
        TM_StepTime = 0;
        TM_ProcessTime = TM_MiniStep;
    }
    
    public Tmsg getNearTmsg()
    {
        if(!isSendTmsgEmpty())
        {
            Tmsg near = new Tmsg();
            near.time = 999999;
            for(Tmsg tg:SendList)
            {
                if(tg.time < near.time)
                    near = tg;
            }
            return near;
        }else
            return null;
    }
    
    public void setGlobalTime(int gt)
    {        
        TM_GlobalTime = gt;
    }
    
    public void setLocalTime(int gt)
    {        
        TM_LocalTime = gt;
    }
        
    public void setTargetTime(float gt)
    {        
        float gttmp = gt * 10000;
        TM_TargetTime = (int)gttmp;
    }
        
    public int getGlobalTime()
    {        
        return TM_GlobalTime;
    }
    
    public int getLocalTime()
    {        
        return TM_LocalTime;
    }  
    
    public int getProcessTime()
    {        
        return TM_ProcessTime;
    } 
    
    public boolean isProcess()
    {
        if(TM_GlobalTime>=TM_ProcessTime)
            return true;
        else
            return false;
    }
    
    public Tmsg getCurrentTmsg()
    {
        Tmsg cur = null;
        if(!isSendTmsgEmpty())
        {
            for (int i=0;i<SendList.size();i++)
            {  
                if(SendList.get(i).time == TM_GlobalTime)
                {
                    cur = SendList.get(i);
                    SendList.remove(i);
                    break;
                }
            }
        }
        return cur;
    }    
    
    public void TimeLapse()
    {
        //StepTime
        if(TM_NextStep > 0)
        {
            if(TM_GlobalTime >= TM_StepTime)
            {
                TM_StepTime = TM_StepTime + TM_NextStep;
                setSendTmsg(TM_StepTime, null);
            }
        }
        
        //LocalTime
        TM_LocalTime = 999999;
        Tmsg near = getNearTmsg();
        if(near != null)
        {
            TM_LocalTime = near.time;
        }
        
        //ProcessTime
        //if(TM_MiniStep > 0)
        //{
        //    if(TM_GlobalTime >= TM_ProcessTime)
        //    {
        //        TM_ProcessTime = TM_ProcessTime + TM_MiniStep;
        //    }
        //}        
    }
    
    public ACLMessage MsgClone(ACLMessage msg)
    {
        //ACLMessage msg1 = new ACLMessage(ACLMessage.INFORM);
        ACLMessage msgtmp = new ACLMessage(ACLMessage.INFORM);
        Iterator iter = msg.getAllReceiver();
        while(iter.hasNext()){
            AID idinfo = (AID)iter.next();
            msgtmp.addReceiver(idinfo);
        } 
        msgtmp.setByteSequenceContent(msg.getByteSequenceContent());
        return msgtmp;
    }
    
    public void setSendTmsg(int tm, ACLMessage msg)
    {
        Tmsg v;
        if(msg != null)
        {
            //ACLMessage msgtmp = MsgClone(msg);
            //v = new Tmsg(tm, msgtmp);
            ACLMessage msgtmp = (ACLMessage) msg.clone();
            v = new Tmsg(tm, msgtmp);            
        }else
        {
            v = new Tmsg(tm, null);
        }
        SendList.add(v);
    }
    
    public int getNextTime()
    {
        if(SamplingAct == 1)
        {
            SamplingAct = 0;
            return (TM_GlobalTime+TM_ProcessStep+SamplingDelay);
        }else
            return (TM_GlobalTime+TM_ProcessStep);
    }
    
    
    public int guessNextTime()
    {
        if(SamplingAct == 1)
        {
            return (TM_GlobalTime+TM_ProcessStep+SamplingDelay);
        }else
            return (TM_GlobalTime+TM_ProcessStep);
    }
    
    public void setSendTmsg(ACLMessage msg)
    {
        setSendTmsg(getNextTime(), msg);
    }
    
    public boolean isSendTmsgEmpty() {
        return SendList.isEmpty();
    }
    
    public void setReceTmsg(int tm, ACLMessage msg)
    {
        Tmsg v = new Tmsg(tm, msg);
        ReceList.add(v);
    }
    
    public void setReceTmsg(ACLMessage msg)
    {
        Tmsg v = new Tmsg(TM_GlobalTime, msg);
        ReceList.add(v);
    }
    
    public boolean isReceTmsgEmpty() {
        return ReceList.isEmpty();
    }

}
