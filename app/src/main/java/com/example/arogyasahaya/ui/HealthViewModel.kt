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
import com.example.arogyasahaya.utils.ReminderWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class HealthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: HealthRepository
    val allMedicines: LiveData<List<Medicine>>
    val allVitals: LiveData<List<Vital>>
    val allSymptoms: LiveData<List<Symptom>>
    val allAppointments: LiveData<List<Appointment>>
    val allMedicalRecords: LiveData<List<MedicalRecord>>
    val allFamilyMembers: LiveData<List<FamilyMember>>
    val allAshaEvents: LiveData<List<AshaEvent>>

    data class HealthInsight(val title: String, val subtitle: String, val icon: String, val color: Long)

    private val _dailyInsights = MutableLiveData<List<HealthInsight>>()
    val dailyInsights: LiveData<List<HealthInsight>> = _dailyInsights

    private val _isSyncing = MutableLiveData<Boolean>(false)
    val isSyncing: LiveData<Boolean> = _isSyncing

    fun syncInsights() {
        viewModelScope.launch {
            // Expanded to 15+ premium insights
            val insights = listOf(
                HealthInsight("Heart Health", "Walking 30 mins a day reduces heart risk by 35%.", "DirectionsWalk", 0xFFE91E63),
                HealthInsight("Hydration", "Drink 8 glasses of water to keep your kidneys healthy.", "WaterDrop", 0xFF2196F3),
                HealthInsight("Sleep Quality", "7-8 hours of sleep helps regulate blood pressure.", "Bedtime", 0xFF673AB7),
                HealthInsight("Sugar Control", "Reduce refined carbs to keep your sugar levels stable.", "Restaurant", 0xFFF44336),
                HealthInsight("Mental Peace", "5 mins of deep breathing lowers cortisol levels.", "SelfImprovement", 0xFF4CAF50),
                HealthInsight("Joint Care", "Stay active to keep your joints lubricated.", "DirectionsWalk", 0xFFFF9800),
                HealthInsight("Stress Less", "Laughter is the best medicine for a healthy heart.", "Psychology", 0xFF9C27B0),
                HealthInsight("Eye Health", "Follow the 20-20-20 rule to reduce eye strain.", "RemoveRedEye", 0xFF3F51B5),
                HealthInsight("Salt Intake", "Less salt means lower risk of high blood pressure.", "Restaurant", 0xFF795548),
                HealthInsight("Posture", "Sit straight to avoid chronic back and neck pain.", "Person", 0xFF607D8B),
                HealthInsight("Social Care", "Connecting with family boosts mental wellness.", "Groups", 0xFFE91E63),
                HealthInsight("Brain Power", "Reading for 15 mins daily keeps the brain sharp.", "MenuBook", 0xFF2196F3),
                HealthInsight("Skin Health", "Sunlight is good, but too much causes skin damage.", "WbSunny", 0xFFFFC107),
                HealthInsight("Fruit Punch", "Eating one apple a day keeps the doctor away.", "Restaurant", 0xFF8BC34A),
                HealthInsight("Meditation", "Start your day with 2 mins of mindful silence.", "SelfImprovement", 0xFF009688),
                HealthInsight("Digital Detox", "Put away screens 1 hour before you sleep.", "PhonelinkOff", 0xFFF44336)
            )
            _dailyInsights.value = insights
        }
    }

    fun populateMockData() {
        viewModelScope.launch {
            repository.populateMockData()
        }
    }

    private val _latestVital = MutableLiveData<Vital?>()
    val latestVital: LiveData<Vital?> = _latestVital

    init {
        val database = AppDatabase.getDatabase(application)
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
        syncInsights()
        syncAshaEvents() // Start with a real-time sync
    }

    private val _healthScore = androidx.lifecycle.MediatorLiveData<Float>().apply {
        addSource(allMedicines) { calculateHealthScore() }
        addSource(allVitals) { calculateHealthScore() }
    }
    val healthScore: LiveData<Float> = _healthScore

    private fun calculateHealthScore() {
        viewModelScope.launch {
            val meds = allMedicines.value ?: emptyList()
            val vitals = allVitals.value ?: emptyList()
            
            if (meds.isEmpty() && vitals.isEmpty()) {
                _healthScore.postValue(0f) // Start at 0 if no data
                return@launch
            }

            var score = 50f // Start at neutral 50 if there's at least some data
            
            // Adherence Factor (Up to 30 points)
            if (meds.isNotEmpty()) {
                val taken = meds.count { it.isTaken }
                val adherence = taken.toFloat() / meds.size
                score += (adherence * 30f)
            }
            
            // Vital Factor (Up to 20 points)
            if (vitals.isNotEmpty()) {
                vitals.lastOrNull()?.let { last ->
                    var vitalPoints = 20f
                    if (last.systolic > 140 || last.diastolic > 90) vitalPoints -= 10f
                    if (last.sugar != null && (last.sugar!! > 180 || last.sugar!! < 70)) vitalPoints -= 10f
                    score += vitalPoints
                }
            } else {
                score += 10f // Bonus for having meds but no vitals yet
            }
            
            _healthScore.postValue(score.coerceIn(0f, 100f))
        }
    }

    fun addFamilyMember(name: String, phoneNumber: String, relation: String = "Family") {
        viewModelScope.launch {
            repository.insertFamilyMember(FamilyMember(name = name, phoneNumber = phoneNumber, relation = relation))
        }
    }

    fun deleteFamilyMember(member: FamilyMember) {
        viewModelScope.launch {
            repository.deleteFamilyMember(member)
        }
    }

    fun addMedicine(name: String, dosage: String, time: Long, totalTablets: Int, frequency: String, pillImageUri: String? = null) {
        viewModelScope.launch {
            val medicine = Medicine(
                name = name,
                dosage = dosage,
                time = time,
                totalTablets = totalTablets,
                remainingTablets = totalTablets,
                frequency = frequency,
                pillImageUri = pillImageUri
            )
            val id = repository.insertMedicine(medicine)
            scheduleReminder(medicine.copy(id = id.toInt()))
        }
    }

    fun markAsTaken(medicine: Medicine) {
        viewModelScope.launch {
            repository.markMedicineAsTaken(medicine.id)
        }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            repository.deleteMedicine(medicine)
        }
    }

    fun addVital(systolic: Int, diastolic: Int, heartRate: Int, sugar: Int?, notes: String?) {
        viewModelScope.launch {
            val vital = Vital(
                date = System.currentTimeMillis(),
                systolic = systolic,
                diastolic = diastolic,
                heartRate = heartRate,
                sugar = sugar,
                notes = notes
            )
            repository.insertVital(vital)
            refreshLatestVital()
        }
    }

    private fun refreshLatestVital() {
        viewModelScope.launch {
            _latestVital.value = repository.getLatestVital()
        }
    }

    fun addSymptom(notes: String, tags: List<String>) {
        viewModelScope.launch {
            val symptom = Symptom(
                date = System.currentTimeMillis(),
                notes = notes,
                tags = tags.joinToString(",")
            )
            repository.insertSymptom(symptom)
        }
    }

    fun addAppointment(doctorName: String, dateTime: Long, notes: String) {
        viewModelScope.launch {
            val appointment = Appointment(
                doctorName = doctorName,
                dateTime = dateTime,
                notes = notes
            )
            repository.insertAppointment(appointment)
        }
    }

    fun deleteAppointment(appointment: Appointment) {
        viewModelScope.launch {
            repository.deleteAppointment(appointment)
        }
    }

    fun addMedicalRecord(title: String, category: String, fileUri: String, notes: String?) {
        viewModelScope.launch {
            val record = MedicalRecord(
                title = title,
                category = category,
                date = System.currentTimeMillis(),
                fileUri = fileUri,
                notes = notes
            )
            repository.insertMedicalRecord(record)
        }
    }

    fun deleteMedicalRecord(record: MedicalRecord) {
        viewModelScope.launch {
            repository.deleteMedicalRecord(record)
        }
    }

    private fun scheduleReminder(medicine: Medicine) {
        val data = Data.Builder()
            .putInt("id", medicine.id)
            .putString("name", medicine.name)
            .build()

        val delay = medicine.time - System.currentTimeMillis()
        if (delay > 0) {
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag("med_${medicine.id}")
                .build()

            WorkManager.getInstance(getApplication()).enqueue(request)
        }
    }

    fun syncWithCloud() {
        viewModelScope.launch {
            _isSyncing.value = true
            val userId = "user_123" // Placeholder for real auth
            repository.syncVitalsToCloud(userId)
            repository.fetchVitalsFromCloud(userId)
            _isSyncing.value = false
        }
    }

    fun syncAshaEvents() {
        viewModelScope.launch {
            _isSyncing.value = true
            kotlinx.coroutines.delay(2000) // Simulate network delay
            
            repository.clearAshaEvents()
            
            val now = System.currentTimeMillis()
            val day = 86400000L
            
            val events = listOf(
                AshaEvent(title = "Polio Vaccination Drive", date = now, location = "Primary Health Center", description = "LIVE NOW: Mandatory vaccination for children under 5.", type = "CAMP"),
                AshaEvent(title = "Maternal Health Checkup", date = now + day * 1, location = "At Your Home", description = "Tomorrow: Routine prenatal/postnatal check by ASHA worker.", type = "VISIT"),
                AshaEvent(title = "BP & Sugar Screening", date = now + day * 3, location = "Village Panchayat Hall", description = "Free screening for all senior citizens.", type = "CAMP"),
                AshaEvent(title = "Nutrition Workshop", date = now + day * 5, location = "Anganwadi Center", description = "Healthy cooking and diet tips for families.", type = "CAMP"),
                AshaEvent(title = "Elderly Care Follow-up", date = now + day * 7, location = "At Your Home", description = "Monthly health monitoring and medicine review.", type = "VISIT"),
                AshaEvent(title = "Eye Checkup Camp", date = now + day * 10, location = "Government School", description = "Free vision screening and cataract check.", type = "CAMP"),
                AshaEvent(title = "General Health Mela", date = now + day * 14, location = "Community Center", description = "Mega health camp with specialists.", type = "CAMP")
            )

            events.forEach { repository.insertAshaEvent(it) }
            _isSyncing.value = false
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            repository.clearAshaEvents()
        }
    }
}
