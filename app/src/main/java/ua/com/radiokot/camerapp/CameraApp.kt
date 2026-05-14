/* Copyright 2026 Oleg Koretsky

   This file is part of the Press-Cut,
   a digital postage stamp cutter Android app.

   Press-Cut is free software: you can redistribute it
   and/or modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation, either version 3 of the License,
   or (at your option) any later version.

   Press-Cut is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
   See the GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Press-Cut. If not, see <http://www.gnu.org/licenses/>.
*/

package ua.com.radiokot.camerapp

import android.app.Application
import android.os.Environment
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ua.com.radiokot.camerapp.cut.cutModule
import ua.com.radiokot.camerapp.intro.introModule
import ua.com.radiokot.camerapp.io.ioModule
import ua.com.radiokot.camerapp.stamps.stampsModule
import ua.com.radiokot.camerapp.util.KoinSlf4jLogger
import java.io.File
import java.lang.Thread.UncaughtExceptionHandler
import kotlin.system.exitProcess

class CameraApp : Application() {

    private val log by lazy {
        KotlinLogging.logger("App")
    }

    override fun onCreate() {
        super.onCreate()

        initLogging()

        startKoin {
            logger(KoinSlf4jLogger)
            androidContext(this@CameraApp)
            modules(
                ioModule,
                cutModule,
                stampsModule,
                introModule,
            )
        }
    }

    @Suppress(
        "KotlinConstantConditions",
        "RedundantSuppression",
        "SimplifyBooleanWithConstants",
    )
    private fun initLogging() {
        // The Logback configuration is in the app/src/main/assets/logback.xml

        System.setProperty(
            "LOG_LEVEL",
            if (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "devRelease")
                "TRACE"
            else
                "INFO"
        )

        if (BuildConfig.BUILD_TYPE != "release") {
            try {
                val logFolder =
                    File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                        "PressCutLogs"
                    )
                        .also(File::mkdirs)

                System.setProperty(
                    "LOG_FILE_DIRECTORY",
                    logFolder.path
                )
            } catch (e: Exception) {
                log.error(e) {
                    "initLogging(): failed log file folder initialization"
                }
            }
        }

        val defaultUncaughtExceptionHandler: UncaughtExceptionHandler? =
            Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            log.error(exception) { "Fatal exception\n" }

            if (defaultUncaughtExceptionHandler != null) {
                defaultUncaughtExceptionHandler.uncaughtException(thread, exception)
            } else {
                exitProcess(10)
            }
        }

        log.trace {
            "initLogging(): trace logger enabled"
        }
        log.debug {
            "initLogging(): debug logger enabled"
        }
        log.info {
            "initLogging(): info logger enabled"
        }
    }
}
