package com.identusbook.flighttix

import android.app.Application
import android.util.Log
import com.identusbook.flighttix.agent.Identus
import com.identusbook.flighttix.agent.IdentusStatus
import com.identusbook.flighttix.agent.IdentusStatusState

/** App entry — initializes the Identus singleton with an application Context. */
class FlightTixApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Identus.init(this)
        installBackgroundCrashGuard()
    }

    /**
     * The Identus SDK runs DIDComm/mediation on internal coroutine scopes that lack a
     * supervisor/handler; when the backend is unreachable those can throw on a background
     * thread. Surface such failures as an Error status instead of killing the app. Genuine
     * main-thread crashes are still delegated to the platform handler.
     */
    private fun installBackgroundCrashGuard() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("FlightTix", "Uncaught exception on ${thread.name}", throwable)
            if (thread === Thread.currentThread() && thread.name == "main") {
                previous?.uncaughtException(thread, throwable)
            } else {
                IdentusStatus.set(IdentusStatusState.Error(throwable.message ?: throwable.toString()))
            }
        }
    }
}
