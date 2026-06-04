package com.f1forhelp.ovo.menu.main

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.f1forhelp.ovo.data.BleedEvent
import com.f1forhelp.ovo.data.Cycle
import com.f1forhelp.ovo.menu.TopButtons
import java.time.DateTimeException
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun MenuMain(navController: NavController) {
    var candidateEpochMillis by remember { mutableStateOf(0L) }

    val onRecord: (Long) -> Unit = { inputEpochMillis ->
        candidateEpochMillis = inputEpochMillis
        BleedEvent(epochMillis = candidateEpochMillis).save()

        // Create new cycle data
        Cycle.generateFromMostRecent()

        navController.popBackStack()    // optional: go back
        navController.navigate("main") {
            launchSingleTop = true
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {
        var showMenu by remember { mutableStateOf(false) }

        TopButtons(navController,Icons.Default.Settings, onClick = { showMenu=true })


        if (showMenu) {
            SettingsDialogBox(onDismiss = {showMenu = false}, navController)
        } else {
            Spacer(modifier = Modifier.height(1.dp)) // ensures column isn't empty
        }

        CalendarGrid()

        Box(modifier=Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal=15.dp)
            .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(10.dp))
            .border(3.dp, Color.Black, shape = RoundedCornerShape(10.dp))
            .padding(10.dp)
        ) {
            StatisticsDisplay(modifier=Modifier.fillMaxSize())
        }

        // Stores the onRecord function inputting for its own fields.
        // Is aligned with the bottom of the screen by default.
        InlineTimeDisplay(onRecord)
    }
}

@Composable
fun RecordEventWithConfirmation(
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    selectedFull: String,
    onRecord: (Long) -> Unit,
    context: Context
) {
    var showDialog by remember { mutableStateOf(false) }
    var candidateEpochMillis by remember { mutableStateOf(0L) }
    var candidateDate by remember { mutableStateOf<ZonedDateTime?>(null) }

    // Your original RecordEvent button
    RecordEventButton {
        val epochMillis = try {
            val date = ZonedDateTime.of(
                ZonedDateTime.now().year,
                month,
                day,
                hour,
                minute,
                0,
                0,
                ZoneId.of(selectedFull)
            )
            candidateDate = date
            date.toInstant().toEpochMilli()
        } catch (e: DateTimeException) {
            null
        }
        candidateEpochMillis = epochMillis ?: 0L
        showDialog = true
    }

    // Confirmation dialog
    if (showDialog && candidateDate != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Event") },
            text = {
                Text("Are you sure you'd like to record event on ${candidateDate!!.toString()}?")
            },
            confirmButton = {
                TextButton(onClick = {
                    onRecord(candidateEpochMillis)
                    Toast.makeText(context, "Record successful", Toast.LENGTH_SHORT).show()
                    showDialog = false
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
fun RecordEventButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
        modifier = Modifier.size(56.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.FiberManualRecord,
            contentDescription = "Record",
            tint = Color.White // icon color
        )
    }
}