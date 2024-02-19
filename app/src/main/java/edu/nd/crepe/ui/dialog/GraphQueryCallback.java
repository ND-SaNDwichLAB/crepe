package edu.nd.crepe.ui.dialog;
import java.io.Serializable;

public interface GraphQueryCallback extends Serializable {
    void onDataReceived(String query, String targetText);
}

