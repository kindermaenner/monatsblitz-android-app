package de.kindermaenner.monatsblitz.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.ModePreselectionType
import de.kindermaenner.monatsblitz.domain.model.PlayerSortField
import de.kindermaenner.monatsblitz.domain.model.PlayerSortOrder
import de.kindermaenner.monatsblitz.domain.usecase.ConnectionTestResult
import de.kindermaenner.monatsblitz.infrastructure.api.client.QrSetupParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val showDetailedSync by viewModel.showDetailedSyncIndicators.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrlInput.collectAsStateWithLifecycle()
    val apiKey by viewModel.apiKeyInput.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTestingConnection.collectAsStateWithLifecycle()
    val testResult by viewModel.testResult.collectAsStateWithLifecycle()
    val isSavedMessageVisible by viewModel.isSavedMessageVisible.collectAsStateWithLifecycle()
    val isConfigured by viewModel.isConfigured.collectAsStateWithLifecycle()
    val isServerConfigured by viewModel.isServerConfigured.collectAsStateWithLifecycle()
    val standaloneOfflineMode by viewModel.standaloneOfflineMode.collectAsStateWithLifecycle()
    val playerSortField by viewModel.playerSortField.collectAsStateWithLifecycle()
    val playerSortOrder by viewModel.playerSortOrder.collectAsStateWithLifecycle()
    val useShortPlayerNames by viewModel.useShortPlayerNames.collectAsStateWithLifecycle()
    val autoAdoptOpenTournament by viewModel.autoAdoptOpenTournament.collectAsStateWithLifecycle()
    val modePreselectionType by viewModel.modePreselectionType.collectAsStateWithLifecycle()
    val fixedMode by viewModel.fixedMode.collectAsStateWithLifecycle()
    val rotationModes by viewModel.rotationModes.collectAsStateWithLifecycle()
    val availableModes by viewModel.availableModes.collectAsStateWithLifecycle()

    var isPasswordVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val barcodeScanner = remember(context) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }

    LaunchedEffect(isSavedMessageVisible) {
        if (isSavedMessageVisible) {
            snackbarHostState.showSnackbar("Einstellungen erfolgreich gespeichert.")
            viewModel.dismissSavedMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (!isConfigured) "Monatsblitz Einrichtung" else "Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WelcomeBanner(isConfigured = isConfigured)

            // Sektion 1: Betriebsmodus
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (standaloneOfflineMode) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Betriebsmodus",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dauerhafter Offline-Betrieb",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "App komplett ohne WordPress-Server nutzen (z. B. für Vereinsfeste, Jugend- oder private Turniere).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = standaloneOfflineMode,
                            onCheckedChange = { viewModel.setStandaloneOfflineMode(it) },
                            modifier = Modifier.testTag("standalone_offline_switch")
                        )
                    }
                }
            }

            // Sektion 2: WordPress & Synchronisation (in eigener Card, animiert ausgeblendet bei Offline-Betrieb)
            AnimatedVisibility(
                visible = !standaloneOfflineMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ServerConnectionHeader(
                            onScanQr = {
                                barcodeScanner.startScan()
                                    .addOnSuccessListener { barcode ->
                                        val raw = barcode.rawValue
                                        val config = raw?.let { QrSetupParser.parse(it) }
                                        if (config != null) {
                                            viewModel.applyQrConfig(config)
                                        } else {
                                            viewModel.onQrScanError("Der gescannte QR-Code enthält keine gültigen Monatsblitz-Daten.")
                                        }
                                    }
                            }
                        )

                        ServerUrlField(
                            serverUrl = serverUrl,
                            onValueChange = { viewModel.onServerUrlChanged(it) }
                        )

                        ApiKeyField(
                            apiKey = apiKey,
                            isVisible = isPasswordVisible,
                            onVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
                            onValueChange = { viewModel.onApiKeyChanged(it) }
                        )

                        ServerActionButtons(
                            isTesting = isTesting,
                            canSubmit = !isTesting && serverUrl.isNotBlank() && apiKey.isNotBlank(),
                            isConfigured = isConfigured,
                            onTest = { viewModel.testConnection() },
                            onSave = {
                                viewModel.saveSettings {
                                    if (!isConfigured) {
                                        onBack()
                                    }
                                }
                            }
                        )

                        testResult?.let { result ->
                            ConnectionTestResultCard(result = result)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "Synchronisation",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        SettingToggleItem(
                            title = "Offene Server-Turniere automatisch fortsetzen",
                            description = "Übernimmt ein offenes Turnier vom Server beim App-Start automatisch ohne Nachfrage.",
                            checked = autoAdoptOpenTournament,
                            onToggle = { viewModel.setAutoAdoptOpenTournament(it) }
                        )

                        SettingToggleItem(
                            title = "Detaillierte Sync-Indikatoren",
                            description = "Zeigt kleine Statuspunkte an ungespeicherten Zellen und neuen Spielern, bis sie synchronisiert sind.",
                            checked = showDetailedSync,
                            onToggle = { viewModel.setShowDetailedSyncIndicators(it) }
                        )
                    }
                }
            }

            // Sektion 3: Darstellung (Card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Darstellung",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    SettingToggleItem(
                        title = "Kurznamen in Tabellen & Rangliste",
                        description = "Spart Platz auf kleinen Displays (z. B. 'Peter M.' statt 'Peter Müller').",
                        checked = useShortPlayerNames,
                        onToggle = { viewModel.setUseShortPlayerNames(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    PlayerSortingSection(
                        playerSortField = playerSortField,
                        playerSortOrder = playerSortOrder,
                        onSortFieldChanged = { viewModel.setPlayerSortField(it) },
                        onSortOrderChanged = { viewModel.setPlayerSortOrder(it) }
                    )
                }
            }

            // Sektion 4: Turnier-Vorauswahl (Card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModePreselectionSection(
                        modePreselectionType = modePreselectionType,
                        fixedMode = fixedMode,
                        rotationModes = rotationModes,
                        availableModes = availableModes,
                        onTypeChanged = { viewModel.setModePreselectionType(it) },
                        onFixedModeChanged = { viewModel.setFixedMode(it) },
                        onToggleRotationMode = { viewModel.toggleRotationMode(it) }
                    )
                }
            }

            // Abschluss-Button für Offline-Onboarding ganz unten
            if (standaloneOfflineMode && !isServerConfigured) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("continue_to_setup_button")
                ) {
                    Text("Weiter zum Turniersetup")
                }
            }
        }
    }
}

@Composable
private fun WelcomeBanner(isConfigured: Boolean) {
    if (isConfigured) return
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Willkommen beim Monatsblitz!",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Bitte gib die Adresse deiner WordPress-Website und deinen API-Schlüssel ein – oder scanne einfach den Einrichtungs-QR-Code im WordPress-Admin. Alternativ kannst du die App auch dauerhaft im Offline-Modus nutzen.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}



@Composable
private fun ServerConnectionHeader(onScanQr: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "WordPress & Synchronisation",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        FilledTonalButton(
            onClick = onScanQr,
            modifier = Modifier.testTag("scan_qr_button")
        ) {
            Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = "QR-Code scannen",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("QR-Code scannen")
        }
    }
}

@Composable
private fun ServerUrlField(
    serverUrl: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = serverUrl,
        onValueChange = onValueChange,
        label = { Text("WordPress-Adresse") },
        placeholder = { Text("https://sg-koenigslutter.de") },
        supportingText = { Text("Website mit installiertem Monatsblitz-Plugin") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("server_url_input")
    )
}

@Composable
private fun ApiKeyField(
    apiKey: String,
    isVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = apiKey,
        onValueChange = onValueChange,
        label = { Text("API-Schlüssel (X-MB-Key)") },
        placeholder = { Text("Geheimer Schlüssel") },
        supportingText = { Text("Im WordPress-Admin unter Einstellungen → Monatsblitz") },
        singleLine = true,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onVisibilityToggle) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isVisible) "Schlüssel verbergen" else "Schlüssel anzeigen"
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("api_key_input")
    )
}

@Composable
private fun ServerActionButtons(
    isTesting: Boolean,
    canSubmit: Boolean,
    isConfigured: Boolean,
    onTest: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onTest,
            enabled = canSubmit,
            modifier = Modifier
                .weight(1f)
                .testTag("test_connection_button")
        ) {
            if (isTesting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Prüfe...")
            } else {
                Text("Verbindung testen")
            }
        }

        Button(
            onClick = onSave,
            enabled = canSubmit,
            modifier = Modifier
                .weight(1f)
                .testTag("save_settings_button")
        ) {
            if (isTesting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (!isConfigured) "Übernehmen & Los" else "Einstellungen übernehmen")
        }
    }
}

@Composable
private fun ConnectionTestResultCard(result: ConnectionTestResult) {
    when (result) {
        is ConnectionTestResult.Success -> {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Erfolg",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Verbindung erfolgreich! ${result.playerCount} Spieler gefunden.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        is ConnectionTestResult.Error -> {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Fehler",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun ModePreselectionSection(
    modePreselectionType: ModePreselectionType,
    fixedMode: String,
    rotationModes: List<String>,
    availableModes: List<GameMode>,
    onTypeChanged: (ModePreselectionType) -> Unit,
    onFixedModeChanged: (String) -> Unit,
    onToggleRotationMode: (String) -> Unit
) {
    Text(
        text = "Turnier-Vorauswahl",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )

    Text(
        text = "Legt fest, welcher Spielmodus beim Erstellen eines neuen Turniers vorgeschlagen wird. Wird beim QR-Code-Scan automatisch von der Vereinswebsite übernommen.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModePreselectionType.entries.forEach { type ->
            FilterChip(
                selected = modePreselectionType == type,
                onClick = { onTypeChanged(type) },
                label = { Text(type.displayName) }
            )
        }
    }

    if (modePreselectionType == ModePreselectionType.FIXED) {
        Text(
            text = "Fester Standard-Modus:",
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableModes.forEach { mode ->
                FilterChip(
                    selected = fixedMode.equals(mode.name, ignoreCase = true),
                    onClick = { onFixedModeChanged(mode.name) },
                    label = { Text(mode.displayName) }
                )
            }
        }
    } else if (modePreselectionType == ModePreselectionType.ROTATION) {
        Text(
            text = "Beteiligte Modi für die Rotation:",
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = "Turniere auf dem Server bestimmen den nächsten Modus in der Reihe.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            availableModes.forEach { mode ->
                val index = rotationModes.indexOfFirst { it.equals(mode.name, ignoreCase = true) }
                val isSelected = index != -1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleRotationMode(mode.name) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleRotationMode(mode.name) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSelected) "${mode.displayName} (Nr. ${index + 1} in der Rotation)" else mode.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerSortingSection(
    playerSortField: PlayerSortField,
    playerSortOrder: PlayerSortOrder,
    onSortFieldChanged: (PlayerSortField) -> Unit,
    onSortOrderChanged: (PlayerSortOrder) -> Unit
) {
    Text(
        text = "Sortierung der Spielerliste",
        style = MaterialTheme.typography.titleSmall
    )

    Text(
        text = "Sortieren nach",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = playerSortField == PlayerSortField.LAST_NAME,
            onClick = { onSortFieldChanged(PlayerSortField.LAST_NAME) },
            label = { Text("Nachname") }
        )
        FilterChip(
            selected = playerSortField == PlayerSortField.FIRST_NAME,
            onClick = { onSortFieldChanged(PlayerSortField.FIRST_NAME) },
            label = { Text("Vorname") }
        )
    }

    Text(
        text = "Reihenfolge",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = playerSortOrder == PlayerSortOrder.ASCENDING,
            onClick = { onSortOrderChanged(PlayerSortOrder.ASCENDING) },
            label = { Text("Aufsteigend (A–Z)") }
        )
        FilterChip(
            selected = playerSortOrder == PlayerSortOrder.DESCENDING,
            onClick = { onSortOrderChanged(PlayerSortOrder.DESCENDING) },
            label = { Text("Absteigend (Z–A)") }
        )
    }
}
