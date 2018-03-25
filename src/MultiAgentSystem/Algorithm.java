package MultiAgentSystem;

import static java.lang.Math.sqrt;

public class Algorithm {
    
    public float[] cal(float[][] nm, float[] RIM, float[][] fm, float[][] dm)
    {   
        //feature matrix prepare
        float[][] nmt = MatrixTranspose(nm);
        float[][] nmta = MatrixABS(nmt);
        float[][] nmp = MatrixMult(nmta, nm);
        float[][] X = MatrixMult(nmp, dm);
        float[][] Xs = MatrixCombin(fm, dm);
        float[][] Xall = MatrixCombin(Xs, X);
        
        float[] radius = MatrixRadius(Xall);
        float radsum = 0;
        for(int i=0;i<radius.length;i++)
            radsum = radsum + radius[i];
        if(radsum > 0.5)
        {
            float[] rimerr = new float[RIM.length];
            int rimmark = MatrixRIMERR(RIM, dm, rimerr);
            float[][] dist = EuclideanDist(Xall);
            float[] lofp = MatrixDotMult(radius, rimerr);            
            float[] lof = CalLOF(rimmark, lofp, dist);         
            return lof;            
        }else
            return null;     
    }
    
    public float[] MatrixDotMult(float[] mx, float[] my)
    { 
        float[] dm = new float[mx.length];
        
        for(int i=0;i<mx.length;i++)
        {
            dm[i] = mx[i] * my[i];
        }
        
        return dm;
    }
    
    public int MatrixRIMERR(float[] RIM, float[][] m, float[] rimout)
    {
        int res=0; 
        
        float[] rimres = new float[m[0].length];
        for(int i=0;i<m[0].length;i++)
        {
            rimres[i] = 0;
            for(int j=0;j<RIM.length;j++)
            {
                //for(int k=0;k<m.length;k++)
                rimres[i] = rimres[i] + RIM[j]*m[j][i];
            }
            if(rimres[i]>1.2)
            {
                res = 1;
            }
        }
        
        if(res == 1)
        {
            for(int i=0;i<RIM.length;i++)
            {
                if(RIM[i] == 0)
                    rimout[i] = 1;
                else
                    rimout[i] = 0;
            }
        }else
        {
            for(int i=0;i<RIM.length;i++)
            {
                if(RIM[i] == 1)
                    rimout[i] = 0;
                else if(RIM[i] == -1)
                    rimout[i] = 1;
            }            
        }
        
        return res;
    }
    
    public float[] MatrixRadius(float[][] mx)
    {
        int res = 0; 
        float[] rad = new float[mx.length];
        
        for(int i=0;i<mx.length;i++)
        {
            for(int j=0;j<mx[0].length;j++)
            {
                if((mx[i][j] > 1.2) || mx[i][j] < -1.2)
                    rad[i] = 1;
            }
        }
        
        return rad;
    }
    
    public float[][] MatrixCombin(float[][] mx, float[][] my)
    {
        int nmx = mx.length;
        int nmy = mx[0].length + my[0].length;
        
        float[][] nm = new float[nmx][nmy];
        
        for(int i=0;i<nmx;i++)
        {
            for(int j=0;j<mx[0].length;j++)
            {
                nm[i][j] = mx[i][j];
            }

            for(int j=0;j<my[0].length;j++)
            {
                nm[i][j+mx[0].length] = my[i][j];
            }
        }
        
        return nm;
    }
    
    public float[][] MatrixMult(float[][] mx, float[][] my)
    {
        int nmx = mx.length;
        int nmy = my[0].length;
        float tmp = 0;
        
        float[][] nm = new float[nmx][nmy];
        
        for(int i=0;i<nmx;i++)
        {
            for(int j=0;j<nmy;j++)
            {
                tmp = 0;
                for(int k=0;k<mx[0].length;k++)
                    tmp = tmp + mx[i][k]*my[k][j];
                nm[i][j] = tmp;
            }
        }
        
        return nm;
    }
    
    public float[][] MatrixTranspose(float[][] mx)
    {
        int nmx = mx[0].length;
        int nmy = mx.length;
        
        float[][] nm = new float[nmx][nmy];
        
        for(int i=0;i<nmx;i++)
        {
            for(int j=0;j<nmy;j++)
            {
                nm[i][j] = mx[j][i];
            }
        }
        
        return nm;
    }
       
    public float[][] MatrixABS(float[][] mx)
    {
        int nmx = mx.length;
        int nmy = mx[0].length;
        
        float[][] nm = new float[nmx][nmy];
        
        for(int i=0;i<nmx;i++)
        {
            for(int j=0;j<nmy;j++)
            {
                if(mx[i][j]>=0)
                    nm[i][j] = mx[i][j];
                else
                    nm[i][j] = -mx[i][j];
            }
        }
        
        return nm;
    }
    
    public float[][] EuclideanDist(float[][] X)
    {
        int i,j,k;
        float sum,tmp;
        int len1 = X.length;
        int len2 = X[0].length;
        float[][] D = new float[len1][len1]; 
        for(i=0;i<len1;i++)
        {
            for(j=0;j<len1;j++)
            {
                sum = 0;
                for(k=0;k<len2;k++)
                {
                    tmp = X[i][k]-X[j][k];
                    sum = sum + tmp * tmp;
                    D[i][j] = (float) sqrt(sum);
                }
            }
        }
        return D;
    }

    public float[] CalLOF(int mark, float[] mask, float[][] X)
    {
        int ms1 = X.length;
        int ms2 = X[0].length;
        float[] D = new float[ms1];
        float[] lrd = new float[ms1];
        int[] lof_n = new int[ms1];
        int[][] pointArray = new int[ms1][ms1];
        int i,j,pos;
        float lrdavr = 0;
        for(i=0;i<ms1;i++)
        {
            lrd[i] = 0;
            for(j=0;j<ms1;j++)
            {
                lrd[i] = lrd[i] + X[i][j];
            }
            lrd[i] = lrd[i]*mask[i];
        } 
        
        float[] outbuf = new float[lrd.length];
        int[] order = new int[lrd.length];
        sortByAscend(lrd, outbuf, order);
        
        for(i=0;i<outbuf.length;i++)
            System.out.println("        ::lrd["+i+"]="+outbuf[i]+".");
        System.out.println("        ::mark="+mark+".");
        
        int lrdcnt;
        if(mark == 1)
            lrdcnt = 2;
        else
            lrdcnt = 1;
        
        lrdavr = outbuf[lrd.length-1];
        for(i=lrd.length-2;i>=0;i--)
        {
            if(lrdavr > outbuf[i])
            {
                if(outbuf[i] == 0)
                {
                    lrdavr = lrdavr/10;
                    break;
                }else
                {
                    lrdavr = outbuf[i];
                    lrdcnt--;
                    if(lrdcnt == 0)
                        break;
                }
            }
        }
        
        for(i=0;i<ms1;i++)
        {
            D[i] = lrd[i]/lrdavr;
        }

        
        return D;
    }
    
    public float[] CalLOF2(int K, float[][] X)
    {
        int ms1 = X.length;
        int ms2 = X[0].length;
        float[] D = new float[ms1];
        float[] lrd = new float[ms1];
        int[] lof_n = new int[ms1];
        int[][] pointArray = new int[ms1][ms1];
        int i,j,pos;
        
        int p;
        //cal lrd for each point
        for(i=0;i<ms1;i++)
        {
            //cal K-distance and K-regin
            float[] distance = new float[ms1];
            int[] num = new int[ms1];
            sortByAscend(X[i],distance, num);
            //kdistance = distance(K+1);
            p = 0;
            for(j=(K+1);j<ms2;j++)
            {
                if(distance[K] == distance[j])
                    p = p + 1;
            }
        
            int alen = K + p; 
            lof_n[i] = alen;  
            pos = 0;    
            for(j=1;j<K+p+1;j++)
            {
                pointArray[i][pos] = num[j];
                pos = pos + 1;
            }
            
            //cal reach distance for point i
            float[] d = new float[alen];
            float[] kdis = new float[alen];
            float[] reachdis = new float[alen];
            
            for(j=0;j<alen;j++)
            {
                pos = pointArray[i][j];
                d[j] = distance[j+1];
                
                float[] distemp = new float[ms1];
                int[] numtemp = new int[ms1];
                sortByAscend(X[pos],distemp, numtemp);
                
                kdis[j] = distemp[K];
                reachdis[j] = max(d[j],kdis[j]);
            }
            
            float sum_reachdis=0;
            for(j=0;j<alen;j++)
            {
                sum_reachdis=sum_reachdis+reachdis[j];
            }
            
            //计算每个点的lrd
            if(sum_reachdis == 0)
                lrd[i] = -1;
            else
                lrd[i]=alen/sum_reachdis;
        }

        //得到lof值
        for(i=0;i<ms1;i++)
        {
            float lrdtmpi = lrd[i];
            if(lrdtmpi == -1)
            {
                D[i] = 0;
            }else{
                float sumlrd = 0;
                int lrdn = lof_n[i];
                float mult = 1;
                for(j=0;j<lof_n[i];j++)
                {
                    pos = pointArray[i][j];
                    float lrdtmp = lrd[pos]; 
                    if(lrdtmp == -1)
                    {
                        lrdn = lrdn - 1;
                        mult = mult + 1;
                    }else
                        sumlrd=sumlrd+lrd[pos]/lrd[i];
                }
                D[i]=sumlrd*mult/lrdn;
            }
        } 
        
        return D;
    }
    
    public float max(float d1, float d2)
    {
        if(d1>=d2)
            return d1;
        else
            return d2;
    }
    
    public int[] getSubArrayInt(int[] array, int sta, int end)
    {
        int alen = end - sta;
        int[] res = new int[alen];
        for(int i=0;i<alen;i++)
            res[i] = array[sta+i];
        return res;
    }
    
    public void sortByAscend(float[] inbuf, float[] outbuf, int[] order)
    {
        float tmp;
        int otmp;
        
        float[] buftmp = new float[inbuf.length]; 
        for(int i=0;i<inbuf.length;i++)
        {
            buftmp[i] = inbuf[i];
            order[i] = i;
        }
        
        for(int i=0;i<inbuf.length;i++)
        {
            for(int j=i;j<inbuf.length;j++)
            {
                if(inbuf[j]<inbuf[i])
                {
                    tmp = inbuf[i];
                    inbuf[i] = inbuf[j];
                    inbuf[j] = tmp;
                    
                    otmp = order[i];
                    order[i] = order[j];                    
                    order[j] = otmp;
                }
            }
        }
        
        for(int i=0;i<inbuf.length;i++)
        {
            outbuf[i] = inbuf[i];
            inbuf[i] = buftmp[i];
        }
    }
}
