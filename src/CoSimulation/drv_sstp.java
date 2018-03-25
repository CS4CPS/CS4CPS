package CoSimulation;

import CoSimulation.TypeConvert;

public class drv_sstp {

    short head_sor;
    short head_des;
    short head_type;
    short head_flag;
    short head_len;
    
    public void SetSor(short value){head_sor = value;}
    public short GetSor(){return head_sor;}
    public void SetDes(short value){head_des = value;}
    public short GetDes(){return head_des;}
    public void SetType(short value){head_type = value;}
    public short GetType(){return head_type;}
    public void SetFlag(short value){head_flag = value;}
    public short GetFlag(){return head_flag;}   
    public void SetLen(short value){head_len = value;}
    public short GetLen(){return head_len;} 
    
    public static final short SSTP_HEAD_ID  = 0x68;
    public static final short SSTP_TAIL_ID = 0x16;
    
    public static final short DRV_SSTP_CMD_CGET  = 0x11;
    public static final short DRV_SSTP_CMD_RDATA  = 0x14;
    public static final short DRV_SSTP_CMD_CSET  = 0x31;
    public static final short DRV_SSTP_CMD_WDATA  = 0x34;
    public static final short DRV_SSTP_CMD_CGET_R  = 0x91;
    public static final short DRV_SSTP_CMD_RDATA_R  = 0x94;
    public static final short DRV_SSTP_CMD_CSET_R  = 0xB1;
    public static final short DRV_SSTP_CMD_WDATA_R  = 0xB4;
    
    public static final short DRV_SSTP_IDXDS = 4;    
    public static final short DRV_SSTP_IDXDL = 16;
    
    //cset
    public static final short DRV_SSTP_CMD_POS  = 0;

    //rw data
    public static final short DRV_SSTP_DATA_BLK  = 0;
    public static final short DRV_SSTP_DATA_ADDR  = 1;
    public static final short DRV_SSTP_DATA_LEN  = 2;
    public static final short DRV_SSTP_DATA_PAR  = 3;

    //rw evt
    public static final short DRV_SSTP_EVT_CLASS  = 0;
    public static final short DRV_SSTP_EVT_ADDR  = 1;
    public static final short DRV_SSTP_EVT_LEN  = 2;

    //response
    public static final short DRV_SSTP_RSP_POS  = 3;
    public static final short DRV_SSTP_RSP_OK  = 1;
    public static final short DRV_SSTP_RSP_ERR  = 10;
    
    public static final short DRV_SSTP_MASTER  = 1;
    public static final short DRV_SSTP_SLAYER  = 2;

    int master,localaddr,flag,redeal;
    
    public int init(int setmod, short setaddr) {
        master = setmod;
        localaddr = setaddr;
        flag = 0;
        redeal = 0;
        return 0;
    }
    
    public void setaddr(short setaddr) {
        localaddr = setaddr;
    }
        
    public int fill(byte[] rbuf, byte[] sbuf) {
	int i,j,sum;

	sbuf[0] = (byte)SSTP_HEAD_ID;
	sbuf[1] = (byte)head_sor;
	sbuf[2] = (byte)head_des;
	sbuf[3] = (byte)head_type;
        if(master == DRV_SSTP_MASTER)
            sbuf[4] = (byte)(flag + 1);
        else
            sbuf[4] = (byte)head_flag;
        sbuf[5] = (byte)head_len;
        
	for(i=0;i<head_len;i++)
                sbuf[6+i] = rbuf[i];
        
	sum = 0;
        for(i=0;i<(6+head_len);i++)
		sum += sbuf[i];
        sbuf[6+head_len] = (byte)sum;
        sbuf[6+head_len+1] = SSTP_TAIL_ID;

        return (head_len+8);   
    }
    
    public int fill(int[] cbuf, byte[] sbuf) {
	int i,j,sum;

	sbuf[0] = (byte)SSTP_HEAD_ID;
	sbuf[1] = (byte)head_sor;
	sbuf[2] = (byte)head_des;
	sbuf[3] = (byte)head_type;
        if(master == DRV_SSTP_MASTER)
            sbuf[4] = (byte)(flag + 1);
        else
            sbuf[4] = (byte)head_flag;
        sbuf[5] = (byte)head_len;
        
        byte[] tmp_int = new byte[4];
	for(i=0;i<head_len/4;i++) {
            tmp_int = TypeConvert.UIntToByteArray(cbuf[i]);
            for(j=0;j<4;j++)
                sbuf[6+i*4+j] = tmp_int[j];
        }
        
	sum = 0;
        for(i=0;i<(6+head_len);i++)
		sum += sbuf[i];
        sbuf[6+head_len] = (byte)sum;
        sbuf[6+head_len+1] = SSTP_TAIL_ID;

        return (head_len+8);        
    }
    
    public int resolve(byte[] buf,int len, byte[] sbuf) {
        
        int sbht = (short)buf[0];
        if(sbht != SSTP_HEAD_ID)
            return 0;
        
        if(len<8)
            return 0;
        
        int tlen = TypeConvert.ByteToInt(buf[5])+8;
        if(tlen != len)
            return 0;
        
        sbht = (short)buf[len-1];
        if(sbht != SSTP_TAIL_ID)
            return 0;
        
	int sum = SSTP_HEAD_ID;
        head_sor = TypeConvert.ByteToShort(buf[1]);
        sum += head_sor;
        head_des = TypeConvert.ByteToShort(buf[2]);
        sum += head_des;
        head_type = TypeConvert.ByteToShort(buf[3]);
        sum += head_type;
        head_flag = TypeConvert.ByteToShort(buf[4]);
        sum += head_flag;
        head_len = TypeConvert.ByteToShort(buf[5]);
        sum += head_len;

        for(int i=0;i<head_len;i++) {
            sbuf[i] = buf[6+i];
            sum += TypeConvert.ByteToInt(sbuf[i]);
	}

	if((sum&0xff) == (buf[len-2]&0xff))
            return 1;
        else
            return 0;
    }
}
