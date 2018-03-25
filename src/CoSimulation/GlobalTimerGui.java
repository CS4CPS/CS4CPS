package CoSimulation;

public interface GlobalTimerGui {
    void setAgent(GlobalTimer a);
    void notifyUser(String message);
    
    void show();
    void hide();
    void dispose();
}
