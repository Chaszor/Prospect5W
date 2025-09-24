@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yourname.prospect5w.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.yourname.prospect5w.EventViewModel
import com.yourname.prospect5w.data.Event
import com.yourname.prospect5w.export.eventsToCsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun AddEditEventScreen(
    vm: EventViewModel,
    eventId: Long? = null,
    onSaved: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val ctx = LocalContext.current

    var loading by remember { mutableStateOf(true) }
    var existing by remember { mutableStateOf<Event?>(null) }

    LaunchedEffect(eventId) {
        if (eventId != null) existing = vm.get(eventId)
        loading = false
    }

    var title by remember(existing) { mutableStateOf(TextFieldValue(existing?.title ?: "")) }
    var location by remember(existing) { mutableStateOf(TextFieldValue(existing?.location ?: "")) }
    var description by remember(existing) { mutableStateOf(TextFieldValue(existing?.description ?: "")) }

    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val zone = ZoneId.systemDefault()

    fun millisToStr(m: Long?) =
        m?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime().format(fmt) } ?: ""

    fun strToMillis(s: String): Long? = runCatching {
        LocalDateTime.parse(s.trim(), fmt).atZone(zone).toInstant().toEpochMilli()
    }.getOrNull()

    var startStr by remember(existing) {
        mutableStateOf(TextFieldValue(millisToStr(existing?.startTime ?: System.currentTimeMillis())))
    }
    var endStr by remember(existing) { mutableStateOf(TextFieldValue(millisToStr(existing?.endTime))) }

    var error by remember { mutableStateOf<String?>(null) }

    // ---------- Location helpers ----------
    val scope = rememberCoroutineScope()
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(ctx) }
    var locating by remember { mutableStateOf(false) }

    // --- Export state & launcher ---
    val events by vm.events.collectAsState(initial = emptyList())
    var pendingExport: Event? by remember { mutableStateOf(null) }
    var pendingArchiveAfterExport by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(CreateDocument("text/csv")) { uri ->
        val ev = pendingExport
        if (uri != null && ev != null) {
            val csv = eventsToCsv(listOf(ev))
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                if (pendingArchiveAfterExport) {
                    scope.launch { vm.archive(ev.id) }
                }
                error = null
            } catch (t: Throwable) {
                error = "Export failed: ${t.message}"
            }
        } else if (uri == null && ev != null) {
            error = "Export cancelled"
        }
        pendingExport = null
        pendingArchiveAfterExport = false
    }

    suspend fun geocodeToAddress(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        try {
            val g = Geocoder(ctx, java.util.Locale.getDefault())
            @Suppress("DEPRECATION")
            val res = g.getFromLocation(lat, lon, 1) ?: return@withContext null
            val addr = res.firstOrNull() ?: return@withContext null

            val street = buildString {
                if (!addr.subThoroughfare.isNullOrBlank()) { append(addr.subThoroughfare); append(" ") }
                if (!addr.thoroughfare.isNullOrBlank()) { append(addr.thoroughfare) }
            }

            val city = addr.locality ?: addr.subAdminArea
            val state = addr.adminArea
            val postal = addr.postalCode
            val tail = listOfNotNull(city, state, postal).joinToString(" ")

            when {
                street.isNotBlank() && tail.isNotBlank() -> "$street, $tail"
                street.isNotBlank() -> street
                !addr.getAddressLine(0).isNullOrBlank() -> addr.getAddressLine(0)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    var pendingResolution by remember { mutableStateOf<ResolvableApiException?>(null) }
    var retryAfterResolution by remember { mutableStateOf(false) }
    var pendingIntentRequest by remember { mutableStateOf<IntentSenderRequest?>(null) }

    suspend fun fetchAndFillAddress() {
        locating = true
        try {
            val settingsClient = LocationServices.getSettingsClient(ctx)
            val settingsReq = LocationSettingsRequest.Builder()
                .addLocationRequest(
                    LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
                        .setMinUpdateIntervalMillis(1_000L).build()
                )
                .setAlwaysShow(true)
                .build()

            val settingsResult =
                runCatching { settingsClient.checkLocationSettings(settingsReq).await() }
            if (settingsResult.isFailure) {
                val ex = settingsResult.exceptionOrNull()
                if (ex is ResolvableApiException) {
                    pendingResolution = ex
                    pendingIntentRequest = IntentSenderRequest.Builder(ex.resolution).build()
                    return
                } else {
                    error = "Location isn’t ready. Turn on Precise/High accuracy and try again."
                    return
                }
            }

            val last = runCatching { fusedClient.lastLocation.await() }.getOrNull()
            val loc = last ?: runCatching {
                val cts = CancellationTokenSource()
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
            }.getOrNull()

            if (loc == null) {
                error = "Couldn’t get current location. Ensure Precise location is allowed and try again."
                return
            }

            val line = geocodeToAddress(loc.latitude, loc.longitude)
            if (line.isNullOrBlank()) error = "Couldn’t resolve address" else location = TextFieldValue(line)
        } finally {
            locating = false
        }
    }

    val settingsLauncher: ManagedActivityResultLauncher<IntentSenderRequest, androidx.activity.result.ActivityResult> =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            retryAfterResolution = (result.resultCode == android.app.Activity.RESULT_OK)
            if (!retryAfterResolution) {
                error = "Location needs to be enabled (Precise/High accuracy) to fetch your address."
            }
        }

    LaunchedEffect(pendingIntentRequest) {
        val req = pendingIntentRequest ?: return@LaunchedEffect
        settingsLauncher.launch(req)
        pendingIntentRequest = null
    }

    LaunchedEffect(retryAfterResolution) {
        if (retryAfterResolution) {
            retryAfterResolution = false
            fetchAndFillAddress()
        }
    }

    val askLocationPerms = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val ok = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) scope.launch { fetchAndFillAddress() }
        else error = "Location permission denied"
    }

    fun onUseCurrentClicked() {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
        val hasPerm = fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
        if (hasPerm) scope.launch { fetchAndFillAddress() } else askLocationPerms.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    fun showDateTimePick(initial: LocalDateTime?, onPicked: (LocalDateTime) -> Unit) {
        val cal = Calendar.getInstance().apply {
            val base = initial ?: LocalDateTime.now()
            set(Calendar.YEAR, base.year)
            set(Calendar.MONTH, base.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, base.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, base.hour)
            set(Calendar.MINUTE, base.minute)
        }
        DatePickerDialog(ctx, { _, y, m, d ->
            TimePickerDialog(ctx, { _, h, min ->
                onPicked(LocalDateTime.of(y, m + 1, d, h, min))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (eventId == null) "Add Event" else "Edit Event") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
        )
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(onClick = { onUseCurrentClicked() }, enabled = !locating) {
                        if (locating) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Locating…")
                        } else {
                            Text("Use current")
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startStr,
                        onValueChange = { startStr = it },
                        label = { Text("Start* (yyyy-MM-dd HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(onClick = {
                        val base = strToMillis(startStr.text)?.let {
                            Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime()
                        }
                        showDateTimePick(base) { dt -> startStr = TextFieldValue(dt.format(fmt)) }
                    }) { Text("Pick") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = endStr,
                        onValueChange = { endStr = it },
                        label = { Text("End (yyyy-MM-dd HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(onClick = {
                        val base = strToMillis(endStr.text)?.let {
                            Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime()
                        }
                        showDateTimePick(base) { dt -> endStr = TextFieldValue(dt.format(fmt)) }
                    }) { Text("Pick") }
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = {
                        val now = LocalDateTime.now().withSecond(0).withNano(0)
                        startStr = TextFieldValue(now.format(fmt))
                        endStr = TextFieldValue(now.plusMinutes(30).format(fmt))
                    }) { Text("Now +30min") }

                    FilledTonalButton(onClick = {
                        val now = LocalDateTime.now().withSecond(0).withNano(0)
                        startStr = TextFieldValue(now.format(fmt))
                        endStr = TextFieldValue(now.plusHours(1).format(fmt))
                    }) { Text("Now +1h") }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    val canSave = title.text.isNotBlank() && strToMillis(startStr.text) != null
                    Button(
                        enabled = canSave,
                        onClick = {
                            val start = strToMillis(startStr.text)
                            val end = endStr.text.trim().takeIf { it.isNotEmpty() }?.let { s -> strToMillis(s) }

                            error = when {
                                title.text.isBlank() -> "Title is required."
                                start == null -> "Start time format invalid."
                                end != null && end < start -> "End must be after start."
                                else -> null
                            }
                            if (error != null) return@Button

                            val event = Event(
                                id = existing?.id ?: 0L,
                                title = title.text.trim(),
                                description = description.text.trim(),
                                location = location.text.trim(),
                                startTime = start!!,
                                endTime = end,
                                archived = existing?.archived ?: false
                            )
                            if (existing == null) vm.add(event) else vm.update(event)
                            onSaved()
                            onBack()
                        }
                    ) { Text(if (existing == null) "Save" else "Update") }

                    OutlinedButton(
                        enabled = existing != null,
                        onClick = {
                            val start = strToMillis(startStr.text)
                            val end = endStr.text.trim().takeIf { it.isNotEmpty() }?.let { s -> strToMillis(s) }
                            if (title.text.isBlank() || start == null) {
                                error = "Fill Title and valid Start to export."
                                return@OutlinedButton
                            }
                            val ev = Event(
                                id = existing?.id ?: 0L,
                                title = title.text.trim(),
                                description = description.text.trim(),
                                location = location.text.trim(),
                                startTime = start,
                                endTime = end,
                                archived = existing?.archived ?: false
                            )
                            pendingArchiveAfterExport = false
                            pendingExport = ev
                            exportLauncher.launch("event-${ev.id.takeIf { it != 0L } ?: "draft"}.csv")
                        }
                    ) { Text("Export") }
                }
            }
        }
    }
}
