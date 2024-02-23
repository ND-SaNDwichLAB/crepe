package edu.nd.crepe.network;

public interface FirebaseCallback<T> {
    void onResponse(T result);
    void onErrorResponse(Exception e);
    default void onComplete() {
        // do nothing, if no async operations are needed
        // this will be the default implementation so not all classes that implement this interface need to implement this method
    }

}
