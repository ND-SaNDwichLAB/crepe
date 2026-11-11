package edu.nd.crepe.network;

import edu.nd.crepe.database.Collector;

// Create an interface to respond with the result after processing
public interface VolleyCallback {
    void onSuccess(Collector result);

    // optional: implementers that do not care about failures do not need to override this
    default void onError(Exception e) {
        // do nothing
    }
}
