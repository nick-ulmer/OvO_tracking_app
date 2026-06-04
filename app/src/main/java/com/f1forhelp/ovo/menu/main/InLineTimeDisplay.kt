package com.f1forhelp.ovo.menu.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.f1forhelp.ovo.AppManager
import com.f1forhelp.ovo.SettingsStore.GeneralSettings
import java.time.DateTimeException
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun InlineTimeDisplay(
    onRecord: (Long) -> Unit) {
    // States
    var month by remember { mutableStateOf(1) }
    var day by remember { mutableStateOf(1) }
    var hour by remember { mutableStateOf(0) }
    var minute by remember { mutableStateOf(0) }

    val dateValid by remember(month, day, hour, minute, GeneralSettings.chosenTzFull) {
        derivedStateOf {
            try {
                ZonedDateTime.of(
                    ZonedDateTime.now().year,
                    month,
                    day,
                    hour,
                    minute,
                    0,
                    0,
                    ZoneId.of(GeneralSettings.chosenTzFull)
                )
                true
            } catch (e: DateTimeException) {
                false
            }
        }
    }

    val validColor = if (dateValid) Color.LightGray else Color(0xFFFFB3B3)

    fun updateToSystemTime() {
        val now = ZonedDateTime.now(ZoneId.of(GeneralSettings.chosenTzFull))
        month = now.monthValue
        day = now.dayOfMonth
        hour = now.hour
        minute = now.minute
    }

    LaunchedEffect(Unit) { updateToSystemTime() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal=5.dp, vertical=15.dp)
            //.background(validColor, shape = RoundedCornerShape(56.dp))
            .background(validColor, shape = RoundedCornerShape(CornerSize(percent = 100)))
            .padding(vertical=10.dp, horizontal=20.dp),
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        Column{
            Spacer(modifier=Modifier.size(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ResetTimeFieldsButton {
                    updateToSystemTime()
                    AppManager.instance.popupMessage(context,"Reset time input fields")
                }

                // Month
                StepperNumber(value = month, onValueChange = { month = it }, min = 1, max = 12)
                Text(" / ", style = MaterialTheme.typography.bodyLarge)

                // Day
                StepperNumber(value = day, onValueChange = { day = it }, min = 1, max = 31)
                Text("  ", style = MaterialTheme.typography.bodyLarge)

                // Hour
                StepperNumber(value = hour, onValueChange = { hour = it }, min = 0, max = 23)
                Text(" : ", style = MaterialTheme.typography.bodyLarge)

                // Minute
                StepperNumber( value = minute, onValueChange = { minute = it }, min = 0, max = 59)


                Text("  ", style = MaterialTheme.typography.bodyLarge)
                //Spacer(modifier = Modifier.width(4.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimezoneDropdown()

                    Spacer(modifier = Modifier.height(4.dp))

                    if (dateValid) {
                        RecordEventWithConfirmation(
                            month,
                            day,
                            hour,
                            minute,
                            GeneralSettings.chosenTzFull,
                            onRecord,
                            LocalContext.current
                        )
                    } else {
                        Button(
                            onClick = {  },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.size(56.dp),
                            contentPadding = PaddingValues(0.dp),
                            enabled = false
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FiberManualRecord,
                                contentDescription = "Record",
                                tint = Color.White // icon color
                            )
                        }
                    }
                }

            } // end row
            Spacer(modifier=Modifier.size(5.dp))
        } // end column
    }
}

@Composable
fun ResetTimeFieldsButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier.size(56.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Restart",
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun StepperNumber(value: Int, onValueChange: (Int) -> Unit, min: Int, max: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Up button
        Button(
            onClick = { if (value < max) onValueChange(value + 1) },
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp)
        ) { Text("▲") }

        val formatted = String.format("%02d", value)
        // Number display
        Text(formatted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(4.dp))

        // Down button
        Button(
            onClick = { if (value > min) onValueChange(value - 1) },
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp)
        ) { Text("▼") }
    }
}