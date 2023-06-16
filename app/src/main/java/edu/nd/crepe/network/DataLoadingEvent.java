package edu.nd.crepe.network;

public class DataLoadingEvent {
    private boolean isCompleted;

    public DataLoadingEvent(boolean isCompleted){
        this.isCompleted = isCompleted;
    }

    public boolean isCompleted(){
        return isCompleted;
    }
}
