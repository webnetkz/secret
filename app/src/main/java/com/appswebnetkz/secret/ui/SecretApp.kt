package com.appswebnetkz.secret.ui

import android.Manifest
import android.app.DownloadManager
import android.graphics.Bitmap
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.view.MotionEvent
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.appswebnetkz.secret.R
import com.appswebnetkz.secret.data.model.ChatMessage
import com.appswebnetkz.secret.data.model.ChatRoom
import com.appswebnetkz.secret.data.model.MessageType
import com.appswebnetkz.secret.data.model.UserProfile
import com.appswebnetkz.secret.data.network.NetworkConfig
import com.appswebnetkz.secret.ui.qr.PortraitQrCaptureActivity
import com.appswebnetkz.secret.ui.theme.SecretTheme
import com.appswebnetkz.secret.viewmodel.AppScreen
import com.appswebnetkz.secret.viewmodel.ChatUiState
import com.appswebnetkz.secret.viewmodel.ChatViewModel
import com.appswebnetkz.secret.viewmodel.ConnectionState
import com.appswebnetkz.secret.viewmodel.LocaleMode
import com.appswebnetkz.secret.viewmodel.ThemeMode
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin

@Composable
fun SecretApp(viewModel: ChatViewModel = viewModel()) {
    val state = viewModel.uiState
    val screen = viewModel.screen
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var askedNotificationPermission by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        askedNotificationPermission = true
    }

    val qrScannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val payload = result.contents?.trim().orEmpty()
        if (payload.isNotBlank()) {
            viewModel.linkProfileFromQrPayload(payload)
        }
    }

    val launchQrScanner = {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt(text(state, "Scan QR to link account", "Сканируйте QR для подключения аккаунта"))
            setBeepEnabled(false)
            setCaptureActivity(PortraitQrCaptureActivity::class.java)
            setOrientationLocked(true)
            setCameraId(0)
        }
        qrScannerLauncher.launch(options)
    }

    BackHandler(enabled = screen != AppScreen.Onboarding) {
        viewModel.goBack()
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    LaunchedEffect(state.infoMessage) {
        val message = state.infoMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearInfo()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || askedNotificationPermission) {
            return@LaunchedEffect
        }
        if (!hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        askedNotificationPermission = true
    }

    LaunchedEffect(state.user?.id) {
        val user = state.user ?: return@LaunchedEffect
        while (user.id == viewModel.uiState.user?.id) {
            delay(7000)
            viewModel.refreshChats(autoOpenFirst = false)
        }
    }

    SecretTheme(darkTheme = state.theme == ThemeMode.DARK, dynamicColor = false) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when {
                    state.isLoading && state.user == null -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    screen is AppScreen.Onboarding -> {
                        OnboardingScreen(
                            state = state,
                            onLinkAccount = launchQrScanner,
                            onCreateNew = viewModel::createNewProfile
                        )
                    }

                    screen is AppScreen.Profile -> {
                        ProfileScreen(
                            state = state,
                            onBack = viewModel::goBack,
                            onSave = viewModel::saveProfile,
                            onLogout = viewModel::logout
                        )
                    }

                    screen is AppScreen.Settings -> {
                        SettingsScreen(
                            state = state,
                            onBack = viewModel::goBack,
                            onSetLocale = viewModel::setLocale,
                            onSetTheme = viewModel::setTheme
                        )
                    }

                    screen is AppScreen.Chat -> {
                        ChatScreen(
                            state = state,
                            onSendText = viewModel::sendText,
                            onSendMedia = viewModel::sendMedia,
                            onDeleteMessage = viewModel::deleteMessage,
                            onDeleteChat = viewModel::deleteChat,
                            onUpdateChatIcon = viewModel::updateActiveChatIcon,
                            onOpenProfile = viewModel::openProfile,
                            onBackToChats = viewModel::closeChat
                        )
                    }

                    else -> {
                        HomeScreen(
                            state = state,
                            onOpenProfile = viewModel::openProfile,
                            onOpenSettings = viewModel::openSettings,
                            onCreateChat = viewModel::createChat,
                            onJoinChat = viewModel::joinChat,
                            onJoinNameChanged = viewModel::onJoinChatNameChanged,
                            onOpenChat = viewModel::openChat,
                            onDeleteChat = viewModel::deleteChat,
                            onMoveChat = viewModel::moveChat,
                            onShowQr = viewModel::generateProfileLinkQr,
                            unreadCount = viewModel::unreadCount
                        )
                    }
                }

                if (state.profileLinkQrText != null) {
                    ProfileQrDialog(
                        state = state,
                        qrText = state.profileLinkQrText,
                        onDismiss = viewModel::clearProfileLinkQr,
                        onNewQr = viewModel::generateProfileLinkQr
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingScreen(
    state: ChatUiState,
    onLinkAccount: () -> Unit,
    onCreateNew: () -> Unit
) {
    val busy = state.isCreatingProfile || state.isLinkingProfile

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text(state, "Connect account", "Подключение аккаунта"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text(state, "Scan QR from web client or create new profile", "Сканируйте QR из web-клиента или создайте новый профиль"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onLinkAccount,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isLinkingProfile) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(text(state, "Connect by QR", "Подключить по QR"))
                    }
                }

                Button(
                    onClick = onCreateNew,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isCreatingProfile) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(text(state, "Create new", "Создать новый"))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileQrDialog(
    state: ChatUiState,
    qrText: String,
    onDismiss: () -> Unit,
    onNewQr: () -> Unit
) {
    val qrBitmap = remember(qrText) { generateQrBitmap(qrText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text(state, "Show QR", "Показать QR")) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text(
                        state,
                        "Scan this QR to authorize on a new device",
                        "Отсканируй для авторизации на новом устройстве"
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (qrBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(8.dp)
                    )
                } else {
                    Text(
                        text(state, "Failed to generate QR", "Не удалось создать QR"),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onNewQr) {
                Text(text(state, "New QR", "Новый QR"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text(state, "Close", "Закрыть"))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: ChatUiState,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateChat: (String, String?, Uri?) -> Unit,
    onJoinChat: (String, String?) -> Unit,
    onJoinNameChanged: () -> Unit,
    onOpenChat: (ChatRoom) -> Unit,
    onDeleteChat: (ChatRoom) -> Unit,
    onMoveChat: (String, Int) -> Unit,
    onShowQr: () -> Unit,
    unreadCount: (String) -> Int
) {
    var showCreateChat by rememberSaveable { mutableStateOf(false) }
    var createName by rememberSaveable { mutableStateOf("") }
    var createPassword by rememberSaveable { mutableStateOf("") }
    var createIconUri by remember { mutableStateOf<Uri?>(null) }
    var draggedChatId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    var joinName by rememberSaveable { mutableStateOf("") }
    var joinPassword by rememberSaveable { mutableStateOf("") }

    val createReady = createName.trim().length >= 4
    val isPasswordStep = state.joinPasswordRequired && state.pendingJoinChatName == joinName.trim()
    val canJoin = joinName.trim().isNotEmpty() && (!isPasswordStep || joinPassword.isNotBlank()) && !state.isJoiningChat

    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        createIconUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { showCreateChat = !showCreateChat },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onShowQr,
                    modifier = Modifier.size(30.dp)
                ) {
                    if (state.isGeneratingProfileQr) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onOpenProfile,
                    modifier = Modifier.size(30.dp)
                ) {
                    Avatar(
                        avatarUrl = state.user?.avatarUrl,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape),
                        fallbackRes = R.drawable.ic_profile_placeholder
                    )
                }
            }
        }

        if (showCreateChat) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text(state, "Create chat", "Создать чат"), style = MaterialTheme.typography.titleMedium)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = createName,
                            onValueChange = { createName = it },
                            label = { Text(text(state, "Chat name (min 4)", "Название (мин. 4)")) },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { iconPicker.launch("image/*") },
                            modifier = Modifier.size(40.dp)
                        ) {
                            if (createIconUri != null) {
                                Avatar(
                                    avatarUrl = createIconUri,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape),
                                    fallbackRes = R.drawable.ic_profile_placeholder
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = text(state, "Select chat icon", "Выбрать иконку чата"),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = createPassword,
                        onValueChange = { createPassword = it },
                        label = { Text(text(state, "Password (optional)", "Пароль (опционально)")) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onCreateChat(createName.trim(), createPassword.ifBlank { null }, createIconUri)
                            createName = ""
                            createPassword = ""
                            createIconUri = null
                            showCreateChat = false
                        },
                        enabled = createReady,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text(state, "Create", "Создать"))
                    }
                }
            }
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = joinName,
                    onValueChange = {
                        joinName = it
                        joinPassword = ""
                        onJoinNameChanged()
                    },
                    label = { Text(text(state, "Chat name", "Название чата")) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (isPasswordStep) {
                    OutlinedTextField(
                        value = joinPassword,
                        onValueChange = { joinPassword = it },
                        label = { Text(text(state, "Chat password", "Пароль чата")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (joinName.isNotBlank() || joinPassword.isNotBlank()) {
                    Button(
                        onClick = {
                            val password = if (isPasswordStep) joinPassword.ifBlank { null } else null
                            onJoinChat(joinName.trim(), password)
                        },
                        enabled = canJoin,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isJoiningChat) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(text(state, "Connect", "Подключиться"))
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(14.dp)
                )
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(state.chats, key = { _, chat -> chat.id }) { index, chat ->
                    val unread = unreadCount(chat.id)
                    val canDelete = state.user != null && (chat.createdBy == state.user.id || state.user.isSuperAdmin) && !chat.isDeleted
                    val rowColor = when {
                        chat.isDeleted -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.24f)
                        unread > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        else -> Color.Transparent
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(chat.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedChatId = chat.id
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggedChatId = null
                                        dragOffsetY = 0f
                                    },
                                    onDragEnd = {
                                        draggedChatId = null
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (draggedChatId != chat.id) {
                                            return@detectDragGesturesAfterLongPress
                                        }
                                        dragOffsetY += dragAmount.y
                                        if (dragOffsetY >= 42f) {
                                            onMoveChat(chat.id, 1)
                                            dragOffsetY = 0f
                                        } else if (dragOffsetY <= -42f) {
                                            onMoveChat(chat.id, -1)
                                            dragOffsetY = 0f
                                        }
                                    }
                                )
                            }
                            .background(rowColor)
                            .clickable { onOpenChat(chat) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(
                            avatarUrl = chat.iconUrl,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape),
                            fallbackRes = R.drawable.ic_web_client_logo
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = chat.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = when {
                                    chat.isDeleted -> text(state, "Deleted chat", "Удаленный чат")
                                    chat.hasPassword -> text(state, "Private chat", "Закрытый чат")
                                    else -> text(state, "Public chat", "Открытый чат")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (unread > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    unread.toString(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Filled.DragHandle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )

                        if (canDelete) {
                            IconButton(onClick = { onDeleteChat(chat) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )
                }

                if (state.chats.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = text(state, "No chats yet", "Пока нет чатов"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    state: ChatUiState,
    onBack: () -> Unit,
    onSetLocale: (LocaleMode) -> Unit,
    onSetTheme: (ThemeMode) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text(state, "Settings", "Настройки")) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text(state, "Back", "Назад"))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text(state, "Language", "Язык"), fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onSetLocale(LocaleMode.EN) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("English")
                        }
                        OutlinedButton(
                            onClick = { onSetLocale(LocaleMode.RU) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Русский")
                        }
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text(state, "Theme", "Тема"), fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onSetTheme(ThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.LightMode, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text(state, "Light", "Светлая"))
                        }
                        OutlinedButton(
                            onClick = { onSetTheme(ThemeMode.DARK) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.DarkMode, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text(state, "Dark", "Темная"))
                        }
                    }
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreen(
    state: ChatUiState,
    onBack: () -> Unit,
    onSave: (String, Uri?) -> Unit,
    onLogout: () -> Unit
) {
    val user = state.user
    var nickname by rememberSaveable(user?.id) { mutableStateOf(user?.nickname.orEmpty()) }
    var pendingAvatarUri by remember(user?.id) { mutableStateOf<Uri?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pendingAvatarUri = uri
        }
    }

    LaunchedEffect(nickname, pendingAvatarUri, user?.id) {
        val current = user ?: return@LaunchedEffect
        val nextNickname = nickname.trim().ifBlank { current.nickname }
        val hasNicknameChange = nextNickname != current.nickname
        val hasAvatarChange = pendingAvatarUri != null
        if (!hasNicknameChange && !hasAvatarChange) {
            return@LaunchedEffect
        }

        delay(650)
        onSave(nextNickname, pendingAvatarUri)
        pendingAvatarUri = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text(state, "Profile", "Профиль")) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text(state, "Back", "Назад"))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .clip(CircleShape)
                        .clickable { avatarPicker.launch("image/*") }
                ) {
                    Avatar(
                        avatarUrl = pendingAvatarUri ?: user?.avatarUrl,
                        modifier = Modifier.fillMaxSize(),
                        fallbackRes = R.drawable.ic_profile_placeholder
                    )
                }
            }

            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text(text(state, "Nickname", "Псевдоним")) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = user?.id.orEmpty(),
                onValueChange = {},
                label = { Text("ID") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = if (state.isSavingProfile) {
                    text(state, "Saving profile...", "Сохранение профиля...")
                } else {
                    text(state, "Tap avatar to change", "Нажмите на аватар, чтобы изменить")
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onLogout,
                enabled = !state.isLoggingOut,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoggingOut) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(text(state, "Log out", "Выйти из аккаунта"))
                }
            }
        }
    }
}

private enum class CameraCaptureMode {
    PHOTO,
    VIDEO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(
    state: ChatUiState,
    onSendText: (String) -> Unit,
    onSendMedia: (Uri, MessageType?) -> Unit,
    onDeleteMessage: (ChatMessage) -> Unit,
    onDeleteChat: (ChatRoom) -> Unit,
    onUpdateChatIcon: (Uri?) -> Unit,
    onOpenProfile: () -> Unit,
    onBackToChats: () -> Unit
) {
    val chat = state.activeChat
    val user = state.user
    val context = LocalContext.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var textValue by rememberSaveable { mutableStateOf("") }
    var activeAudioMessageId by remember { mutableStateOf<String?>(null) }

    var isAudioRecording by remember { mutableStateOf(false) }
    var recordingStartedAt by remember { mutableLongStateOf(0L) }
    var recordingElapsedSec by remember { mutableIntStateOf(0) }
    var audioRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioOutputFile by remember { mutableStateOf<File?>(null) }
    var micButtonSize by remember { mutableStateOf(IntSize.Zero) }
    var isMicTouchActive by remember { mutableStateOf(false) }
    var isMicCancelMode by remember { mutableStateOf(false) }
    var audioLevelSmoothed by remember { mutableFloatStateOf(0f) }
    val recordingWaveSamples = remember { mutableStateListOf<Float>() }

    var showCameraChooser by remember { mutableStateOf(false) }
    var pendingCameraMode by remember { mutableStateOf<CameraCaptureMode?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingVideoUri by remember { mutableStateOf<Uri?>(null) }
    var previewImageMessage by remember { mutableStateOf<ChatMessage?>(null) }
    val destroyingProgress = remember { mutableStateMapOf<String, Float>() }
    val maxAudioRecordSec = 1_000

    val finishAudioRecording = { shouldSend: Boolean ->
        val recorder = audioRecorder
        val file = audioOutputFile

        runCatching { recorder?.stop() }
        recorder?.release()

        audioRecorder = null
        isAudioRecording = false
        audioOutputFile = null
        isMicCancelMode = false
        audioLevelSmoothed = 0f
        recordingWaveSamples.clear()

        if (shouldSend && file != null && file.exists() && file.length() > 0L) {
            onSendMedia(Uri.fromFile(file), MessageType.AUDIO)
        } else {
            runCatching {
                if (file != null && file.exists()) {
                    file.delete()
                }
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onSendMedia(it, null) }
    }

    val chatIconPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            onUpdateChatIcon(uri)
        }
    }

    val photoCaptureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            pendingPhotoUri?.let { onSendMedia(it, null) }
        }
        pendingPhotoUri = null
    }

    val videoCaptureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) {
            pendingVideoUri?.let { onSendMedia(it, MessageType.VIDEO) }
        }
        pendingVideoUri = null
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            // Hold-to-record mode: user starts recording by pressing mic again.
            // Avoid auto-start after permission dialog.
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            when (pendingCameraMode) {
                CameraCaptureMode.PHOTO -> {
                    val imageUri = createImageUri(context)
                    pendingPhotoUri = imageUri
                    photoCaptureLauncher.launch(imageUri)
                }

                CameraCaptureMode.VIDEO -> {
                    val videoUri = createVideoUri(context)
                    pendingVideoUri = videoUri
                    videoCaptureLauncher.launch(videoUri)
                }

                null -> Unit
            }
        }
        pendingCameraMode = null
    }

    LaunchedEffect(isAudioRecording, recordingStartedAt) {
        while (isAudioRecording) {
            recordingElapsedSec = ((System.currentTimeMillis() - recordingStartedAt) / 1000L).toInt()
            val rawAmplitude = runCatching { audioRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
            val normalized = normalizeRecorderAmplitude(rawAmplitude)
            audioLevelSmoothed = (audioLevelSmoothed * 0.72f + normalized * 0.28f).coerceIn(0f, 1f)
            appendWaveSample(recordingWaveSamples, audioLevelSmoothed)
            if (recordingElapsedSec >= maxAudioRecordSec) {
                finishAudioRecording(true)
                break
            }
            delay(110)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isAudioRecording || audioRecorder != null || audioOutputFile != null) {
                finishAudioRecording(false)
            }
        }
    }

    LaunchedEffect(state.messages.size, chat?.id) {
        if (state.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(state.messages.lastIndex)
            }
        }
    }

    LaunchedEffect(state.messages) {
        val activeIds = state.messages.mapNotNull { it.id.takeIf { id -> id.isNotBlank() } }.toHashSet()
        val stale = destroyingProgress.keys.filterNot { activeIds.contains(it) }
        stale.forEach { destroyingProgress.remove(it) }
    }

    val canManageChat = user != null && chat != null && (chat.createdBy == user.id || user.isSuperAdmin)

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToChats,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = text(state, "Back to chats", "Назад к чатам"),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (chat != null) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable(enabled = canManageChat && !state.isUpdatingChatIcon) {
                                    chatIconPicker.launch("image/*")
                                }
                        ) {
                            Avatar(
                                avatarUrl = chat.iconUrl,
                                modifier = Modifier.fillMaxSize(),
                                fallbackRes = R.drawable.ic_web_client_logo
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(chat.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else {
                        Text(text(state, "Chat", "Чат"))
                    }
                }

                if (chat != null && canManageChat && !chat.isDeleted) {
                    IconButton(
                        onClick = { onDeleteChat(chat) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onOpenProfile,
                    modifier = Modifier.size(30.dp)
                ) {
                    Avatar(
                        avatarUrl = user?.avatarUrl,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape),
                        fallbackRes = R.drawable.ic_profile_placeholder
                    )
                }
            }
        }

        if (state.isLoading && state.messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            SecretChatPatternBackground(
                modifier = Modifier.fillMaxSize(),
                dark = state.theme == ThemeMode.DARK
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(state.messages, key = { _, message -> message.id }) { index, message ->
                    val messageId = message.id
                    val currentDateKey = messageDateKey(message.createdAt)
                    val previousDateKey = state.messages.getOrNull(index - 1)?.let { previous ->
                        messageDateKey(previous.createdAt)
                    }
                    val shouldShowDateDivider = index == 0 || currentDateKey != previousDateKey

                    if (shouldShowDateDivider) {
                        DateDivider(
                            label = formatMessageDateLabel(state, message.createdAt)
                        )
                    }

                    MessageBubble(
                        state = state,
                        message = message,
                        isOwn = message.userId == user?.id,
                        canDelete = user != null && !message.isDeleted && (message.userId == user.id || user.isSuperAdmin),
                        onDelete = {
                            val currentUser = user
                            val canAnimateDestroy = currentUser != null &&
                                !currentUser.isSuperAdmin &&
                                message.userId == currentUser.id &&
                                !message.isDeleted &&
                                messageId.isNotBlank()

                            if (!canAnimateDestroy) {
                                onDeleteMessage(message)
                            } else if (!destroyingProgress.containsKey(messageId)) {
                                coroutineScope.launch {
                                    val anim = Animatable(0f)
                                    destroyingProgress[messageId] = 0f
                                    anim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 520, easing = LinearEasing)
                                    ) {
                                        destroyingProgress[messageId] = value
                                    }
                                    onDeleteMessage(message)
                                    delay(80)
                                    destroyingProgress.remove(messageId)
                                }
                            }
                        },
                        destructionProgress = destroyingProgress[messageId] ?: 0f,
                        onOpenImagePreview = { previewImageMessage = it },
                        activeAudioMessageId = activeAudioMessageId,
                        onAudioPlayRequest = { requestedId ->
                            activeAudioMessageId = if (activeAudioMessageId == requestedId) null else requestedId
                        }
                    )
                }

                if (state.messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = text(state, "No messages yet", "Пока нет сообщений"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        val hasDraft = textValue.trim().isNotEmpty()

        val imeBottomPx = WindowInsets.ime.getBottom(density)
        val navBottomPx = WindowInsets.navigationBars.getBottom(density)
        val keyboardLiftBottom = with(density) { (imeBottomPx - navBottomPx).coerceAtLeast(0).toDp() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = keyboardLiftBottom)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { filePicker.launch("*/*") },
                        enabled = chat != null && !chat.isDeleted,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = {
                            showCameraChooser = true
                        },
                        enabled = chat != null && !chat.isDeleted,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 0.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (textValue.isBlank()) {
                            Text(
                                text(state, "Type a message", "Введите сообщение"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            maxLines = 5
                        )
                    }

                    if (hasDraft) {
                        FilledIconButton(
                            onClick = {
                                onSendText(textValue)
                                textValue = ""
                            },
                            enabled = !state.isSendingMessage && chat != null && !chat.isDeleted,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        val micEnabled = chat != null && !chat.isDeleted
                        IconButton(
                            onClick = {},
                            enabled = micEnabled,
                            modifier = Modifier
                                .size(42.dp)
                                .onSizeChanged { micButtonSize = it }
                                .pointerInteropFilter { event ->
                                    if (!micEnabled) {
                                        return@pointerInteropFilter false
                                    }

                                    fun isInsideCurrentButton(): Boolean {
                                        if (micButtonSize.width <= 0 || micButtonSize.height <= 0) {
                                            return true
                                        }
                                        return event.x >= 0f &&
                                            event.y >= 0f &&
                                            event.x <= micButtonSize.width.toFloat() &&
                                            event.y <= micButtonSize.height.toFloat()
                                    }

                                    when (event.actionMasked) {
                                        MotionEvent.ACTION_DOWN -> {
                                            if (state.isSendingMessage || isAudioRecording) {
                                                return@pointerInteropFilter true
                                            }
                                            val granted = hasPermission(context, Manifest.permission.RECORD_AUDIO)
                                            if (!granted) {
                                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                return@pointerInteropFilter true
                                            }

                                            val (recorder, file) = startAudioRecording(context)
                                            audioRecorder = recorder
                                            audioOutputFile = file
                                            if (recorder == null) {
                                                return@pointerInteropFilter true
                                            }

                                            isAudioRecording = true
                                            isMicTouchActive = true
                                            isMicCancelMode = false
                                            audioLevelSmoothed = 0f
                                            recordingWaveSamples.clear()
                                            repeat(20) { recordingWaveSamples.add(0f) }
                                            recordingStartedAt = System.currentTimeMillis()
                                            recordingElapsedSec = 0
                                            true
                                        }

                                        MotionEvent.ACTION_MOVE -> {
                                            if (isMicTouchActive) {
                                                isMicCancelMode = !isInsideCurrentButton()
                                            }
                                            isMicTouchActive
                                        }

                                        MotionEvent.ACTION_UP -> {
                                            if (isMicTouchActive) {
                                                val shouldSend = isInsideCurrentButton()
                                                if (isAudioRecording || audioRecorder != null || audioOutputFile != null) {
                                                    finishAudioRecording(shouldSend)
                                                }
                                                isMicTouchActive = false
                                                isMicCancelMode = false
                                                true
                                            } else {
                                                false
                                            }
                                        }

                                        MotionEvent.ACTION_CANCEL -> {
                                            if (isMicTouchActive) {
                                                if (isAudioRecording || audioRecorder != null || audioOutputFile != null) {
                                                    finishAudioRecording(false)
                                                }
                                                isMicTouchActive = false
                                                isMicCancelMode = false
                                            }
                                            true
                                        }

                                        else -> false
                                    }
                                }
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            if (chat?.isDeleted == true) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text(state, "Chat is deleted. Read only", "Чат удален. Только просмотр"))
                }
            } else if (state.isSendingMessage) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text(state, "Sending...", "Отправка..."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (isAudioRecording) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000)),
            contentAlignment = Alignment.Center
        ) {
            Card {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text(state, "Recording", "Запись"), fontWeight = FontWeight.Bold)
                    Text(formatDuration(recordingElapsedSec), style = MaterialTheme.typography.headlineMedium)
                    RecordingLiveEqualizer(
                        samples = recordingWaveSamples,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    )
                    Text(
                        text = if (isMicCancelMode) {
                            text(state, "Cancel sending", "Отмена отправки")
                        } else {
                            text(state, "Release to send", "Отпустите для отправки")
                        },
                        color = if (isMicCancelMode) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }

    if (showCameraChooser) {
        AlertDialog(
            onDismissRequest = { showCameraChooser = false },
            title = { Text(text(state, "Camera", "Камера")) },
            text = { Text(text(state, "Choose action", "Выберите действие")) },
            confirmButton = {
                TextButton(onClick = {
                    showCameraChooser = false
                    val mode = CameraCaptureMode.VIDEO
                    val granted = hasPermission(context, Manifest.permission.CAMERA)
                    if (granted) {
                        val videoUri = createVideoUri(context)
                        pendingVideoUri = videoUri
                        videoCaptureLauncher.launch(videoUri)
                    } else {
                        pendingCameraMode = mode
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Text(text(state, "Video", "Видео"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCameraChooser = false
                    val mode = CameraCaptureMode.PHOTO
                    val granted = hasPermission(context, Manifest.permission.CAMERA)
                    if (granted) {
                        val imageUri = createImageUri(context)
                        pendingPhotoUri = imageUri
                        photoCaptureLauncher.launch(imageUri)
                    } else {
                        pendingCameraMode = mode
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Text(text(state, "Photo", "Фото"))
                }
            }
        )
    }

    previewImageMessage?.let { message ->
        ImagePreviewDialog(
            state = state,
            message = message,
            onClose = { previewImageMessage = null }
        )
    }
}

@Composable
private fun DateDivider(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ConnectionIndicator(state: ChatUiState) {
    val (label, color) = when (state.connectionState) {
        ConnectionState.CONNECTED -> text(state, "Connected", "Подключено") to MaterialTheme.colorScheme.primary
        ConnectionState.RECONNECTING -> text(state, "Reconnecting", "Переподключение") to Color(0xFFF9AB00)
        ConnectionState.DISCONNECTED -> text(state, "Disconnected", "Нет соединения") to Color(0xFFD93025)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ImagePreviewDialog(
    state: ChatUiState,
    message: ChatMessage,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val imageUri = remember(message.fileUrl) { resolveRemoteUri(message.fileUrl) }
    var scale by remember(message.id) { mutableStateOf(1f) }
    var offset by remember(message.id) { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
        ) {
            AsyncImage(
                model = imageUri ?: message.fileUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(message.id) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nextScale = (scale * zoom).coerceIn(1f, 6f)
                            scale = nextScale
                            offset = if (nextScale <= 1.02f) {
                                Offset.Zero
                            } else {
                                offset + pan
                            }
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                contentScale = ContentScale.Fit
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        startDirectFileDownload(
                            context = context,
                            rawUrl = message.fileUrl,
                            suggestedFileName = message.fileName ?: "image"
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = text(state, "Download", "Скачать"),
                        tint = Color.White
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = text(state, "Close", "Закрыть"),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SecretChatPatternBackground(
    modifier: Modifier = Modifier,
    dark: Boolean
) {
    val backgroundColor = if (dark) Color(0xFF0B1119) else Color(0xFFF7FAFD)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    )
}

private fun DrawScope.drawDarkThemePattern(
    symbolColor: Color,
    accentColor: Color
) {
    val tile = 90.dp.toPx()
    val radius = tile * 0.17f
    val stroke = 2.15.dp.toPx()
    val rows = (size.height / tile).toInt() + 2
    val cols = (size.width / tile).toInt() + 2

    for (row in 0..rows) {
        val y = row * tile + tile * 0.44f
        val xOffset = if (row % 2 == 0) tile * 0.22f else tile * 0.72f
        for (col in 0..cols) {
            val x = col * tile + xOffset
            val center = Offset(x, y)
            when ((row + col) % 5) {
                0 -> drawMagnifierSymbol(center, radius, symbolColor, stroke)
                1 -> drawHatSymbol(center, radius, symbolColor, stroke)
                2 -> drawEyeSymbol(center, radius, accentColor, stroke)
                3 -> drawCaseSymbol(center, radius, symbolColor, stroke)
                else -> drawFingerprintSymbol(center, radius, accentColor, stroke)
            }

            if ((row + col) % 4 == 0) {
                drawCircle(
                    color = accentColor.copy(alpha = 0.42f),
                    radius = radius * 0.12f,
                    center = Offset(center.x + radius * 1.35f, center.y - radius * 1.18f)
                )
            }
        }
    }
}

private fun DrawScope.drawLightThemePattern(
    symbolColor: Color,
    accentColor: Color
) {
    val tile = 116.dp.toPx()
    val radius = tile * 0.15f
    val stroke = 1.8.dp.toPx()
    val rows = (size.height / tile).toInt() + 2
    val cols = (size.width / tile).toInt() + 2

    for (row in 0..rows) {
        val y = row * tile + tile * 0.48f
        val xOffset = if (row % 2 == 0) tile * 0.2f else tile * 0.66f
        for (col in 0..cols) {
            val x = col * tile + xOffset
            val center = Offset(x, y)

            when ((row * 3 + col) % 4) {
                0 -> drawMessageSymbol(center, radius, symbolColor, stroke)
                1 -> drawLockSymbol(center, radius, accentColor, stroke)
                2 -> drawEnvelopeSymbol(center, radius, symbolColor, stroke)
                else -> drawSparkSymbol(center, radius, accentColor, stroke)
            }
        }
    }
}

private fun DrawScope.drawMessageSymbol(
    center: Offset,
    radius: Float,
    color: Color,
    stroke: Float
) {
    val width = radius * 2.45f
    val height = radius * 1.62f
    val left = center.x - width / 2f
    val top = center.y - height / 2f

    drawRoundRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.3f),
        style = Stroke(width = stroke)
    )

    val tailPath = Path().apply {
        moveTo(left + width * 0.28f, top + height)
        lineTo(left + width * 0.38f, top + height + radius * 0.34f)
        lineTo(left + width * 0.52f, top + height)
        close()
    }
    drawPath(tailPath, color = color, style = Stroke(width = stroke))
}

private fun DrawScope.drawLockSymbol(
    center: Offset,
    radius: Float,
    color: Color,
    stroke: Float
) {
    val bodyWidth = radius * 1.72f
    val bodyHeight = radius * 1.28f
    val bodyTop = center.y - bodyHeight * 0.15f
    val bodyLeft = center.x - bodyWidth / 2f

    drawRoundRect(
        color = color,
        topLeft = Offset(bodyLeft, bodyTop),
        size = Size(bodyWidth, bodyHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.23f),
        style = Stroke(width = stroke)
    )
    drawArc(
        color = color,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.86f, center.y - radius * 1.18f),
        size = Size(radius * 1.72f, radius * 1.55f),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
    drawCircle(color = color, radius = radius * 0.14f, center = Offset(center.x, center.y + radius * 0.26f))
}

private fun DrawScope.drawEnvelopeSymbol(
    center: Offset,
    radius: Float,
    color: Color,
    stroke: Float
) {
    val width = radius * 2.45f
    val height = radius * 1.56f
    val left = center.x - width / 2f
    val top = center.y - height / 2f

    drawRoundRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.22f),
        style = Stroke(width = stroke)
    )
    drawLine(
        color = color,
        start = Offset(left + stroke, top + stroke),
        end = Offset(center.x, top + height * 0.54f),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(left + width - stroke, top + stroke),
        end = Offset(center.x, top + height * 0.54f),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawSparkSymbol(
    center: Offset,
    radius: Float,
    color: Color,
    stroke: Float
) {
    drawLine(
        color = color,
        start = Offset(center.x, center.y - radius * 0.92f),
        end = Offset(center.x, center.y + radius * 0.92f),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(center.x - radius * 0.92f, center.y),
        end = Offset(center.x + radius * 0.92f, center.y),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
    drawCircle(color = color, radius = radius * 0.16f, center = center)
}

private fun DrawScope.drawMagnifierSymbol(
    center: Offset,
    radius: Float,
    color: Color,
    stroke: Float
) {
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(width = stroke)
    )
    drawLine(
        color = color,
        start = Offset(center.x + radius * 0.62f, center.y + radius * 0.62f),
        end = Offset(center.x + radius * 1.35f, center.y + radius * 1.35f),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawHatSymbol(
    center: Offset,
    radius: Float,
    color: Color,
    stroke: Float
) {
    drawArc(
        color = color,
        startAngle = 198f,
        sweepAngle = 144f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 1.15f, center.y - radius * 1.15f),
        size = Size(radius * 2.3f, radius * 2.1f),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
    drawLine(
        color = color,
        start = Offset(center.x - radius * 1.55f, center.y + radius * 0.3f),
        end = Offset(center.x + radius * 1.55f, center.y + radius * 0.3f),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawEyeSymbol(
    center: Offset,
    radius: Float,
    color: Color,
    stroke: Float
) {
    val width = radius * 2.6f
    val height = radius * 1.35f
    drawArc(
        color = color,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - width / 2f, center.y - height / 2f),
        size = Size(width, height),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - width / 2f, center.y - height / 2f),
        size = Size(width, height),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
    drawCircle(
        color = color,
        radius = radius * 0.28f,
        center = center
    )
}

private fun DrawScope.drawCaseSymbol(
    center: Offset,
    radius: Float,
    color: Color,
    stroke: Float
) {
    val width = radius * 2.2f
    val height = radius * 1.6f
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - width / 2f, center.y - height / 2f),
        size = Size(width, height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.24f),
        style = Stroke(width = stroke)
    )
    drawLine(
        color = color,
        start = Offset(center.x - width * 0.25f, center.y - height * 0.15f),
        end = Offset(center.x + width * 0.25f, center.y - height * 0.15f),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawFingerprintSymbol(
    center: Offset,
    radius: Float,
    color: Color,
    stroke: Float
) {
    drawArc(
        color = color,
        startAngle = 190f,
        sweepAngle = 160f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 1.25f, center.y - radius * 1.12f),
        size = Size(radius * 2.5f, radius * 2.2f),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
    drawArc(
        color = color,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.95f, center.y - radius * 0.86f),
        size = Size(radius * 1.9f, radius * 1.72f),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
    drawArc(
        color = color,
        startAngle = 210f,
        sweepAngle = 118f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.66f, center.y - radius * 0.62f),
        size = Size(radius * 1.32f, radius * 1.24f),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
}

@Composable
private fun MessageBubble(
    state: ChatUiState,
    message: ChatMessage,
    isOwn: Boolean,
    canDelete: Boolean,
    onDelete: () -> Unit,
    destructionProgress: Float,
    onOpenImagePreview: (ChatMessage) -> Unit,
    activeAudioMessageId: String?,
    onAudioPlayRequest: (String) -> Unit
) {
    val context = LocalContext.current
    val isAudio = isAudioMessage(message)
    val renderRawAudio = isAudio && !message.isDeleted
    val bubbleShape = RoundedCornerShape(14.dp)
    val bubbleColor = when {
        message.isDeleted -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f)
        isOwn -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val bubbleBorderColor = if (message.isDeleted) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.86f)
    } else {
        Color.Transparent
    }
    val includeMetaHeader = !isAudio
    val destroyProgress = destructionProgress.coerceIn(0f, 1f)
    val isDestroying = destroyProgress > 0f
    val contentAlpha = (1f - destroyProgress * 1.7f).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = contentAlpha },
            horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .border(width = 1.dp, color = bubbleBorderColor, shape = bubbleShape)
                    .clip(bubbleShape)
                    .background(if (renderRawAudio) Color.Transparent else bubbleColor)
                    .padding(if (renderRawAudio) 0.dp else 10.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (includeMetaHeader) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Avatar(
                                    avatarUrl = message.senderAvatarUrl,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape),
                                    fallbackRes = R.drawable.ic_profile_placeholder
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = message.senderNickname,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = userIdTail(message.userId),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                if (canDelete) {
                                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        when {
                            isAudio -> {
                                AudioMessageCard(
                                    message = message,
                                    isExternallyActive = activeAudioMessageId == message.id,
                                    onPlayRequest = { onAudioPlayRequest(message.id) }
                                )
                            }

                            isVideoMessage(message) -> {
                                VideoMessageCard(
                                    message = message
                                )
                            }

                            isImageAttachment(message) -> {
                                val chatImageModel = remember(message.fileUrl) {
                                    resolveRemoteUri(message.fileUrl) ?: message.fileUrl
                                }
                                AsyncImage(
                                    model = chatImageModel,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpenImagePreview(message) },
                                    contentScale = ContentScale.Fit,
                                    error = painterResource(id = R.drawable.ic_profile_placeholder),
                                    fallback = painterResource(id = R.drawable.ic_profile_placeholder)
                                )
                            }

                            hasFilePayload(message) -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            startDirectFileDownload(
                                                context = context,
                                                rawUrl = message.fileUrl,
                                                suggestedFileName = message.fileName
                                            )
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.AttachFile, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = message.fileName ?: text(state, "Open", "Открыть"),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            else -> {
                                Text(message.text.orEmpty())
                            }
                        }

                        if (isAudioMessage(message) && !message.isDeleted) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val tail = userIdTail(message.userId)
                                if (tail.isNotEmpty()) {
                                    Text(
                                        text = tail,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = formatTime(message.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (canDelete) {
                                    IconButton(onClick = onDelete, modifier = Modifier.size(22.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        } else if (!isVideoMessage(message)) {
                            Text(
                                text = formatTime(message.createdAt),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (isDestroying) {
            PixelDisintegrationOverlay(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .fillMaxHeight()
                    .align(if (isOwn) Alignment.CenterEnd else Alignment.CenterStart),
                progress = destroyProgress,
                baseColor = if (renderRawAudio) MaterialTheme.colorScheme.surfaceVariant else bubbleColor,
                fromRight = isOwn
            )
        }
    }
}

@Composable
private fun PixelDisintegrationOverlay(
    modifier: Modifier = Modifier,
    progress: Float,
    baseColor: Color,
    fromRight: Boolean
) {
    val safeProgress = progress.coerceIn(0f, 1f)
    Canvas(modifier = modifier) {
        val cell = 4.dp.toPx()
        val cols = (size.width / cell).toInt().coerceAtLeast(6)
        val rows = (size.height / cell).toInt().coerceAtLeast(4)

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val idx = row * cols + col
                val seed = ((idx * 1103515245L + 12345L) and 0x7fffffffL)
                val normalized = (seed % 10_000L).toFloat() / 10_000f
                if (normalized > safeProgress) {
                    continue
                }

                val angle = ((idx * 37) % 360) * (PI / 180.0)
                val drift = 12f + normalized * 52f
                val spread = safeProgress * drift
                val directionX = if (fromRight) 1f else -1f
                val dx = (cos(angle).toFloat() * spread) + directionX * safeProgress * 26f
                val dy = sin(angle).toFloat() * spread + safeProgress * 14f
                val particleAlpha = ((1f - safeProgress) * (0.45f + (1f - normalized) * 0.55f)).coerceIn(0f, 1f)

                drawRect(
                    color = baseColor.copy(alpha = particleAlpha),
                    topLeft = Offset(col * cell + dx, row * cell + dy),
                    size = Size(cell, cell)
                )
            }
        }
    }
}

@Composable
private fun AudioMessageCard(
    message: ChatMessage,
    isExternallyActive: Boolean,
    onPlayRequest: () -> Unit
) {
    val context = LocalContext.current
    val sourceUri = remember(message.fileUrl) { resolveRemoteUri(message.fileUrl) }
    var mediaPlayer by remember(message.id) { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember(message.id) { mutableStateOf(false) }
    var isPreparing by remember(message.id) { mutableStateOf(false) }
    var durationMs by remember(message.id) { mutableIntStateOf(0) }
    var positionMs by remember(message.id) { mutableIntStateOf(0) }
    var speedIndex by remember(message.id) { mutableIntStateOf(0) }
    val waveformBars = remember {
        listOf(6, 10, 14, 11, 8, 16, 12, 9, 17, 13, 8, 15, 11, 7, 14, 10, 6, 12, 18, 11, 8, 16, 12, 7, 14, 9, 6, 13, 17, 11, 8, 14, 10, 7)
    }

    val speeds = remember { listOf(1.0f, 1.5f, 2.0f) }
    val speedLabel = remember(speedIndex) {
        when (speeds[speedIndex]) {
            1.0f -> "1x"
            1.5f -> "1.5x"
            2.0f -> "2x"
            else -> "${speeds[speedIndex]}x"
        }
    }
    val progress = remember(positionMs, durationMs) {
        if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }
    val activeBars = remember(progress, waveformBars.size) {
        if (progress <= 0f) 0 else (progress * waveformBars.size).toInt().coerceIn(0, waveformBars.size)
    }

    fun applySpeed(player: MediaPlayer) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        runCatching {
            player.playbackParams = player.playbackParams.setSpeed(speeds[speedIndex])
        }
    }

    fun releasePlayer() {
        runCatching {
            mediaPlayer?.stop()
        }
        runCatching {
            mediaPlayer?.release()
        }
        mediaPlayer = null
        isPreparing = false
        isPlaying = false
    }

    LaunchedEffect(isPlaying, isExternallyActive, message.id) {
        if (!isExternallyActive && isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
        }

        while (isPlaying) {
            val player = mediaPlayer
            if (player == null) {
                isPlaying = false
                break
            }
            positionMs = runCatching { player.currentPosition }.getOrDefault(positionMs)
            durationMs = runCatching { player.duration }.getOrDefault(durationMs).coerceAtLeast(1)
            delay(180)
        }
    }

    LaunchedEffect(sourceUri, message.id) {
        if (durationMs > 0) {
            return@LaunchedEffect
        }
        val source = sourceUri ?: return@LaunchedEffect
        val resolved = readMediaDurationMs(context, source) ?: return@LaunchedEffect
        durationMs = resolved
    }

    DisposableEffect(message.id) {
        onDispose {
            releasePlayer()
        }
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val source = sourceUri ?: return@IconButton
                    onPlayRequest()
                    val existing = mediaPlayer

                    if (existing == null) {
                        val player = MediaPlayer()
                        isPreparing = true
                        runCatching {
                            player.setDataSource(context, source)
                            player.setOnPreparedListener { prepared ->
                                durationMs = prepared.duration.coerceAtLeast(1)
                                isPreparing = false
                                applySpeed(prepared)
                                prepared.start()
                                isPlaying = true
                            }
                            player.setOnCompletionListener { completed ->
                                isPlaying = false
                                positionMs = 0
                                runCatching { completed.seekTo(0) }
                            }
                            player.setOnErrorListener { _, _, _ ->
                                releasePlayer()
                                true
                            }
                            player.prepareAsync()
                            mediaPlayer = player
                        }.onFailure {
                            runCatching { player.release() }
                            mediaPlayer = null
                            isPreparing = false
                            isPlaying = false
                        }
                        return@IconButton
                    }

                    if (isPreparing) {
                        return@IconButton
                    }

                    if (isPlaying) {
                        existing.pause()
                        isPlaying = false
                    } else {
                        applySpeed(existing)
                        existing.start()
                        isPlaying = true
                    }
                },
                modifier = Modifier.size(34.dp)
            ) {
                if (isPreparing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 3.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        waveformBars.forEachIndexed { index, height ->
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(height.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (index < activeBars) {
                                            Color(0xFF66C3FF)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                        }
                                    )
                            )
                        }
                    }

                    Slider(
                        value = positionMs.toFloat().coerceIn(0f, durationMs.toFloat()),
                        onValueChange = { positionMs = it.toInt() },
                        valueRange = 0f..durationMs.toFloat(),
                        modifier = Modifier.fillMaxWidth(),
                        onValueChangeFinished = {
                            mediaPlayer?.seekTo(positionMs.coerceIn(0, durationMs))
                        },
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = Color.Transparent,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDuration(positionMs / 1000),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )

                    TextButton(
                        onClick = {
                            speedIndex = (speedIndex + 1) % speeds.size
                            mediaPlayer?.let { applySpeed(it) }
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = speedLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = when {
                            isPreparing -> "--:--"
                            durationMs > 0 -> formatDuration(durationMs / 1000)
                            else -> "--:--"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Avatar(
                avatarUrl = message.senderAvatarUrl,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape),
                fallbackRes = R.drawable.ic_profile_placeholder
            )
        }
    }
}

@Composable
private fun VideoMessageCard(
    message: ChatMessage
) {
    val source = remember(message.fileUrl) { resolveRemoteUri(message.fileUrl) }
    var videoView by remember(message.id) { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember(message.id) { mutableStateOf(false) }

    DisposableEffect(message.id) {
        onDispose {
            runCatching { videoView?.stopPlayback() }
            videoView = null
        }
    }

    Column {
        if (source != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.08f))
            ) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(source)
                            setOnPreparedListener { player ->
                                player.isLooping = false
                                runCatching { seekTo(1) }
                                pause()
                                isPlaying = false
                            }
                            setOnCompletionListener {
                                isPlaying = false
                                runCatching { seekTo(1) }
                            }
                        }.also { created ->
                            videoView = created
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { updated ->
                        videoView = updated
                    }
                )

                FilledIconButton(
                    onClick = {
                        val player = videoView ?: return@FilledIconButton
                        if (isPlaying) {
                            runCatching { player.pause() }
                            isPlaying = false
                        } else {
                            runCatching { player.start() }
                            isPlaying = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(46.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                }
            }
        }

        Text(
            text = formatTime(message.createdAt),
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UserHeader(
    user: UserProfile?,
    state: ChatUiState
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                avatarUrl = user?.avatarUrl,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
                fallbackRes = R.drawable.ic_profile_placeholder
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user?.nickname ?: "...", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "ID: ${user?.id ?: "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = when (state.connectionState) {
                    ConnectionState.CONNECTED -> text(state, "online", "online")
                    ConnectionState.RECONNECTING -> text(state, "sync", "sync")
                    ConnectionState.DISCONNECTED -> text(state, "offline", "offline")
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun Avatar(
    avatarUrl: Any?,
    modifier: Modifier = Modifier,
    fallbackRes: Int
) {
    val uri = when (avatarUrl) {
        null -> null
        is Uri -> avatarUrl
        is String -> resolveRemoteUri(avatarUrl)
        else -> null
    }

    if (uri == null) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = fallbackRes),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
        return
    }

    AsyncImage(
        model = uri,
        contentDescription = null,
        placeholder = painterResource(id = fallbackRes),
        error = painterResource(id = fallbackRes),
        fallback = painterResource(id = fallbackRes),
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

private fun resolveRemoteUri(raw: String?): Uri? {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) {
        return null
    }

    val lower = value.lowercase(Locale.ROOT)
    if (lower == "null" || lower == "undefined") {
        return null
    }

    if (lower.startsWith("content://") || lower.startsWith("file://") || lower.startsWith("data:")) {
        return Uri.parse(value)
    }

    val apiBase = Uri.parse(NetworkConfig.BASE_API_URL)
    val backendScheme = apiBase.scheme ?: "https"
    val backendHost = apiBase.host.orEmpty()
    val backendPort = if (apiBase.port > 0) ":${apiBase.port}" else ""
    val backendOrigin = "$backendScheme://$backendHost$backendPort"

    if (lower.startsWith("http://") || lower.startsWith("https://")) {
        val parsed = Uri.parse(value)
        val parsedHost = parsed.host.orEmpty()
        val normalizedPath = parsed.encodedPath.orEmpty()
        val shouldForceBackendOrigin =
            parsedHost.equals(backendHost, ignoreCase = true) || normalizedPath.startsWith("/uploads/")
        if (shouldForceBackendOrigin) {
            val rebuiltPath = if (normalizedPath.startsWith("/")) normalizedPath else "/$normalizedPath"
            val queryPart = parsed.encodedQuery?.let { "?$it" }.orEmpty()
            val fragmentPart = parsed.encodedFragment?.let { "#$it" }.orEmpty()
            return Uri.parse("$backendOrigin$rebuiltPath$queryPart$fragmentPart")
        }
        if (parsed.scheme == "http" && backendScheme == "https") {
            return parsed.buildUpon().scheme("https").build()
        }
        return parsed
    }

    if (value.startsWith("//")) {
        return Uri.parse("$backendScheme:$value")
    }

    if (value.startsWith("/")) {
        return Uri.parse("$backendOrigin$value")
    }

    val normalized = value.removePrefix("./")
    return Uri.parse("$backendOrigin/$normalized")
}

private suspend fun readMediaDurationMs(context: Context, uri: Uri): Int? = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
        val scheme = uri.scheme?.lowercase(Locale.ROOT).orEmpty()
        if (scheme == "content" || scheme == "file") {
            retriever.setDataSource(context, uri)
        } else {
            retriever.setDataSource(uri.toString())
        }
        retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
    } catch (_: Throwable) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private fun normalizeRecorderAmplitude(rawAmplitude: Int): Float {
    if (rawAmplitude <= 0) {
        return 0f
    }
    val normalized = ln(rawAmplitude.toFloat() + 1f) / ln(32768f)
    return normalized.coerceIn(0f, 1f)
}

private fun appendWaveSample(samples: MutableList<Float>, value: Float, maxSize: Int = 64) {
    samples.add(value.coerceIn(0f, 1f))
    while (samples.size > maxSize) {
        samples.removeAt(0)
    }
}

@Composable
private fun RecordingLiveEqualizer(
    samples: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) {
            return@Canvas
        }

        val centerY = height * 0.5f
        val points = maxOf(samples.size, 42)

        fun drawWave(
            color: Color,
            amplitudeFactor: Float,
            frequency: Float,
            phase: Float,
            strokeWidth: Float,
            alpha: Float
        ) {
            val path = Path()
            for (index in 0 until points) {
                val t = if (points <= 1) 0f else index.toFloat() / (points - 1).toFloat()
                val sampleIndex = ((samples.size - 1) * t).toInt().coerceAtLeast(0)
                val sample = samples.getOrElse(sampleIndex) { 0f }
                val envelope = sample * amplitudeFactor
                val wave = sin((t * frequency * (2f * PI).toFloat()) + phase)
                val x = t * width
                val y = centerY - (wave * envelope * (height * 0.42f))
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            drawPath(
                path = path,
                color = color.copy(alpha = alpha),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        drawWave(
            color = Color(0xFF5A6DFF),
            amplitudeFactor = 1.0f,
            frequency = 2.2f,
            phase = 0.0f,
            strokeWidth = 3.0f,
            alpha = 0.95f
        )
        drawWave(
            color = Color(0xFF69D2A6),
            amplitudeFactor = 0.84f,
            frequency = 1.7f,
            phase = 0.85f,
            strokeWidth = 2.4f,
            alpha = 0.88f
        )
        drawWave(
            color = Color(0xFF6D8CFF),
            amplitudeFactor = 0.70f,
            frequency = 1.35f,
            phase = 1.65f,
            strokeWidth = 2.1f,
            alpha = 0.8f
        )
    }
}

private fun createVideoUri(context: Context): Uri {
    val file = File.createTempFile("secret-video-", ".mp4", context.cacheDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

private fun createImageUri(context: Context): Uri {
    val file = File.createTempFile("secret-photo-", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

private fun startDirectFileDownload(
    context: Context,
    rawUrl: String?,
    suggestedFileName: String?
) {
    val uri = resolveRemoteUri(rawUrl) ?: return
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
    val fileName = buildDownloadFileName(uri, suggestedFileName)

    val request = DownloadManager.Request(uri).apply {
        setTitle(fileName)
        setDescription("Downloading")
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setAllowedOverMetered(true)
        setAllowedOverRoaming(true)
        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
    }

    runCatching {
        manager.enqueue(request)
    }
}

private fun buildDownloadFileName(uri: Uri, suggestedFileName: String?): String {
    val baseRaw = suggestedFileName?.trim().takeIf { !it.isNullOrBlank() }
        ?: uri.lastPathSegment
        ?: "file"
    val decodedBase = decodeFileNameComponent(baseRaw)
    val safeBase = sanitizeFileName(decodedBase)
        .ifBlank { "file" }

    val extFromBase = safeBase.substringAfterLast('.', "").trim().lowercase()
    val extFromUri = decodeFileNameComponent(uri.lastPathSegment.orEmpty())
        ?.substringAfterLast('.', "")
        ?.substringBefore('?')
        ?.substringBefore('#')
        ?.trim()
        ?.lowercase()
        .orEmpty()

    val finalName = if (extFromBase.isBlank() && extFromUri.isNotBlank() && extFromUri.length <= 8) {
        "$safeBase.$extFromUri"
    } else {
        safeBase
    }
    return "${System.currentTimeMillis()}-$finalName"
}

private fun decodeFileNameComponent(raw: String): String {
    val clean = raw.trim().substringAfterLast('/').substringBefore('?').substringBefore('#')
    if (clean.isBlank()) {
        return clean
    }
    return runCatching {
        if (clean.contains('%')) {
            URLDecoder.decode(clean, StandardCharsets.UTF_8.name())
        } else {
            clean
        }
    }.getOrDefault(clean)
}

private fun sanitizeFileName(raw: String): String {
    return raw
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .replace(Regex("[\\p{Cntrl}]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun startAudioRecording(context: Context): Pair<MediaRecorder?, File?> {
    val file = File.createTempFile("secret-audio-", ".m4a", context.cacheDir)

    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
    }

    return runCatching {
        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44_100)
            setAudioEncodingBitRate(96_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        Pair(recorder, file)
    }.getOrElse {
        recorder.release()
        Pair(null, null)
    }
}

private fun hasPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun isAudioMessage(message: ChatMessage): Boolean {
    if (message.type == MessageType.AUDIO) {
        return true
    }
    val source = "${message.fileName.orEmpty()} ${message.fileUrl.orEmpty()}".lowercase()
    return source.contains(Regex("\\.(mp3|m4a|aac|ogg|oga|opus|wav|flac|webm)(\\?.*)?$"))
}

private fun isVideoMessage(message: ChatMessage): Boolean {
    if (message.type == MessageType.VIDEO) {
        return true
    }
    val source = "${message.fileName.orEmpty()} ${message.fileUrl.orEmpty()}".lowercase()
    return source.contains(Regex("\\.(mp4|mov|m4v|webm|mkv|avi|wmv)(\\?.*)?$"))
}

private fun hasFilePayload(message: ChatMessage): Boolean {
    return !message.fileUrl.isNullOrBlank() || !message.fileName.isNullOrBlank()
}

private fun isImageAttachment(message: ChatMessage): Boolean {
    if (!hasFilePayload(message)) {
        return false
    }
    val source = "${message.fileName.orEmpty()} ${message.fileUrl.orEmpty()}".lowercase()
    return source.contains(Regex("\\.(png|jpe?g|gif|webp|bmp|svg|avif|heic|heif|jfif)(\\?.*)?$"))
}

private fun formatTime(raw: String): String {
    return runCatching {
        OffsetDateTime.parse(raw).format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault(raw)
}

private fun parseMessageLocalDate(raw: String): LocalDate? {
    return runCatching {
        OffsetDateTime.parse(raw)
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }.getOrNull()
}

private fun messageDateKey(raw: String): String {
    val parsed = parseMessageLocalDate(raw)
    if (parsed != null) {
        return parsed.toString()
    }
    val fallback = raw.substringBefore('T').substringBefore(' ')
    return fallback.ifBlank { raw.trim() }
}

private fun formatMessageDateLabel(state: ChatUiState, raw: String): String {
    val date = parseMessageLocalDate(raw)
    if (date == null) {
        val fallback = raw.substringBefore('T').substringBefore(' ')
        return fallback.ifBlank { raw }
    }
    if (date == LocalDate.now()) {
        return text(state, "Today", "Сегодня")
    }
    return if (state.locale == LocaleMode.RU) {
        date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    } else {
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
    }
}

private fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val mins = safe / 60
    val sec = safe % 60
    return String.format("%02d:%02d", mins, sec)
}

private fun userIdTail(userId: String?): String {
    val value = userId?.trim().orEmpty()
    if (value.isBlank()) {
        return ""
    }
    return value.takeLast(12)
}

private fun generateQrBitmap(raw: String, sizePx: Int = 720): Bitmap? {
    return runCatching {
        val bitMatrix = QRCodeWriter().encode(raw, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    }.getOrNull()
}

private fun text(state: ChatUiState, en: String, ru: String): String {
    return if (state.locale == LocaleMode.RU) ru else en
}
