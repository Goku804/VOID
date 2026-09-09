package com.core.voidapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.core.voidapp.data.ai.AIConfig
import com.core.voidapp.data.ai.AIConfigStore
import com.core.voidapp.data.ai.AIProvider
import com.core.voidapp.data.ai.AIRepository
import com.core.voidapp.data.ai.AIResult
import kotlinx.coroutines.launch

/**
 * SETTINGS -> INTEGRATIONS -> AI. Lets the user pick a provider and
 * enter/replace/remove credentials, and test the connection before
 * relying on it in Chat. The key is never displayed again once saved —
 * only a masked preview (AIConfigStore.maskKey).
 */
@Composable
fun AISettingsScreen() {
    val context = LocalContext.current
    var savedConfig by remember { mutableStateOf(AIConfigStore.load(context)) }

    var provider by remember { mutableStateOf(savedConfig?.provider ?: AIProvider.OPENAI) }
    // The fallback chain being edited — starts from whatever was last saved.
    // Nothing here is persisted until SAVE is pressed, same as the rest of this screen.
    var keys by remember { mutableStateOf(savedConfig?.apiKeys ?: emptyList()) }
    var newKeyInput by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var model by remember { mutableStateOf(savedConfig?.model ?: provider.defaultModel) }
    var baseUrl by remember { mutableStateOf(savedConfig?.baseUrl ?: provider.defaultBaseUrl) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusColor by remember { mutableStateOf(VoidColors.TextSecondary) }
    var testing by remember { mutableStateOf(false) }
    var toolsEnabled by remember { mutableStateOf(AIConfigStore.areToolsEnabled(context)) }
    val scope = rememberCoroutineScope()
    val hasSavedKey = savedConfig != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        VoidSectionLabel("AI PROVIDER")
        Spacer(modifier = Modifier.height(8.dp))
        ProviderDropdown(selected = provider, onSelected = {
            val wasDefaultModel = model == provider.defaultModel || model.isBlank()
            val wasDefaultUrl = baseUrl == provider.defaultBaseUrl || baseUrl.isBlank()
            provider = it
            if (wasDefaultModel) model = it.defaultModel
            if (wasDefaultUrl) baseUrl = it.defaultBaseUrl
        })

        Spacer(modifier = Modifier.height(16.dp))
        VoidSectionLabel("API KEYS")
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Add more than one key to build a fallback chain \u2014 if the first runs out of quota or gets rate-limited, VOID automatically retries with the next one, in order.",
            color = VoidColors.TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (keys.isEmpty()) {
            VoidCard {
                Text("No keys added yet.", color = VoidColors.TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            keys.forEachIndexed { index, key ->
                VoidCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(if (index == 0) VoidColors.Success else VoidColors.TextSecondary, Modifier.size(7.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${index + 1}. ${AIConfigStore.maskKey(key)}" + if (index == 0) "  (primary)" else "  (fallback)",
                            color = VoidColors.TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "REMOVE",
                            color = VoidColors.Danger,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.clickable {
                                keys = keys.toMutableList().also { it.removeAt(index) }
                            }.padding(4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newKeyInput,
                onValueChange = { newKeyInput = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Paste another API key", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Icon(
                        imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = VoidColors.TextSecondary,
                        modifier = Modifier.clickable { showKey = !showKey }
                    )
                },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = VoidColors.TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VoidColors.Cyan,
                    unfocusedBorderColor = VoidColors.Border,
                    cursorColor = VoidColors.Cyan
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "ADD",
                color = VoidColors.Cyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.clickable {
                    val trimmed = newKeyInput.trim()
                    if (trimmed.isNotBlank() && trimmed !in keys) {
                        keys = keys + trimmed
                        newKeyInput = ""
                        status = null
                    }
                }.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        var availableModels by remember { mutableStateOf<List<String>>(emptyList()) }
        var showModelPicker by remember { mutableStateOf(false) }
        var loadingModels by remember { mutableStateOf(false) }

        VoidSectionLabel("MODEL")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsField(value = model, onValueChange = { model = it }, placeholder = provider.defaultModel.ifBlank { "model name" })
        Spacer(modifier = Modifier.height(6.dp))
        Box {
            Text(
                text = if (loadingModels) "LOADING MODELS..." else "SEE AVAILABLE MODELS FOR THIS KEY",
                color = VoidColors.Cyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.clickable(enabled = !loadingModels) {
                    val key = keys.firstOrNull() ?: newKeyInput.trim()
                    if (key.isBlank()) {
                        status = "Enter an API key first."
                        statusColor = VoidColors.Warning
                        return@clickable
                    }
                    loadingModels = true
                    status = null
                    val config = AIConfig(provider = provider, apiKey = key, model = model, baseUrl = baseUrl.ifBlank { provider.defaultBaseUrl })
                    scope.launch {
                        AIRepository.fetchModels(config).fold(
                            onSuccess = { models ->
                                availableModels = models
                                showModelPicker = models.isNotEmpty()
                                if (models.isEmpty()) {
                                    status = "Provider returned no models."
                                    statusColor = VoidColors.Warning
                                }
                            },
                            onFailure = {
                                status = it.message ?: "Could not load model list."
                                statusColor = VoidColors.Danger
                            }
                        )
                        loadingModels = false
                    }
                }
            )
            DropdownMenu(expanded = showModelPicker, onDismissRequest = { showModelPicker = false }) {
                availableModels.forEach { id ->
                    DropdownMenuItem(
                        text = { Text(id, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        onClick = { model = id; showModelPicker = false }
                    )
                }
            }
        }

        if (provider == AIProvider.OPENAI_COMPATIBLE) {
            Spacer(modifier = Modifier.height(16.dp))
            VoidSectionLabel("BASE URL")
            Spacer(modifier = Modifier.height(8.dp))
            SettingsField(value = baseUrl, onValueChange = { baseUrl = it }, placeholder = "https://your-provider.example.com/v1")
        }

        Spacer(modifier = Modifier.height(16.dp))
        VoidSectionLabel("AI PERMISSIONS")
        Spacer(modifier = Modifier.height(8.dp))
        VoidCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "CREATE \u00b7 EDIT \u00b7 DELETE \u00b7 ANALYZE",
                        color = VoidColors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Lets VOID AI act directly on your data \u2014 subjects, units, marks, tasks, exams, Circle Plan slots, and scores \u2014 instead of only reading it. Granted by default; turn off for read-only.",
                        color = VoidColors.TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Switch(
                    checked = toolsEnabled,
                    onCheckedChange = {
                        toolsEnabled = it
                        AIConfigStore.setToolsEnabled(context, it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VoidColors.Success,
                        checkedTrackColor = VoidColors.Success.copy(alpha = 0.4f),
                        uncheckedThumbColor = VoidColors.TextSecondary,
                        uncheckedTrackColor = VoidColors.Surface2
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        status?.let {
            Text(it, color = statusColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(10.dp))
        }

        /** Whatever's typed in the "add key" field but not yet added counts too, so a forgotten tap on ADD doesn't lose it. */
        fun pendingKeyChain(): List<String> {
            val pending = newKeyInput.trim()
            return (keys + listOfNotNull(pending.takeIf { it.isNotBlank() && it !in keys })).distinct()
        }

        SettingsButton(text = if (testing) "TESTING..." else "TEST CONNECTION", accent = VoidColors.Info, enabled = !testing) {
            val chain = pendingKeyChain()
            if (chain.isEmpty()) {
                status = "Add at least one API key first."
                statusColor = VoidColors.Warning
                return@SettingsButton
            }
            testing = true
            status = null
            val config = AIConfig(
                provider = provider, apiKey = chain.first(), apiKeys = chain,
                model = model.ifBlank { provider.defaultModel }, baseUrl = baseUrl.ifBlank { provider.defaultBaseUrl }
            )
            scope.launch {
                when (val result = AIRepository.testConnection(config)) {
                    is AIResult.Success -> { status = "Connected (${chain.size} key(s) in chain)."; statusColor = VoidColors.Success }
                    is AIResult.Failure -> { status = result.message; statusColor = VoidColors.Danger }
                }
                testing = false
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        SettingsButton(text = "SAVE", accent = VoidColors.Success) {
            val chain = pendingKeyChain()
            if (chain.isEmpty()) {
                status = "Add at least one API key before saving."
                statusColor = VoidColors.Warning
                return@SettingsButton
            }
            val newConfig = AIConfig(
                provider = provider, apiKey = chain.first(), apiKeys = chain,
                model = model.ifBlank { provider.defaultModel }, baseUrl = baseUrl.ifBlank { provider.defaultBaseUrl }
            )
            AIConfigStore.save(context, newConfig)
            savedConfig = newConfig
            keys = chain
            newKeyInput = ""
            status = "Saved ${chain.size} key(s)."
            statusColor = VoidColors.Success
        }

        if (hasSavedKey) {
            Spacer(modifier = Modifier.height(10.dp))
            SettingsButton(text = "CLEAR ALL KEYS", accent = VoidColors.Danger) {
                AIConfigStore.clear(context)
                savedConfig = null
                keys = emptyList()
                newKeyInput = ""
                status = "Removed."
                statusColor = VoidColors.TextSecondary
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Your key is stored only on this device and sent directly to the provider you selected. VOID never logs it or includes it in reports.",
            color = VoidColors.TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
private fun ProviderDropdown(selected: AIProvider, onSelected: (AIProvider) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        VoidCard(modifier = Modifier.clickable { expanded = true }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(selected.displayName, color = VoidColors.TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = VoidColors.TextSecondary)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AIProvider.entries.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.displayName, fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                    onClick = { onSelected(p); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun SettingsField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = VoidColors.TextSecondary) },
        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = VoidColors.TextPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VoidColors.Cyan,
            unfocusedBorderColor = VoidColors.Border,
            cursorColor = VoidColors.Cyan
        )
    )
}

@Composable
private fun SettingsButton(text: String, accent: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = if (enabled) 0.12f else 0.05f))
            .border(1.dp, if (enabled) accent else VoidColors.Border, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = if (enabled) accent else VoidColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}
