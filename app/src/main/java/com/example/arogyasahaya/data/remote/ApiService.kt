package com.example.arogyasahaya.data.remote

import com.example.arogyasahaya.data.local.entity.*
import retrofit2.http.*

interface ApiService {
    @POST("api/auth/register")
    suspend fun register(@Body data: Map<String, String>): retrofit2.Response<Map<String, Any>>

    @POST("api/auth/login")
    suspend fun login(@Body data: Map<String, String>): retrofit2.Response<Map<String, Any>>

    @POST("api/health/vitals")
    suspend fun syncVital(@Header("Authorization") token: String, @Body vital: Vital)

    @GET("api/health/vitals")
    suspend fun getVitals(@Header("Authorization") token: String): List<Vital>

    @POST("api/health/medicines")
    suspend fun syncMedicine(@Header("Authorization") token: String, @Body medicine: Medicine)

    @GET("api/health/medicines")
    suspend fun getMedicines(@Header("Authorization") token: String): List<Medicine>

    @POST("api/health/symptoms")
    suspend fun syncSymptom(@Header("Authorization") token: String, @Body symptom: Symptom)

    @GET("api/health/symptoms")
    suspend fun getSymptoms(@Header("Authorization") token: String): List<Symptom>

    @POST("api/health/records")
    suspend fun syncRecord(@Header("Authorization") token: String, @Body record: MedicalRecord)

    @GET("api/health/records")
    suspend fun getRecords(@Header("Authorization") token: String): List<MedicalRecord>

    @POST("api/health/appointments")
    suspend fun syncAppointment(@Header("Authorization") token: String, @Body appointment: Appointment)

    @GET("api/health/appointments")
    suspend fun getAppointments(@Header("Authorization") token: String): List<Appointment>

    @POST("api/health/family")
    suspend fun syncFamilyMember(@Header("Authorization") token: String, @Body member: FamilyMember)

    @GET("api/health/family")
    suspend fun getFamily(@Header("Authorization") token: String): List<FamilyMember>

    @GET("api/asha/events")
    suspend fun getAshaEvents(): List<AshaEvent>

    @POST("api/asha/events")
    suspend fun addAshaEvent(@Body event: AshaEvent)
}
