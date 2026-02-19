package com.browserstack.assignment.Utils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class HTMLFetcher {
    public static void main(String[] args) {
        OkHttpClient client = new OkHttpClient();
        String url = args.length > 0 ? args[0] : "https://elpais.com/opinion/";

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                System.out.println(response.body().string());
            } else {
                System.err.println("Request failed: " + response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
