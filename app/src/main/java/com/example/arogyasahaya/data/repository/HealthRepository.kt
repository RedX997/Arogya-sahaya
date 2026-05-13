package com.example.arogyasahaya.data.repository

import androidx.lifecycle.LiveData
import com.example.arogyasahaya.data.local.dao.*
import com.example.arogyasahaya.data.local.entity.*
import androidx.lifecycle.asLiveData
import com.example.arogyasahaya.data.remote.ApiService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import retrofit2.Response

class HealthRepository(
    private val medicineDao: MedicineDao,
    private val vitalDao: VitalDao,
    private val symptomDao: SymptomDao,
    private val appointmentDao: AppointmentDao,
    private val medicalRecordDao: MedicalRecordDao,
    private val familyMemberDao: FamilyMemberDao,
    private val ashaEventDao: AshaEventDao,
    private val apiService: ApiService,
    private val firestoreService: com.example.arogyasahaya.data.remote.FirestoreService = com.example.arogyasahaya.data.remote.FirestoreService()
) {
    val allMedicines: LiveData<List<Medicine>> = medicineDao.getAllMedicines()
    val allVitals: LiveData<List<Vital>> = vitalDao.getVitals()
    val allSymptoms: LiveData<List<Symptom>> = symptomDao.getAllSymptoms()
    val allAppointments: LiveData<List<Appointment>> = appointmentDao.getAllAppointments()
    val allMedicalRecords: LiveData<List<MedicalRecord>> = medicalRecordDao.getAllRecords()
    val allFamilyMembers: LiveData<List<FamilyMember>> = familyMemberDao.getAllFamilyMembers()
    val allAshaEvents: LiveData<List<AshaEvent>> = ashaEventDao.getAllEvents().asLiveData()

    // --- Auth ---
    suspend fun register(name: String, email: String, password: String): Response<Map<String, Any>> = 
        apiService.register(mapOf("name" to name, "email" to email, "password" to password))

    suspend fun login(email: String, password: String): Response<Map<String, Any>> = 
        apiService.login(mapOf("email" to email, "password" to password))

    // --- Local Operations ---
    suspend fun insertAshaEvent(event: AshaEvent) = ashaEventDao.insertEvent(event)
    suspend fun clearAshaEvents() = ashaEventDao.deleteAll()
    suspend fun updateAshaEvent(event: AshaEvent) = ashaEventDao.insertEvent(event)
    suspend fun updateAshaEventLocally(event: AshaEvent) = ashaEventDao.insertEvent(event)

    suspend fun insertFamilyMember(member: FamilyMember) = familyMemberDao.insert(member)
    suspend fun deleteFamilyMember(member: FamilyMember) = familyMemberDao.delete(member)
    suspend fun insertMedicine(medicine: Medicine): Long = medicineDao.insert(medicine)
    suspend fun updateMedicine(medicine: Medicine) = medicineDao.update(medicine)
    suspend fun deleteMedicine(medicine: Medicine) = medicineDao.delete(medicine)
    suspend fun markMedicineAsTaken(id: Int) = medicineDao.markAsTaken(id, true, System.currentTimeMillis())
    suspend fun resetDailyMedicines() = medicineDao.resetDailyStatus()

    suspend fun insertVital(vital: Vital): Long = vitalDao.insert(vital)
    suspend fun getLatestVital() = vitalDao.getLatestVital()

    suspend fun insertSymptom(symptom: Symptom): Long = symptomDao.insert(symptom)
    suspend fun deleteSymptom(symptom: Symptom) = symptomDao.delete(symptom)
    fun getRecentSymptoms(since: Long) = symptomDao.getRecentSymptoms(since)

    suspend fun insertAppointment(appointment: Appointment): Long = appointmentDao.insert(appointment)
    fun getUpcomingAppointments(currentTime: Long) = appointmentDao.getUpcomingAppointments(currentTime)
    suspend fun deleteAppointment(appointment: Appointment) = appointmentDao.delete(appointment)

    suspend fun insertMedicalRecord(record: MedicalRecord): Long = medicalRecordDao.insertRecord(record)
    suspend fun deleteMedicalRecord(record: MedicalRecord) = medicalRecordDao.deleteRecord(record)

    suspend fun populateMockData() {
        insertMedicine(Medicine(name = "Amlodipine", dosage = "5mg", time = System.currentTimeMillis() + 3600000, totalTablets = 30, remainingTablets = 25, frequency = "Daily"))
        insertMedicine(Medicine(name = "Metformin", dosage = "500mg", time = System.currentTimeMillis() + 7200000, totalTablets = 60, remainingTablets = 45, frequency = "Twice Daily"))
        insertVital(Vital(date = System.currentTimeMillis() - 86400000, systolic = 125, diastolic = 82, heartRate = 72, sugar = 110))
        insertFamilyMember(FamilyMember(name = "Anjali", phoneNumber = "9876543210", relation = "Wife"))
    }

    suspend fun clearAllData() {
        medicineDao.deleteAll()
        vitalDao.deleteAll()
        symptomDao.deleteAll()
        appointmentDao.deleteAll()
        medicalRecordDao.deleteAll()
        familyMemberDao.deleteAll()
    }

    // --- Cloud Sync ---
    suspend fun syncAllData(token: String) {
        val authHeader = "Bearer $token"
        try {
            vitalDao.getVitalsList().forEach { apiService.syncVital(authHeader, it) }
            medicineDao.getAllMedicinesList().forEach { apiService.syncMedicine(authHeader, it) }
            symptomDao.getAllSymptomsList().forEach { apiService.syncSymptom(authHeader, it) }
            medicalRecordDao.getAllRecordsList().forEach { apiService.syncRecord(authHeader, it) }
            appointmentDao.getAllAppointmentsList().forEach { apiService.syncAppointment(authHeader, it) }
            familyMemberDao.getAllFamilyMembersList().forEach { apiService.syncFamilyMember(authHeader, it) }
        } catch (e: Exception) {
            android.util.Log.e("Repository", "Sync failed: ${e.message}")
        }
    }

    suspend fun fetchAllData(token: String) {
        val authHeader = "Bearer $token"
        try {
            apiService.getVitals(authHeader).forEach { vitalDao.insert(it) }
            apiService.getMedicines(authHeader).forEach { medicineDao.insert(it) }
            apiService.getSymptoms(authHeader).forEach { symptomDao.insert(it) }
            apiService.getRecords(authHeader).forEach { medicalRecordDao.insertRecord(it) }
            apiService.getAppointments(authHeader).forEach { appointmentDao.insert(it) }
            apiService.getFamily(authHeader).forEach { familyMemberDao.insert(it) }
        } catch (e: Exception) {
            android.util.Log.e("Repository", "Fetch failed: ${e.message}")
        }
    }

    suspend fun fetchAshaEventsFromCloud() {
        try {
            apiService.getAshaEvents().forEach { insertAshaEvent(it) }
        } catch (e: Exception) {
            android.util.Log.e("Repository", "Failed to fetch cloud events: ${e.message}")
        }
    }

    suspend fun addAshaEventToCloud(event: AshaEvent) {
        try {
            insertAshaEvent(event)
            apiService.addAshaEvent(event)
            firestoreService.addEvent(event)
            fetchAshaEventsFromCloud()
        } catch (e: Exception) {
            android.util.Log.e("Sync", "Cloud sync failed: ${e.message}")
        }
    }

    suspend fun syncOnlineCampaigns() {
        com.example.arogyasahaya.data.remote.HealthNewsFetcherService.fetchOnlineCampaigns().forEach { 
            ashaEventDao.insertEvent(it) 
        }
    }

    fun listenToFirestoreEvents() {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            firestoreService.getEventsFlow().collect { cloudEvents ->
                cloudEvents.forEach { ashaEventDao.insertEvent(it) }
            }
        }
    }
}
