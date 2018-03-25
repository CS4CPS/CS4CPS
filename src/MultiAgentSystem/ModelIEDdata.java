package MultiAgentSystem;

public class ModelIEDdata {

    short des;
    int[] cmd;
    byte[] data;
    int[] sw;
    int datalen;
    int ctrl;

    public ModelIEDdata(){
        cmd = new int[4];
        data = new byte[256];
        sw = new int[32];
    }
    
    public short getDes(){
        return des;
    }
    
    public byte[] getData(){
        return data;
    }
    
    public int getDatalen(){
        return datalen;
    }
    
    public int getCtrl(){
        return ctrl;
    }    
}
