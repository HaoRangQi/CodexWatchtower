package com.codextraffic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TrafficViewModelFactory(
    private val repository: TrafficRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrafficViewModel::class.java)) {
            return TrafficViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

