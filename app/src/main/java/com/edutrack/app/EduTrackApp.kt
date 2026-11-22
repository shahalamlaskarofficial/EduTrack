package com.edutrack.app

import android.app.Application
import com.edutrack.app.data.local.EduTrackDatabase
import com.edutrack.app.data.preferences.UserPreferencesManager
import com.edutrack.app.data.repository.EduTrackRepository

class EduTrackApp : Application() {
    lateinit var repository: EduTrackRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = EduTrackDatabase.getInstance(this)
        val prefs = UserPreferencesManager(this)
        repository = EduTrackRepository(database.eduTrackDao(), prefs)
    }
}
