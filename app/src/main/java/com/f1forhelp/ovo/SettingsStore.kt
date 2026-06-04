package com.f1forhelp.ovo

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit
import com.f1forhelp.ovo.SettingsStore.NotificationSettings.enabled
import com.f1forhelp.ovo.SettingsStore.NotificationSettings.notifications
import com.f1forhelp.ovo.SettingsStore.NotificationSettings.scheduledNotifications
import com.f1forhelp.ovo.SettingsStore.NotificationSettings.windowEnd
import com.f1forhelp.ovo.SettingsStore.NotificationSettings.windowStart
import com.f1forhelp.ovo.notifications.NotificationService

data class NotificationObject(
    val name: String, // Custom name if desired
    val enabled: Boolean, // Currently being used or not
    val type: NotificationType, // How the value is used
    val value: Double // Relative time before OR after predicted bleed event
) {
    fun notificationMessage(): String {
        return when (type) {
            NotificationType.DAY_OF ->
                "Your cycle is predicted to start today."

            NotificationType.MAD -> {
                val coverage = madCoveragePercent(value)
                "You are now within ±$value median absolute deviation(s) of the predicted cycle start. Roughly $coverage% of your historical cycles have fallen within this range."
            }

            NotificationType.DAYS ->
                "Your predicted cycle is approaching in $value day(s)."

            NotificationType.OVO_DAY ->
                "Your predicted ovulation day is today."
        }
    }
    fun madCoveragePercent(mads: Double): Int {
        return when {
            mads < 0.75 -> 26   // about 0.5 MAD
            mads < 1.25 -> 50   // about 1 MAD
            mads < 1.75 -> 69   // about 1.5 MAD
            mads < 2.25 -> 82   // about 2 MAD
            mads < 2.75 -> 91   // about 2.5 MAD
            else -> 96          // about 3 MAD
        }
    }
}
enum class NotificationType {
    DAYS, // Days from PREDICTED BLEED EVENT
    MAD, // median average deviations from PREDICTED BLEED EVENT
    DAY_OF, // Day of PREDICTED BLEED EVENT
    OVO_DAY // Day of predicted ovulation
}

object SettingsStore {
    private const val PREFS_NAME = "prefs"
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    object GeneralSettings {
        val timeZoneList = listOf(
            "🇺🇸 -5" to "US/Eastern", // US Emoji
            "🇺🇸 -6" to "US/Central", // US Emoji
            "🇯🇵 +9" to "Asia/Tokyo", // Japan Emoji
            "🇨🇳 +8" to "Asia/Shanghai" // China Emoji
        )
        var chosenTimeZone = mutableIntStateOf(0)
        val chosenTzShort: String
            get() = timeZoneList[chosenTimeZone.intValue].first
        val chosenTzFull: String
            get() = timeZoneList[chosenTimeZone.intValue].second
        fun save() {
            prefs.edit {
                putInt("chosenTimeZone", chosenTimeZone.intValue)
            }
        }
        fun load() {
            with (GeneralSettings) {
                chosenTimeZone.intValue = prefs.getInt("chosenTimeZone", 0)
            }
        }
    }
    object NotificationSettings {
        var enabled = mutableStateOf(false)
        var windowStart = mutableIntStateOf(0)
        var windowEnd = mutableIntStateOf(0)
        val notifications = mutableStateListOf<NotificationObject>()
        var scheduledNotifications:Set<String> = emptySet()
        fun updateScheduledNotifications(set: Set<String>) { scheduledNotifications = set; save() }


        fun add(newNotification: NotificationObject) {
            notifications.add(newNotification)
            save()
        }
        fun update(oldNotification: NotificationObject, newNotification: NotificationObject) {
            //val index = notifications.indexOf(oldNotification)
            val index = notifications.indexOfFirst { it === oldNotification }
            if (index != -1) notifications[index] = newNotification
            save()
        }
        fun update(
                old: NotificationObject,
                name: String = old.name,
                enabled: Boolean = old.enabled,
                type: NotificationType = old.type,
                value: Double = old.value
        ) {
            update(old, NotificationObject(name, enabled, type, value))
        }
        fun remove(oldNotification: NotificationObject) {
            //notifications.remove(oldNotification)
            notifications.removeAll { it === oldNotification }
            save()
        }
        //@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
        fun setEnabled(context: Context, isEnabled: Boolean) {
            enabled.value = isEnabled
            if (isEnabled) {
                NotificationService.scheduleAllNotifications(context)
            } else {
                NotificationService.clearAllNotifications(context)
            }

            save()
        }
        fun save() {
            val json = gson.toJson(notifications)
            prefs.edit {
                putString("notifications", json)
                putBoolean("enabled", enabled.value)
                putInt("windowStart", windowStart.intValue)
                putInt("windowEnd", windowEnd.intValue)
                putStringSet("scheduledNotifications", scheduledNotifications)
            }
        }
        fun load() {
            val json = prefs.getString("notifications", null)
            val type = object : TypeToken<List<NotificationObject>>() {}.type
            val list: List<NotificationObject> = gson.fromJson(json, type) ?: emptyList()
            with (NotificationSettings) {
                notifications.clear()
                notifications.addAll(list)
                notifications.forEach { Log.d("Notifications:",it.toString()) }
                enabled.value = prefs.getBoolean("enabled", false)
                windowStart.intValue = prefs.getInt("windowStart", 0)
                windowEnd.intValue = prefs.getInt("windowEnd", 0)

                scheduledNotifications = prefs.getStringSet("scheduledNotifications", emptySet()) ?: emptySet()
            }
        }
    }
    object CalculationSettings {
        var inclusionCap = mutableIntStateOf(24) // How many recent cycle lengths are included
        var useSanityFilter = mutableStateOf(true)
        var sanityFilterMinDays = mutableIntStateOf(15)
        var sanityFilterMaxDays = mutableIntStateOf(60)
        var useOutlierFilter = mutableStateOf(true)
        var exclusionDeviationCap = mutableDoubleStateOf(2.0) // how many median standard deviations a value can be before excluding data point.


        // Weighted Settings
        var useWeighted = mutableStateOf(true) // do or don't use weighted calculation
        var weightedInclusionCap = mutableIntStateOf(48) // cap on included data before weighting
        var halfLife = mutableDoubleStateOf(10.0) // the half-life on the importance of a cycle length as it goes from recent to old

        fun edit(
            inclusionCap: Int = CalculationSettings.inclusionCap.intValue,
            useSanityFilter: Boolean = CalculationSettings.useSanityFilter.value,
            sanityFilterMinDays: Int = CalculationSettings.sanityFilterMinDays.intValue,
            sanityFilterMaxDays: Int = CalculationSettings.sanityFilterMaxDays.intValue,
            useOutlierFilter: Boolean = CalculationSettings.useOutlierFilter.value,
            exclusionDeviationCap: Double = CalculationSettings.exclusionDeviationCap.doubleValue,

            useWeighted: Boolean = CalculationSettings.useWeighted.value,
            weightedInclusionCap: Int = CalculationSettings.weightedInclusionCap.intValue,
            halfLife: Double = CalculationSettings.halfLife.doubleValue
        ) {
            this.inclusionCap.intValue = inclusionCap
            this.useSanityFilter.value = useSanityFilter
            this.sanityFilterMinDays.intValue = sanityFilterMinDays
            this.sanityFilterMaxDays.intValue = sanityFilterMaxDays
            this.useOutlierFilter.value = useOutlierFilter
            this.exclusionDeviationCap.doubleValue = exclusionDeviationCap

            this.useWeighted.value = useWeighted
            this.weightedInclusionCap.intValue = weightedInclusionCap
            this.halfLife.doubleValue = halfLife

            this.save()
        }
        fun save() {
            prefs.edit {
                putInt("inclusionCap", inclusionCap.intValue)
                putBoolean("useSanityFilter", useSanityFilter.value)
                putInt("sanityFilterMinDays", sanityFilterMinDays.intValue)
                putInt("sanityFilterMaxDays", sanityFilterMaxDays.intValue)
                putBoolean("useOutlierFilter", useOutlierFilter.value)
                putFloat("exclusionDeviationCap", exclusionDeviationCap.doubleValue.toFloat())

                putBoolean("useWeighted", useWeighted.value)
                putInt("weightedInclusionCap", weightedInclusionCap.intValue)
                putFloat("halfLife", halfLife.doubleValue.toFloat())
            }
        }
        fun load() {
            with (CalculationSettings) {
                inclusionCap.intValue = prefs.getInt("inclusionCap", 24)
                useSanityFilter.value = prefs.getBoolean("useSanityFilter", true)
                sanityFilterMinDays.intValue = prefs.getInt("sanityFilterMinDays", 15)
                sanityFilterMaxDays.intValue = prefs.getInt("sanityFilterMaxDays", 60)
                useOutlierFilter.value = prefs.getBoolean("useOutlierFilter", true)
                exclusionDeviationCap.doubleValue = prefs.getFloat("exclusionDeviationCap", 2F).toDouble()

                useWeighted.value = prefs.getBoolean("useWeighted", true)
                weightedInclusionCap.intValue = prefs.getInt("weightedInclusionCap", 48)
                halfLife.doubleValue = prefs.getFloat("halfLife", 10F).toDouble()
            }
        }
    }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        GeneralSettings.load()
        NotificationSettings.load()
        CalculationSettings.load()
    }



}