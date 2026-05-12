package com.example.arogyasahaya.data.repository

import androidx.lifecycle.LiveData
import com.example.arogyasahaya.data.local.dao.*
import com.example.arogyasahaya.data.local.entity.*
import androidx.lifecycle.asLiveData

import com.example.arogyasahaya.data.remote.ApiService

class HealthRepository(
    private val medicineDao: MedicineDao,
    private val vitalDao: VitalDao,
    private val symptomDao: SymptomDao,
    private val appointmentDao: AppointmentDao,
    private val medicalRecordDao: MedicalRecordDao,
    private val familyMemberDao: FamilyMemberDao,
    private val ashaEventDao: AshaEventDao,
    private val apiService: ApiService
) {
    val allMedicines: LiveData<List<Medicine>> = medicineDao.getAllMedicines()
    val allVitals: LiveData<List<Vital>> = vitalDao.getVitals()
    val allSymptoms: LiveData<List<Symptom>> = symptomDao.getAllSymptoms()
    val allAppointments: LiveData<List<Appointment>> = appointmentDao.getAllAppointments()
    val allMedicalRecords: LiveData<List<MedicalRecord>> = medicalRecordDao.getAllRecords()
    val allFamilyMembers: LiveData<List<FamilyMember>> = familyMemberDao.getAllFamilyMembers()
    val allAshaEvents: LiveData<List<AshaEvent>> = ashaEventDao.getAllEvents().asLiveData()

    suspend fun insertAshaEvent(event: AshaEvent) = ashaEventDao.insertEvent(event)
    suspend fun clearAshaEvents() = ashaEventDao.deleteAll()

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
    fun getRecentSymptoms(since: Long) = symptomDao.getRecentSymptoms(since)

    suspend fun insertAppointment(appointment: Appointment): Long = appointmentDao.insert(appointment)
    fun getUpcomingAppointments(currentTime: Long) = appointmentDao.getUpcomingAppointments(currentTime)
    suspend fun deleteAppointment(appointment: Appointment) = appointmentDao.delete(appointment)

    suspend fun insertMedicalRecord(record: MedicalRecord): Long = medicalRecordDao.insertRecord(record)
    suspend fun deleteMedicalRecord(record: MedicalRecord) = medicalRecordDao.deleteRecord(record)

    suspend fun populateMockData() {
        // Add Medicines
        insertMedicine(Medicine(name = "Amlodipine", dosage = "5mg", time = System.currentTimeMillis() + 3600000, totalTablets = 30, remainingTablets = 25, frequency = "Daily"))
        insertMedicine(Medicine(name = "Metformin", dosage = "500mg", time = System.currentTimeMillis() + 7200000, totalTablets = 60, remainingTablets = 45, frequency = "Twice Daily"))
        
        // Add Vitals
        insertVital(Vital(date = System.currentTimeMillis() - 86400000, systolic = 125, diastolic = 82, heartRate = 72, sugar = 110))
        insertVital(Vital(date = System.currentTimeMillis() - 172800000, systolic = 130, diastolic = 85, heartRate = 75, sugar = 115))
        
        // Add Family Members
        insertFamilyMember(FamilyMember(name = "Anjali", phoneNumber = "9876543210", relation = "Wife"))
        insertFamilyMember(FamilyMember(name = "Rahul", phoneNumber = "9123456789", relation = "Son"))
        
        // Add Medical Records
        insertMedicalRecord(MedicalRecord(title = "Annual Blood Test", category = "Lab Report", date = System.currentTimeMillis() - 2592000000, fileUri = "", notes = "All values normal."))
        insertMedicalRecord(MedicalRecord(title = "Chest X-Ray", category = "Scan", date = System.currentTimeMillis() - 5184000000, fileUri = "", notes = "Clear lungs."))
        
        // Add Symptoms
        insertSymptom(Symptom(date = System.currentTimeMillis() - 43200000, notes = "Mild headache in the morning", tags = "Headache"))
    }

    suspend fun clearAllData() {
        medicineDao.deleteAll()
        vitalDao.deleteAll()
        symptomDao.deleteAll()
        appointmentDao.deleteAll()
        medicalRecordDao.deleteAll()
        familyMemberDao.deleteAll()
    }

    // Cloud Sync Methods
    suspend fun syncVitalsToCloud(userId: String) {
        val localVitals = vitalDao.getVitalsList() // Assuming this exists or I'll add it
        localVitals.forEach { vital ->
            try {
                apiService.syncVital(vital)
            } catch (e: Exception) {
                // Log error or handle retry
            }
        }
    }

    suspend fun fetchVitalsFromCloud(userId: String) {
        try {
            val cloudVitals = apiService.getVitals(userId)
            cloudVitals.forEach { vital ->
                vitalDao.insert(vital)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
}
