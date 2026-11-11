package edu.nd.crepe.network;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ApiCallManager {

    private static final String TAG = "ApiCallManager";

    private Context context;
    private OkHttpClient httpClient;
    private ExecutorService executorService;

    private static final String GCLOUD_API_URL = "http://35.223.210.17:8001/generate"; // Replace with your actual server IP and port
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public ApiCallManager(Context context) {
        this.context = context;
        // explicit timeouts, so a stalled server cannot hold a worker thread (and the caller waiting on it) indefinitely
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Sends the prompt to the server. Exactly one of the callback methods is invoked, on a background thread.
     */
    public void getResponse(String prompt, ApiCallback callback) {
        executorService.execute(() -> {
            String content;
            try {
                RequestBody body = RequestBody.create(JSON, buildJsonBody(prompt));
                Request request = new Request.Builder()
                        .url(GCLOUD_API_URL)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    // Check for server error (HTTP 500)
                    if (response.code() == 500) {
                        handleError("Server error (500)", callback);
                        return;
                    }

                    // Check for any other unsuccessful response
                    if (!response.isSuccessful()) {
                        handleError("Unexpected response code: " + response.code(), callback);
                        return;
                    }

                    // Assuming the server response is a JSON object
                    JsonObject jsonResponse = parseResponseToJson(response);
                    JsonElement contentElement = jsonResponse.get("content");
                    if (contentElement == null || contentElement.isJsonNull()) {
                        handleError("Response does not contain a \"content\" field", callback);
                        return;
                    }
                    Log.i(TAG, "Content: " + contentElement);
                    content = contentElement.toString();
                }
            } catch (IOException e) {
                handleError("API call failed: " + e.getMessage(), callback);
                return;
            } catch (RuntimeException e) {
                // malformed JSON, unexpected response shape, ... must never escape the worker thread
                handleError("Could not process the API response: " + e.getMessage(), callback);
                return;
            }

            try {
                callback.onResponse(content);
            } catch (RuntimeException e) {
                // a failing callback must not take the worker thread (and with it the app) down
                Log.e(TAG, "The response callback threw an exception", e);
            }
        });
    }

    /**
     * Releases the worker thread. Requests that were already submitted still run to completion.
     */
    public void shutdown() {
        executorService.shutdown();
    }

    private void handleError(String errorMessage, ApiCallback callback) {
        Log.e(TAG, errorMessage);
        try {
            callback.onErrorResponse(new Exception(errorMessage));
        } catch (RuntimeException e) {
            Log.e(TAG, "The error callback threw an exception", e);
        }
    }


    // Build JSON body for the server request
    private String buildJsonBody(String prompt) {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("prompt", prompt);
        return jsonBody.toString();
    }

    private JsonObject parseResponseToJson(Response response) throws IOException {
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }
        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            throw new IOException("Empty response body");
        }
        // Read the response body as a string
        String responseText = responseBody.string();
        // Use Gson to parse the string into a JsonObject
        JsonObject jsonResponse = new Gson().fromJson(responseText, JsonObject.class);
        if (jsonResponse == null) {
            throw new IOException("Response body is empty or not a JSON object");
        }

        return jsonResponse;
    }


    public interface ApiCallback {
        void onResponse(String response);

        void onErrorResponse(Exception e);
    }
}
