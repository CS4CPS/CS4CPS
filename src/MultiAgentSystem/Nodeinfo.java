package MultiAgentSystem;

public class Nodeinfo {

    int[] nodes;
    
    String zName;
    int agentZA;
    int NodesCnt;
    int GTime;
            
    public Nodeinfo(){
        agentZA = 0;
        NodesCnt = 0;
    } 
    
    public void setlen(int len)
    {
        nodes = new int[len];
    }
    
    public int findpos(int id)
    {
        int res = -1;
        for(int i=0;i<NodesCnt;i++)
        {
            if(id == nodes[i])
                res = i;
        }
                
        return res;
    }
    
    public String getZName()
    {
        return zName;
    }
    
    public int getAZA()
    {
        return agentZA;
    }
    
    public void setAZA(int aza)
    {
        agentZA = aza;
    }
    
    public int getAgentCnt()
    {
        return NodesCnt;
    }
    
    public void setAgentCnt(int ac)
    {
        NodesCnt = ac;
    }
    
    public int getAgents(int id)
    {
        return nodes[id];
    }
    
    public void setALen(int len)
    {
        setlen(len);
    }  
            
    public void setAgents(int id, int ac)
    {
        nodes[id] = ac;
    }   
    
    public int getGTime()
    {
        return GTime;
    }
    
    public void setGTime(int p)
    {
        GTime = p;
    }    
}
