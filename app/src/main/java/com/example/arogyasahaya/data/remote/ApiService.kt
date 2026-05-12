package com.example.arogyasahaya.data.remote

import com.example.arogyasahaya.data.local.entity.Vital
import com.example.arogyasahaya.data.local.entity.Medicine
import retrofit2.http.*

interface ApiService {
    @GET("api/vitals/{userId}")
    suspend fun getVitals(@Path("userId") userId: String): List<Vital>

    @POST("api/vitals")
    suspend fun syncVital(@Body vital: Vital): Vital

    @GET("api/medicines/{userId}")
    suspend fun getMedicines(@Path("userId") userId: String): List<Medicine>

    @POST("api/medicines")
    suspend fun syncMedicine(@Body medicine: Medicine): Medicine
}
