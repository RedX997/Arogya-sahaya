package com.example.arogyasahaya.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.arogyasahaya.data.local.AppDatabase
import com.example.arogyasahaya.data.local.entity.*
import com.example.arogyasahaya.data.repository.HealthRepository
import com.example.arogyasahaya.utils.PreferenceManager
import com.example.arogyasahaya.utils.ReminderWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import retrofit2.Response

class HealthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: HealthRepository
    private val prefManager: PreferenceManager
    
    val allMedicines: LiveData<List<Medicine>>
    val allVitals: LiveData<List<Vital>>
    val allSymptoms: LiveData<List<Symptom>>
    val allAppointments: LiveData<List<Appointment>>
    val allMedicalRecords: LiveData<List<MedicalRecord>>
    val allFamilyMembers: LiveData<List<FamilyMember>>
    val allAshaEvents: LiveData<List<AshaEvent>>

    data class HealthInsight(val title: String, val subtitle: String, val icon: String, val color: Long)
    data class HealthTrends(val avgSystolic: Int, val avgDiastolic: Int, val avgHeartRate: Int, val avgSugar: Int, val bpStatus: String, val sugarStatus: String)

    private val _dailyInsights = MutableLiveData<List<HealthInsight>>()
    val dailyInsights: LiveData<List<HealthInsight>> = _dailyInsights

    private val _isSyncing = MutableLiveData<Boolean>(false)
    val isSyncing: LiveData<Boolean> = _isSyncing

    private val _latestVital = MutableLiveData<Vital?>()
    val latestVital: LiveData<Vital?> = _latestVital

    init {
        val database = AppDatabase.getDatabase(application)
        prefManager = PreferenceManager(application)
        repository = HealthRepository(
            database.medicineDao(),
            database.vitalDao(),
            database.symptomDao(),
            database.appointmentDao(),
            database.medicalRecordDao(),
            database.familyMemberDao(),
            database.ashaEventDao(),
            com.example.arogyasahaya.data.remote.NetworkModule.apiService
        )
        allMedicines = repository.allMedicines
        allVitals = repository.allVitals
        allSymptoms = repository.allSymptoms
        allAppointments = repository.allAppointments
        allMedicalRecords = repository.allMedicalRecords
        allFamilyMembers = repository.allFamilyMembers
        allAshaEvents = repository.allAshaEvents
        refreshLatestVital()
        syncAshaEvents()
        syncInsights()
    }

    val healthScore = androidx.lifecycle.MediatorLiveData<Float>().apply {
        addSource(allMedicines) { calculateHealthScore() }
        addSource(allVitals) { calculateHealthScore() }
    }

    val sevenDayTrends = androidx.lifecycle.MediatorLiveData<HealthTrends>().apply {
        addSource(allVitals) { vitals -> value = calculateTrends(vitals) }
    }

    private fun calculateTrends(vitals: List<Vital>): HealthTrends {
        val last7Days = vitals.takeLast(7)
        if (last7Days.isEmpty()) return HealthTrends(0, 0, 0, 0, "STABLE", "STABLE")
        val avgSys = last7Days.map { it.systolic }.average().toInt()
        val avgDia = last7Days.map { it.diastolic }.average().toInt()
        val avgHr = last7Days.map { it.heartRate }.average().toInt()
        val avgSugar = last7Days.mapNotNull { it.sugar }.let { if (it.isEmpty()) 0 else it.average().toInt() }
        val bpStatus = if (last7Days.size >= 3) {
            val diff = last7Days.last().systolic - last7Days.first().systolic
            when { diff > 10 -> "RISING"; diff < -10 -> "IMPROVING"; else -> "STABLE" }
        } else "STABLE"
        val sugarStatus = if (last7Days.size >= 3) {
            val validSugars = last7Days.mapNotNull { it.sugar }
            if (validSugars.size >= 2) {
                val diff = validSugars.last() - validSugars.first()
                when { diff > 15 -> "RISING"; diff < -15 -> "IMPROVING"; else -> "STABLE" }
            } else "STABLE"
        } else "STABLE"
        return HealthTrends(avgSys, avgDia, avgHr, avgSugar, bpStatus, sugarStatus)
    }

    private fun calculateHealthScore() {
        val meds = allMedicines.value ?: emptyList()
        val vitals = allVitals.value ?: emptyList()
        if (meds.isEmpty() && vitals.isEmpty()) { healthScore.postValue(0f); return }
        var score = 50f
        if (meds.isNotEmpty()) {
            val taken = meds.count { it.isTaken }
            score += (taken.toFloat() / meds.size * 30f)
        }
        if (vitals.isNotEmpty()) {
            vitals.lastOrNull()?.let { last ->
                var points = 20f
                if (last.systolic > 140 || last.diastolic > 90) points -= 10f
                if (last.sugar != null && (last.sugar!! > 180 || last.sugar!! < 70)) points -= 10f
                score += points
            }
        }
        healthScore.postValue(score.coerceIn(0f, 100f))
    }

    fun syncInsights() {
        _dailyInsights.value = listOf(
            HealthInsight("Heart Health", "Walking 30 mins a day reduces heart risk by 35%.", "DirectionsWalk", 0xFFE91E63),
            HealthInsight("Hydration", "Drink 8 glasses of water to keep your kidneys healthy.", "WaterDrop", 0xFF2196F3),
            HealthInsight("Sleep Quality", "7-8 hours of sleep helps regulate blood pressure.", "Bedtime", 0xFF673AB7),
            HealthInsight("Sugar Control", "Reduce refined carbs to keep your sugar levels stable.", "Restaurant", 0xFFF44336),
            HealthInsight("Mental Peace", "5 mins of deep breathing lowers cortisol levels.", "SelfImprovement", 0xFF4CAF50)
        )
    }

    // --- Auth Actions ---
    @Suppress("UNCHECKED_CAST")
    suspend fun register(name: String, email: String, password: String): String? {
        return try {
            val response: Response<Map<String, Any>> = repository.register(name, email, password)
            if (response.isSuccessful) {
                val body = response.body()
                val token = body?.get("token") as? String ?: ""
                val user = body?.get("user") as? Map<String, String>
                if (user != null) {
                    prefManager.saveAuthData(token, user["id"]!!, user["name"]!!, user["email"]!!)
                    null
                } else "Registration failed"
            } else {
                val errorBody = response.errorBody()?.string()
                if (errorBody != null) JSONObject(errorBody).optString("message", "Registration failed") else "Registration failed"
            }
        } catch (e: Exception) {
            e.message ?: "Network error"
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun login(email: String, password: String): String? {
        return try {
            val response: Response<Map<String, Any>> = repository.login(email, password)
            if (response.isSuccessful) {
                val body = response.body()
                val token = body?.get("token") as? String ?: ""
                val user = body?.get("user") as? Map<String, String>
                if (user != null) {
                    prefManager.saveAuthData(token, user["id"]!!, user["name"]!!, user["email"]!!)
                    null
                } else "Login failed"
            } else {
                val errorBody = response.errorBody()?.string()
                if (errorBody != null) JSONObject(errorBody).optString("message", "Login failed") else "Login failed"
            }
        } catch (e: Exception) {
            e.message ?: "Network error"
        }
    }

    fun guestLogin() {
        prefManager.saveAuthData("guest_token", "guest_id", "Guest User", "guest@example.com", isGuest = true)
        prefManager.saveProfileData("Guest User", "30", "Not Specified", "", "None", "O+", "Demo Contact", "9999999999")
        viewModelScope.launch {
            repository.populateMockData()
        }
    }

    fun logout() {
        prefManager.clear()
        viewModelScope.launch { repository.clearAllData(); repository.clearAshaEvents() }
    }

    // --- Sync Actions ---
    fun syncWithCloud() {
        viewModelScope.launch {
            val token = prefManager.getToken() ?: return@launch
            _isSyncing.value = true
            repository.syncAllData(token)
            repository.fetchAllData(token)
            _isSyncing.value = false
        }
    }

    fun syncAshaEvents() {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.fetchAshaEventsFromCloud()
            _isSyncing.value = false
        }
    }

    // --- Data Actions ---
    fun addVital(systolic: Int, diastolic: Int, heartRate: Int, sugar: Int?, notes: String?) {
        viewModelScope.launch {
            repository.insertVital(Vital(date = System.currentTimeMillis(), systolic = systolic, diastolic = diastolic, heartRate = heartRate, sugar = sugar, notes = notes))
            refreshLatestVital()
            syncWithCloud()
        }
    }

    private fun refreshLatestVital() {
        viewModelScope.launch { _latestVital.value = repository.getLatestVital() }
    }

    fun addMedicine(name: String, dosage: String, time: Long, totalTablets: Int, frequency: String, pillImageUri: String? = null) {
        viewModelScope.launch {
            val med = Medicine(name = name, dosage = dosage, time = time, totalTablets = totalTablets, remainingTablets = totalTablets, frequency = frequency, pillImageUri = pillImageUri)
            val id = repository.insertMedicine(med)
            scheduleReminder(med.copy(id = id.toInt()))
            syncWithCloud()
        }
    }

    fun markAsTaken(medicine: Medicine) {
        viewModelScope.launch { repository.markMedicineAsTaken(medicine.id); syncWithCloud() }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch { repository.deleteMedicine(medicine); syncWithCloud() }
    }

    fun addSymptom(notes: String, tags: List<String>) {
        viewModelScope.launch { repository.insertSymptom(Symptom(date = System.currentTimeMillis(), notes = notes, tags = tags.joinToString(","))); syncWithCloud() }
    }

    fun deleteSymptom(symptom: Symptom) {
        viewModelScope.launch { repository.deleteSymptom(symptom); syncWithCloud() }
    }

    fun addAppointment(doctorName: String, dateTime: Long, notes: String) {
        viewModelScope.launch { repository.insertAppointment(Appointment(doctorName = doctorName, dateTime = dateTime, notes = notes)); syncWithCloud() }
    }

    fun deleteAppointment(appointment: Appointment) {
        viewModelScope.launch { repository.deleteAppointment(appointment); syncWithCloud() }
    }

    fun addMedicalRecord(title: String, category: String, fileUri: String, notes: String?) {
        viewModelScope.launch { repository.insertMedicalRecord(MedicalRecord(title = title, category = category, date = System.currentTimeMillis(), fileUri = fileUri, notes = notes)); syncWithCloud() }
    }

    fun deleteMedicalRecord(record: MedicalRecord) {
        viewModelScope.launch { repository.deleteMedicalRecord(record); syncWithCloud() }
    }

    fun addFamilyMember(name: String, phoneNumber: String, relation: String = "Family") {
        viewModelScope.launch { repository.insertFamilyMember(FamilyMember(name = name, phoneNumber = phoneNumber, relation = relation)); syncWithCloud() }
    }

    fun deleteFamilyMember(member: FamilyMember) {
        viewModelScope.launch { repository.deleteFamilyMember(member); syncWithCloud() }
    }

    fun addAshaEvent(title: String, date: Long, location: String, description: String, type: String, time: String? = null, organizer: String? = null, lat: Double? = null, lon: Double? = null) {
        viewModelScope.launch {
            val event = AshaEvent(title = title, date = date, location = location, description = description, type = type, time = time, organizer = organizer, latitude = lat, longitude = lon, reminderEnabled = true)
            repository.addAshaEventToCloud(event)
            com.example.arogyasahaya.utils.AshaReminderWorker.scheduleReminders(getApplication(), event.id, title, location, time ?: "", date)
        }
    }

    fun updateAshaEventStatus(event: AshaEvent, status: String) {
        viewModelScope.launch { repository.updateAshaEvent(event.copy(status = status)) }
    }

    fun toggleAshaEventReminder(event: AshaEvent) {
        viewModelScope.launch {
            val updated = event.copy(reminderEnabled = !event.reminderEnabled)
            repository.updateAshaEventLocally(updated)
            if (updated.reminderEnabled) {
                com.example.arogyasahaya.utils.AshaReminderWorker.scheduleReminders(getApplication(), updated.id, updated.title, updated.location, updated.time ?: "", updated.date)
            } else {
                com.example.arogyasahaya.utils.AshaReminderWorker.cancelReminders(getApplication(), updated.id)
            }
        }
    }

    private fun scheduleReminder(medicine: Medicine) {
        val data = Data.Builder().putInt("id", medicine.id).putString("name", medicine.name).build()
        val delay = medicine.time - System.currentTimeMillis()
        if (delay > 0) {
            val request = OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).setInputData(data).addTag("med_${medicine.id}").build()
            WorkManager.getInstance(getApplication()).enqueue(request)
        }
    }

    fun populateMockData() { viewModelScope.launch { repository.populateMockData() } }

    fun clearAllData() { viewModelScope.launch { repository.clearAllData(); repository.clearAshaEvents() } }
}
