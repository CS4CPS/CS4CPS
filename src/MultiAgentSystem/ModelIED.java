package MultiAgentSystem;

import CoSimulation.ModelConfig;
import static CoSimulation.TypeConvert.ByteArrayToFloat;
import static CoSimulation.TypeConvert.FloatToByteArray;
import static CoSimulation.TypeConvert.IntToByteArray;
import static CoSimulation.TypeConvert.byteArrayToInt;
import CoSimulation.drv_sstp;

public class ModelIED {
    public void init(short id, ModelConfig devConfig, Nodeinfo dev_nodeinfo){
    
    }  
        
    public void reset(short id, ModelConfig devConfig, Nodeinfo dev_nodeinfo){

    }
        
    public int intr_ADSample(Nodeinfo dev_nodeinfo, drv_sstp sstphead, int[] cmd, byte[] obuf, ModelIEDdata[] devIEDdata){
    
        return 0;
    }  
    
    public int intr_GetMSG(Nodeinfo dev_nodeinfo, drv_sstp sstphead, int[] cmd, byte[] obuf, ModelIEDdata[] devIEDdata){
    
        return 0;
    }  
    
    public float getFData(byte[] obuf,int pos)
    {
        byte[] tmpb = new byte[4];
        System.arraycopy(obuf, pos, tmpb, 0, 4);
        return ByteArrayToFloat(tmpb);
    }  
    
    public void setFData(byte[] obuf,int pos, float data)
    {
        byte[] tmpb = FloatToByteArray(data);
        System.arraycopy(tmpb, 0, obuf, pos, 4);
    }  
    
    public int getIData(byte[] obuf,int pos)
    {
        byte[] tmpb = new byte[4];
        System.arraycopy(obuf, pos, tmpb, 0, 4);
        return byteArrayToInt(tmpb);
    }    
    
    public void setIData(byte[] obuf,int pos, int data)
    {
        byte[] tmpb = IntToByteArray(data);
        System.arraycopy(tmpb, 0, obuf, pos, 4);
    }      
}
