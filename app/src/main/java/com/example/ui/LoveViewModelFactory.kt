package com.example.ui

import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.ChatRepository

class LoveViewModelFactory(
    private val repository: ChatRepository,
    private val tts: TextToSpeech?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoveViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoveViewModel(repository, tts) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
