package CoSimulation;

import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class drv_ComStream extends drv_ComChannel {
        
    Socket socket;
    InputStream in;
    OutputStream out;
    
    public int init(int setmod, short setaddr, String server, int servPort) throws IOException {
        super.init(setmod, setaddr);
        chtype = CHANNEL_SOCKET;
        
        try
        { 
        System.out.println("SYSTEM\tLocal IP Address = " + InetAddress.getLocalHost());
        } catch (UnknownHostException e)
        { 
        e.printStackTrace();
        }
        
        socket = new Socket(server, servPort);
        in = socket.getInputStream();
        out = socket.getOutputStream();
        return 0;
    }
    
    public int fin() throws IOException {

        socket.close();

        return 0;
    }
    
    public byte[] getRecebuf()
    {
        byte[] prottmp = new byte[sendhead.GetLen()];
        System.arraycopy(protbuf, 0, prottmp, 0, sendhead.GetLen());
        return prottmp;
    }
    
    public int commandSend(short Des, int[] cmd) {
        int ressend,res;
        ressend = super.commandSend(Des, cmd, sendbuf);
        res = DataSend(ressend);
        return res;        
    } 
    
    public int RcmdSend(short Des, int[] cmd) {        
        int ressend,res;
        ressend = super.commandSend(Des, cmd, sendbuf);
        res = DataSend(ressend);
        return res;
    } 
    
    public int DataSend(int ressend) {        
        
        int resrece, resprot;
        int res = 0;
        
        if(ressend > 0) {
            try {
                //String hexString="";
                //for(int i=0;i<ressend;i++)
                //    hexString+= Integer.toHexString(0xff&sendbuf[i])+",";
                //System.out.println("CStream::DataSend::Send: " + hexString);                
                out.write(sendbuf, 0, ressend);
                
                resrece = in.read(recebuf);
                //hexString="";
                //for(int i=0;i<resrece;i++)
                //    hexString+= Integer.toHexString(0xff&recebuf[i])+",";
                //System.out.println("CStream::DataSend::Rece: " + hexString);               
                if(resrece > 0) {
                    resprot = resolve(recebuf,resrece,protbuf);
                    if(resprot > 0) {
                        byte[] rep = new byte[4];
                        System.arraycopy(protbuf, 12,rep, 0, 4);
                        res = TypeConvert.byteArrayToUInt(rep); 
                    }
                }
            } catch (Exception e) {
                res = -1;
                System.out.println("::cmd falure::"+e.getMessage());
            }
        }
        return res;
    } 
}
