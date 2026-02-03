package com.metrolist.music.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import com.metrolist.music.CrashActivity
import com.metrolist.music.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val stackTrace = StringWriter().apply { throwable.printStackTrace(PrintWriter(this)) }.toString()
            
            val logcat = try {
                val process = Runtime.getRuntime().exec("logcat -d -v threadtime")
                val bufferedReader = process.inputStream.bufferedReader()
                bufferedReader.use { it.readText() }
            } catch (e: Exception) {
                "Failed to retrieve logcat: ${e.message}"
            }

            val report = buildString {
                appendLine("Metrolist Crash Report")
                appendLine("========================")
                appendLine("Time: $timestamp")
                appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
                appendLine("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                appendLine("Brand: ${Build.BRAND}")
                appendLine("Board: ${Build.BOARD}")
                appendLine("Hardware: ${Build.HARDWARE}")
                appendLine("Architecture: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
                appendLine("\nStack Trace:")
                appendLine(stackTrace)
                appendLine("\nLogcat:")
                // Limit logcat to last 2000 lines to avoid too large files
                val lines = logcat.lines()
                val lastLines = if (lines.size > 2000) lines.takeLast(2000) else lines
                appendLine(lastLines.joinToString("\n"))
            }

            val file = File(context.cacheDir, "crash_report.txt")
            file.writeText(report)

            val intent = Intent(context, CrashActivity::class.java).apply {
                putExtra("CRASH_REPORT_PATH", file.absolutePath)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)

            Process.killProcess(Process.myPid())
            exitProcess(10)
        } catch (e: Exception) {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
