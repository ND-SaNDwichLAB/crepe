package edu.nd.crepe.network;

public interface FirebaseCallback<T> {
    void onResponse(T result);
    void onErrorResponse(Exception e);


}
