package CoSimulation;

import static CoSimulation.TypeConvert.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import static java.lang.Thread.sleep;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ModelConfig {
    
    public static final int MODELCONFIG_NODELIST_SWLEN  = 8;
    public static final int MODELCONFIG_NODELIST_ZONELEN  = 6;
    
    public class DataDescrib {
        public String asName;
        public int Type;    
        public String Value;
    }
        
    public class SwitchDescrib {
        public int[] asZoneName;
        public int[] asDir;
        public SwitchDescrib() {
            asZoneName = new int[MODELCONFIG_NODELIST_ZONELEN];
            asDir = new int[MODELCONFIG_NODELIST_ZONELEN];
        }
    }
        
    public class IedDescrib {
        public String Name;
        public int DevId, asType, asIsMaster, asDelayProcess, asDelaySampling;
        public int Mbase, Pbase, Cbase, Sbase;
        public int Mcount, Pcount, Ccount, SCount;
        public SwitchDescrib[] Switch;
    }
    
    xmlImpl xmlfile;
    public IedDescrib []devIED;
    public int IEDCount,Tmslen, Tpalen, Tctlen, Tswlen;
    public int SamplingPeriod;
    
    DataDescrib[] Measure;
    DataDescrib[] Para;
    DataDescrib[] Ctrl;
    
    public void XMLconfig(String fpth){
        
        Node iedNode, swNode, zNode;
        int cnttmp,zntmp;
                    
        xmlfile=new xmlImpl();
        xmlfile.init(fpth);
        iedNode = xmlfile.findIED("GENERAL",null);
        String tmp = xmlfile.GetValue(iedNode,"IEDCount",null);
        IEDCount = Integer.parseInt(tmp);
        tmp = xmlfile.GetValue(iedNode,"SamplingPeriod",null);
        SamplingPeriod = Integer.parseInt(tmp);
        
        devIED = new IedDescrib[IEDCount];
        for(int i=0;i<IEDCount;i++)
            devIED[i] = new IedDescrib();
        Measure = new DataDescrib[4096];
        Para = new DataDescrib[4096];
        Ctrl = new DataDescrib[4096];
        for(int i=0;i<4096;i++)
        {
            Measure[i] = new DataDescrib();
            Para[i] = new DataDescrib();
            Ctrl[i] = new DataDescrib();
        }
        Tmslen = 0;
        Tpalen = 0;
        Tctlen = 0;
        Tswlen = 0;

        System.out.println("XML Init::------------------------------.");
        for(int i=0;i<IEDCount;i++)
        {
            System.out.println("----- CIED"+i+" -----");
            iedNode = xmlfile.findIED("CIED",Integer.toString(i));
            devIED[i].Name = xmlfile.GetValue(iedNode, "Name", null);
            tmp = xmlfile.GetValue(iedNode, "DevId", null);
            devIED[i].DevId = Integer.parseInt(tmp); 
            tmp = xmlfile.GetValue(iedNode, "asType", null);
            devIED[i].asType = Integer.parseInt(tmp); 
            tmp = xmlfile.GetValue(iedNode, "asIsMaster", null);
            devIED[i].asIsMaster = Integer.parseInt(tmp); 
            tmp = xmlfile.GetValue(iedNode, "asDelayProcess", null);
            devIED[i].asDelayProcess = Integer.parseInt(tmp); 
            tmp = xmlfile.GetValue(iedNode, "asDelaySampling", null);
            devIED[i].asDelaySampling = Integer.parseInt(tmp); 
            System.out.println(">>"+devIED[i].Name+"::"+devIED[i].DevId+"::"+devIED[i].asType+"::"+devIED[i].asIsMaster+".");
            devIED[i].Mbase = Tmslen;
            devIED[i].Pbase = Tpalen;
            devIED[i].Cbase = Tctlen;
            datainit(iedNode, i);
            
            cnttmp = xmlfile.getCount(iedNode, "Switch");
            devIED[i].Sbase = Tswlen;
            devIED[i].SCount = cnttmp;
            Tswlen = Tswlen + cnttmp;
            devIED[i].Switch = new SwitchDescrib[cnttmp];
            for(int j=0;j<cnttmp;j++)
            {
                System.out.println("  >> Switch"+j);
                devIED[i].Switch[j] = new SwitchDescrib();
                swNode = xmlfile.findNode(iedNode,"Switch",Integer.toString(j));
                zntmp = xmlfile.getCount(swNode, "asZoneName");
                for(int k=0;k<zntmp;k++)
                {
                    tmp = xmlfile.GetValue(swNode, "asZoneName", Integer.toString(k));
                    devIED[i].Switch[j].asZoneName[k] = Integer.parseInt(tmp);
                    tmp = xmlfile.GetAttr(swNode, "asZoneName", "asDir", Integer.toString(k));
                    devIED[i].Switch[j].asDir[k] = Integer.parseInt(tmp);
                    System.out.println("    >> asZoneName "+k+"::"+devIED[i].Switch[j].asZoneName[k]+"::"+devIED[i].Switch[j].asDir[k]+".");
                }
                datainit(swNode, i);
            }
        }
        System.out.println("----- summary -----");
        System.out.println("MData: "+Tmslen+"; PData: "+Tpalen+"; CData: "+Tctlen+"; SCount: "+Tswlen+".");
        System.out.print("\n\n");
    }
    
    public void datainit(Node iedNode, int cnt)
    {
        int cnttmp;
        
        System.out.println("    >> MData.");
        cnttmp = dataprocess(iedNode, "Measure", Tmslen, Measure);
        devIED[cnt].Mcount += cnttmp;
        Tmslen = Tmslen + cnttmp;
        System.out.println("    >> PData.");
        cnttmp = dataprocess(iedNode, "Para", Tpalen, Para);
        devIED[cnt].Pcount += cnttmp;
        Tpalen = Tpalen + cnttmp; 
        System.out.println("    >> CData.");
        cnttmp = dataprocess(iedNode, "Ctrl", Tctlen, Ctrl);
        devIED[cnt].Ccount += cnttmp;
        Tctlen = Tctlen + cnttmp; 
    }
    
    public int dataprocess(Node iedNode, String Mem, int base, DataDescrib[] databuf)
    {
        int cnttmp;
        String tmp;
        
        cnttmp = xmlfile.getCount(iedNode, Mem);
        for(int j=0;j<cnttmp;j++)
        {
            tmp = xmlfile.GetAttr(iedNode, Mem, "asName", Integer.toString(j));
            databuf[base+j].asName = tmp;                
            tmp = xmlfile.GetAttr(iedNode, Mem, "Type", Integer.toString(j));
            databuf[base+j].Type = Integer.parseInt(tmp); 
            tmp = xmlfile.GetValue(iedNode, Mem, Integer.toString(j));
            databuf[base+j].Value = tmp; 
            System.out.println("      "+databuf[base+j].asName+"::"+databuf[base+j].Type+"::"+databuf[base+j].Value+".");
        }
        
        try {
            sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(ModelConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return cnttmp;
    }
}
