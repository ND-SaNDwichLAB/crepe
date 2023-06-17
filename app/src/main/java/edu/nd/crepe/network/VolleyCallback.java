package edu.nd.crepe.network;

import edu.nd.crepe.database.Collector;

// Create an interface to respond with the result after processing
public interface VolleyCallback {
    void onSuccess(Collector result);
}
