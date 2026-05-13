package com.example.arogyasahaya.utils

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import java.util.*

object ArogyaAssistant {
    private var tts: TextToSpeech? = null

    fun init(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                if (status != TextToSpeech.ERROR) {
                    tts?.language = Locale.getDefault()
                }
            }
        }
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun parseCommand(command: String): AssistantAction {
        val cmd = command.lowercase()
        return when {
            cmd.contains("log") && (cmd.contains("bp") || cmd.contains("pressure")) -> {
                val numbers = "\\d+".toRegex().findAll(cmd).map { it.value }.toList()
                if (numbers.size >= 2) {
                    AssistantAction.LogBP(numbers[0].toInt(), numbers[1].toInt())
                } else {
                    AssistantAction.Navigate("vitals")
                }
            }
            cmd.contains("medicine") || cmd.contains("remind") -> AssistantAction.Navigate("medicines")
            cmd.contains("trend") || cmd.contains("graph") || cmd.contains("chart") -> AssistantAction.Navigate("trends")
            cmd.contains("vault") || cmd.contains("record") || cmd.contains("file") -> AssistantAction.Navigate("vault")
            cmd.contains("appointment") || cmd.contains("doctor") -> AssistantAction.Navigate("appointments")
            cmd.contains("sos") || cmd.contains("emergency") || cmd.contains("help") -> AssistantAction.Emergency
            cmd.contains("health score") || cmd.contains("my status") || cmd.contains("how am i") -> AssistantAction.CheckHealthScore
            cmd.contains("camp") || cmd.contains("event") || cmd.contains("asha") -> AssistantAction.QueryEvents
            (cmd.contains("attend") || cmd.contains("joining")) && (cmd.contains("polio") || cmd.contains("drive") || cmd.contains("camp")) -> AssistantAction.AttendPolio
            else -> AssistantAction.Unknown
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

sealed class AssistantAction {
    data class LogBP(val sys: Int, val dia: Int) : AssistantAction()
    data class Navigate(val destination: String) : AssistantAction()
    object Emergency : AssistantAction()
    object CheckHealthScore : AssistantAction()
    object QueryEvents : AssistantAction()
    object AttendPolio : AssistantAction()
    object Unknown : AssistantAction()
}
