package com.mailgw.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mailgw.data.repository.MailRepository

class MailViewModelFactory(
    private val repository: MailRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MailViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}