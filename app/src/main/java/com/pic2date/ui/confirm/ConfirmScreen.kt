package com.pic2date.ui.confirm

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pic2date.R
import com.pic2date.calendar.CalendarRepository
import com.pic2date.model.EventDraft
import com.pic2date.ui.labelRes
import com.pic2date.model.ReminderOption
import com.pic2date.util.DateTimeFormat
import com.pic2date.util.MapsLauncher
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmScreen(
    draft: EventDraft,
    @StringRes errorRes: Int?,
    calendarRepository: CalendarRepository,
    onErrorShown: () -> Unit,
    onDraftChange: (EventDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onRescan: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showScannedText by remember { mutableStateOf(false) }

    val errorMessage = errorRes?.let { stringResource(it) }
    LaunchedEffect(errorRes) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onErrorShown()
        }
    }

    // Request calendar permissions, then save.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.all { it }) {
            onSave()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.confirm_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        stringResource(
                            R.string.detected_type,
                            stringResource(draft.eventType.labelRes()),
                        ),
                    )
                },
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = draft.title,
                onValueChange = { onDraftChange(draft.copy(title = it)) },
                label = { Text(stringResource(R.string.field_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            ClickableField(
                label = stringResource(R.string.field_date),
                value = DateTimeFormat.date(draft.date).ifEmpty { stringResource(R.string.hint_not_set) },
                leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                onClick = {
                    showDatePicker(context, draft.date ?: LocalDate.now()) {
                        onDraftChange(draft.copy(date = it))
                    }
                },
            )

            Spacer(Modifier.height(12.dp))

            ClickableField(
                label = stringResource(R.string.field_time),
                value = DateTimeFormat.time(draft.time).ifEmpty { stringResource(R.string.hint_not_set) },
                leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                onClick = {
                    showTimePicker(context, draft.time ?: LocalTime.of(9, 0)) {
                        onDraftChange(draft.copy(time = it))
                    }
                },
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = draft.location,
                onValueChange = { onDraftChange(draft.copy(location = it)) },
                label = { Text(stringResource(R.string.field_location)) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (draft.hasLocation) {
                TextButton(onClick = { MapsLauncher.navigate(context, draft.location) }) {
                    Icon(Icons.Filled.Directions, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_navigate))
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = draft.notes,
                onValueChange = { onDraftChange(draft.copy(notes = it)) },
                label = { Text(stringResource(R.string.field_notes)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.field_reminder),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(6.dp))
            ReminderChips(
                selectedMinutes = draft.reminderMinutes,
                onSelect = { onDraftChange(draft.copy(reminderMinutes = it)) },
            )

            if (draft.rawText.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { showScannedText = !showScannedText }) {
                    Text(stringResource(R.string.show_scanned_text))
                }
                if (showScannedText) {
                    Text(
                        text = draft.rawText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 20,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR),
                    )
                },
                enabled = draft.date != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onRescan, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_rescan))
                }
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_cancel))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ClickableField(
    label: String,
    value: String,
    leadingIcon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            leadingIcon = leadingIcon,
            readOnly = true,
            enabled = false,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = disabledLooksEnabledColors(),
        )
        // Transparent overlay captures the tap (a disabled TextField ignores clicks).
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 6.dp)
                .clickableNoRipple(onClick),
        )
    }
}

@Composable
private fun ReminderChips(selectedMinutes: Int?, onSelect: (Int?) -> Unit) {
    val options = ReminderOption.entries
    var showCustom by remember { mutableStateOf(false) }
    val isPreset = ReminderOption.fromMinutes(selectedMinutes) != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selectedMinutes == option.minutes,
                onClick = { onSelect(option.minutes) },
                label = { Text(stringResource(option.labelRes)) },
            )
        }
        FilterChip(
            selected = !isPreset && selectedMinutes != null,
            onClick = { showCustom = true },
            label = {
                val custom = if (!isPreset && selectedMinutes != null) {
                    "$selectedMinutes min"
                } else {
                    stringResource(R.string.reminder_custom)
                }
                Text(custom)
            },
        )
    }

    if (showCustom) {
        CustomReminderDialog(
            initialMinutes = selectedMinutes?.takeIf { !isPreset } ?: 15,
            onDismiss = { showCustom = false },
            onConfirm = {
                onSelect(it)
                showCustom = false
            },
        )
    }
}
