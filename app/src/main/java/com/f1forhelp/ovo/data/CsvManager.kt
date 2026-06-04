package com.f1forhelp.ovo.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.OutputStreamWriter
import java.io.IOException
import java.time.Instant
import java.time.ZoneId

object CsvManager { // CSV: Comma-separated values
    fun saveBleedEvents(context: Context) {
        val folder = context.getExternalFilesDir(null)
        if (!folder!!.exists()) folder.mkdir()
        val file = File(folder, "bleedEvents.csv")

        //writeBleedEventsToFile(context, file)
        writeBleedEventsToFile(file)

        // Now copy file for backups
        val backupsFolder = File(context.getExternalFilesDir(null), "backups")
        if (!backupsFolder.exists()) backupsFolder.mkdir()
        val destFile = File(backupsFolder, "bleedEvents_${System.currentTimeMillis()}.csv")

        // Copy contents
        file.inputStream().use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    fun writeBleedEventsToFile(
        file: File,
        //bleedEventDao: BleedEventDao,
        format: BleedEvent.Format = BleedEvent.Format.EPOCH_MILLIS
    ) {
        //val events = bleedEventDao.getAll()
        val events = BleedEvent.getAll()

        file.printWriter().use { out ->
            // Header
            out.println(
                when (format) {
                    BleedEvent.Format.EPOCH_MILLIS -> "epochMillis"
                    BleedEvent.Format.ZONED_DATE_TIME -> "zonedDateTime"
                }
            )

            // Rows
            events.forEach { event ->
                val value = when (format) {
                    BleedEvent.Format.EPOCH_MILLIS -> {
                        event.epochMillis.toString()
                    }
                    BleedEvent.Format.ZONED_DATE_TIME -> {
                        Instant.ofEpochMilli(event.epochMillis)
                            .atZone(ZoneId.systemDefault())
                            .toString()
                    }
                }

                out.println(value)
            }
        }
    }

    fun writeBleedEventsToUri(
        context: Context,
        uri: Uri,
        //events: List<BleedEvent>,
        format: BleedEvent.Format
    ) {
        val events = BleedEvent.getAll()

        context.contentResolver.openOutputStream(uri)?.use { output ->
            OutputStreamWriter(output).use { out ->

                // Header
                out.appendLine(
                    when (format) {
                        BleedEvent.Format.EPOCH_MILLIS -> "epochMillis"
                        BleedEvent.Format.ZONED_DATE_TIME -> "zonedDateTime"
                    }
                )

                // Rows
                events.forEach { event ->
                    val value = when (format) {
                        BleedEvent.Format.EPOCH_MILLIS -> {
                            event.epochMillis.toString()
                        }
                        BleedEvent.Format.ZONED_DATE_TIME -> {
                            event.asFormattedString()
                        }
                    }

                    out.appendLine(value)
                }
            }
        } ?: throw IOException("Failed to open output stream")
    }
}