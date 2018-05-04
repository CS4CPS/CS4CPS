package MultiAgentSystem;

import CoSimulation.ModelConfig;
import static CoSimulation.TypeConvert.UIntToByteArray;
import static CoSimulation.drv_ComAgent.DRV_AGENT_LOCK_POS;
import CoSimulation.drv_sstp;
import static CoSimulation.sysconfig.ID_COMIED;
import static CoSimulation.sysconfig.ID_COMPOWERFFACTORY;
import static CoSimulation.sysconfig.SSTP_DEF_CMD_PROCESS;
import static MultiAgentSystem.ModelIEDbk.MODELIED_EVENT_INIT;
import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.sqrt;

public class ModelServer extends ModelIED {
    
    int cfgaddr;
    int[][][][] nodelist;
    int[][][] zonelist;
    int[] nodecount;
    int nodecountmax;
    int iedcount;
    int zonecount;
    int[] swcount;
    int fincntglobal;
    
    NodeDataPar[][][] nodedata;
    int samplefin;
    
    int FaultZone[];
    int FaultDelay;
    double FaultBase;
    double FaultRate;
    int syn;
    int pagecnt;
        
    public static final int MODELSERVER_NODEDATAPAR_DATALEN  = 9;
    public static final int MODELSERVER_NODELIST_SWLEN  = 8;
    public static final int MODELSERVER_NODELIST_ZONELEN  = 6;
    public static final int MODELSERVER_ZONELIST_ZONELEN  = 100;
    
    public class NodeDataPar {
        public float[] Data;
        public int Dir;
        public int fin;    
        public int avid;
        public int changed;
        public NodeDataPar() {
            Data = new float[MODELSERVER_NODEDATAPAR_DATALEN];
        }
    }
    
    public void init(short id, ModelConfig devConfig, Nodeinfo dev_nodeinfo){
    
        cfgaddr = id - ID_COMIED;
        nodelist = new int[devConfig.IEDCount][MODELSERVER_NODELIST_SWLEN][MODELSERVER_NODELIST_ZONELEN][2];
        zonelist = new int[MODELSERVER_ZONELIST_ZONELEN][devConfig.IEDCount][2];
        nodecount = new int[MODELSERVER_ZONELIST_ZONELEN];
        swcount = new int[devConfig.IEDCount];
        for(int i=0;i<MODELSERVER_ZONELIST_ZONELEN;i++)
        {
            nodecount[i] = 0;
        }
        iedcount = 0;
        
        int master,act;
        act = 0;
        for(int i=0;i<devConfig.IEDCount;i++)
        {
            swcount[i] = devConfig.devIED[i].SCount;
            for(int j=0;j<devConfig.devIED[i].SCount;j++)
            {
                for(int k=0;k<MODELSERVER_NODELIST_ZONELEN;k++)
                {
                    master = devConfig.devIED[i].Switch[j].asZoneName[k];
                    if(devConfig.devIED[i].Switch[j].asZoneName[k] > 0)
                    {
                        nodelist[i][j][k][0] = master;
                        nodelist[i][j][k][1] = nodecount[master-1];
                        
                        zonelist[master-1][nodecount[master-1]][0] = i;
                        zonelist[master-1][nodecount[master-1]][1] = j;
                        
                        nodecount[master-1] = nodecount[master-1] + 1;
                        act = 1;
                    }
                }
            }
            if(act == 1)
            {
                iedcount = iedcount + 1;
                act = 0;
            }
        }
        
        zonecount = 0;
        nodecountmax = 0;
        for(int i=0;i<MODELSERVER_ZONELIST_ZONELEN;i++)
        {
            if(nodecount[i]>0)
            {
                zonecount = zonecount + 1;
                if(nodecount[i]> nodecountmax)
                    nodecountmax = nodecount[i];
            }
        }
        
        fincntglobal =  zonecount*nodecountmax*20;
        nodedata = new NodeDataPar[zonecount][nodecountmax][20];
        int diried,dirsw,dirtmp;
        for(int i=0;i<zonecount;i++)
        {
            for(int j=0;j<nodecountmax;j++)
            {
                for(int k=0;k<20;k++)
                {
                    nodedata[i][j][k] = new NodeDataPar();
                
                    diried = zonelist[i][j][0];
                    dirsw = zonelist[i][j][1];

                    nodedata[i][j][0].avid = 1;
                    if((diried == 0) && (dirsw == 0))
                    {
                        nodedata[i][j][k].avid = 0;
                        fincntglobal -= 1;
                        continue;
                    }
                    for(int kk=0;kk<MODELSERVER_NODELIST_ZONELEN;kk++)
                    {
                        dirtmp = devConfig.devIED[diried].Switch[dirsw].asZoneName[kk];
                        if(dirtmp == i+1)
                        {
                            nodedata[i][j][k].Dir = devConfig.devIED[diried].Switch[dirsw].asDir[kk];
                        }
                    }
                    nodedata[i][j][k].fin = 0;
                    nodedata[i][j][k].changed = 0;
                }
            }
        }
        fincntglobal = fincntglobal / 20;
        samplefin = 0;
        
        FaultZone = new int[zonecount];
        for(int i=0;i<zonecount;i++)
            FaultZone[i] = 0;
        FaultDelay = 100;
        FaultBase = 0.01;
        FaultRate = 0.1;
        syn = 0;
        pagecnt = 0;
    }
    
    public void reset(short id, ModelConfig devConfig, Nodeinfo dev_nodeinfo){
        for(int i=0;i<zonecount;i++)
        {
            for(int j=0;j<nodecountmax;j++)
            {
                for(int k=0;k<20;k++)
                {
                    nodedata[i][j][k].fin = 0;
                    nodedata[i][j][k].changed = 0;
                }
            }
        }
        samplefin = 0;
    }
    
    public int intr_GetMSG(Nodeinfo dev_nodeinfo, drv_sstp sstphead, int[] cmd, byte[] obuf, ModelIEDdata[] devIEDdata){
    
        int res;
        int rcnt = 0;
        byte[] tmp = new byte[4];
        int sor,zone,sw,pos;
        
        sor = sstphead.GetSor();
        int adpage = getIData(obuf, 32);
        System.out.println("\t\tData collection from "+sor+" at "+adpage+".");
        

        for(int i=0;i<swcount[sor-ID_COMIED];i++)
        {
            for(int j=0;j<MODELSERVER_NODELIST_ZONELEN;j++)
            {
                zone = nodelist[sor-ID_COMIED][i][j][0];
                if(zone>0)
                {
                    if(syn == 1)
                    {
                        pos = nodelist[sor-ID_COMIED][i][j][1];
                        nodedata[zone-1][pos][adpage].Data[0] = getFData(obuf, 32+0*4+i*40);
                        nodedata[zone-1][pos][adpage].Data[1] = getFData(obuf, 32+1*4+i*40);
                        nodedata[zone-1][pos][adpage].Data[2] = getFData(obuf, 32+2*4+i*40);
                        nodedata[zone-1][pos][adpage].Data[3] = getFData(obuf, 32+3*4+i*40);
                        nodedata[zone-1][pos][adpage].Data[4] = getFData(obuf, 32+4*4+i*40);
                        nodedata[zone-1][pos][adpage].Data[5] = getFData(obuf, 32+5*4+i*40);
                        nodedata[zone-1][pos][adpage].Data[6] = getFData(obuf, 32+6*4+i*40);
                        nodedata[zone-1][pos][adpage].Data[7] = getFData(obuf, 32+7*4+i*40);
                        nodedata[zone-1][pos][adpage].Data[8] = getFData(obuf, 32+8*4+i*40);
                        nodedata[zone-1][pos][adpage].fin = 1;
                        
                        //System.out.println("\t\tData["+sor+"]["+i+"]["+j+"]");
                        //System.out.println("\t\t\tData[0] = "+nodedata[zone-1][pos][pagecnt].Data[0]);
                        //System.out.println("\t\t\tData[3] = "+nodedata[zone-1][pos][pagecnt].Data[3]);
                        //System.out.println("\t\t\tData[6] = "+nodedata[zone-1][pos][pagecnt].Data[6]);                        
                    }else
                    {
                        pos = nodelist[sor-ID_COMIED][i][j][1];
                        nodedata[zone-1][pos][pagecnt].Data[0] = getFData(obuf, 32+0*4+i*40);
                        nodedata[zone-1][pos][pagecnt].Data[1] = getFData(obuf, 32+1*4+i*40);
                        nodedata[zone-1][pos][pagecnt].Data[2] = getFData(obuf, 32+2*4+i*40);
                        nodedata[zone-1][pos][pagecnt].Data[3] = getFData(obuf, 32+3*4+i*40);
                        nodedata[zone-1][pos][pagecnt].Data[4] = getFData(obuf, 32+4*4+i*40);
                        nodedata[zone-1][pos][pagecnt].Data[5] = getFData(obuf, 32+5*4+i*40);
                        nodedata[zone-1][pos][pagecnt].Data[6] = getFData(obuf, 32+6*4+i*40);
                        nodedata[zone-1][pos][pagecnt].Data[7] = getFData(obuf, 32+7*4+i*40);
                        nodedata[zone-1][pos][pagecnt].Data[8] = getFData(obuf, 32+8*4+i*40);
                        nodedata[zone-1][pos][pagecnt].fin = 1;
                        
                        //System.out.println("\t\tData["+sor+"]["+i+"]["+j+"]");
                        //System.out.println("\t\t\tData[0] = "+nodedata[zone-1][pos][pagecnt].Data[0]);
                        //System.out.println("\t\t\tData[3] = "+nodedata[zone-1][pos][pagecnt].Data[3]);
                        //System.out.println("\t\t\tData[6] = "+nodedata[zone-1][pos][pagecnt].Data[6]);
                    }
                }
            }
        }
        samplefin += 1;
        
        int fincnt[] = new int[20];
        for(int ij=0;ij<20;ij++)
        {
            for(int i=0;i<zonecount;i++)
            {
                for(int j=0;j<nodecountmax;j++)
                {
                    if(nodedata[i][j][ij].fin == 1)
                        fincnt[ij] += 1;
                }
            }
        }
        System.out.println("\t\tData ready cnt "+fincnt[pagecnt]+" of "+fincntglobal+" at page "+pagecnt+".");
        System.out.println("\t\t\tpage0 = "+fincnt[0]+"; page1 = "+fincnt[1]+"; page2 = "+fincnt[2]+".");
        
        //if(samplefin == iedcount)
        if(fincnt[pagecnt] == fincntglobal)
        {
            samplefin = 0;
            System.out.println("\t\tCDM Cal.");
        
            float[] result = new float[3*zonecount];
            float ctmpr,ctmpi,caltmpr,caltmpi,thdr,thdi,thdtmp,thdt,thdangle;
            int faultflag = 0;
            int faultzone = 0;
            for(int i=0;i<zonecount;i++)
            {
                for(int k=0;k<3;k++)
                {
                    caltmpr = 0;
                    caltmpi = 0;
                    thdr = 0;
                    thdi = 0;
                    thdt = 0;
                    for(int j=0;j<nodecount[i];j++)
                    {
                        ctmpr = nodedata[i][j][pagecnt].Dir*nodedata[i][j][pagecnt].Data[k+3];
                        ctmpi = nodedata[i][j][pagecnt].Dir*nodedata[i][j][pagecnt].Data[k+6];
                        caltmpr += ctmpr;
                        caltmpi += ctmpi;
                        thdtmp =  (float) sqrt(ctmpr*ctmpr+ctmpi*ctmpi);
                        if(ctmpr != 0)
                            thdangle = (float) (atan(ctmpi/ctmpr)*180/(3.1415926)+90);
                        else
                            thdangle = 0;
                        thdt += thdtmp;
                        System.out.println("\t\t\tcinp=> "+(i+1)+" :: "+j+" :: "+(zonelist[i][j][0]+ID_COMIED)+" :: "+zonelist[i][j][1]+" => "+thdtmp+" :: "+thdangle+".");
                        //System.out.println("\t\t\tcinp=> "+nodedata[i][j][pagecnt].Dir+" :: "+nodedata[i][j][pagecnt].Data[k+3]+" :: "+nodedata[i][j][pagecnt].Data[k+6]+".");
                        //System.out.println("\t\t\tctmp=> "+ctmpr+" :: "+ctmpi+" :: "+caltmpr+" :: "+caltmpi+" :: "+thdtmp+" :: "+thdt+".");
                    }
                    result[i*3+k] = (float) sqrt(caltmpr*caltmpr+caltmpi*caltmpi);
                    float thd = thdt;
                    float percent;
                    if(thd > FaultBase)
                        percent = result[i*3+k]*100/thd;
                    else
                        percent = 0;
                    System.out.println("\t\tresult("+i+","+k+") = "+result[i*3+k]+", threshold = "+thd+", "+percent+".");
                    if((result[i*3+k] > (thd*FaultRate)) && (thd > FaultBase))
                    {
                        faultflag = 1;
                        FaultZone[i] += 1;
                        //faultzone = i+1;
                    }
                }
            }

            if(faultflag == 1)
            {
                for(int k=0;k<zonecount;k++)
                {
                    if(FaultZone[k]>0)
                    {
                        System.out.println("\t\tFaultOccurAtZone "+k+" <"+dev_nodeinfo.getGTime()+">.");
                        FaultZone[k] = 0;  //Auction! The switch will not open!
                        
                        if(FaultZone[k]>FaultDelay)
                        {
                            int[] iedsended = new int[100];
                            int iedsendedcnt = 0;
                            int iedsendedrst;
                            for(int i=0;i<nodecount[k];i++)
                            { 
                                if(nodedata[k][i][pagecnt].changed == 0)
                                {
                                    nodedata[k][i][pagecnt].changed = 1;
                                    devIEDdata[rcnt].des = (short) (zonelist[k][i][0]+ID_COMIED);
                                    iedsendedrst = 0;
                                    for(int j=0;j<iedsendedcnt;j++)
                                    {
                                        if(devIEDdata[rcnt].des == iedsended[j])
                                        {
                                            iedsendedrst = 1;
                                            break;
                                        }
                                    }
                                    if(iedsendedrst == 0)
                                    {
                                        iedsended[iedsendedcnt] = devIEDdata[rcnt].des;
                                        iedsendedcnt += 1;
                                        tmp = UIntToByteArray(k+1);
                                        System.arraycopy(tmp, 0, devIEDdata[rcnt].data, 0, 4);
                                        devIEDdata[rcnt].datalen = 4;
                                        devIEDdata[rcnt].ctrl = 0;                  
                                        rcnt++; 
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            for(int i=0;i<zonecount;i++)
            {
                for(int j=0;j<nodecountmax;j++)
                {
                    nodedata[i][j][pagecnt].fin = 0;
                }
            }
            
            if(syn == 1)
            {
                pagecnt += 1;
                if(pagecnt ==20)
                    pagecnt = 0;
            }
        }
        
        return rcnt;
    }      
}
