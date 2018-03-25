package MultiAgentSystem;

public class ModelSw {
    
    short devID;
    
    int status;
    int cnt;
    float limit;
    int delay;
    String name;
    float[] I;
    int isclose;
    int swActiv;
    
    public  ModelSw() {
        I = new float[3];
    }
    
    public void reset(float lim, int dy)
    {
        limit = lim;
        delay = dy;
        status = 0;
        cnt = 0;
    }
    
    public void reset()
    {
        status = 0;
        cnt = 0;
    }    
    
    public int measure(float[] values)
    {
        int res = 0;
        if(values[0]>limit || values[1]>limit || values[2]>limit)
        {
            if(cnt>=delay)
            {
                if(status == 0)
                    res = 1;
            }else
                cnt++;

        }
        return res;
    }    
}
