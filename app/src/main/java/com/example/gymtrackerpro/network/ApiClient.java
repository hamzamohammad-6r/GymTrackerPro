package com.example.gymtrackerpro.network;

import android.content.Context;

import com.example.gymtrackerpro.GymTrackerSession;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL =
            "http://192.168.1.129:8080/";

    private static Retrofit retrofit;

    public static synchronized Retrofit getClient(Context context) {

        if (retrofit == null) {

            Context appContext = context.getApplicationContext();

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {

                        String accessToken =
                                GymTrackerSession.getAccessToken(appContext);

                        Request originalRequest = chain.request();

                        Request.Builder requestBuilder =
                                originalRequest.newBuilder();

                        if (accessToken != null
                                && !accessToken.trim().isEmpty()) {

                            requestBuilder.header(
                                    "Authorization",
                                    "Bearer " + accessToken
                            );
                        }

                        return chain.proceed(
                                requestBuilder.build()
                        );
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(
                            GsonConverterFactory.create()
                    )
                    .build();
        }

        return retrofit;
    }
}
