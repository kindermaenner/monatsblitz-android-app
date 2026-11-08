package de.kindermaenner.monatsblitz.ui.settings

import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.ModePreselectionType
import de.kindermaenner.monatsblitz.domain.model.PlayerSortField
import de.kindermaenner.monatsblitz.domain.model.PlayerSortOrder
import de.kindermaenner.monatsblitz.domain.model.TournamentDumpFileInfo
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
    val preferRufnameAsShortName by viewModel.preferRufnameAsShortName.collectAsStateWithLifecycle()
    val autoAdoptOpenTournament by viewModel.autoAdoptOpenTournament.collectAsStateWithLifecycle()
    val modePreselectionType by viewModel.modePreselectionType.collectAsStateWithLifecycle()
    val fixedMode by viewModel.fixedMode.collectAsStateWithLifecycle()
    val rotationModes by viewModel.rotationModes.collectAsStateWithLifecycle()
    val availableModes by viewModel.availableModes.collectAsStateWithLifecycle()
    val activePlayersCutoffMonths by viewModel.activePlayersCutoffMonths.collectAsStateWithLifecycle()
    val autoCleanupDumps by viewModel.autoCleanupDumps.collectAsStateWithLifecycle()
    val dumpRetentionMonths by viewModel.dumpRetentionMonths.collectAsStateWithLifecycle()
    val dumps by viewModel.dumps.collectAsStateWithLifecycle()
    val maintenanceKeyInput by viewModel.maintenanceKeyInput.collectAsStateWithLifecycle()
    val isAdminMode by viewModel.isAdminMode.collectAsStateWithLifecycle()
    val isAdminVerifying by viewModel.isAdminVerifying.collectAsStateWithLifecycle()
    val adminMessage by viewModel.adminMessage.collectAsStateWithLifecycle()
    val adminError by viewModel.adminError.collectAsStateWithLifecycle()

    var isPasswordVisible by remember { mutableStateOf(false) }
    var showExitWithoutConfigDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val handleBackAttempt = {
        if (!isConfigured) {
            showExitWithoutConfigDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(enabled = true) {
        handleBackAttempt()
    }

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

    if (showExitWithoutConfigDialog) {
        ExitWithoutConfigDialog(
            onDismiss = { showExitWithoutConfigDialog = false },
            onUseOfflineMode = {
                viewModel.setStandaloneOfflineMode(true)
                showExitWithoutConfigDialog = false
                onBack()
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = handleBackAttempt) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    if (isConfigured || isServerConfigured || standaloneOfflineMode) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Einrichtung abschließen",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isConfigured || isServerConfigured || standaloneOfflineMode) {
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
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

            OperatingModeSection(
                standaloneOfflineMode = standaloneOfflineMode,
                onStandaloneOfflineModeChanged = { viewModel.setStandaloneOfflineMode(it) }
            )

            WordPressServerSection(
                standaloneOfflineMode = standaloneOfflineMode,
                serverUrl = serverUrl,
                apiKey = apiKey,
                isPasswordVisible = isPasswordVisible,
                onPasswordVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
                isTesting = isTesting,
                isServerConfigured = isServerConfigured,
                testResult = testResult,
                autoAdoptOpenTournament = autoAdoptOpenTournament,
                showDetailedSync = showDetailedSync,
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
                },
                onServerUrlChanged = { viewModel.onServerUrlChanged(it) },
                onApiKeyChanged = { viewModel.onApiKeyChanged(it) },
                onTestConnection = { viewModel.testConnection() },
                onSaveSettings = { viewModel.saveSettings() },
                onToggleAutoAdopt = { viewModel.setAutoAdoptOpenTournament(it) },
                onToggleDetailedSync = { viewModel.setShowDetailedSyncIndicators(it) }
            )

            DisplaySettingsSection(
                useShortPlayerNames = useShortPlayerNames,
                preferRufnameAsShortName = preferRufnameAsShortName,
                playerSortField = playerSortField,
                playerSortOrder = playerSortOrder,
                activePlayersCutoffMonths = activePlayersCutoffMonths,
                onUseShortNamesChanged = { viewModel.setUseShortPlayerNames(it) },
                onPreferRufnameChanged = { viewModel.setPreferRufnameAsShortName(it) },
                onSortFieldChanged = { viewModel.setPlayerSortField(it) },
                onSortOrderChanged = { viewModel.setPlayerSortOrder(it) },
                onMonthsSelected = { viewModel.setActivePlayersCutoffMonths(it) }
            )

            TournamentPreselectionCard(
                modePreselectionType = modePreselectionType,
                fixedMode = fixedMode,
                rotationModes = rotationModes,
                availableModes = availableModes,
                onTypeChanged = { viewModel.setModePreselectionType(it) },
                onFixedModeChanged = { viewModel.setFixedMode(it) },
                onToggleRotationMode = { viewModel.toggleRotationMode(it) }
            )

            TournamentBackupCard(
                autoCleanupDumps = autoCleanupDumps,
                dumpRetentionMonths = dumpRetentionMonths,
                dumps = dumps,
                onAutoCleanupChanged = { viewModel.setAutoCleanupDumps(it) },
                onShareDump = { fileName ->
                    val file = viewModel.getDumpFile(fileName)
                    if (file != null && file.exists()) {
                        try {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_STREAM, uri)
                                type = "application/json"
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Turnier-Backup teilen")
                            context.startActivity(shareIntent)
                        } catch (e: Exception) {
                            Log.w(TAG, "Teilen des Backups fehlgeschlagen oder abgebrochen", e)
                        }
                    }
                }
            )

            AdminSection(
                isAdminMode = isAdminMode,
                isAdminVerifying = isAdminVerifying,
                maintenanceKeyInput = maintenanceKeyInput,
                adminMessage = adminMessage,
                adminError = adminError,
                onMaintenanceKeyChanged = { viewModel.onMaintenanceKeyChanged(it) },
                onVerifyAdminKey = { viewModel.verifyAndActivateAdminKey() },
                onLockAdminMode = { viewModel.lockAdminMode() },
                onReopenTournament = { tournamentId -> viewModel.reopenTournament(tournamentId) },
                onRecreatePosts = { viewModel.recreatePosts() },
                onRestoreDump = { content -> viewModel.restoreDump(content) },
                onClearAdminFeedback = { viewModel.clearAdminFeedback() }
            )

            HelpAndFeedbackSection(serverUrl = serverUrl)
        }
    }
}

@Composable
private fun ExitWithoutConfigDialog(
    onDismiss: () -> Unit,
    onUseOfflineMode: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Betriebsmodus erforderlich") },
        text = {
            Text(
                "Du hast bisher keinen gültigen Betriebsmodus ausgewählt bzw. eingerichtet. Bitte entscheide dich für den dauerhaften Offline-Modus oder schließe die WordPress-Verbindung ab, bevor du fortfährst."
            )
        },
        confirmButton = {
            Button(onClick = onUseOfflineMode) {
                Text("Offline-Modus nutzen")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Hier bleiben & einrichten")
            }
        }
    )
}

@Composable
private fun OperatingModeSection(
    standaloneOfflineMode: Boolean,
    onStandaloneOfflineModeChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Betriebsmodus",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Wähle aus, wie Monatsblitz auf diesem Gerät betrieben werden soll:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option A: WordPress-Vereinswebsite
            Card(
                onClick = { onStandaloneOfflineModeChanged(false) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (!standaloneOfflineMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                border = if (!standaloneOfflineMode) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !standaloneOfflineMode,
                        onClick = { onStandaloneOfflineModeChanged(false) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WordPress-Vereinswebsite",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Synchronisiert Spieler, Turniere und Ergebnisse mit dem Monatsblitz-WordPress-Plugin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Option B: Dauerhafter Offline-Betrieb
            Card(
                onClick = { onStandaloneOfflineModeChanged(true) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (standaloneOfflineMode) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                ),
                border = if (standaloneOfflineMode) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = standaloneOfflineMode,
                            onClick = { onStandaloneOfflineModeChanged(true) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Dauerhafter Offline-Betrieb",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "App komplett ohne WordPress-Server nutzen (z. B. für Vereinsfeste, Jugend- oder private Turniere).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = standaloneOfflineMode,
                        onCheckedChange = onStandaloneOfflineModeChanged,
                        modifier = Modifier.testTag("standalone_offline_switch")
                    )
                }
            }
        }
    }
}

@Composable
private fun WordPressServerSection(
    standaloneOfflineMode: Boolean,
    serverUrl: String,
    apiKey: String,
    isPasswordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    isTesting: Boolean,
    isServerConfigured: Boolean,
    testResult: ConnectionTestResult?,
    autoAdoptOpenTournament: Boolean,
    showDetailedSync: Boolean,
    onScanQr: () -> Unit,
    onServerUrlChanged: (String) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSaveSettings: () -> Unit,
    onToggleAutoAdopt: (Boolean) -> Unit,
    onToggleDetailedSync: (Boolean) -> Unit
) {
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
                ServerConnectionHeader(onScanQr = onScanQr)

                ServerUrlField(
                    serverUrl = serverUrl,
                    onValueChange = onServerUrlChanged
                )

                ApiKeyField(
                    apiKey = apiKey,
                    isVisible = isPasswordVisible,
                    onVisibilityToggle = onPasswordVisibilityToggle,
                    onValueChange = onApiKeyChanged
                )

                ServerActionButtons(
                    isTesting = isTesting,
                    canSubmit = !isTesting && serverUrl.isNotBlank() && apiKey.isNotBlank(),
                    isServerConfigured = isServerConfigured,
                    onTest = onTestConnection,
                    onSave = onSaveSettings
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
                    onToggle = onToggleAutoAdopt
                )

                SettingToggleItem(
                    title = "Detaillierte Sync-Indikatoren",
                    description = "Zeigt kleine Statuspunkte an ungespeicherten Zellen und neuen Spielern, bis sie synchronisiert sind.",
                    checked = showDetailedSync,
                    onToggle = onToggleDetailedSync
                )
            }
        }
    }
}

@Composable
private fun DisplaySettingsSection(
    useShortPlayerNames: Boolean,
    preferRufnameAsShortName: Boolean,
    playerSortField: PlayerSortField,
    playerSortOrder: PlayerSortOrder,
    activePlayersCutoffMonths: Int,
    onUseShortNamesChanged: (Boolean) -> Unit,
    onPreferRufnameChanged: (Boolean) -> Unit,
    onSortFieldChanged: (PlayerSortField) -> Unit,
    onSortOrderChanged: (PlayerSortOrder) -> Unit,
    onMonthsSelected: (Int) -> Unit
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
            Text(
                text = "Darstellung",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            SettingToggleItem(
                title = "Kurznamen in Tabellen & Rangliste",
                description = "Spart Platz auf kleinen Displays (z. B. 'Peter M.' statt 'Peter Müller').",
                checked = useShortPlayerNames,
                onToggle = onUseShortNamesChanged
            )

            if (useShortPlayerNames) {
                SettingToggleItem(
                    title = "Hinterlegte Rufnamen bevorzugen",
                    description = "Verwendet den Rufnamen als Kurznamen (z. B. 'Thien' statt 'Pham D.'), falls vorhanden.",
                    checked = preferRufnameAsShortName,
                    onToggle = onPreferRufnameChanged
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            PlayerSortingSection(
                playerSortField = playerSortField,
                playerSortOrder = playerSortOrder,
                onSortFieldChanged = onSortFieldChanged,
                onSortOrderChanged = onSortOrderChanged
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            ActivePlayersCutoffSection(
                selectedMonths = activePlayersCutoffMonths,
                onMonthsSelected = onMonthsSelected
            )
        }
    }
}

@Composable
private fun TournamentPreselectionCard(
    modePreselectionType: ModePreselectionType,
    fixedMode: String,
    rotationModes: List<String>,
    availableModes: List<GameMode>,
    onTypeChanged: (ModePreselectionType) -> Unit,
    onFixedModeChanged: (String) -> Unit,
    onToggleRotationMode: (String) -> Unit
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModePreselectionSection(
                modePreselectionType = modePreselectionType,
                fixedMode = fixedMode,
                rotationModes = rotationModes,
                availableModes = availableModes,
                onTypeChanged = onTypeChanged,
                onFixedModeChanged = onFixedModeChanged,
                onToggleRotationMode = onToggleRotationMode
            )
        }
    }
}

@Composable
private fun TournamentBackupCard(
    autoCleanupDumps: Boolean,
    dumpRetentionMonths: Int,
    dumps: List<TournamentDumpFileInfo>,
    onAutoCleanupChanged: (Boolean) -> Unit,
    onShareDump: (String) -> Unit
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Turnier-Datensicherung (Haltenetz)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Vor dem Finalisieren eines Turniers wird automatisch eine vollständige Sicherung lokal auf dem Gerät gespeichert.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Alte Dumps automatisch bereinigen",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (autoCleanupDumps) "Sicherungen älter als $dumpRetentionMonths Monate werden gelöscht" else "Alle Sicherungen dauerhaft behalten",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoCleanupDumps,
                    onCheckedChange = onAutoCleanupChanged,
                    modifier = Modifier.testTag("settings_auto_cleanup_dumps_switch")
                )
            }

            HorizontalDivider()

            Text(
                text = "Lokale Sicherungen (${dumps.size})",
                style = MaterialTheme.typography.titleSmall
            )

            if (dumps.isEmpty()) {
                Text(
                    text = "Noch keine Sicherungen vorhanden. Sicherungen entstehen automatisch vor jedem Finalisieren.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                dumps.forEach { dump ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (dump.tournamentId > 0) "Turnier #${dump.tournamentId}" else dump.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val sizeKb = dump.sizeBytes / 1024.0
                                val sizeStr = String.format(java.util.Locale.ROOT, "%.1f KB", sizeKb)
                                Text(
                                    text = "${dump.fileName} • $sizeStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = { onShareDump(dump.fileName) },
                                modifier = Modifier.testTag("share_dump_${dump.fileName}")
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Backup teilen",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Teilen")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSection(
    isAdminMode: Boolean,
    isAdminVerifying: Boolean,
    maintenanceKeyInput: String,
    adminMessage: String?,
    adminError: String?,
    onMaintenanceKeyChanged: (String) -> Unit,
    onVerifyAdminKey: () -> Unit,
    onLockAdminMode: () -> Unit,
    onReopenTournament: (Long) -> Unit,
    onRecreatePosts: () -> Unit,
    onRestoreDump: (String) -> Unit,
    onClearAdminFeedback: () -> Unit
) {
    val context = LocalContext.current
    var isKeyVisible by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingDumpContent by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    pendingDumpContent = content
                    showRestoreConfirmDialog = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Lesen des Backup-Dumps", e)
            }
        }
    }

    if (showRestoreConfirmDialog && pendingDumpContent != null) {
        RestoreConfirmDialog(
            onConfirm = {
                val content = pendingDumpContent
                showRestoreConfirmDialog = false
                pendingDumpContent = null
                if (content != null) {
                    onRestoreDump(content)
                }
            },
            onDismiss = {
                showRestoreConfirmDialog = false
                pendingDumpContent = null
            }
        )
    }

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
            AdminHeader(
                isAdminMode = isAdminMode,
                onLockAdminMode = onLockAdminMode
            )

            if (!isAdminMode) {
                AdminLockedContent(
                    maintenanceKeyInput = maintenanceKeyInput,
                    isKeyVisible = isKeyVisible,
                    isAdminVerifying = isAdminVerifying,
                    onKeyChange = onMaintenanceKeyChanged,
                    onToggleVisibility = { isKeyVisible = !isKeyVisible },
                    onVerify = onVerifyAdminKey
                )
            } else {
                AdminActiveControls(
                    isAdminVerifying = isAdminVerifying,
                    onReopenTournament = onReopenTournament,
                    onRecreatePosts = onRecreatePosts,
                    onPickFile = { filePickerLauncher.launch("application/json") }
                )
            }

            if (adminMessage != null) {
                AdminFeedbackBanner(
                    message = adminMessage,
                    isError = false,
                    onDismiss = onClearAdminFeedback
                )
            }

            if (adminError != null) {
                AdminFeedbackBanner(
                    message = adminError,
                    isError = true,
                    onDismiss = onClearAdminFeedback
                )
            }
        }
    }
}

@Composable
private fun AdminHeader(
    isAdminMode: Boolean,
    onLockAdminMode: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isAdminMode) Icons.Default.LockOpen else Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Admin-Funktionen (Wartung)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (isAdminMode) {
            OutlinedButton(
                onClick = onLockAdminMode,
                modifier = Modifier.testTag("admin_lock_button")
            ) {
                Text("Sperren")
            }
        }
    }
}

@Composable
private fun AdminLockedContent(
    maintenanceKeyInput: String,
    isKeyVisible: Boolean,
    isAdminVerifying: Boolean,
    onKeyChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onVerify: () -> Unit
) {
    Text(
        text = "Gib den WordPress-Wartungsschlüssel (MB_MAINTENANCE_KEY) ein, um erweiterte administrative Aktionen wie Turnier-Wiedereröffnung oder Beitragsneugenerierung freizuschalten.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    OutlinedTextField(
        value = maintenanceKeyInput,
        onValueChange = onKeyChange,
        label = { Text("Wartungs-API-Key") },
        placeholder = { Text("Geheimer Wartungsschlüssel") },
        singleLine = true,
        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isKeyVisible) "Key verbergen" else "Key anzeigen"
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("maintenance_key_input")
    )

    Button(
        onClick = onVerify,
        enabled = !isAdminVerifying && maintenanceKeyInput.isNotBlank(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("verify_admin_key_button")
    ) {
        if (isAdminVerifying) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Prüfe...")
        } else {
            Text("Prüfen & Freischalten")
        }
    }
}

@Composable
private fun AdminActiveControls(
    isAdminVerifying: Boolean,
    onReopenTournament: (Long) -> Unit,
    onRecreatePosts: () -> Unit,
    onPickFile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Admin-Modus aktiv",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    AdminReopenTournamentCard(
        isAdminVerifying = isAdminVerifying,
        onReopenTournament = onReopenTournament
    )

    AdminRecreatePostsCard(
        isAdminVerifying = isAdminVerifying,
        onRecreatePosts = onRecreatePosts
    )

    AdminRestoreDumpCard(
        isAdminVerifying = isAdminVerifying,
        onPickFile = onPickFile
    )
}

@Composable
private fun AdminReopenTournamentCard(
    isAdminVerifying: Boolean,
    onReopenTournament: (Long) -> Unit
) {
    var reopenTournamentIdInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Turnier wieder öffnen",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Setzt ein finalisiertes Turnier auf dem Server wieder auf den Status 'open' zurück. Dadurch können Runden oder Ergebnisse erneut korrigiert und synchronisiert werden.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = reopenTournamentIdInput,
                    onValueChange = { reopenTournamentIdInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Turnier-ID") },
                    placeholder = { Text("z.B. 42") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reopen_tournament_id_input")
                )
                Button(
                    onClick = {
                        reopenTournamentIdInput.toLongOrNull()?.let { onReopenTournament(it) }
                    },
                    enabled = !isAdminVerifying && reopenTournamentIdInput.isNotBlank(),
                    modifier = Modifier.testTag("reopen_tournament_button")
                ) {
                    Text("Wieder öffnen")
                }
            }
        }
    }
}

@Composable
private fun AdminRecreatePostsCard(
    isAdminVerifying: Boolean,
    onRecreatePosts: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "WordPress-Beiträge neu generieren",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Baut alle WordPress-Turnierbeiträge aus den gespeicherten Rohdaten neu auf. Nützlich nach Template-Änderungen oder Korrekturen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onRecreatePosts,
                enabled = !isAdminVerifying,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recreate_posts_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Beiträge neu generieren (/recreatePosts)")
            }
        }
    }
}

@Composable
private fun AdminRestoreDumpCard(
    isAdminVerifying: Boolean,
    onPickFile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Backup-Dump wiederherstellen",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Importiert einen gesicherten JSON-Dump in die lokale Datenbank, markiert das Turnier als unfinalisiert und bereitet es für den erneuten Abgleich mit dem Server vor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onPickFile,
                enabled = !isAdminVerifying,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_dump_button")
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("JSON-Dump importieren")
            }
        }
    }
}

@Composable
private fun AdminFeedbackBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    val icon = if (isError) Icons.Default.Error else Icons.Default.CheckCircle
    val iconTint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(24.dp)
                    .testTag("dismiss_admin_feedback_button")
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Meldung schließen",
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RestoreConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup wiederherstellen?") },
        text = {
            Text(
                "Dieses Backup wird in die lokale Datenbank eingespielt. " +
                "Das Turnier wird wieder als unfinalisiert markiert und kann synchronisiert werden. " +
                "Möchtest du fortfahren?"
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Wiederherstellen")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun HelpAndFeedbackSection(serverUrl: String) {
    val context = LocalContext.current
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Hilfe & Feedback",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Probleme oder Fragen? Sende einen Fehlerbericht oder Feedback direkt an den Entwickler.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = {
                    de.kindermaenner.monatsblitz.ui.util.ErrorReportSender.sendErrorReport(
                        context = context,
                        actionTitle = "Allgemeines Feedback / Fehlerbericht",
                        errorMessage = "Aus den Einstellungen gesendet",
                        serverUrl = serverUrl
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Fehlerbericht / Feedback senden")
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "WordPress & Synchronisation",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Verbinde die App mit deiner WordPress-Vereinswebsite für automatische Spieler- und Turniersynchronisation.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilledTonalButton(
            onClick = onScanQr,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("scan_qr_button")
        ) {
            Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
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
    isServerConfigured: Boolean,
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
            Text(if (isServerConfigured) "Verbindung aktualisieren" else "Verbindung speichern")
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
            selected = playerSortField == PlayerSortField.FIRST_NAME,
            onClick = { onSortFieldChanged(PlayerSortField.FIRST_NAME) },
            label = {
                Text(
                    text = "Vorname",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = playerSortField == PlayerSortField.LAST_NAME,
            onClick = { onSortFieldChanged(PlayerSortField.LAST_NAME) },
            label = {
                Text(
                    text = "Nachname",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            modifier = Modifier.weight(1f)
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
            label = {
                Text(
                    text = "Aufsteigend (A–Z)",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = playerSortOrder == PlayerSortOrder.DESCENDING,
            onClick = { onSortOrderChanged(PlayerSortOrder.DESCENDING) },
            label = {
                Text(
                    text = "Absteigend (Z–A)",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActivePlayersCutoffSection(
    selectedMonths: Int,
    onMonthsSelected: (Int) -> Unit
) {
    Text(
        text = "Aktivitätszeitraum für Spieler",
        style = MaterialTheme.typography.titleSmall
    )

    Text(
        text = "Spieler mit einer Teilnahme innerhalb dieses Zeitraums gelten als aktiv.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(6 to "6 Monate", 12 to "12 Monate", 24 to "24 Monate").forEach { (months, label) ->
            FilterChip(
                selected = selectedMonths == months,
                onClick = { onMonthsSelected(months) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private const val TAG = "SettingsScreen"
