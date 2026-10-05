// viewmodel/MailViewModel.kt
package com.mailgw.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mailgw.data.models.*
import com.mailgw.data.repository.MailRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MailViewModel(
    private val repository: MailRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _activeAccount = MutableStateFlow<Account?>(null)
    val activeAccount: StateFlow<Account?> = _activeAccount.asStateFlow()

    private val _domains = MutableStateFlow<List<Domain>>(emptyList())
    val domains: StateFlow<List<Domain>> = _domains.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageSummary>>(emptyList())
    val messages: StateFlow<List<MessageSummary>> = _messages.asStateFlow()

    private val _currentMessage = MutableStateFlow<MessageDetail?>(null)
    val currentMessage: StateFlow<MessageDetail?> = _currentMessage.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.getActiveAccountFlow().collect { account ->
                _activeAccount.value = account
                if (account != null) {
                    refreshMessages()
                }
            }
        }
    }

    fun loadAccounts() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getAllAccounts()
            result.onSuccess { accounts ->
                _accounts.value = accounts
            }.onFailure {
                _error.emit(it.message ?: "Failed to load accounts")
            }
            _isLoading.value = false
        }
    }

    fun loadDomains() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getDomains()
            result.onSuccess { domains ->
                _domains.value = domains
            }.onFailure {
                _error.emit(it.message ?: "Failed to load domains")
            }
            _isLoading.value = false
        }
    }

    suspend fun createAccount(username: String, domain: String, password: String): Result<Account> {
        return try {
            _isLoading.value = true
            val result = repository.createEmailAccount(username, domain, password)
            result.onSuccess { account ->
                loadAccounts()
                setActiveAccount(account)
            }
            result
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun setActiveAccount(account: Account) {
        repository.setActiveAccount(account)
        _activeAccount.value = account
        refreshMessages()
    }

    fun refreshMessages() {
        viewModelScope.launch {
            val account = _activeAccount.value ?: return@launch
            _isLoading.value = true
            val result = repository.getMessages(account)
            result.onSuccess { messages ->
                _messages.value = messages
            }.onFailure {
                _error.emit(it.message ?: "Failed to load messages")
            }
            _isLoading.value = false
        }
    }

    fun loadMessageDetail(messageId: String) {
        viewModelScope.launch {
            val account = _activeAccount.value ?: return@launch
            _isLoading.value = true
            val result = repository.getMessageDetail(account, messageId)
            result.onSuccess { message ->
                _currentMessage.value = message
                refreshMessages() // Update seen status in list
            }.onFailure {
                _error.emit(it.message ?: "Failed to load message")
            }
            _isLoading.value = false
        }
    }

    fun deleteCurrentMessage() {
        viewModelScope.launch {
            val account = _activeAccount.value ?: return@launch
            val message = _currentMessage.value ?: return@launch
            _isLoading.value = true
            val result = repository.deleteMessage(account, message.id)
            result.onSuccess {
                _currentMessage.value = null
                refreshMessages()
            }.onFailure {
                _error.emit(it.message ?: "Failed to delete message")
            }
            _isLoading.value = false
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteAccountPermanently(account)
            result.onSuccess {
                loadAccounts()
                if (_activeAccount.value?.id == account.id) {
                    val nextAccount = _accounts.value.firstOrNull()
                    if (nextAccount != null) {
                        setActiveAccount(nextAccount)
                    } else {
                        _activeAccount.value = null
                    }
                }
            }.onFailure {
                _error.emit(it.message ?: "Failed to delete account")
            }
            _isLoading.value = false
        }
    }
}