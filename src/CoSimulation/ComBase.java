package CoSimulation;

import static CoSimulation.drv_ComAgent.DRV_AGENT_CMD_POS;
import static CoSimulation.drv_ComAgent.DRV_AGENT_PAR1_POS;
import static CoSimulation.drv_sstp.DRV_SSTP_IDXDL;
import static CoSimulation.drv_sstp.DRV_SSTP_SLAYER;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

abstract class ComBase extends Agent{
    
    short id_self;
    ModelConfig devConfig;
    String df_service;
        
    drv_ComAgent devComCh;
    synlock devRTsynlock;
    
    protected void setup(){

        Object[] args = getArguments();
        if (args != null) {
            int id_tmp = Integer.parseInt( (String) args[0] );
            id_self = (short)id_tmp;
            devConfig = (ModelConfig) args[1];
        }
        System.out.println(getLocalName()+"\t::Runing::ID="+id_self+".");
        
        ServiceDescription sd  = new ServiceDescription();
        sd.setType(df_service);
        sd.setName(getLocalName());
        DFregister(sd);
        
        devRTsynlock = new RTsynlock();
    }
    
    protected void takeDown() {
        try { DFService.deregister(this); }
        catch (Exception e) {}
    }
    
    abstract class MSGReceive extends CyclicBehaviour {
        
        int[] Rececmd = new int[4];
        byte[] obuf = new byte[512];
        byte[] sbuf = new byte[512];
        byte[] cpbuf = new byte[512];
        int cpbuflen = 0;
                        
        public MSGReceive() {
            devComCh = new drv_ComAgent();
            devComCh.init(DRV_SSTP_SLAYER, id_self);
        }
        
        public int ReturnCmd(int par0, int par1, int par2) {
            byte[] tmp;
            
            tmp = TypeConvert.UIntToByteArray(Rececmd[DRV_AGENT_CMD_POS]);
            System.arraycopy(tmp, 0, sbuf, 0, 4);
            tmp = TypeConvert.UIntToByteArray(par0);
            System.arraycopy(tmp, 0, sbuf, 4, 4);
            tmp = TypeConvert.UIntToByteArray(par1);
            System.arraycopy(tmp, 0, sbuf, 8, 4);
            tmp = TypeConvert.UIntToByteArray(par2);
            System.arraycopy(tmp, 0, sbuf, 12, 4);
            
            return DRV_SSTP_IDXDL;
        }
        
        public void action() {
            int resprot,resdata,ressend;
            ACLMessage msg= receive();
            if (msg!=null) {
                if(msg.getSender().equals(getDefaultDF())) {
                    try {
                        DFAgentDescription[] dfds = DFService.decodeNotification(msg.getContent());
                        if (dfds.length > 0) {
                            DFresult(dfds);
                        }
                    }catch (Exception ex) {}
                }else {
                    byte[] rbuf = msg.getByteSequenceContent(); 

                    //System.out.println(getLocalName()+"::");
                    //System.out.println("Get msg from "+msg.getSender()+"::");
                    //String hexString="";
                    //for(int i=0;i<rbuf.length;i++)
                    //    hexString+= Integer.toHexString(0xff&rbuf[i])+",";
                    //System.out.println("Received: " + hexString);
            
                    resprot = devComCh.resolve(rbuf,rbuf.length,obuf);
                    if(resprot > 0)
                    {
                        cpbuflen = rbuf.length;
                        System.arraycopy(rbuf, 0, cpbuf, 0, cpbuflen);
                        
                        byte[] tpbuf = new byte[4];
                        for(int i=0;i<4;i++) {
                            for(int j=0;j<4;j++)
                                tpbuf[j] = obuf[i*4+j];
                            Rececmd[i] = TypeConvert.byteArrayToUInt(tpbuf);
                        }
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.INFORM);
                        resdata = process(devComCh.sendhead, Rececmd, obuf, sbuf, reply);
                        if(resdata > 0)
                        {
                            ressend = devComCh.reply(sbuf, resdata, obuf);
                            if(ressend > 0) {
                                byte[] stmpbuf = new byte[ressend];
                                System.arraycopy(obuf, 0, stmpbuf, 0, ressend);
                                reply.setByteSequenceContent(stmpbuf);
                                send(reply);
                            }
                        }
                    }
                }
            }else{block();}
        } 
            
        abstract int process(drv_sstp sstphead, int[] cmd, byte[] obuf, byte[] sbuf, ACLMessage reply);
        abstract void DFresult(DFAgentDescription[] dfds);
    }
    
    void DFregister( ServiceDescription sd) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        try {
        	DFAgentDescription list[] = DFService.search(this, dfd);
		if ( list.length>0 ) 
                    DFService.deregister(this);
                dfd.addServices(sd);
		DFService.register(this,dfd);
	}catch (FIPAException fe) { fe.printStackTrace(); }
    }

    void DFregisterMult( ServiceDescription[] sd, int cnt) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        try {
        	DFAgentDescription list[] = DFService.search(this, dfd);
		if ( list.length>0 ) 
                    DFService.deregister(this);
                for(int i=0;i<cnt;i++)
                    dfd.addServices(sd[i]);
		DFService.register(this,dfd);
	}catch (FIPAException fe) { fe.printStackTrace(); }
    }
        
    AID DFgetService( String service ) {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType( service );
        dfd.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, dfd);
            if (result.length>0)
                return result[0].getName() ;
        }
        catch (FIPAException fe) { fe.printStackTrace(); }
      	return null;
    }

    AID[] DFsearch( String service ) {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType( service );
        dfd.addServices(sd);
        SearchConstraints ALL = new SearchConstraints();
        ALL.setMaxResults(new Long(-1));
        try
        {
            DFAgentDescription[] result = DFService.search(this, dfd, ALL);
            AID[] agents = new AID[result.length];
            for (int i=0; i<result.length; i++) 
                agents[i] = result[i].getName() ;
            return agents;
        }
        catch (FIPAException fe) { fe.printStackTrace(); }
      	return null;
    }
    
    void DFsubscription( String service ) {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(service);
        dfd.addServices(sd);
        SearchConstraints sc = new SearchConstraints();
        sc.setMaxResults(new Long(1));
        send(DFService.createSubscriptionMessage(this, getDefaultDF(), dfd, sc));
    }
    
    public class RTsynlock extends synlock {  
        
        void finAction()
        {
            //int cmdtmp[] = {0,cmdstat,0,DRV_SSTP_RSP_OK};
            drv_ComAgent devTmp = new drv_ComAgent();
            devTmp.init(DRV_SSTP_SLAYER, id_self, getMSG());
            devTmp.RcmdSend(getID(),cmdbuf);
            send(devTmp.GetMsg());
            
            System.out.println(getLocalName()+"\t::Process::finAction::>"+(getID())+">"+(cmdbuf[DRV_AGENT_PAR1_POS])+".");
        }
    }
}
