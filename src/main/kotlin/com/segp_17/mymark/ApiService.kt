package com.segp_17.mymark

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService{

    @GET("/modules/{module}/exercises")
    fun fetchExercises(@Path("module") module: String): Call<Exercises>

    @GET("/modules")
    fun fetchModules(): Call<Modules>

    @POST("/modules/{module}/exercises/{exercise}/autota/ask")
    fun askQuestion(@Body postData: QuestionData, @Path("exercise") exercise: String, @Path("module") module: String): Call<Answer>
}