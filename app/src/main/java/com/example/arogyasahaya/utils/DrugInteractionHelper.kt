package com.example.arogyasahaya.utils

import android.content.Context
import com.example.arogyasahaya.R

data class Interaction(
    val drug1: String,
    val drug2: String,
    val riskLevel: RiskLevel,
    val description: String
)

enum class RiskLevel {
    HIGH, MODERATE, LOW
}

object DrugInteractionHelper {
    private val interactions = listOf(
        Interaction("Warfarin", "Aspirin", RiskLevel.HIGH, "Increased risk of severe bleeding. Consult your doctor immediately."),
        Interaction("Aspirin", "Ibuprofen", RiskLevel.MODERATE, "May reduce the effectiveness of Aspirin for heart protection."),
        Interaction("Metformin", "Contrast", RiskLevel.HIGH, "Risk of lactic acidosis. Stop Metformin 48 hours before any CT scan."),
        Interaction("Lisinopril", "Spironolactone", RiskLevel.MODERATE, "Risk of dangerously high potassium levels."),
        Interaction("Atorvastatin", "Clarithromycin", RiskLevel.HIGH, "Increased risk of muscle damage (Rhabdomyolysis)."),
        Interaction("Sildenafil", "Nitroglycerin", RiskLevel.HIGH, "Dangerously low blood pressure risk."),
        Interaction("Digoxin", "Amiodarone", RiskLevel.HIGH, "Risk of Digoxin toxicity."),
        Interaction("Ciprofloxacin", "Antacid", RiskLevel.LOW, "Antacids may reduce the absorption of the antibiotic.")
    )

    fun checkInteraction(newDrug: String, currentDrugs: List<String>): Interaction? {
        for (current in currentDrugs) {
            val match = interactions.find { 
                (it.drug1.equals(newDrug, ignoreCase = true) && it.drug2.equals(current, ignoreCase = true)) ||
                (it.drug2.equals(newDrug, ignoreCase = true) && it.drug1.equals(current, ignoreCase = true))
            }
            if (match != null) return match
        }
        return null
    }
}
