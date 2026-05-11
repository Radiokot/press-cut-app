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

    private fun initLogging() {
        // The Logback configuration is in the app/src/main/assets/logback.xml

        @Suppress(
            "KotlinConstantConditions",
            "RedundantSuppression",
            "SimplifyBooleanWithConstants",
        )
        System.setProperty(
            "LOG_LEVEL",
            if (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "devRelease")
                "TRACE"
            else
                "INFO"
        )

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
