package com.smartcalc.ai

import android.app.Application
import com.smartcalc.ai.data.local.AppDatabase
import com.smartcalc.ai.data.remote.NetworkModule
import com.smartcalc.ai.data.repository.HistoryRepository
import com.smartcalc.ai.data.repository.SolverRepository
import com.smartcalc.ai.util.ImageProcessor
import com.smartcalc.ai.util.NetworkMonitor

class SmartCalcApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Manual dependency container - no external DI framework needed. */
class AppContainer(application: Application) {

    private val database by lazy { AppDatabase.getInstance(application) }

    val imageProcessor: ImageProcessor by lazy { ImageProcessor(application) }

    private val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(application) }

    val historyRepository: HistoryRepository by lazy {
        HistoryRepository(database.historyDao())
    }

    val solverRepository: SolverRepository by lazy {
        SolverRepository(
            api = NetworkModule.solverApi,
            networkMonitor = networkMonitor,
            imageProcessor = imageProcessor
        )
    }

    fun isOnline(): Boolean = networkMonitor.isOnline()
}
