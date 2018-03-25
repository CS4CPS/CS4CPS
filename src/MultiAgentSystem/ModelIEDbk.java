package MultiAgentSystem;

import CoSimulation.TypeConvert;
import static CoSimulation.TypeConvert.*;
import static CoSimulation.drv_ComAgent.DRV_AGENT_LOCK_POS;
import CoSimulation.drv_sstp;
import static CoSimulation.sysconfig.ID_COMIED;
import static CoSimulation.sysconfig.ID_COMPOWERFFACTORY;
import static CoSimulation.sysconfig.SSTP_DEF_CMD_PROCESS;
import CoSimulation.xmlImpl;

abstract class ModelIEDbk {
    
    public class SWData {
        int id;
        float[] mdata;
        int[] ctrl;
        int sw;
        int sampled;
        int changed;
        
        public SWData() {
            mdata = new float[36];
            ctrl = new int[12];
        }  
    }
    SWData[] dev_swdata;
    
    short devID;
    
    int datalen,paralen;
    
    int swCount;    
    ModelSw[] dev_sw;
    Algorithm dev_cal;

    float[] U;
    int Zone;
    int zaID;
    int[] nbID;
    int nbCount,nbActive;
    
    int sendflag;
    int sendID;
    
    int LocalTime;
    
    float ilimit;
    int VoltageBase, swBase, swAddr, CurrentAddr,swSize;
    
    public ModelIEDbk(short id)
    {
        devID = id;
        U = new float[3]; 
        dev_swdata = new SWData[128];
        for(int i=0;i<128;i++)
            dev_swdata[i] = new SWData();
    }
        
    xmlImpl xmlfile;
    public static final int MODELIED_IED_IN_ZONE_MAX  = 128;
        
    public void init(String fpth, Nodeinfo dev_nodeinfo){
        
        dev_cal = new Algorithm();
        xmlfile=new xmlImpl();
        xmlfile.init(fpth);
        
        configIED(fpth, "CIED",Integer.toString(devID-ID_COMIED), dev_nodeinfo);
        dev_sw = new ModelSw[swCount];
        for(int i=0;i<swCount;i++)
        {
            dev_sw[i] = new ModelSw();
            configSW("SW",i,dev_sw[i]);
        }
    }
    
    public void configIED(String fpth, String name, String id, Nodeinfo dev_nodeinfo){
        /*
        xmlfile.findIED(name,id);
        String tmp = xmlfile.GetIEDValue("swCount",null);
        swCount = Integer.parseInt(tmp);
        
        tmp = xmlfile.GetIEDValue("zName",null);
        dev_nodeinfo.zName = tmp;
        tmp = dev_nodeinfo.zName.substring(4);
        dev_nodeinfo.agentZA = Integer.parseInt(tmp);
        
        tmp = xmlfile.GetIEDValue("datalen",null);
        datalen = Integer.parseInt(tmp);
        tmp = xmlfile.GetIEDValue("paralen",null);
        paralen = Integer.parseInt(tmp);*/
    }
    
    public void configSW (String name, int id, ModelSw sw){
        /*
        xmlfile.findSW(name,Integer.toString(id));
        
        sw.devID = (short) id;
        String tmp = xmlfile.GetSWValue("measure",Integer.toString(id));
        sw.swActiv = Integer.parseInt(tmp);                    
        tmp = xmlfile.GetSWValue("measure",Integer.toString(id));
        sw.limit = Float.parseFloat(tmp);
        tmp = xmlfile.GetSWValue("measure",Integer.toString(id));
        sw.delay = Integer.parseInt(tmp);  */
    }
    
    public static final int MODELIED_EVENT_INIT  = 0;
    public static final int MODELIED_EVENT_AD  = 1;
    public static final int MODELIED_EVENT_GETMSG  = 2;
    
    public int BaseCal(int interrupt, Nodeinfo dev_nodeinfo, drv_sstp sstphead, byte[] obuf, ModelIEDdata[] devIEDdata)
    {
        int i,j,res;
        int rcnt = 0;
        byte[] tmp = new byte[4];
        float[] cpI = new float[3];
        byte[] tmpb = new byte[4];
        float tmpf;

        int[] cmd = new int[4];
        byte[] tpbuf = new byte[4];
        for(i=0;i<4;i++) {
            for(j=0;j<4;j++)
                tpbuf[j] = obuf[i*4+j];
            cmd[i] = TypeConvert.byteArrayToUInt(tpbuf);
        }
        
        if(dev_nodeinfo.agentZA != devID)  //node
        {
            System.out.println("    IED::Node.");
            rcnt = NodeCal(interrupt, dev_nodeinfo, sstphead, cmd, obuf, devIEDdata);
        }else                           //ZA
        {
            System.out.println("    IED::ZA.");
            rcnt = ZACal(interrupt, dev_nodeinfo, sstphead, cmd, obuf, devIEDdata);
        }
        
        return rcnt;
    }  
    
    public int NodeCal(int interrupt, Nodeinfo dev_nodeinfo, drv_sstp sstphead, int[] cmd, byte[] obuf, ModelIEDdata[] devIEDdata)
    {
        int i,j,res;
        int rcnt = 0;
        byte[] tmp = new byte[4];
        float[] cpI = new float[3];
        byte[] tmpb = new byte[4];
        float tmpf;
        int pos;        
        int desaddr, soraddr,totlen;

        switch(interrupt)
        {
            case MODELIED_EVENT_INIT:
                System.out.println("        EVENT::INIT.");
                break;
            case MODELIED_EVENT_AD:
                System.out.println("        EVENT::AD.");

                devIEDdata[rcnt].des = (short) dev_nodeinfo.agentZA;

                devIEDdata[rcnt].cmd[0] = SSTP_DEF_CMD_PROCESS;               
                devIEDdata[rcnt].cmd[1] = cmd[DRV_AGENT_LOCK_POS];
                devIEDdata[rcnt].cmd[2] = 0;
                devIEDdata[rcnt].cmd[3] = 0;
                
                System.arraycopy(obuf, 16, devIEDdata[rcnt].data, 0, 4);
                System.arraycopy(obuf, 20, devIEDdata[rcnt].data, 4, 4);
                System.arraycopy(obuf, 24, devIEDdata[rcnt].data, 8, 4); 
                soraddr = 32;
                desaddr = 12;
                totlen = 12;
                for(i=0;i<swCount;i++)
                {
                    System.arraycopy(obuf, soraddr, devIEDdata[rcnt].data, desaddr, 4);
                    System.arraycopy(obuf, soraddr+4, devIEDdata[rcnt].data, desaddr+4, 4);
                    System.arraycopy(obuf, soraddr+8, devIEDdata[rcnt].data, desaddr+8, 4); 
                    soraddr = soraddr + 20;
                    desaddr = desaddr + 12;
                    totlen = totlen + 12;
                    //System.arraycopy(obuf, 32, devIEDdata[rcnt].data, 0, 4);
                    //System.arraycopy(obuf, 36, devIEDdata[rcnt].data, 4, 4);
                    //System.arraycopy(obuf, 40, devIEDdata[rcnt].data, 8, 4);   
                }
                
                devIEDdata[rcnt].datalen = totlen;
                devIEDdata[rcnt].ctrl = 0;
                rcnt++;
                break;
            case MODELIED_EVENT_GETMSG:
                System.out.println("        EVENT::GETMSG.");

                devIEDdata[rcnt].des = ID_COMPOWERFFACTORY;

                devIEDdata[rcnt].cmd[0] = SSTP_DEF_CMD_PROCESS;               
                devIEDdata[rcnt].cmd[1] = cmd[DRV_AGENT_LOCK_POS];
                devIEDdata[rcnt].cmd[2] = 0;
                devIEDdata[rcnt].cmd[3] = 0;

                pos = 0;
                for(i=0;i<swCount;i++)
                {
                    System.arraycopy(obuf, 16+pos, devIEDdata[rcnt].data, pos, 4);
                    pos = pos + 4;
                }

                devIEDdata[rcnt].datalen = swCount*4;
                devIEDdata[rcnt].ctrl = 1;
                rcnt++;                   
                break;      
        }
        
        return rcnt;
    }  
    
    public int ZACal(int interrupt, Nodeinfo dev_nodeinfo, drv_sstp sstphead, int[] cmd, byte[] obuf, ModelIEDdata[] devIEDdata)
    {
        int i,j,k,res;
        int rcnt = 0;
        byte[] tmp = new byte[4];
        float[] cpI = new float[3];
        byte[] tmpb = new byte[4];
        float tmpf;
        int pos;
        int desaddr, soraddr,totlen;
        
        switch(interrupt)
        {
            case MODELIED_EVENT_INIT:
                System.out.println("        EVENT::INIT.");
                break;
            case MODELIED_EVENT_AD:
                System.out.println("        EVENT::AD.");

                pos = dev_nodeinfo.findpos(devID);
                dev_swdata[pos].sw = swCount;
                dev_swdata[pos].id = devID;
                
                dev_swdata[pos].mdata[0] = getFData(obuf,16); 
                dev_swdata[pos].mdata[1] = getFData(obuf,20); 
                dev_swdata[pos].mdata[2] = getFData(obuf,24);      
                
                soraddr = 32;
                desaddr = 3;
                for(i=0;i<dev_swdata[pos].sw;i++)
                {
                    dev_swdata[pos].mdata[desaddr] = getFData(obuf,soraddr); 
                    dev_swdata[pos].mdata[desaddr+1] = getFData(obuf,soraddr+4); 
                    dev_swdata[pos].mdata[desaddr+2] = getFData(obuf,soraddr+8);                      
                    soraddr = soraddr + 20;
                    desaddr = desaddr + 3;
                }    
                dev_swdata[pos].sampled = 1;
                //System.arraycopy(obuf, 52, tmpb, 0, 4);
                //tmpf = ByteArrayToFloat(tmpb);
                //dev_sw[1].I[0] = tmpf; 
                //System.arraycopy(obuf, 56, tmpb, 0, 4);
                //tmpf = ByteArrayToFloat(tmpb);
                //dev_sw[1].I[1] = tmpf;
                //System.arraycopy(obuf, 60, tmpb, 0, 4);
                //tmpf = ByteArrayToFloat(tmpb);
                //dev_sw[1].I[2] = tmpf;                    
                break;
            case MODELIED_EVENT_GETMSG:
                System.out.println("        EVENT::GETMSG.");
                
                pos = dev_nodeinfo.findpos(sstphead.GetSor());
                dev_swdata[pos].sw = (sstphead.GetLen()-16-12)/12;
                dev_swdata[pos].id = sstphead.GetSor();
                
                for(i=0;i<(dev_swdata[pos].sw+1)*3;i++)
                    dev_swdata[pos].mdata[i] = getFData(obuf,16+i*4); 
                dev_swdata[pos].sampled = 1;
                
                int samplefin = 1;
                for(i=0;i<dev_nodeinfo.NodesCnt;i++)
                {
                    if(dev_swdata[i].sampled != 1)
                        samplefin = 0;
                }
                
                if(samplefin == 1)
                {
                    System.out.println("        LOF Cal.");
                    for(i=0;i<dev_nodeinfo.NodesCnt;i++)
                        dev_swdata[i].sampled = 0;
                                        
                    int matrixX, matrixY1, matrixY2;
                    matrixX = 0;
                    for(i=0;i<dev_nodeinfo.NodesCnt;i++)
                        matrixX = matrixX + dev_swdata[i].sw;
                    matrixY1 = 3;
                    matrixY2 = 3;
                                        
                    float[][] datamatrixf = new float[matrixX][matrixY1];
                    float[][] datamatrix = new float[matrixX][matrixY1];
                    pos = 0;
                    for(i=0;i<dev_nodeinfo.NodesCnt;i++)
                    {
                        for(j=0;j<dev_swdata[i].sw;j++)
                        {
                            datamatrixf[pos][0] = dev_swdata[i].mdata[0];
                            datamatrixf[pos][1] = dev_swdata[i].mdata[1];
                            datamatrixf[pos][2] = dev_swdata[i].mdata[2];
                            datamatrix[pos][0] = dev_swdata[i].mdata[3+j*3+0];
                            datamatrix[pos][1] = dev_swdata[i].mdata[3+j*3+1];
                            datamatrix[pos][2] = dev_swdata[i].mdata[3+j*3+2];       
                            pos = pos + 1;
                        }
                    }
                    
                    float[][] rematrix= {
                        {0,1,0,-1,0,0,0,0,0,0,0,0,0,0,0},
                        {0,0,0,0,1,0,-1,0,0,0,0,0,0,0,0},
                        {0,0,0,0,0,0,0,1,0,0,-1,0,0,0,0},
                        {0,0,0,0,0,0,0,0,1,0,0,0,-1,0,0},
                    };
                    float[] RIM = {1,0,-1,0,0,-1,0,0,0,-1,0,-1,0,-1,-1};
                    
                    float[] lof = dev_cal.cal(rematrix, RIM, datamatrixf, datamatrix);
                    res = 0;

                    if(lof != null)
                    {
                        pos = 0;
                        String outstr = "        ::lof=";
                        for(i=0;i<dev_nodeinfo.NodesCnt;i++)
                        {
                            for(j=0;j<dev_swdata[i].sw;j++)
                            {
                                outstr += Float.toString(lof[pos])+" ";
                                if(lof[pos] > 1)
                                {
                                    dev_swdata[i].ctrl[j] = 0;
                                    dev_swdata[i].changed = 1;
                                    res = 1;
                                }else
                                {
                                    dev_swdata[i].ctrl[j] = 1;
                                }

                                pos = pos + 1;
                            }
                        }
                        System.out.println(outstr);
                    }
                    
                    if(res == 1)
                    {
                        for(i=0;i<dev_nodeinfo.NodesCnt;i++)
                        {
                            if(dev_swdata[i].changed == 1)
                            {
                                if(i == dev_nodeinfo.findpos(devID))
                                {
                                    devIEDdata[rcnt].des = ID_COMPOWERFFACTORY;

                                    devIEDdata[rcnt].cmd[0] = SSTP_DEF_CMD_PROCESS;               
                                    devIEDdata[rcnt].cmd[1] = cmd[DRV_AGENT_LOCK_POS];
                                    devIEDdata[rcnt].cmd[2] = 0;
                                    devIEDdata[rcnt].cmd[3] = 0;

                                    pos = 0;
                                    for(j=0;j<dev_swdata[i].sw;j++)
                                    {
                                        tmp = UIntToByteArray((int)dev_swdata[i].ctrl[j]);
                                        System.arraycopy(tmp, 0, devIEDdata[rcnt].data, pos, 4);
                                        pos = pos + 4;
                                    }

                                    devIEDdata[rcnt].datalen = dev_swdata[i].sw*4;
                                    devIEDdata[rcnt].ctrl = 1;
                                    rcnt++;  
                                }else
                                {
                                    devIEDdata[rcnt].des = (short) dev_swdata[i].id;

                                    pos = 0;
                                    for(j=0;j<dev_swdata[i].sw;j++)
                                    {
                                        tmp = UIntToByteArray((int)dev_swdata[i].ctrl[j]);
                                        System.arraycopy(tmp, 0, devIEDdata[rcnt].data, pos, 4);
                                        pos = pos + 4;
                                    }

                                    devIEDdata[rcnt].datalen = dev_swdata[i].sw*4;
                                    devIEDdata[rcnt].ctrl = 0;                  
                                    rcnt++; 
                                }
                            }
                        }
                    }
                }
                break;           
        }
        
        return rcnt;
    }  
    
    public float getFData(byte[] obuf,int pos)
    {
        byte[] tmpb = new byte[4];
        System.arraycopy(obuf, pos, tmpb, 0, 4);
        return ByteArrayToFloat(tmpb);
    }  
    
    int[][] AdjMatrix;
    public  void AdjacentMatrixCreat()
    {   
        int dim = 15;        
        AdjMatrix = new int[dim][dim];
        for(int i=0;i<dim;i++)
            for(int j=0;j<dim;j++)
                AdjMatrix[i][j] = 0;
        
        AdjMatrix[0][1] = 1;
        AdjMatrix[0][2] = 1;
        
        AdjMatrix[1][0] = 1;
        AdjMatrix[1][2] = 1;
        AdjMatrix[1][3] = 1;
        
        AdjMatrix[2][0] = 1;
        AdjMatrix[2][1] = 1;
        
        AdjMatrix[3][1] = 1;
        AdjMatrix[3][4] = 1;
        AdjMatrix[3][5] = 1;
        
        AdjMatrix[4][3] = 1;
        AdjMatrix[4][5] = 1;
        AdjMatrix[4][6] = 1;
        
        AdjMatrix[5][3] = 1;
        AdjMatrix[5][4] = 1;
        
        AdjMatrix[6][4] = 1;
        AdjMatrix[6][7] = 1;
        AdjMatrix[6][8] = 1;
        AdjMatrix[6][9] = 1;
        
        AdjMatrix[7][6] = 1;
        AdjMatrix[7][8] = 1;
        AdjMatrix[7][9] = 1;
        AdjMatrix[7][10] = 1;
        
        AdjMatrix[8][6] = 1;
        AdjMatrix[8][7] = 1;
        AdjMatrix[8][9] = 1;
        AdjMatrix[8][12] = 1;
        
        AdjMatrix[9][6] = 1;
        AdjMatrix[9][8] = 1;
        AdjMatrix[9][7] = 1;
        
        AdjMatrix[10][7] = 1;
        AdjMatrix[10][11] = 1;
        
        AdjMatrix[11][10] = 1;
        
        AdjMatrix[12][8] = 1;
        AdjMatrix[12][13] = 1;
        AdjMatrix[12][14] = 1;
        
        AdjMatrix[13][12] = 1;
        AdjMatrix[13][14] = 1;
        
        AdjMatrix[14][12] = 1;
        AdjMatrix[14][13] = 1;
    }
}
