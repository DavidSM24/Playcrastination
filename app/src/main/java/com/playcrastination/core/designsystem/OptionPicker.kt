package com.playcrastination.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.playcrastination.R

@Composable
fun <T> OptionPicker(
    label: String,
    value: T,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onChange: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.current_choice, label, optionLabel(value)))
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onChange(option)
                    }
                )
            }
        }
    }
}

@Composable
fun DailyHoursPicker(hours: Int, onChange: (Int) -> Unit) {
    OptionPicker(
        label = stringResource(R.string.play_budget),
        value = hours,
        options = (1..24).toList(),
        optionLabel = { stringResource(R.string.hours_per_day, it) },
        onChange = onChange
    )
}
