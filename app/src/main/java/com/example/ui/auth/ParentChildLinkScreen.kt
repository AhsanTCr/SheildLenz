package com.example.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ParentChildRepository
import com.example.services.protection.ParentChildSyncController
import com.example.ui.components.CyberTopBar
import com.example.ui.parents.ParentsScreen

@Composable
fun ParentChildLinkScreen(
    repository: ParentChildRepository,
    preferences: AegisPreferences,
    syncController: ParentChildSyncController? = null,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CyberTopBar(
                title = "Parent & Child Link",
                subtitle = "Guardian Control",
                onNavigateBack = onBack
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ParentsScreen(
                repository = repository,
                preferences = preferences,
                syncController = syncController
            )
        }
    }
}
