package com.example.gymtrackerpro.network;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Header;

public interface AuthApi {

    @POST("api/auth/register")
    Call<Map<String, Object>> register(
            @Body RegisterRequest request
    );

    @POST("api/auth/login")
    Call<Map<String, Object>> login(
            @Body Map<String, String> request
    );

    @GET("api/auth/me")
    Call<Map<String, Object>> getCurrentAccount(
            @Header("Authorization") String token
    );

    @POST("api/auth/forgot-password")
    Call<Map<String, String>> forgotPassword(
            @Body Map<String, String> request);

    @POST("api/auth/reset-password")
    Call<Map<String, String>> resetPassword(
            @Body Map<String, String> request);
}

