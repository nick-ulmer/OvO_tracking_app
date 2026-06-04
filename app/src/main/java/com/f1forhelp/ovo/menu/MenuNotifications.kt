package com.f1forhelp.ovo.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.graphics.ColorUtils
import com.f1forhelp.ovo.NotificationObject
import com.f1forhelp.ovo.NotificationType
import com.f1forhelp.ovo.SettingsStore.NotificationSettings
import com.f1forhelp.ovo.menu.main.TimezoneDropdown

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.f1forhelp.ovo.notifications.NotificationService

@Composable
fun MenuNotifications(navController: NavController) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopButtons(navController, onClick = {
            // Schedule notifications (if able), then exit menu (if able).
            if (NotificationSettings.enabled.value) {
                NotificationService.scheduleAllNotifications(context)
            }
            if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
            }
        })
        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable Notifications", modifier = Modifier.weight(1f))
                Switch(checked = NotificationSettings.enabled.value, onCheckedChange = { NotificationSettings.setEnabled(context, it) })
            }
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    //.padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Notifications time window:", modifier = Modifier.weight(1f))
                    TimezoneDropdown(enabled=NotificationSettings.enabled.value)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier=Modifier.weight(1f)) { Text("Start:") }
                    Slider(
                        modifier=Modifier.weight(6f),
                        value = NotificationSettings.windowStart.intValue.toFloat(),
                        onValueChange = { newValue ->
                            NotificationSettings.windowStart.intValue = newValue.toInt()
                        },
                        valueRange = 0f..1440f,
                        steps = 1440,
                        enabled = NotificationSettings.enabled.value
                    )
                    Box(modifier=Modifier.weight(1f)) { Text(formatTime(NotificationSettings.windowStart.intValue)) }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) { Text("End:") }
                    Slider(
                        modifier=Modifier.weight(6f),
                        value = NotificationSettings.windowEnd.intValue.toFloat(),
                        onValueChange = { newValue ->
                            NotificationSettings.windowEnd.intValue = newValue.toInt()
                        },
                        valueRange = 0f..1440f,
                        steps = 1440,
                        enabled = NotificationSettings.enabled.value
                    )
                    Box(modifier=Modifier.weight(1f)) { Text(formatTime(NotificationSettings.windowEnd.intValue)) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))


            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        color = if (NotificationSettings.enabled.value) Color(0xFFE0E0E0) else Color.LightGray,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            NotificationSettings.add(NotificationObject(
                                "Testing",
                                true,
                                NotificationType.DAY_OF,
                                0.0
                            ))
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp),
                        enabled = NotificationSettings.enabled.value
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Notification"
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = {
                            NotificationSettings.add(NotificationObject("-1 MAD", true, NotificationType.MAD, -1.0))
                            NotificationSettings.add(NotificationObject("+1 MAD", true, NotificationType.MAD, 1.0))
                        },
                        modifier = Modifier
                            .height(42.dp),
                        //.width(200.dp),
                        enabled = NotificationSettings.enabled.value,
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(text="±1 MAD", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = {
                            NotificationSettings.add(NotificationObject("-2 Days", true, NotificationType.DAYS, -2.0))
                            NotificationSettings.add(NotificationObject("+2 Days", true, NotificationType.DAYS, 2.0))
                        },
                        modifier = Modifier
                            .height(42.dp),
                        //.width(200.dp),
                        enabled = NotificationSettings.enabled.value,
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(text="±2 Days", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = {
                            NotificationSettings.add(NotificationObject("OvO Day!", true, NotificationType.OVO_DAY, 0.0))
                        },
                        modifier = Modifier
                            .height(42.dp),
                        //.width(200.dp),
                        enabled = NotificationSettings.enabled.value,
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(text="OvO", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = {
                            NotificationSettings.add(NotificationObject("Day Of", true, NotificationType.DAY_OF, 0.0))
                        },
                        modifier = Modifier
                            .height(42.dp),
                        //.width(200.dp),
                        enabled = NotificationSettings.enabled.value,
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(text="Day Of", fontSize = 12.sp)
                    }
                }


                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                ) {
                    itemsIndexed(NotificationSettings.notifications) { _, item ->
                        NotificationObjectSettings(item)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationObjectSettings(item: NotificationObject) {
    @Composable
    fun notificationButtonColors(enabled: Boolean) = ButtonDefaults.buttonColors(
        containerColor = if (enabled) ButtonDefaults.buttonColors().containerColor else ButtonDefaults.buttonColors().disabledContainerColor,
        contentColor = if (enabled) ButtonDefaults.buttonColors().contentColor else ButtonDefaults.buttonColors().disabledContentColor
    )
    val rowHeight = 56.dp

    Row(
        modifier= Modifier
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Switch(checked = item.enabled, onCheckedChange = { NotificationSettings.update(item, enabled=!item.enabled)}, enabled = NotificationSettings.enabled.value)
        Spacer(modifier = Modifier.width(4.dp))

        Button(
            onClick = {/* Nothing since this is a display thing only */},
            enabled = NotificationSettings.enabled.value,
            colors = notificationButtonColors(item.enabled),
            modifier = Modifier
                .height(rowHeight)
                .weight(1f),
            contentPadding = PaddingValues(0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = item.name,
                    onValueChange = { NotificationSettings.update(item, name=it) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = Color.White),
                    modifier = Modifier.weight(1f).height(rowHeight),
                    decorationBox = { innerTextField ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.width(4.dp))
                var isFocused by remember { mutableStateOf(false) }
                var textValue by remember { mutableStateOf(item.value.toString()) }

                if (item.type != NotificationType.DAY_OF && item.type != NotificationType.OVO_DAY) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = { newText ->
                            textValue = newText
                        },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .width(80.dp)
                            .height(rowHeight)
                            .onFocusChanged { focusState ->
                                if (isFocused && !focusState.isFocused) {
                                    // Lost focus → commit final value
                                    textValue.toDoubleOrNull()?.let { finalValue ->
                                        NotificationSettings.update(item, value = finalValue)
                                        textValue = finalValue.toString()
                                    }
                                }
                                isFocused = focusState.isFocused
                            },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                innerTextField()
                            }
                        }
                    )
                }

                NotificationTypeDropdown(modifier=Modifier.height(rowHeight).padding(6.dp),selectedType=item.type) { NotificationSettings.update(item, type = it) }
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Button(
            onClick = {
                NotificationSettings.remove(item)
            },
            shape = CircleShape,
            modifier = Modifier.size(rowHeight),
            contentPadding = PaddingValues(0.dp),
            enabled = NotificationSettings.enabled.value
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = "Remove Notification"
            )
        }
    }
}

@Composable
fun NotificationTypeDropdown(
    modifier:Modifier=Modifier,
    selectedType: NotificationType,
    onTypeSelected: (NotificationType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val enabledColor = ButtonDefaults.buttonColors().containerColor.adjustBrightness(0.8f)
    Box(modifier=modifier) {
        Button(
            enabled = NotificationSettings.enabled.value,
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = enabledColor
            )
        ) {
            Text(
                text=selectedType.name,
                //style = TextStyle(fontSize = 10.sp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            NotificationType.entries.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text=type.name,
                            //style = TextStyle(fontSize = 10.sp)
                    ) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun Color.adjustBrightness(factor: Float): Color {
    // Convert Compose Color to ARGB Int
    val argb = this.toArgb()

    // Convert to HSL
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)

    // Adjust lightness
    hsl[2] = (hsl[2] * factor).coerceIn(0f, 1f)

    // Convert back to ARGB and then Compose Color
    val newArgb = ColorUtils.HSLToColor(hsl)
    return Color(newArgb)
}

fun formatTime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "%02d:%02d".format(h, m)
}