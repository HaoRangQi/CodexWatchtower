package com.codextraffic

import android.content.Context

interface HiddenProjectStore {
    suspend fun hiddenProjectIds(): Set<String>

    suspend fun saveHiddenProjectIds(ids: Set<String>)
}

class SharedPreferencesHiddenProjectStore(
    context: Context,
) : HiddenProjectStore {
    private val preferences = context.getSharedPreferences("hidden_projects", Context.MODE_PRIVATE)

    override suspend fun hiddenProjectIds(): Set<String> =
        preferences.getStringSet(KeyHiddenProjectIds, emptySet()).orEmpty()

    override suspend fun saveHiddenProjectIds(ids: Set<String>) {
        preferences.edit().putStringSet(KeyHiddenProjectIds, ids).apply()
    }

    private companion object {
        const val KeyHiddenProjectIds = "hidden_project_ids"
    }
}
