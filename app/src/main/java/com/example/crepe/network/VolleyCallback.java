package com.example.crepe.network;

import com.example.crepe.database.Collector;

// Create an interface to respond with the result after processing
public interface VolleyCallback {
    void onSuccess(Collector result);
}
