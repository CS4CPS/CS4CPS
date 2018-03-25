package CoSimulation;

import static CoSimulation.sysconfig.*;
import MultiAgentSystem.Algorithm;
import MultiAgentSystem.ModelSw;
import MultiAgentSystem.Nodeinfo;
import jade.core.Agent;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import static java.lang.Thread.sleep;

public class CoSimManager extends Agent{
        
    ModelConfig devConfig;
        
    protected void setup(){
        System.out.println(getLocalName()+" is running.");
        
        String name;
        AgentContainer c = getContainerController();
        AgentController a;
        
        Object [] args = new Object[2];
        args[0] = "";
        args[1] = "";
        
        devConfig = new ModelConfig();
        devConfig.XMLconfig("C:/ModelConfig.xml");
        args[1] = devConfig;
        
        //creat ComPowerFactory
        name = "insCPF";
        args[0] = Integer.toString(ID_COMPOWERFFACTORY);
    	try {
    		a = c.createNewAgent(name, "CoSimulation.ComPowerFactory", args );
    		a.start();
                sleep(1000);
    	}catch (Exception e){
            System.out.println(e.getMessage());
        }
        
        //creat ComOMNETpp
        name = "insOMT";
        args[0] = Integer.toString(ID_COMOMNETPP);
    	try {
    		a = c.createNewAgent(name, "CoSimulation.ComOMNETpp", args );
    		a.start();
                sleep(1000);
    	}catch (Exception e){
            System.out.println(e.getMessage());
        }
        
        //creat ComIED
        int idbase;
        for(int i=0;i<devConfig.IEDCount;i++)
        //for(int i=0;i<2;i++)
        {
            name = "CIED";
            idbase = ID_COMIED + i;
            args[0] = Integer.toString(idbase);//idbase
            try {
                a = c.createNewAgent(name+idbase, "CoSimulation.ComIED", args );
                a.start();
                sleep(1000);
            }catch (Exception e){
                System.out.println(e.getMessage());
            }
        }
        
        //creat GlobalTimer
        name = "insGBT" ;
        args[0] = Integer.toString(ID_GLOBALTIMER);
    	try {
    		a = c.createNewAgent(name, "CoSimulation.GlobalTimer", args );
    		a.start();
                sleep(1000);
    	}catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
}
