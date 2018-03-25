package CoSimulation;


public class TypeConvert {
    
    public static int ByteToInt(byte num) { 
        int result;
        if(num>=0)
            result = num;
        else
            result = num+256;
        
        return result; 
    }
       
    public static short ByteToShort(byte num) { 
        short result;
        if(num>=0)
            result = num;
        else
            result = (short) (num+256);
        
        return result; 
    }
    
    public static byte[] IntToByteArray(int num) { 
        byte[] result = new byte[4]; 
        result[0] = (byte)(num);
        result[1] = (byte)(num >>> 8);
        result[2] = (byte)(num >>> 16);
        result[3] = (byte)(num >>> 24);
        return result; 
    }
    
    public static int byteArrayToInt(byte[] num) {
        int result;
        result = ((num[3]&0xff)<<24) | ((num[2]&0xff)<<16) | ((num[1]&0xff)<<8) | (num[0]&0xff);
        return result;
    }
    
    public static byte[] UIntToByteArray(int num) { 
        byte[] result = new byte[4]; 
        result[0] = (byte)(num);
        result[1] = (byte)(num >>> 8);
        result[2] = (byte)(num >>> 16);
        result[3] = (byte)(num >>> 24);
        return result; 
    }
    
    public static int byteArrayToUInt(byte[] num) {
        int result;
        result = ((num[3]&0xff)<<24) | ((num[2]&0xff)<<16) | ((num[1]&0xff)<<8) | (num[0]&0xff);
        return result;
    }
    
    public static byte[] FloatToByteArray(float num) { 
        int tmp = Float.floatToIntBits(num); 
        byte[] result = UIntToByteArray(tmp);
        return result; 
    }
    
    public static float ByteArrayToFloat(byte[] num) { 
        int tmp = byteArrayToUInt(num);
        float result = Float.intBitsToFloat(tmp); 
        return result; 
    }
            
    public static byte[] IPAdressToByteArray(String num) { 
        String[] tmp =num.split("\\."); 
        byte[] result = new byte[4]; 
        for(int i=0;i<4;i++)
            result[i] = (byte)Integer.parseInt(tmp[i]);
        return result; 
    }
    
    public static String ByteArrayToIPAdress(byte[] num) { 
        String tmp;
        short[] datatmp = new short[4];
        for(int i=0;i<4;i++)
        {
            if(num[i]<0)
                datatmp[i] = (short) (num[i] + 256);
            else
                datatmp[i] = num[i];
        }       
        tmp = Short.toString(datatmp[0])+"."+Short.toString(datatmp[1])+"."+Short.toString(datatmp[2])+"."+Short.toString(datatmp[3]);
        return tmp;
    }
    
    public static String UIntToIPAdress(int num) { 
        byte[] tmp = UIntToByteArray(num);
        String result = ByteArrayToIPAdress(tmp);
        return result; 
    }
        
    public static int IPAdressToUInt(String num) { 
        byte[] tmp = IPAdressToByteArray(num);
        int result = byteArrayToUInt(tmp);
        return result; 
    }   
}
