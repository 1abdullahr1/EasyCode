package com.easycode.ide

import android.app.Application
import com.easycode.ide.data.db.EasyCodeDatabase
import com.easycode.ide.data.repository.FiddleRepository
import com.easycode.ide.util.ThemeManager

class EasyCodeApp : Application() {

    val database by lazy { EasyCodeDatabase.getInstance(this) }
    val fiddleRepository by lazy { FiddleRepository(database.fiddleDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ThemeManager.applySavedTheme(this)
    }

    companion object {
        lateinit var instance: EasyCodeApp
            private set
    }
}
