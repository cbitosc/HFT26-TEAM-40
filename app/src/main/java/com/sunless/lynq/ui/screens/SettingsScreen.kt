package com.sunless.lynq.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Wifi
import com.sunless.lynq.ai.AiPreferences
import com.sunless.lynq.ai.LanguageRegistry
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sunless.lynq.ui.ThemeMode
import com.sunless.lynq.demo.DemoData
import com.sunless.lynq.ui.components.AppleDivider
import com.sunless.lynq.ui.components.AppleGroupedCard
import com.sunless.lynq.ui.components.CmdBar
import com.sunless.lynq.ui.components.SectionHeader
import com.sunless.lynq.ui.components.TopBar
import com.sunless.lynq.ui.components.UserAvatar
import com.sunless.lynq.ui.navigation.Screen
import com.sunless.lynq.ui.navigation.navigateTab
import com.sunless.lynq.ui.theme.AppleBody
import com.sunless.lynq.ui.theme.AppleCaption
import com.sunless.lynq.ui.theme.AppleFootnote
import com.sunless.lynq.ui.theme.AppleHeadline
import com.sunless.lynq.ui.theme.AppleSubhead
import com.sunless.lynq.ui.theme.Lynq
import com.sunless.lynq.ui.theme.TechCodeStyle

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunless.lynq.ui.LynqViewModel

@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: LynqViewModel = viewModel(),
) {
    val pal = Lynq.pal
    val isMeshActive by viewModel.isMeshActive.collectAsStateWithLifecycle()
    val activeReachable by viewModel.activeReachableCount.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    var autoRelay by remember { mutableStateOf(true) }
    var locationSharing by remember { mutableStateOf(true) }

    val myDeviceName by viewModel.myDeviceName.collectAsStateWithLifecycle()
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editingName by remember(myDeviceName) { mutableStateOf(myDeviceName) }

    val aiApiKey by viewModel.aiApiKey.collectAsStateWithLifecycle()
    val aiModel by viewModel.aiModel.collectAsStateWithLifecycle()
    val aiBaseUrl by viewModel.aiBaseUrl.collectAsStateWithLifecycle()
    val aiTargetLang by viewModel.aiTargetLanguage.collectAsStateWithLifecycle()

    var showAiKeyDialog by remember { mutableStateOf(false) }
    var showAiModelDialog by remember { mutableStateOf(false) }
    var showAiLangDialog by remember { mutableStateOf(false) }
    var showAiBaseUrlDialog by remember { mutableStateOf(false) }
    var editingBaseUrl by remember(aiBaseUrl) { mutableStateOf(aiBaseUrl) }

    if (showAiKeyDialog) {
        FeatherlessApiKeyDialog(
            currentKey = aiApiKey,
            onDismiss = { showAiKeyDialog = false },
            onSave = {
                viewModel.updateAiApiKey(it)
                showAiKeyDialog = false
            },
        )
    }

    if (showAiLangDialog) {
        LanguageSelectorDialog(
            currentLang = aiTargetLang,
            onDismiss = { showAiLangDialog = false },
            onSelect = {
                viewModel.setTargetLanguage(it)
                showAiLangDialog = false
            },
        )
    }

    if (showAiModelDialog) {
        AlertDialog(
            onDismissRequest = { showAiModelDialog = false },
            title = {
                Text(
                    text = "Select Featherless LLM",
                    style = AppleHeadline,
                    fontWeight = FontWeight.Bold,
                    color = pal.textPrimary,
                )
            },
            text = {
                Column {
                    AiPreferences.AVAILABLE_MODELS.forEach { modelName ->
                        val isSelected = modelName == aiModel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateAiModel(modelName)
                                    showAiModelDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = modelName.substringAfterLast("/"),
                                    style = AppleHeadline,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) pal.accentBlue else pal.textPrimary,
                                )
                                Text(
                                    text = modelName,
                                    style = AppleCaption,
                                    color = pal.textSecondary,
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = pal.accentBlue,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        AppleDivider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAiModelDialog = false }) {
                    Text("Cancel", color = pal.textSecondary)
                }
            },
        )
    }

    if (showAiBaseUrlDialog) {
        AlertDialog(
            onDismissRequest = { showAiBaseUrlDialog = false },
            title = {
                Text(
                    text = "AI Endpoint Base URL",
                    style = AppleHeadline,
                    fontWeight = FontWeight.Bold,
                    color = pal.textPrimary,
                )
            },
            text = {
                Column {
                    Text(
                        text = "Defaults to your live Render backend (https://lynq-ai-backend.onrender.com). The backend securely holds the Featherless API key so no key is required on client devices.",
                        style = AppleFootnote,
                        color = pal.textSecondary,
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = editingBaseUrl,
                        onValueChange = { editingBaseUrl = it },
                        singleLine = true,
                        label = { Text("Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateAiBaseUrl(editingBaseUrl)
                        showAiBaseUrlDialog = false
                    }
                ) {
                    Text("Save", color = pal.accentBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editingBaseUrl = AiPreferences.DEFAULT_BASE_URL
                        viewModel.updateAiBaseUrl(AiPreferences.DEFAULT_BASE_URL)
                        showAiBaseUrlDialog = false
                    }
                ) {
                    Text("Reset Default", color = pal.textSecondary)
                }
            },
        )
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    text = "Edit Mesh Device Name",
                    style = AppleHeadline,
                    fontWeight = FontWeight.Bold,
                    color = pal.textPrimary,
                )
            },
            text = {
                Column {
                    Text(
                        text = "This name is broadcast to nearby peers across the mesh network. By default it uses your Quick Share / device name (e.g. sunless).",
                        style = AppleFootnote,
                        color = pal.textSecondary,
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = editingName,
                        onValueChange = { editingName = it },
                        singleLine = true,
                        placeholder = { Text("e.g. sunless") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateDeviceName(editingName)
                        showEditNameDialog = false
                    }
                ) {
                    Text("Save", color = pal.accentBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.resetDeviceNameToSystem()
                        showEditNameDialog = false
                    }
                ) {
                    Text("Reset to Quick Share", color = pal.textSecondary)
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
        TopBar(reachableCount = activeReachable, title = "Settings")

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Profile Section
                AppleGroupedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingName = myDeviceName
                                showEditNameDialog = true
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        UserAvatar(name = myDeviceName, size = 52.dp, isRelay = true)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = myDeviceName,
                                    style = AppleHeadline,
                                    fontWeight = FontWeight.Bold,
                                    color = pal.textPrimary,
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit name",
                                    tint = pal.accentBlue,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Quick Share / Mesh Identity · Tap to edit",
                                style = AppleFootnote,
                                color = pal.accentGreen,
                            )
                        }
                    }

                    AppleDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Device Fingerprint (ID)",
                                style = AppleFootnote,
                                color = pal.textSecondary,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = viewModel.myDeviceId,
                                style = TechCodeStyle,
                                color = pal.textPrimary,
                                fontSize = 12.sp,
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = pal.textMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Appearance Section
                SectionHeader(title = "Appearance")
                Spacer(Modifier.height(10.dp))

                AppleGroupedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppearanceOption(
                            label = "System",
                            icon = Icons.Rounded.BrightnessAuto,
                            selected = themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                            modifier = Modifier.weight(1f),
                        )
                        AppearanceOption(
                            label = "Light",
                            icon = Icons.Rounded.LightMode,
                            selected = themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f),
                        )
                        AppearanceOption(
                            label = "Dark",
                            icon = Icons.Rounded.DarkMode,
                            selected = themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Radio & Mesh Connectivity
                SectionHeader(title = "Radio & Mesh")
                Spacer(Modifier.height(10.dp))

                AppleGroupedCard {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Wifi,
                        iconBg = pal.accentBlue,
                        title = "Mesh Radio & Discovery",
                        subtitle = if (isMeshActive) "Strategy.P2P_CLUSTER online" else "Mesh offline",
                        checked = isMeshActive,
                        onCheckedChange = { viewModel.toggleMesh(it) },
                    )
                    AppleDivider()
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Hub,
                        iconBg = pal.accentGreen,
                        title = "Automatic SOS Relaying",
                        subtitle = "Hop-by-hop mesh forwarding",
                        checked = autoRelay,
                        onCheckedChange = { autoRelay = it },
                    )
                    AppleDivider()
                    SettingsValueRow(
                        icon = Icons.Rounded.Bluetooth,
                        iconBg = Color(0xFF5E5CE6),
                        title = "Hop Limit Budget (TTL)",
                        value = "6 hops",
                    )
                }

                Spacer(Modifier.height(22.dp))

                // Emergency & Security
                SectionHeader(title = "Security & Privacy")
                Spacer(Modifier.height(10.dp))

                AppleGroupedCard {
                    SettingsValueRow(
                        icon = Icons.Rounded.Lock,
                        iconBg = pal.accentGreen,
                        title = "Chat Payload Encryption",
                        value = "AES-256-GCM",
                    )
                    AppleDivider()
                    SettingsValueRow(
                        icon = Icons.Rounded.Fingerprint,
                        iconBg = pal.accentBlue,
                        title = "Hardware Keystore",
                        value = "ECDSA P-256",
                    )
                    AppleDivider()
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Emergency,
                        iconBg = pal.emergencyRed,
                        title = "Emergency Location Broadcast",
                        subtitle = "Include GPS in SOS alerts",
                        checked = locationSharing,
                        onCheckedChange = { locationSharing = it },
                    )
                }

                Spacer(Modifier.height(22.dp))

                // Featherless AI & Intelligence
                SectionHeader(title = "Featherless AI & Intelligence")
                Spacer(Modifier.height(10.dp))

                AppleGroupedCard {
                    SettingsClickableRow(
                        icon = Icons.Rounded.Dns,
                        iconBg = pal.accentGreen,
                        title = "AI Cloud Backend (Render)",
                        subtitle = if (aiBaseUrl.contains("render.com")) "$aiBaseUrl (Ready)" else aiBaseUrl,
                        onClick = {
                            editingBaseUrl = aiBaseUrl
                            showAiBaseUrlDialog = true
                        },
                    )
                    AppleDivider()
                    SettingsClickableRow(
                        icon = Icons.Rounded.Key,
                        iconBg = if (aiApiKey.isNotBlank()) pal.accentGreen else Color(0xFF007AFF),
                        title = "Featherless API Key",
                        subtitle = if (aiApiKey.isNotBlank()) "sk-•••••••• (Client Override)" else "Managed by Render Backend (Ready)",
                        onClick = { showAiKeyDialog = true },
                    )
                    AppleDivider()
                    SettingsClickableRow(
                        icon = Icons.Rounded.AutoAwesome,
                        iconBg = Color(0xFFAF52DE),
                        title = "Active Model",
                        subtitle = aiModel.substringAfterLast("/"),
                        onClick = { showAiModelDialog = true },
                    )
                    AppleDivider()
                    SettingsClickableRow(
                        icon = Icons.Rounded.Language,
                        iconBg = Color(0xFF007AFF),
                        title = "Crisis Translation Language",
                        subtitle = "${LanguageRegistry.find(aiTargetLang).flag} $aiTargetLang (${LanguageRegistry.find(aiTargetLang).nativeName})",
                        onClick = { showAiLangDialog = true },
                    )
                }

                Spacer(Modifier.height(22.dp))

                // About
                SectionHeader(title = "About")
                Spacer(Modifier.height(10.dp))

                AppleGroupedCard {
                    SettingsValueRow(
                        icon = Icons.Rounded.Security,
                        iconBg = Color(0xFF636366),
                        title = "Protocol Version",
                        value = "Lynq v1.0",
                    )
                    AppleDivider()
                    SettingsValueRow(
                        icon = Icons.Rounded.CheckCircle,
                        iconBg = Color(0xFF30D158),
                        title = "Local Database",
                        value = "Room SQLite Encrypted",
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Lynq Emergency Mesh · 100% Offline · No Cellular Needed",
                    style = AppleCaption,
                    color = pal.textMuted,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val pal = Lynq.pal
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column {
                Text(
                    text = title,
                    style = AppleHeadline,
                    color = pal.textPrimary,
                    fontSize = 15.sp,
                )
                Text(
                    text = subtitle,
                    style = AppleCaption,
                    color = pal.textSecondary,
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = pal.accentGreen,
                uncheckedThumbColor = pal.textMuted,
                uncheckedTrackColor = pal.surfaceElevated,
            ),
        )
    }
}

@Composable
private fun SettingsValueRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    value: String,
) {
    val pal = Lynq.pal
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = title,
                style = AppleHeadline,
                color = pal.textPrimary,
                fontSize = 15.sp,
            )
        }

        Text(
            text = value,
            style = AppleFootnote,
            color = pal.textSecondary,
        )
    }
}

@Composable
private fun AppearanceOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pal = Lynq.pal
    val borderAnim by animateColorAsState(
        targetValue = if (selected) pal.accentBlue else pal.borderSubtle,
        animationSpec = tween(180),
        label = "themeBorderAnim",
    )
    val bgAnim by animateColorAsState(
        targetValue = if (selected) pal.accentBlue.copy(alpha = 0.14f) else pal.surfaceElevated,
        animationSpec = tween(180),
        label = "themeBgAnim",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) pal.accentBlue else pal.textSecondary,
        animationSpec = tween(180),
        label = "themeColorAnim",
    )

    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(12.dp),
        color = bgAnim,
        border = BorderStroke(if (selected) 1.5.dp else 0.5.dp, borderAnim),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = AppleCaption,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val pal = Lynq.pal
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column {
                Text(
                    text = title,
                    style = AppleHeadline,
                    color = pal.textPrimary,
                    fontSize = 15.sp,
                )
                Text(
                    text = subtitle,
                    style = AppleCaption,
                    color = pal.textSecondary,
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = pal.textMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}