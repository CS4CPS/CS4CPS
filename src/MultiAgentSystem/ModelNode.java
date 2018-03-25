package MultiAgentSystem;

import CoSimulation.ModelConfig;
import static CoSimulation.TypeConvert.ByteArrayToFloat;
import static CoSimulation.TypeConvert.UIntToByteArray;
import static CoSimulation.drv_ComAgent.DRV_AGENT_LOCK_POS;
import CoSimulation.drv_sstp;
import static CoSimulation.sysconfig.ID_COMIED;
import static CoSimulation.sysconfig.ID_COMPOWERFFACTORY;
import static CoSimulation.sysconfig.SSTP_DEF_CMD_PROCESS;
import static MultiAgentSystem.ModelIEDbk.MODELIED_EVENT_GETMSG;

public class ModelNode extends ModelIED {

    private byte[] IntToByteArray() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public static final int MODELNODE_NODELIST_SWLEN  = 8;
    public static final int MODELNODE_NODELIST_ZONELEN  = 6;
    
    public class ZoneInfo {
        public int zonename;
        public int[] sw;
        public int cnt;
        public ZoneInfo() {
            sw = new int[MODELNODE_NODELIST_SWLEN];
        }
    }
        
    int MasterID,SCount;
    ZoneInfo[] instZoneInfo;
    int zonecount;
    
    public void init(short id, ModelConfig devConfig, Nodeinfo dev_nodeinfo){
    
        for(int i=0;i<devConfig.IEDCount;i++)
        {
            if(devConfig.devIED[i].asIsMaster == 1)
            {
                MasterID = devConfig.devIED[i].DevId;
            }
        }
        SCount = devConfig.devIED[id-ID_COMIED].SCount;
        instZoneInfo = new ZoneInfo[MODELNODE_NODELIST_ZONELEN];
        
        zonecount = 0;
        int tmp,match;
        for(int i=0;i<SCount;i++)
        {
            for(int j=0;j<MODELNODE_NODELIST_ZONELEN;j++)
            {
                tmp = devConfig.devIED[id-ID_COMIED].Switch[i].asZoneName[j];
                if(tmp > 0)
                {
                    match = 0; 
                    for(int k=0;k<zonecount;k++)
                    {
                        if(tmp == instZoneInfo[k].zonename)
                        {
                            match = 1;
                            instZoneInfo[k].sw[instZoneInfo[k].cnt] = i;
                            instZoneInfo[k].cnt += 1;
                            break;
                        }
                    }
                    if(match == 0)
                    {
                        instZoneInfo[zonecount] = new ZoneInfo();
                        instZoneInfo[zonecount].cnt = 0;
                        instZoneInfo[zonecount].zonename = tmp;
                        instZoneInfo[zonecount].sw[instZoneInfo[zonecount].cnt] = i;
                        instZoneInfo[zonecount].cnt = instZoneInfo[zonecount].cnt + 1;
                        zonecount = zonecount + 1;
                    }
                }
            }
        }
    }  
        
    public void reset(short id, ModelConfig devConfig, Nodeinfo dev_nodeinfo){

    }
        
    public int intr_ADSample(Nodeinfo dev_nodeinfo, drv_sstp sstphead, int[] cmd, byte[] obuf, ModelIEDdata[] devIEDdata){
                
        System.out.println("\t\tEVENT::AD.");

        devIEDdata[0].des = (short) MasterID;
        devIEDdata[0].datalen = sstphead.GetLen()-16;
        devIEDdata[0].ctrl = 0;
        System.arraycopy(obuf, 16, devIEDdata[0].data, 0, sstphead.GetLen()-16);
        
        return 1;
    }  
    
    public int intr_GetMSG(Nodeinfo dev_nodeinfo, drv_sstp sstphead, int[] cmd, byte[] obuf, ModelIEDdata[] devIEDdata){
        
        int rcnt = 0;
                
        System.out.println("\t\tEVENT::GETMSG.");
   
        int faultzone = getIData(obuf, 16);
        int[] swctrl = new int[SCount];
        for(int i=0;i<SCount;i++)
            swctrl[i] = 1;
        
        for(int i=0;i<zonecount;i++)
        {
            if(instZoneInfo[i].zonename == faultzone)
            {
                for(int j=0;j<instZoneInfo[i].cnt;j++)
                {
                    System.out.println("\t\t     ::Zonename = "+faultzone+"; Sw = "+instZoneInfo[i].sw[j]+".");
                    swctrl[instZoneInfo[i].sw[j]] = 0;
                }
            }
        }
        
        devIEDdata[rcnt].des = ID_COMPOWERFFACTORY;
        int pos = 0;
        for(int i=0;i<SCount;i++)
        {
            byte[] btmp = UIntToByteArray(swctrl[i]);
            System.arraycopy(btmp, 0, devIEDdata[rcnt].data, pos, 4);
            pos = pos + 4;
        }
        devIEDdata[rcnt].datalen = SCount*4;
        devIEDdata[rcnt].ctrl = 0;
        rcnt++;                   
           
        return rcnt;
    }  
}
