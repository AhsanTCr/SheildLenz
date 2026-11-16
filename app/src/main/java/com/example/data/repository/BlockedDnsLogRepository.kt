package com.example.data.repository

import com.example.data.local.BlockedDnsLogDao
import com.example.data.local.BlockedDnsLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BlockedDnsLogItem(
    val id: Long = 0L,
    val domain: String,
    val reasonCategory: String,
    val matchedRule: String,
    val timestampMillis: Long
)

class BlockedDnsLogRepository(
    private val dao: BlockedDnsLogDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _liveLogs = MutableStateFlow<List<BlockedDnsLogItem>>(emptyList())
    val liveLogs: StateFlow<List<BlockedDnsLogItem>> = _liveLogs.asStateFlow()

    init {
        scope.launch {
            dao.observeRecent(MAX_LOG_ITEMS).collect { entities ->
                val mapped = entities.map {
                    BlockedDnsLogItem(
                        id = it.id,
                        domain = it.domain,
                        reasonCategory = it.reasonCategory,
                        matchedRule = it.matchedRule,
                        timestampMillis = it.timestampMillis
                    )
                }
                if (mapped.isNotEmpty() || _liveLogs.value.isEmpty()) {
                    _liveLogs.value = mapped
                }
            }
        }
    }

    fun recordBlockedQuery(
        domain: String,
        reasonCategory: String,
        matchedRule: String,
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        val cleanDomain = domain.trim().trimEnd('.')
        if (cleanDomain.isBlank()) return

        val item = BlockedDnsLogItem(
            id = timestampMillis,
            domain = cleanDomain,
            reasonCategory = reasonCategory,
            matchedRule = matchedRule,
            timestampMillis = timestampMillis
        )
        _liveLogs.value = (listOf(item) + _liveLogs.value).take(MAX_LOG_ITEMS)

        scope.launch {
            dao.insert(
                BlockedDnsLogEntity(
                    domain = cleanDomain,
                    reasonCategory = reasonCategory,
                    matchedRule = matchedRule,
                    timestampMillis = timestampMillis
                )
            )
            dao.trimOldEntries(MAX_LOG_ITEMS)
        }
    }

    fun removeEntriesForDomain(domain: String) {
        val clean = domain.trim().trimEnd('.').lowercase()
        _liveLogs.value = _liveLogs.value.filterNot { it.domain.lowercase() == clean }
        scope.launch {
            dao.deleteByDomain(clean)
        }
    }

    fun clearLogs() {
        _liveLogs.value = emptyList()
        scope.launch {
            dao.clearAll()
        }
    }

    companion object {
        const val MAX_LOG_ITEMS = 250
        const val CATEGORY_ADULT = "ADULT"
        const val CATEGORY_GAMBLING = "GAMBLING"
        const val CATEGORY_CUSTOM_DOMAIN = "CUSTOM_DOMAIN"
        const val CATEGORY_CUSTOM_KEYWORD = "CUSTOM_KEYWORD"
    }
}
