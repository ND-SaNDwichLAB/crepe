package edu.nd.crepe.network;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiCallManager {

    private Context context;
    private OkHttpClient httpClient;
    private ExecutorService executorService;

    private static final String GCLOUD_API_URL = "http://35.223.210.17:8001/generate"; // Replace with your actual server IP and port
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public ApiCallManager(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void getResponse(String prompt, ApiCallback callback) {
        executorService.execute(() -> {
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
                Log.i("ApiCallManager", "Response: " + response);
                Log.i("ApiCallManager", "Content: " + jsonResponse.get("content"));
                callback.onResponse(String.valueOf(jsonResponse.get("content")));
            } catch (IOException e) {
                handleError("API call failed: " + e.getMessage(), callback);
            }
        });
    }

    private void handleError(String errorMessage, ApiCallback callback) {
        Log.e("ApiCallManager", errorMessage);
        callback.onErrorResponse(new Exception(errorMessage));
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
        // Read the response body as a string
        String responseBody = response.body().string();
        // Use Gson to parse the string into a JsonObject
        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

        return jsonResponse;
    }


    public interface ApiCallback {
        void onResponse(String response);

        void onErrorResponse(Exception e);
    }
}
