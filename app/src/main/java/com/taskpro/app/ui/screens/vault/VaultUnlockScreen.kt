package com.taskpro.app.ui.screens.vault

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.SecureFlagPolicy
import com.taskpro.app.R
import com.taskpro.app.vault.VaultDeviceUnlock
import com.taskpro.app.vault.VaultRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val UnlockBackground = Color(0xff0c1110)
private val UnlockSurface = Color(0xff131a19)
private val UnlockAccent = Color(0xff5ee0b0)
private val UnlockText = Color(0xffe7eeec)
private val UnlockMuted = Color(0xff8e9b98)
private val UnlockLine = Color(0xffdcfff0).copy(alpha = 0.07f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultUnlockScreen(repository: VaultRepository, device: VaultDeviceUnlock?, busy: Boolean, error: Int?, notice: Int?,
                      unlock: (CharArray) -> Unit, recover: (CharArray, CharArray) -> Unit, createMaster: () -> Unit) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val appDark = com.taskpro.app.ui.theme.LocalAppDarkTheme.current
    val window = (context as? android.app.Activity)?.window
    SideEffect { window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view).apply {
        isAppearanceLightStatusBars = false; isAppearanceLightNavigationBars = false
    } } }
    DisposableEffect(window, appDark) {
        onDispose { window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view).apply {
            isAppearanceLightStatusBars = !appDark; isAppearanceLightNavigationBars = !appDark
        } } }
    }
    val panelHeight = (LocalConfiguration.current.screenHeightDp * 0.78f).dp
    val host = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var sheet by remember { mutableStateOf<String?>(null) }
    var pinReady by remember { mutableStateOf(false) }
    var biometricReady by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }
    var operation by remember { mutableStateOf<VaultDeviceUnlock.BiometricOperation?>(null) }
    var prompt by remember { mutableStateOf<BiometricPrompt?>(null) }
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var forgot by remember { mutableStateOf(false) }
    var cooldown by remember { mutableIntStateOf(0) }
    var failures by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }
    val hardware = remember { host != null && runCatching {
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }.getOrDefault(false) }
    fun action(block: suspend () -> Unit) {
        if (working || busy) return
        working = true; message = null
        scope.launch {
            try { block() } catch (e: CancellationException) { throw e }
            catch (e: VaultDeviceUnlock.Failure) { message = when (e.stage) {
                VaultDeviceUnlock.FailureStage.MASTER_PASSWORD -> R.string.unlock_master_error
                VaultDeviceUnlock.FailureStage.PIN_ENCRYPTION -> R.string.unlock_pin_setup_error
                VaultDeviceUnlock.FailureStage.KEYSTORE -> R.string.unlock_keystore_error
                VaultDeviceUnlock.FailureStage.STORAGE -> R.string.unlock_storage_error
                VaultDeviceUnlock.FailureStage.SESSION_CHANGED -> R.string.unlock_changed_error
            } }
            catch (_: Exception) { message = R.string.unlock_device_error }
            finally { working = false }
        }
    }
    fun biometric(master: CharArray? = null) {
        val store = device
        if (store == null || host == null || !hardware) { master?.fill('\u0000'); sheet = "master"; return }
        action {
            val prepared = store.prepareBiometric(master)
            if (!lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                prepared.clear(); return@action
            }
            operation?.clear(); operation = prepared
            val biometricPrompt = BiometricPrompt(host, ContextCompat.getMainExecutor(context), object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    action {
                        try {
                            store.finishBiometric(prepared, checkNotNull(result.cryptoObject?.cipher))
                            if (prepared.setup) { biometricReady = true; sheet = null }
                        } finally { operation = null }
                    }
                }
                override fun onAuthenticationError(code: Int, text: CharSequence) {
                    prepared.clear(); operation = null
                    if (code == BiometricPrompt.ERROR_NEGATIVE_BUTTON) sheet = if (pinReady) "pin" else "master"
                    else if (code != BiometricPrompt.ERROR_USER_CANCELED && code != BiometricPrompt.ERROR_CANCELED) message = R.string.unlock_device_error
                }
                override fun onAuthenticationFailed() { message = R.string.unlock_biometric_retry }
            })
            prompt = biometricPrompt
            biometricPrompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(context.getString(R.string.unlock_title))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText(context.getString(if (pinReady) R.string.unlock_pin else R.string.unlock_master))
                .build(), BiometricPrompt.CryptoObject(prepared.cipher))
        }
    }
    DisposableEffect(Unit) { onDispose { prompt?.cancelAuthentication(); operation?.clear() } }
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                prompt?.cancelAuthentication(); operation?.clear(); operation = null
                password = ""; pin = ""; confirmation = ""
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        val available = runCatching { device?.available() }.getOrNull()
        pinReady = available?.first == true; biometricReady = available?.second == true
        if (biometricReady && hardware) biometric()
    }
    LaunchedEffect(sheet) { password = ""; pin = ""; confirmation = ""; message = null }
    LaunchedEffect(sheet, failures) {
        while (sheet == "pin") { cooldown = runCatching { device?.remainingSeconds() ?: 0 }.getOrDefault(30); delay(250) }
    }
    LaunchedEffect(failures) {
        if (failures > 0) { repeat(3) { shake.animateTo(8f); shake.animateTo(-8f) }; shake.animateTo(0f) }
    }
    val colors = darkColorScheme(primary = UnlockAccent, onPrimary = UnlockBackground, background = UnlockBackground,
        surface = UnlockSurface, onSurface = UnlockText, onSurfaceVariant = UnlockMuted,
        error = Color(0xffff8a7a), outline = UnlockLine, outlineVariant = UnlockLine)
    MaterialTheme(colorScheme = colors) {
        Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(UnlockBackground).padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.unlock_vault), Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = UnlockText)
                if (device != null) FilledIconButton(onClick = { sheet = "setup" }, shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = UnlockSurface)) {
                    Icon(Icons.Default.Tune, stringResource(R.string.unlock_settings))
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(44.dp))
                Box(Modifier.size(64.dp).background(UnlockAccent.copy(alpha = 0.16f), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Lock, null, Modifier.size(30.dp), tint = UnlockAccent)
                }
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.unlock_locked), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = UnlockText, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.unlock_timeout), fontSize = 13.sp, color = UnlockMuted, textAlign = TextAlign.Center)
                if (hardware && device != null) {
                    Spacer(Modifier.height(28.dp))
                    Box(Modifier.size(168.dp).background(UnlockAccent.copy(alpha = 0.025f), CircleShape), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(140.dp).background(UnlockAccent.copy(alpha = 0.04f), CircleShape), contentAlignment = Alignment.Center) {
                            Surface(onClick = { if (biometricReady) biometric() else sheet = "setup" }, enabled = !working && !busy, shape = CircleShape, color = UnlockBackground,
                                border = BorderStroke(1.dp, UnlockText.copy(alpha = 0.12f)), modifier = Modifier.size(112.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Box(Modifier.size(84.dp).background(Color(0xff1a2322), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Fingerprint, stringResource(if (biometricReady) R.string.unlock_fingerprint else R.string.unlock_enable_biometric), Modifier.size(36.dp), tint = UnlockAccent)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(26.dp))
                    Text(stringResource(if (biometricReady) R.string.unlock_touch else R.string.unlock_enable_biometric), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = UnlockText, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.unlock_another), fontSize = 11.sp, color = UnlockMuted)
                }
                if (sheet == null) message?.let { Text(stringResource(it), color = colors.error, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center) }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (device != null) OutlinedButton(onClick = { sheet = if (pinReady) "pin" else "setup" }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = UnlockSurface, contentColor = UnlockText), border = BorderStroke(1.dp, UnlockLine)) {
                    Icon(Icons.Default.Dialpad, null, Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.unlock_pin), fontSize = 13.sp, textAlign = TextAlign.Center)
                }
                OutlinedButton(onClick = { sheet = "master" }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = UnlockSurface, contentColor = UnlockText), border = BorderStroke(1.dp, UnlockLine)) {
                    Icon(Icons.Default.Key, null, Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.unlock_master), fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
            if (sheet == null) TextButton(onClick = createMaster, enabled = !busy && !working, modifier = Modifier.padding(bottom = 8.dp)) {
                Text(stringResource(R.string.unlock_create_master))
            }
        }
        if (sheet != null && !forgot) Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = panelHeight),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), shadowElevation = 8.dp, border = BorderStroke(1.dp, UnlockLine)) {
            Column(Modifier.vaultActivity().fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(if (sheet == "pin") R.string.unlock_pin else if (sheet == "setup") R.string.unlock_settings else R.string.unlock_master), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton(enabled = !busy && !working, onClick = { sheet = null }) { Icon(Icons.Default.Close, stringResource(R.string.cancel)) }
                }
                if (sheet == "pin") {
                    Text("•".repeat(pin.length).padEnd(6, '○'), Modifier.fillMaxWidth().graphicsLayer { translationX = shake.value }, textAlign = TextAlign.Center, fontSize = 24.sp)
                    if (cooldown > 0) Text(stringResource(R.string.unlock_wait, cooldown), color = colors.error)
                    listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "back")).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { digit ->
                                TextButton(enabled = cooldown == 0 && !working && digit.isNotEmpty(), modifier = Modifier.weight(1f).heightIn(min = 64.dp), onClick = {
                                    pin = if (digit == "back") pin.dropLast(1) else (pin + digit).take(6)
                                }) { if (digit == "back") Icon(Icons.AutoMirrored.Filled.Backspace, stringResource(R.string.unlock_erase)) else Text(digit, fontSize = 24.sp) }
                            }
                        }
                    }
                    Button(enabled = pin.length in 4..6 && cooldown == 0 && !working, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                        val value = pin.toCharArray(); pin = ""
                        action {
                            try { checkNotNull(device).unlockPin(value) }
                            catch (e: CancellationException) { throw e }
                            catch (_: VaultDeviceUnlock.WrongPin) { message = R.string.unlock_wrong_pin; failures++ }
                        }
                    }) { Text(stringResource(R.string.vault_unlock)) }
                    TextButton(onClick = { sheet = "master" }) { Text(stringResource(R.string.unlock_master)) }
                } else {
                    if (sheet == "setup") {
                        Text(stringResource(R.string.unlock_setup_note), color = UnlockMuted)
                        TextButton(onClick = createMaster, enabled = !busy && !working) { Text(stringResource(R.string.unlock_create_master)) }
                    }
                    VaultInput(stringResource(if (sheet == "setup") R.string.unlock_existing_master else R.string.vault_master), password, { password = it.take(1024) }, secret = true)
                    if (sheet == "setup") {
                        VaultInput(stringResource(R.string.unlock_new_pin), pin, { pin = it.filter { c -> c in '0'..'9' }.take(6) }, secret = true)
                        VaultInput(stringResource(R.string.unlock_confirm_pin), confirmation, { confirmation = it.filter { c -> c in '0'..'9' }.take(6) }, secret = true)
                        Button(enabled = !working && password.isNotEmpty() && pin.length in 4..6 && pin == confirmation, onClick = {
                            val p = password.toCharArray(); val value = pin.toCharArray(); password = ""; pin = ""; confirmation = ""
                            action { checkNotNull(device).setPin(p, value); pinReady = true; sheet = "pin" }
                        }) { Text(stringResource(R.string.unlock_save_pin)) }
                        if (hardware) OutlinedButton(enabled = !working && password.isNotEmpty(), onClick = {
                            val p = password.toCharArray(); password = ""; biometric(p)
                        }) { Text(stringResource(R.string.unlock_enable_biometric)) }
                    } else {
                        Button(enabled = !busy && password.isNotEmpty(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                            val p = password.toCharArray(); password = ""; unlock(p)
                        }) { Text(stringResource(R.string.vault_unlock)) }
                        TextButton(onClick = { password = ""; forgot = true }) { Text(stringResource(R.string.vault_forgot)) }
                    }
                }
                (message ?: error)?.let { Text(stringResource(it), color = colors.error) }
                notice?.let { Text(stringResource(it)) }
                if (busy || working) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.vault_version, com.taskpro.app.BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelSmall, color = UnlockMuted)
            }
        }
        }
        androidx.activity.compose.BackHandler(sheet != null && !forgot) { if (!busy && !working) sheet = null }
        if (forgot) RecoveryForm(busy, error, { forgot = false }, recover)
    }
}
