<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from "vue"
import QRCode from "qrcode"
import type { ChatMessage, ChatRoom, MessageType, UploadResponse, UserProfile } from "~/types/chat"

type Locale = "en" | "ru"
type ThemeMode = "light" | "dark"
type ImageCropTarget = "avatar" | "create_chat_icon" | "active_chat_icon"

type SocketEnvelope =
  | { type: "message"; payload: ChatMessage }
  | { type: "message_deleted"; payload: { id: string; chatId: string } }
  | { type: "chat_deleted"; payload: { chatId: string; isDeleted?: boolean } }

interface DeleteChatResponse {
  chatId: string
  removedMessagesCount: number
  isDeleted?: boolean
  chat?: ChatRoom | null
}

interface ProfileLinkSessionResponse {
  sessionId: string
  status: "PENDING" | "COMPLETED" | "EXPIRED"
  source: "WEB" | "ANDROID"
  createdAt: string
  expiresAt: string
  completedAt?: string | null
  initiatorUserId?: string | null
  resolvedUserId?: string | null
  qrText: string
  user?: UserProfile | null
}

const dictionary: Record<Locale, Record<string, string>> = {
  en: {
    settings: "Settings",
    settingsTitle: "Settings",
    language: "Language",
    theme: "Theme",
    lightTheme: "Light",
    darkTheme: "Dark",
    english: "English",
    russian: "Russian",
    close: "Close",
    profile: "Profile",
    nickname: "Nickname",
    changeAvatarHint: "Click avatar to change it",
    profileSyncing: "Saving profile...",
    createChat: "Create chat",
    createChatBtn: "Create",
    chatNameMin: "Chat name (min. 4 chars)",
    chatIcon: "Chat icon",
    chatIconHint: "Click icon to choose image",
    linkAccount: "Link account",
    scanQr: "Scan QR",
    showQr: "Show QR",
    newQr: "New QR",
    createNewProfile: "Create new profile",
    scanToLink: "Scan to authorize on a new device",
    scanToConnect: "Scan profile QR to sign in on this device",
    qrGenerating: "Generating QR...",
    qrExpiresAt: "QR expires at",
    qrExpired: "QR expired. Generate a new one",
    linkedFromQr: "Profile linked",
    onboardingTitle: "Connect account",
    onboardingDescription: "You can link existing profile via QR or create a new one",
    createProfileInProgress: "Creating profile...",
    linkInProgress: "Waiting for scan...",
    optionalPassword: "Password (optional)",
    joinChat: "Join chat",
    chatName: "Chat name",
    chatPassword: "Password",
    connect: "Connect",
    connecting: "Connecting...",
    myChats: "My chats",
    noChats: "No chats yet",
    privateChat: "Private chat",
    publicChat: "Public chat",
    deletedChat: "Deleted chat",
    deleteChat: "Delete chat",
    deleteChatConfirm: "Delete this chat and all messages?",
    chatDeleted: "Chat deleted",
    closeChat: "Close chat",
    selectChat: "Select a chat to start messaging",
    loading: "Loading...",
    loadingMessages: "Loading messages...",
    noMessages: "No messages yet",
    attach: "Attach",
    attachFile: "Attach file",
    startVideoRecord: "Start video recording",
    stopVideoSend: "Stop and send video",
    microphone: "Microphone",
    recording: "Recording",
    pauseSend: "Pause and send",
    recOverlayHint: "Audio will be sent immediately",
    videoRecording: "Video recording",
    typeMessage: "Type a message",
    send: "Send",
    connected: "Connected",
    reconnecting: "Reconnecting...",
    disconnected: "Disconnected",
    logout: "Log out",
    loggedOut: "Logged out",
    today: "Today",
    yesterday: "Yesterday",
    newMessage: "New message",
    notificationAudio: "Audio message",
    notificationVideo: "Video message",
    notificationFile: "File attachment",
    chatNotSelected: "Chat is not selected",
    profileSaved: "Profile saved",
    chatCreated: "Chat created",
    chatJoined: "Connected to chat",
    needChatName: "Enter chat name",
    needChatNameMin: "Chat name must be at least 4 characters",
    needPassword: "Chat found. Enter password",
    wrongPassword: "Wrong password",
    networkError: "Network error. Check backend connection",
    rateLimitExceeded: "Too many messages. Max 5 per second",
    microphoneDenied: "Microphone permission denied",
    cameraDenied: "Camera permission denied",
    cameraSecureRequired: "Camera on mobile requires HTTPS. Open this site over HTTPS and allow camera access",
    imageCropTitle: "Adjust image",
    imageCropHint: "Move and scale image to fit the circle",
    imageCropApply: "Apply",
    imageCropZoom: "Zoom",
    imageCropError: "Failed to process image",
    deleteMessage: "Delete",
    deleteMessageConfirm: "Delete this message?",
    deletedMessage: "Message deleted",
    deletedChatReadonly: "Chat is deleted. Read-only mode",
    openFile: "Open",
    statusSending: "Sending...",
    superAdminActivated: "Super admin rights activated",
    superAdminInvalidKey: "Invalid admin key"
  },
  ru: {
    settings: "Настройки",
    settingsTitle: "Настройки",
    language: "Язык",
    theme: "Тема",
    lightTheme: "Светлая",
    darkTheme: "Темная",
    english: "English",
    russian: "Русский",
    close: "Закрыть",
    profile: "Профиль",
    nickname: "Псевдоним",
    changeAvatarHint: "Нажмите на аватар, чтобы изменить",
    profileSyncing: "Сохранение профиля...",
    createChat: "Создать чат",
    createChatBtn: "Создать",
    chatNameMin: "Название (мин. 4 символа)",
    chatIcon: "Иконка чата",
    chatIconHint: "Нажмите на иконку, чтобы выбрать изображение",
    linkAccount: "Привязать аккаунт",
    scanQr: "Сканировать QR",
    showQr: "Показать QR",
    newQr: "Новый QR",
    createNewProfile: "Создать новый профиль",
    scanToLink: "Отсканируйте для авторизации на новом устройстве",
    scanToConnect: "Сканируйте QR профиля для входа на этом устройстве",
    qrGenerating: "Генерация QR...",
    qrExpiresAt: "QR действует до",
    qrExpired: "QR истек. Сгенерируйте новый",
    linkedFromQr: "Профиль подключен",
    onboardingTitle: "Подключение аккаунта",
    onboardingDescription: "Можно привязать существующий профиль по QR или создать новый",
    createProfileInProgress: "Создание профиля...",
    linkInProgress: "Ожидание сканирования...",
    optionalPassword: "Пароль (опционально)",
    joinChat: "Подключиться к чату",
    chatName: "Название чата",
    chatPassword: "Пароль",
    connect: "Подключиться",
    connecting: "Подключение...",
    myChats: "Мои чаты",
    noChats: "Пока нет чатов",
    privateChat: "Закрытый чат",
    publicChat: "Открытый чат",
    deletedChat: "Удаленный чат",
    deleteChat: "Удалить чат",
    deleteChatConfirm: "Удалить чат и все сообщения?",
    chatDeleted: "Чат удален",
    closeChat: "Закрыть чат",
    selectChat: "Выберите чат, чтобы начать переписку",
    loading: "Загрузка...",
    loadingMessages: "Загрузка сообщений...",
    noMessages: "Пока нет сообщений",
    attach: "Прикрепить",
    attachFile: "Прикрепить файл",
    startVideoRecord: "Начать запись видео",
    stopVideoSend: "Остановить и отправить видео",
    microphone: "Микрофон",
    recording: "Запись",
    pauseSend: "Пауза и отправка",
    recOverlayHint: "После паузы аудио отправится в чат",
    videoRecording: "Идет запись видео",
    typeMessage: "Введите сообщение",
    send: "Отправить",
    connected: "Подключено",
    reconnecting: "Переподключение...",
    disconnected: "Нет соединения",
    logout: "Выйти",
    loggedOut: "Вы вышли",
    today: "Сегодня",
    yesterday: "Вчера",
    newMessage: "Новое сообщение",
    notificationAudio: "Аудио сообщение",
    notificationVideo: "Видео сообщение",
    notificationFile: "Файл",
    chatNotSelected: "Чат не выбран",
    profileSaved: "Профиль сохранен",
    chatCreated: "Чат создан",
    chatJoined: "Подключено к чату",
    needChatName: "Введите название чата",
    needChatNameMin: "Название чата должно быть от 4 символов",
    needPassword: "Чат найден. Введите пароль",
    wrongPassword: "Неверный пароль",
    networkError: "Ошибка сети. Проверьте backend",
    rateLimitExceeded: "Слишком много сообщений. Не более 5 в секунду",
    microphoneDenied: "Нет доступа к микрофону",
    cameraDenied: "Нет доступа к камере",
    cameraSecureRequired: "Камера на телефоне требует HTTPS. Откройте сайт по HTTPS и разрешите доступ к камере",
    imageCropTitle: "Подстроить изображение",
    imageCropHint: "Переместите и увеличьте изображение под круг",
    imageCropApply: "Применить",
    imageCropZoom: "Масштаб",
    imageCropError: "Не удалось обработать изображение",
    deleteMessage: "Удалить",
    deleteMessageConfirm: "Удалить это сообщение?",
    deletedMessage: "Сообщение удалено",
    deletedChatReadonly: "Чат удален. Только просмотр",
    openFile: "Открыть",
    statusSending: "Отправка...",
    superAdminActivated: "Права супер админа активированы",
    superAdminInvalidKey: "Неверный admin ключ"
  }
}

const config = useRuntimeConfig()
const apiBase = computed(() => String(config.public.apiBase).replace(/\/$/, ""))
const wsBase = computed(() => String(config.public.wsBase))

const locale = ref<Locale>("en")
const theme = ref<ThemeMode>("light")
const settingsOpen = ref(false)
const profilePanelOpen = ref(false)
const createChatPanelOpen = ref(false)
const onboardingVisible = ref(false)
const onboardingCreatingProfile = ref(false)
const qrModalOpen = ref(false)
const qrScannerModalOpen = ref(false)
const qrSessionLoading = ref(false)
const qrSession = ref<ProfileLinkSessionResponse | null>(null)
const qrImageDataUrl = ref("")
const qrScannerLoading = ref(false)
const qrScannerError = ref("")
const appLoading = ref(true)
const chatLoading = ref(false)
const profileSaving = ref(false)
const joiningChat = ref(false)
const sendingMessage = ref(false)

const errorMessage = ref("")
const infoMessage = ref("")

const user = ref<UserProfile | null>(null)
const chats = ref<ChatRoom[]>([])
const activeChat = ref<ChatRoom | null>(null)
const messages = ref<ChatMessage[]>([])

const createForm = reactive({
  name: "",
  password: "",
  iconFile: null as File | null
})

const joinForm = reactive({
  name: "",
  password: ""
})

const profileForm = reactive({
  nickname: "",
  avatarFile: null as File | null
})

const textMessage = ref("")
const joinPasswordRequired = ref(false)
const pendingJoinName = ref("")
const composerInputRef = ref<HTMLTextAreaElement | null>(null)

const messageListRef = ref<HTMLElement | null>(null)
const mediaInputRef = ref<HTMLInputElement | null>(null)
const avatarInputRef = ref<HTMLInputElement | null>(null)
const createChatIconInputRef = ref<HTMLInputElement | null>(null)
const activeChatIconInputRef = ref<HTMLInputElement | null>(null)
const imageCropAreaRef = ref<HTMLElement | null>(null)
const mediaPickerPending = ref(false)
const avatarPickerPending = ref(false)
const createChatIconPickerPending = ref(false)
const activeChatIconPickerPending = ref(false)
const draggedChatId = ref<string | null>(null)
const dragOverChatId = ref<string | null>(null)
const chatIconUpdating = ref(false)

const canCreateChat = computed(() => createForm.name.trim().length >= 4)
const joinPasswordStep = computed(
  () => joinPasswordRequired.value && pendingJoinName.value === joinForm.name.trim()
)
const canJoinChat = computed(
  () =>
    joinForm.name.trim().length > 0 &&
    (!joinPasswordStep.value || joinForm.password.trim().length > 0) &&
    !joiningChat.value
)
const showJoinConnectButton = computed(() => joinForm.name.trim().length > 0 || joinForm.password.trim().length > 0)
const hasDraftMessage = computed(() => textMessage.value.trim().length > 0)
const activeChatDeleted = computed(() => Boolean(activeChat.value?.isDeleted))
const isAuthenticated = computed(() => Boolean(user.value))
const showOnboarding = computed(() => onboardingVisible.value && !isAuthenticated.value)
const MOBILE_ONBOARDING_BREAKPOINT = 1080
const isMobileViewport = ref(false)
const hideChatBoardInOnboarding = computed(
  () => showOnboarding.value && isMobileViewport.value
)
const sidebarPanelOpen = computed(() => profilePanelOpen.value || createChatPanelOpen.value)
const qrIsExpired = computed(() => qrSession.value?.status === "EXPIRED")
const imageCropBaseScale = computed(() => {
  if (!imageCropNaturalWidth.value || !imageCropNaturalHeight.value) {
    return 1
  }
  return Math.max(
    imageCropViewSize.value / imageCropNaturalWidth.value,
    imageCropViewSize.value / imageCropNaturalHeight.value
  )
})
const imageCropRenderScale = computed(() => imageCropBaseScale.value * imageCropZoom.value)
const imageCropDisplayWidth = computed(() => imageCropNaturalWidth.value * imageCropRenderScale.value)
const imageCropDisplayHeight = computed(() => imageCropNaturalHeight.value * imageCropRenderScale.value)
const imageCropImageStyle = computed(() => ({
  width: `${imageCropDisplayWidth.value}px`,
  height: `${imageCropDisplayHeight.value}px`,
  transform: `translate(calc(-50% + ${imageCropOffsetX.value}px), calc(-50% + ${imageCropOffsetY.value}px))`
}))

const LS_USER_ID = "secret_web_user_id"
const LS_NICK = "secret_web_nickname"
const LS_AVATAR = "secret_web_avatar"
const LS_LOCALE = "secret_web_locale"
const LS_THEME = "secret_web_theme"
const LS_CHAT_ORDER = "secret_web_chat_order"
const LS_CHAT_UNREAD = "secret_web_chat_unread"
const LS_PENDING_ADMIN_KEY = "secret_web_pending_admin_key"
const CLIENT_MESSAGE_LIMIT_PER_SECOND = 5
const CLIENT_MESSAGE_WINDOW_MS = 1000
const ADMIN_QUERY_PARAM = "admin"
const IMAGE_CROP_VIEW_SIZE = 280
const IMAGE_CROP_OUTPUT_SIZE = 640

const avatarPreviewUrl = ref<string | null>(null)
const createChatIconPreviewUrl = ref<string | null>(null)
const imageCropModalOpen = ref(false)
const imageCropDataUrl = ref("")
const imageCropZoom = ref(1)
const imageCropOffsetX = ref(0)
const imageCropOffsetY = ref(0)
const imageCropNaturalWidth = ref(0)
const imageCropNaturalHeight = ref(0)
const imageCropViewSize = ref(IMAGE_CROP_VIEW_SIZE)
const imageCropApplying = ref(false)
const imageCropTarget = ref<ImageCropTarget | null>(null)
const imageCropFileName = ref("image.png")
const imageCropDragging = ref(false)
const unreadByChat = ref<Record<string, number>>({})
const profileAvatar = computed(() => avatarPreviewUrl.value || user.value?.avatarUrl || "/default-profile.svg")
const profileIdTail = computed(() => {
  const id = String(user.value?.id || "")
  return id ? id.slice(-12) : ""
})
const createChatIconPreview = computed(() => createChatIconPreviewUrl.value || "/default-chat.svg")
const isSuperUser = computed(() => Boolean(user.value?.isSuperAdmin))
const canEditActiveChatIcon = computed(
  () =>
    Boolean(
      activeChat.value &&
      user.value &&
      (activeChat.value.createdBy === user.value.id || isSuperUser.value)
    )
)
const clientSendTimestamps = ref<number[]>([])
let profileSaveTimer: ReturnType<typeof setTimeout> | null = null
let suppressProfileWatch = false
let adminActivationInFlight = false
const pendingAdminKey = ref("")
let imageCropSourceImage: HTMLImageElement | null = null
let imageCropStartX = 0
let imageCropStartY = 0
let imageCropStartOffsetX = 0
let imageCropStartOffsetY = 0

let socket: WebSocket | null = null
const joinedRoomIds = new Set<string>()
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let allowReconnect = true
const socketConnected = ref(false)
const reconnecting = ref(false)
const backendHealthy = ref(false)
let healthTimer: ReturnType<typeof setInterval> | null = null
let healthCheckInFlight = false
let qrPollTimer: ReturnType<typeof setInterval> | null = null
let qrPollInFlight = false
let viewportResizeHandler: (() => void) | null = null
let qrScannerInstance:
  | {
      start: (...args: unknown[]) => Promise<void>
      stop: () => Promise<void>
      clear: () => Promise<void>
    }
  | null = null
let qrScanInFlight = false
const qrReaderElementId = "profile-link-qr-reader"

const connectionStateKey = computed(() => {
  if (socketConnected.value && backendHealthy.value) {
    return "connected"
  }
  if (reconnecting.value) {
    return "reconnecting"
  }
  return "disconnected"
})

let audioRecorder: MediaRecorder | null = null
let audioStream: MediaStream | null = null
let audioChunks: Blob[] = []
let audioTicker: ReturnType<typeof setInterval> | null = null
let audioStartedAt = 0
const audioRecording = ref(false)
const audioElapsed = ref(0)

let videoRecorder: MediaRecorder | null = null
let videoStream: MediaStream | null = null
let videoChunks: Blob[] = []
let videoTicker: ReturnType<typeof setInterval> | null = null
let videoStartedAt = 0
const videoRecording = ref(false)
const videoElapsed = ref(0)

function t(key: string): string {
  return dictionary[locale.value][key] || key
}

function setLocale(nextLocale: Locale) {
  locale.value = nextLocale
  getStorage().setItem(LS_LOCALE, nextLocale)
}

function setTheme(nextTheme: ThemeMode) {
  theme.value = nextTheme
  getStorage().setItem(LS_THEME, nextTheme)
}

function getStorage() {
  return window.localStorage
}

function readChatOrder(): string[] {
  try {
    const raw = getStorage().getItem(LS_CHAT_ORDER)
    if (!raw) {
      return []
    }
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return []
    }
    return parsed.filter((value): value is string => typeof value === "string")
  } catch {
    return []
  }
}

function persistChatOrder(chatList: ChatRoom[]) {
  getStorage().setItem(LS_CHAT_ORDER, JSON.stringify(chatList.map((chat) => chat.id)))
}

function applyStoredChatOrder(chatList: ChatRoom[]): ChatRoom[] {
  const order = readChatOrder()
  if (order.length === 0) {
    return chatList
  }

  const rank = new Map(order.map((id, index) => [id, index]))
  return chatList
    .map((chat, index) => ({ chat, index }))
    .sort((left, right) => {
      const leftRank = rank.get(left.chat.id) ?? Number.MAX_SAFE_INTEGER
      const rightRank = rank.get(right.chat.id) ?? Number.MAX_SAFE_INTEGER
      if (leftRank === rightRank) {
        return left.index - right.index
      }
      return leftRank - rightRank
    })
    .map((item) => item.chat)
}

function readUnreadMap(): Record<string, number> {
  try {
    const raw = getStorage().getItem(LS_CHAT_UNREAD)
    if (!raw) {
      return {}
    }
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== "object") {
      return {}
    }

    const normalized: Record<string, number> = {}
    for (const [chatId, value] of Object.entries(parsed as Record<string, unknown>)) {
      if (typeof chatId !== "string" || !chatId) {
        continue
      }
      const count = Math.floor(Number(value))
      if (Number.isFinite(count) && count > 0) {
        normalized[chatId] = count
      }
    }
    return normalized
  } catch {
    return {}
  }
}

function persistUnreadMap() {
  getStorage().setItem(LS_CHAT_UNREAD, JSON.stringify(unreadByChat.value))
}

function unreadCount(chatId: string): number {
  return unreadByChat.value[chatId] || 0
}

function incrementUnread(chatId: string) {
  const nextCount = unreadCount(chatId) + 1
  unreadByChat.value = {
    ...unreadByChat.value,
    [chatId]: nextCount
  }
  persistUnreadMap()
}

function clearUnread(chatId: string) {
  if (!unreadByChat.value[chatId]) {
    return
  }
  const next = { ...unreadByChat.value }
  delete next[chatId]
  unreadByChat.value = next
  persistUnreadMap()
}

function retainUnreadForVisibleChats() {
  const chatIds = new Set(chats.value.map((chat) => chat.id))
  const next: Record<string, number> = {}
  for (const [chatId, count] of Object.entries(unreadByChat.value)) {
    if (chatIds.has(chatId) && count > 0) {
      next[chatId] = count
    }
  }
  unreadByChat.value = next
  persistUnreadMap()
}

function reserveClientMessageSlot(): boolean {
  const now = Date.now()
  const windowStart = now - CLIENT_MESSAGE_WINDOW_MS
  const recent = clientSendTimestamps.value.filter((timestamp) => timestamp > windowStart)
  if (recent.length >= CLIENT_MESSAGE_LIMIT_PER_SECOND) {
    clientSendTimestamps.value = recent
    return false
  }
  recent.push(now)
  clientSendTimestamps.value = recent
  return true
}

function setError(message: string) {
  errorMessage.value = message
  if (!message) {
    return
  }
  setTimeout(() => {
    if (errorMessage.value === message) {
      errorMessage.value = ""
    }
  }, 4200)
}

function setInfo(message: string) {
  infoMessage.value = message
  if (!message) {
    return
  }
  setTimeout(() => {
    if (infoMessage.value === message) {
      infoMessage.value = ""
    }
  }, 2400)
}

function extractError(error: unknown): { message: string; statusCode?: number } {
  const anyError = error as {
    data?: { message?: string }
    statusCode?: number
    status?: number
    response?: { status?: number }
    statusMessage?: string
    message?: string
  }

  return {
    message:
      anyError?.data?.message || anyError?.statusMessage || anyError?.message || t("networkError"),
    statusCode: anyError?.statusCode || anyError?.status || anyError?.response?.status
  }
}

function mapQrScannerError(error: unknown) {
  const message = extractError(error).message
  const normalized = message.toLowerCase()

  if (
    normalized.includes("permission") ||
    normalized.includes("denied") ||
    normalized.includes("notallowederror") ||
    normalized.includes("not allowed")
  ) {
    return t("cameraDenied")
  }

  if (
    normalized.includes("secure context") ||
    normalized.includes("https") ||
    normalized.includes("only secure origins") ||
    normalized.includes("insecure context")
  ) {
    return t("cameraSecureRequired")
  }

  return message
}

function isLiveQrCameraAvailable() {
  if (typeof window === "undefined" || typeof navigator === "undefined") {
    return false
  }

  return Boolean(window.isSecureContext && navigator.mediaDevices?.getUserMedia)
}

function syncViewportState() {
  if (typeof window === "undefined") {
    return
  }
  isMobileViewport.value = window.innerWidth <= MOBILE_ONBOARDING_BREAKPOINT
}

function startViewportWatcher() {
  syncViewportState()
  if (typeof window === "undefined") {
    return
  }

  viewportResizeHandler = () => {
    syncViewportState()
  }
  window.addEventListener("resize", viewportResizeHandler)
}

function stopViewportWatcher() {
  if (typeof window === "undefined" || !viewportResizeHandler) {
    return
  }
  window.removeEventListener("resize", viewportResizeHandler)
  viewportResizeHandler = null
}

function toggleProfilePanel() {
  const next = !profilePanelOpen.value
  profilePanelOpen.value = next
  if (next) {
    createChatPanelOpen.value = false
  }
}

function toggleCreateChatPanel() {
  const next = !createChatPanelOpen.value
  createChatPanelOpen.value = next
  if (next) {
    profilePanelOpen.value = false
  }
}

function safeTrim(value: unknown): string {
  return typeof value === "string" ? value.trim() : ""
}

function rememberPendingAdminKey(value: string) {
  const next = safeTrim(value)
  pendingAdminKey.value = next
  const storage = getStorage()
  if (next) {
    storage.setItem(LS_PENDING_ADMIN_KEY, next)
  } else {
    storage.removeItem(LS_PENDING_ADMIN_KEY)
  }
}

function capturePendingAdminKeyFromUrl() {
  const storage = getStorage()

  if (typeof window === "undefined") {
    const stored = safeTrim(storage.getItem(LS_PENDING_ADMIN_KEY))
    if (stored) {
      pendingAdminKey.value = stored
    }
    return
  }

  const current = new URL(window.location.href)
  const fromQuery = safeTrim(current.searchParams.get(ADMIN_QUERY_PARAM))
  if (fromQuery) {
    rememberPendingAdminKey(fromQuery)
    current.searchParams.delete(ADMIN_QUERY_PARAM)
    const nextUrl = `${current.pathname}${current.search}${current.hash}`
    window.history.replaceState({}, "", nextUrl || "/")
    return
  }

  const stored = safeTrim(storage.getItem(LS_PENDING_ADMIN_KEY))
  if (stored) {
    pendingAdminKey.value = stored
  }
}

async function activateSuperAdminIfRequested() {
  if (!user.value || adminActivationInFlight) {
    return
  }
  if (!pendingAdminKey.value) {
    return
  }
  if (user.value.isSuperAdmin) {
    rememberPendingAdminKey("")
    return
  }

  adminActivationInFlight = true
  try {
    const updated = await apiPost<UserProfile>("/admin/activate", {
      userId: user.value.id,
      key: pendingAdminKey.value
    })
    applyUserProfile(updated)
    rememberPendingAdminKey("")
    setInfo(t("superAdminActivated"))
  } catch (error) {
    const parsed = extractError(error)
    if (parsed.statusCode === 401) {
      rememberPendingAdminKey("")
      setError(t("superAdminInvalidKey"))
      return
    }
    setError(parsed.message)
  } finally {
    adminActivationInFlight = false
  }
}

function randomGuestNickname(): string {
  return `guest-${Math.random().toString(16).slice(2, 8)}`
}

function persistUserSession(current: UserProfile) {
  const storage = getStorage()
  storage.setItem(LS_USER_ID, current.id)
  storage.setItem(LS_NICK, current.nickname)
  storage.setItem(LS_AVATAR, current.avatarUrl || "")
}

function cleanupAvatarPreview() {
  if (!avatarPreviewUrl.value) {
    return
  }
  URL.revokeObjectURL(avatarPreviewUrl.value)
  avatarPreviewUrl.value = null
}

function cleanupCreateChatIconPreview() {
  if (!createChatIconPreviewUrl.value) {
    return
  }
  URL.revokeObjectURL(createChatIconPreviewUrl.value)
  createChatIconPreviewUrl.value = null
}

function clampImageCropOffsets() {
  const maxX = Math.max(0, (imageCropDisplayWidth.value - imageCropViewSize.value) / 2)
  const maxY = Math.max(0, (imageCropDisplayHeight.value - imageCropViewSize.value) / 2)
  imageCropOffsetX.value = Math.min(maxX, Math.max(-maxX, imageCropOffsetX.value))
  imageCropOffsetY.value = Math.min(maxY, Math.max(-maxY, imageCropOffsetY.value))
}

function resetImageCropState() {
  imageCropModalOpen.value = false
  imageCropDataUrl.value = ""
  imageCropZoom.value = 1
  imageCropOffsetX.value = 0
  imageCropOffsetY.value = 0
  imageCropNaturalWidth.value = 0
  imageCropNaturalHeight.value = 0
  imageCropViewSize.value = IMAGE_CROP_VIEW_SIZE
  imageCropApplying.value = false
  imageCropTarget.value = null
  imageCropFileName.value = "image.png"
  imageCropDragging.value = false
  imageCropSourceImage = null
}

function closeImageCropper() {
  if (imageCropApplying.value) {
    return
  }
  resetImageCropState()
}

function syncImageCropViewSize() {
  const measured = Math.round(imageCropAreaRef.value?.clientWidth || IMAGE_CROP_VIEW_SIZE)
  imageCropViewSize.value = Math.max(160, measured)
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const result = reader.result
      if (typeof result === "string" && result) {
        resolve(result)
        return
      }
      reject(new Error("Empty file data"))
    }
    reader.onerror = () => reject(reader.error || new Error("Failed to read image file"))
    reader.readAsDataURL(file)
  })
}

function loadImageElement(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error("Failed to decode image"))
    image.src = src
  })
}

async function openImageCropper(file: File, target: ImageCropTarget) {
  if (!file.type.startsWith("image/")) {
    setError(t("imageCropError"))
    return
  }

  try {
    const dataUrl = await readFileAsDataUrl(file)
    const image = await loadImageElement(dataUrl)

    imageCropDataUrl.value = dataUrl
    imageCropNaturalWidth.value = image.naturalWidth || image.width
    imageCropNaturalHeight.value = image.naturalHeight || image.height
    imageCropTarget.value = target
    imageCropFileName.value = file.name || "image.png"
    imageCropZoom.value = 1
    imageCropOffsetX.value = 0
    imageCropOffsetY.value = 0
    imageCropSourceImage = image
    imageCropModalOpen.value = true
    await nextTick()
    syncImageCropViewSize()
    clampImageCropOffsets()
  } catch {
    resetImageCropState()
    setError(t("imageCropError"))
  }
}

function onImageCropPointerDown(event: PointerEvent) {
  if (imageCropApplying.value) {
    return
  }

  imageCropDragging.value = true
  imageCropStartX = event.clientX
  imageCropStartY = event.clientY
  imageCropStartOffsetX = imageCropOffsetX.value
  imageCropStartOffsetY = imageCropOffsetY.value
  syncImageCropViewSize()
  imageCropAreaRef.value?.setPointerCapture(event.pointerId)
}

function onImageCropPointerMove(event: PointerEvent) {
  if (!imageCropDragging.value || imageCropApplying.value) {
    return
  }

  imageCropOffsetX.value = imageCropStartOffsetX + (event.clientX - imageCropStartX)
  imageCropOffsetY.value = imageCropStartOffsetY + (event.clientY - imageCropStartY)
  clampImageCropOffsets()
}

function onImageCropPointerEnd(event: PointerEvent) {
  imageCropDragging.value = false
  if (imageCropAreaRef.value?.hasPointerCapture(event.pointerId)) {
    imageCropAreaRef.value.releasePointerCapture(event.pointerId)
  }
}

function buildPngFileName(originalName: string) {
  const trimmed = safeTrim(originalName)
  const base = trimmed ? trimmed.replace(/\.[^/.]+$/, "") : "image"
  return `${base || "image"}.png`
}

async function buildCroppedImageFile(sourceImage: HTMLImageElement, originalName: string): Promise<File> {
  const canvas = document.createElement("canvas")
  canvas.width = IMAGE_CROP_OUTPUT_SIZE
  canvas.height = IMAGE_CROP_OUTPUT_SIZE
  const ctx = canvas.getContext("2d")
  if (!ctx) {
    throw new Error("Canvas context unavailable")
  }

  const outputScale = IMAGE_CROP_OUTPUT_SIZE / imageCropViewSize.value
  const drawWidth = imageCropDisplayWidth.value * outputScale
  const drawHeight = imageCropDisplayHeight.value * outputScale
  const drawX =
    ((imageCropViewSize.value - imageCropDisplayWidth.value) / 2 + imageCropOffsetX.value) * outputScale
  const drawY =
    ((imageCropViewSize.value - imageCropDisplayHeight.value) / 2 + imageCropOffsetY.value) * outputScale

  ctx.clearRect(0, 0, canvas.width, canvas.height)
  ctx.drawImage(sourceImage, drawX, drawY, drawWidth, drawHeight)
  ctx.globalCompositeOperation = "destination-in"
  ctx.beginPath()
  ctx.arc(
    IMAGE_CROP_OUTPUT_SIZE / 2,
    IMAGE_CROP_OUTPUT_SIZE / 2,
    IMAGE_CROP_OUTPUT_SIZE / 2,
    0,
    Math.PI * 2
  )
  ctx.closePath()
  ctx.fill()
  ctx.globalCompositeOperation = "source-over"

  const blob = await new Promise<Blob | null>((resolve) => {
    canvas.toBlob(resolve, "image/png", 0.95)
  })
  if (!blob) {
    throw new Error("Failed to export cropped image")
  }

  return new File([blob], buildPngFileName(originalName), { type: "image/png" })
}

async function applyCroppedAvatar(file: File) {
  profileForm.avatarFile = file
  cleanupAvatarPreview()
  avatarPreviewUrl.value = URL.createObjectURL(file)
  await saveProfile({ immediate: true })
}

async function applyCroppedCreateChatIcon(file: File) {
  createForm.iconFile = file
  cleanupCreateChatIconPreview()
  createChatIconPreviewUrl.value = URL.createObjectURL(file)
}

async function updateActiveChatIconWithFile(file: File) {
  if (!user.value || !activeChat.value || !canEditActiveChatIcon.value) {
    return
  }

  chatIconUpdating.value = true
  try {
    const uploaded = await uploadFile(file)
    const updated = await apiPut<ChatRoom>(`/chats/${encodeURIComponent(activeChat.value.id)}/icon`, {
      userId: user.value.id,
      iconUrl: uploaded.url
    })
    applyUpdatedChat(updated)
  } catch (error) {
    setError(extractError(error).message)
  } finally {
    chatIconUpdating.value = false
  }
}

async function applyImageCrop() {
  if (!imageCropSourceImage || !imageCropTarget.value || imageCropApplying.value) {
    return
  }

  imageCropApplying.value = true
  try {
    syncImageCropViewSize()
    const croppedFile = await buildCroppedImageFile(imageCropSourceImage, imageCropFileName.value)
    if (imageCropTarget.value === "avatar") {
      await applyCroppedAvatar(croppedFile)
    } else if (imageCropTarget.value === "create_chat_icon") {
      await applyCroppedCreateChatIcon(croppedFile)
    } else if (imageCropTarget.value === "active_chat_icon") {
      await updateActiveChatIconWithFile(croppedFile)
    }
    resetImageCropState()
  } catch {
    setError(t("imageCropError"))
  } finally {
    imageCropApplying.value = false
  }
}

function chatIconUrl(chat: ChatRoom | null | undefined): string {
  return chat?.iconUrl || "/default-chat.svg"
}

function onChatIconError(event: Event) {
  const image = event.target as HTMLImageElement
  if (image.src.endsWith("/default-chat.svg")) {
    return
  }
  image.src = "/default-chat.svg"
}

function onProfileAvatarError(event: Event) {
  const image = event.target as HTMLImageElement
  if (image.src.endsWith("/default-profile.svg")) {
    return
  }
  image.src = "/default-profile.svg"
  if (user.value?.avatarUrl) {
    user.value = { ...user.value, avatarUrl: null }
    persistUserSession(user.value)
  }
}

async function apiGet<T>(path: string): Promise<T> {
  return await $fetch<T>(`${apiBase.value}${path}`)
}

async function apiPost<T>(path: string, body: unknown): Promise<T> {
  return await $fetch<T>(`${apiBase.value}${path}`, {
    method: "POST",
    body
  })
}

async function apiPut<T>(path: string, body: unknown): Promise<T> {
  return await $fetch<T>(`${apiBase.value}${path}`, {
    method: "PUT",
    body
  })
}

async function apiDelete<T>(path: string): Promise<T> {
  return await $fetch<T>(`${apiBase.value}${path}`, {
    method: "DELETE"
  })
}

async function checkBackendHealthOnce() {
  if (healthCheckInFlight) {
    return
  }

  healthCheckInFlight = true
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 2400)

  try {
    await $fetch<{ status: string }>(`${apiBase.value}/health`, {
      signal: controller.signal
    })
    backendHealthy.value = true

    if (allowReconnect && user.value && (!socket || socket.readyState === WebSocket.CLOSED)) {
      connectSocket()
    }
  } catch {
    backendHealthy.value = false
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.close()
    }
  } finally {
    clearTimeout(timeout)
    healthCheckInFlight = false
  }
}

function startHealthChecks() {
  stopHealthChecks()
  void checkBackendHealthOnce()
  healthTimer = setInterval(() => {
    void checkBackendHealthOnce()
  }, 4000)
}

function stopHealthChecks() {
  if (!healthTimer) {
    return
  }
  clearInterval(healthTimer)
  healthTimer = null
}

function applyUserProfile(nextUser: UserProfile) {
  user.value = nextUser
  suppressProfileWatch = true
  profileForm.nickname = nextUser.nickname
  suppressProfileWatch = false
  persistUserSession(nextUser)
}

function closeRealtimeConnection() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }

  joinedRoomIds.clear()
  socketConnected.value = false
  reconnecting.value = false

  if (socket) {
    try {
      socket.close(1000)
    } catch {
      // Ignore close errors.
    }
  }
  socket = null
}

async function restoreUserFromStorage(): Promise<boolean> {
  const storage = getStorage()
  const savedUserId = storage.getItem(LS_USER_ID)

  if (!savedUserId) {
    return false
  }

  try {
    const existing = await apiGet<UserProfile>(`/users/${encodeURIComponent(savedUserId)}`)
    applyUserProfile(existing)
    return true
  } catch {
    storage.removeItem(LS_USER_ID)
    return false
  }
}

async function createNewProfile() {
  const storage = getStorage()
  const fallbackNickname = storage.getItem(LS_NICK) || randomGuestNickname()

  onboardingCreatingProfile.value = true
  try {
    const created = await apiPost<UserProfile>("/users/register", {
      nickname: fallbackNickname,
      avatarUrl: null
    })
    applyUserProfile(created)
    onboardingVisible.value = false
    await initializeSessionAfterAuth()
  } catch (error) {
    setError(extractError(error).message)
  } finally {
    onboardingCreatingProfile.value = false
  }
}

async function logout() {
  const storage = getStorage()
  const currentUserId = String(user.value?.id || "").trim()

  allowReconnect = false
  closeRealtimeConnection()

  try {
    if (currentUserId) {
      await apiPost<{ status: string }>("/auth/logout", { userId: currentUserId })
    }
  } catch {
    // Logout should still complete locally if backend is unreachable.
  }

  if (profileSaveTimer) {
    clearTimeout(profileSaveTimer)
    profileSaveTimer = null
  }

  storage.removeItem(LS_USER_ID)
  storage.removeItem(LS_NICK)
  storage.removeItem(LS_AVATAR)
  storage.removeItem(LS_CHAT_ORDER)
  storage.removeItem(LS_CHAT_UNREAD)

  stopQrPolling()
  qrModalOpen.value = false
  await stopQrScanner(true)
  qrScannerModalOpen.value = false
  qrSession.value = null
  qrImageDataUrl.value = ""
  qrSessionLoading.value = false
  qrScannerLoading.value = false
  qrScannerError.value = ""

  settingsOpen.value = false
  profilePanelOpen.value = false
  createChatPanelOpen.value = false
  onboardingVisible.value = true
  onboardingCreatingProfile.value = false

  user.value = null
  chats.value = []
  activeChat.value = null
  messages.value = []
  unreadByChat.value = {}

  joinPasswordRequired.value = false
  pendingJoinName.value = ""
  joinForm.name = ""
  joinForm.password = ""
  createForm.name = ""
  createForm.password = ""
  createForm.iconFile = null
  profileForm.nickname = ""
  profileForm.avatarFile = null
  textMessage.value = ""

  cleanupAvatarPreview()
  cleanupCreateChatIconPreview()
  resetImageCropState()

  errorMessage.value = ""
  setInfo(t("loggedOut"))
}

async function initializeSessionAfterAuth() {
  if (!user.value) {
    return
  }

  await activateSuperAdminIfRequested()

  allowReconnect = true
  closeRealtimeConnection()
  activeChat.value = null
  messages.value = []
  joinPasswordRequired.value = false
  pendingJoinName.value = ""

  await refreshChats()
  connectSocket()
}

function stopQrPolling() {
  if (!qrPollTimer) {
    return
  }
  clearInterval(qrPollTimer)
  qrPollTimer = null
  qrPollInFlight = false
}

async function fetchProfileLinkSession(sessionId: string): Promise<ProfileLinkSessionResponse | null> {
  try {
    const session = await apiGet<ProfileLinkSessionResponse>(`/profile-link/sessions/${encodeURIComponent(sessionId)}`)
    qrSession.value = session
    return session
  } catch (error) {
    setError(extractError(error).message)
    return null
  }
}

async function applyLinkedProfile(session: ProfileLinkSessionResponse) {
  if (!session.user) {
    setError(t("networkError"))
    return
  }

  applyUserProfile(session.user)
  onboardingVisible.value = false
  await initializeSessionAfterAuth()
  setInfo(t("linkedFromQr"))
}

async function pollProfileLinkSession() {
  if (!qrSession.value || qrPollInFlight) {
    return
  }

  qrPollInFlight = true
  try {
    const session = await fetchProfileLinkSession(qrSession.value.sessionId)
    if (!session) {
      return
    }

    if (session.status === "COMPLETED" && session.user) {
      stopQrPolling()
      await applyLinkedProfile(session)
      qrModalOpen.value = false
      qrSessionLoading.value = false
      return
    }

    if (session.status === "EXPIRED") {
      stopQrPolling()
      qrSessionLoading.value = false
      return
    }
  } finally {
    qrPollInFlight = false
  }
}

function startQrPolling() {
  stopQrPolling()
  qrPollTimer = setInterval(() => {
    void pollProfileLinkSession()
  }, 1300)
}

function extractProfileLinkSessionId(rawPayload: string): string {
  const trimmed = rawPayload.trim()
  if (!trimmed) {
    return ""
  }

  const uuidRegex =
    /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/
  if (uuidRegex.test(trimmed)) {
    return trimmed
  }

  try {
    const parsed = new URL(trimmed)
    const fromQuery = parsed.searchParams.get("session")
    if (fromQuery) {
      return fromQuery.trim()
    }
  } catch {
    // Ignore URL parse errors and fallback below.
  }

  const markerIndex = trimmed.toLowerCase().indexOf("session=")
  if (markerIndex >= 0) {
    const raw = trimmed.slice(markerIndex + "session=".length)
    return raw.split("&")[0]?.split("#")[0]?.trim() || ""
  }

  return ""
}

async function completeProfileLinkSessionByScan(rawPayload: string) {
  const sessionId = extractProfileLinkSessionId(rawPayload)
  if (!sessionId) {
    qrScannerError.value = t("networkError")
    return
  }

  const completed = await apiPost<ProfileLinkSessionResponse>(
    `/profile-link/sessions/${encodeURIComponent(sessionId)}/complete`,
    { userId: user.value?.id || null }
  )

  if (completed.status === "EXPIRED") {
    qrScannerError.value = t("qrExpired")
    return
  }

  if (!completed.user) {
    qrScannerError.value = t("networkError")
    return
  }

  await applyLinkedProfile(completed)
  qrScannerModalOpen.value = false
}

async function stopQrScanner(resetError = false) {
  const scanner = qrScannerInstance
  qrScannerInstance = null

  if (scanner) {
    try {
      await scanner.stop()
    } catch {
      // Ignore stop errors.
    }

    try {
      await scanner.clear()
    } catch {
      // Ignore clear errors.
    }
  }

  qrScannerLoading.value = false
  if (resetError) {
    qrScannerError.value = ""
  }
}

async function startQrScanner() {
  if (!qrScannerModalOpen.value || isAuthenticated.value) {
    return
  }

  if (!isLiveQrCameraAvailable()) {
    qrScannerLoading.value = false
    qrScannerError.value = t("cameraSecureRequired")
    return
  }

  qrScannerLoading.value = true
  qrScannerError.value = ""
  qrScanInFlight = false
  await nextTick()

  try {
    const module = await import("html5-qrcode")
    const Html5Qrcode = module.Html5Qrcode
    const scanner = new Html5Qrcode(qrReaderElementId)
    qrScannerInstance = scanner

    let cameraConfig: string | { facingMode: string } = { facingMode: "environment" }
    try {
      const cameras = (await Html5Qrcode.getCameras()) as Array<{ id: string; label: string }>
      if (cameras.length > 0) {
        const preferred =
          cameras.find((camera) => /back|rear|environment/i.test(camera.label)) || cameras[0]
        cameraConfig = preferred.id
      }
    } catch {
      cameraConfig = { facingMode: "environment" }
    }

    await scanner.start(
      cameraConfig,
      { fps: 10, qrbox: { width: 220, height: 220 } },
      async (decodedText: string) => {
        if (qrScanInFlight) {
          return
        }
        qrScanInFlight = true
        try {
          await stopQrScanner(false)
          await completeProfileLinkSessionByScan(decodedText)
        } catch (error) {
          qrScannerError.value = extractError(error).message
        } finally {
          qrScanInFlight = false
        }
      },
      () => {
        // no-op on scan failure frames
      }
    )

    qrScannerLoading.value = false
  } catch (error) {
    qrScannerLoading.value = false
    qrScannerError.value = mapQrScannerError(error)
  }
}

async function openQrScannerModal() {
  if (isAuthenticated.value) {
    return
  }

  qrScannerModalOpen.value = true
  await startQrScanner()
}

async function restartQrScanner() {
  if (!qrScannerModalOpen.value) {
    return
  }
  await stopQrScanner(true)
  await startQrScanner()
}

async function closeQrScannerModal() {
  qrScannerModalOpen.value = false
  await stopQrScanner(true)
}

async function openQrModal() {
  if (!isAuthenticated.value) {
    return
  }

  await closeQrScannerModal()
  qrModalOpen.value = true
  qrSessionLoading.value = true
  qrImageDataUrl.value = ""
  qrSession.value = null

  try {
    const session = await apiPost<ProfileLinkSessionResponse>("/profile-link/sessions", {
      userId: user.value?.id || null,
      source: "WEB"
    })

    qrSession.value = session
    qrImageDataUrl.value = await QRCode.toDataURL(session.qrText, {
      width: 320,
      margin: 1
    })
    qrSessionLoading.value = false
    startQrPolling()
    void pollProfileLinkSession()
  } catch (error) {
    qrSessionLoading.value = false
    setError(extractError(error).message)
  }
}

function closeQrModal() {
  qrModalOpen.value = false
  stopQrPolling()
}

async function refreshChats(options: { autoOpenFirst?: boolean } = {}) {
  if (!user.value) {
    return
  }

  const { autoOpenFirst = true } = options
  const data = await apiGet<ChatRoom[]>(`/chats?userId=${encodeURIComponent(user.value.id)}`)
  chats.value = applyStoredChatOrder(data)
  persistChatOrder(chats.value)
  retainUnreadForVisibleChats()

  const activeId = activeChat.value?.id
  if (activeId) {
    const matched = chats.value.find((item) => item.id === activeId)
    if (matched) {
      activeChat.value = matched
    } else {
      closeChat()
    }
  }

  syncSocketRooms()

  if (autoOpenFirst && chats.value.length > 0) {
    const firstChat = chats.value[0]
    if (activeChat.value?.id !== firstChat.id) {
      await openChat(firstChat)
    }
  }
}

async function createChat() {
  if (!user.value) {
    return
  }

  const chatName = createForm.name.trim()
  if (chatName.length < 4) {
    setError(t("needChatNameMin"))
    return
  }

  try {
    let iconUrl: string | null = null
    if (createForm.iconFile) {
      const uploaded = await uploadFile(createForm.iconFile)
      iconUrl = uploaded.url
    }

    const created = await apiPost<ChatRoom>("/chats/create", {
      name: chatName,
      password: createForm.password.trim() || null,
      iconUrl,
      userId: user.value.id
    })

    createForm.name = ""
    createForm.password = ""
    createForm.iconFile = null
    cleanupCreateChatIconPreview()
    await refreshChats({ autoOpenFirst: false })
    await openChat(created)
    createChatPanelOpen.value = false
    setInfo(t("chatCreated"))
  } catch (error) {
    setError(extractError(error).message)
  }
}

async function joinChat() {
  if (!user.value) {
    return
  }

  const chatName = joinForm.name.trim()
  if (!chatName) {
    setError(t("needChatName"))
    return
  }

  joiningChat.value = true

  try {
    const joined = await apiPost<ChatRoom>("/chats/join", {
      name: chatName,
      password: joinPasswordStep.value ? joinForm.password.trim() || null : null,
      userId: user.value.id
    })

    joinPasswordRequired.value = false
    pendingJoinName.value = ""
    joinForm.password = ""

    await refreshChats({ autoOpenFirst: false })
    await openChat(joined)
    setInfo(t("chatJoined"))
  } catch (error) {
    const parsed = extractError(error)
    if (parsed.statusCode === 401) {
      joinPasswordRequired.value = true
      pendingJoinName.value = chatName
      setError(joinForm.password.trim() ? t("wrongPassword") : t("needPassword"))
    } else {
      joinPasswordRequired.value = false
      pendingJoinName.value = ""
      setError(parsed.message)
    }
  } finally {
    joiningChat.value = false
  }
}

function canManageChat(chat: ChatRoom): boolean {
  if (!user.value) {
    return false
  }
  return chat.createdBy === user.value.id || isSuperUser.value
}

function canViewDeletedInChat(chat: ChatRoom | null | undefined): boolean {
  if (!chat || !user.value) {
    return false
  }
  return chat.createdBy === user.value.id || isSuperUser.value
}

function canViewDeletedMessages(): boolean {
  return Boolean(isSuperUser.value)
}

async function deleteChat(chat: ChatRoom) {
  if (!user.value || !canManageChat(chat)) {
    return
  }

  try {
    const response = await apiDelete<DeleteChatResponse>(
      `/chats/${encodeURIComponent(chat.id)}?userId=${encodeURIComponent(user.value.id)}`
    )
    const serverChat = response?.chat || null
    const shouldKeepDeleted = canViewDeletedInChat(chat)
    clearUnread(chat.id)

    if (serverChat && shouldKeepDeleted) {
      applyUpdatedChat({ ...serverChat, isDeleted: true })
      if (activeChat.value?.id === chat.id) {
        activeChat.value = { ...serverChat, isDeleted: true }
      }
      joinedRoomIds.delete(chat.id)
      syncSocketRooms()
    } else if (shouldKeepDeleted) {
      applyUpdatedChat({ ...chat, isDeleted: true })
      if (activeChat.value?.id === chat.id) {
        activeChat.value = { ...chat, isDeleted: true }
      }
      joinedRoomIds.delete(chat.id)
      syncSocketRooms()
    } else {
      chats.value = chats.value.filter((item) => item.id !== chat.id)
      clearUnread(chat.id)
      persistChatOrder(chats.value)
      syncSocketRooms()
      if (activeChat.value?.id === chat.id) {
        closeChat()
        if (chats.value.length > 0) {
          await openChat(chats.value[0])
        }
      }
    }

    setInfo(t("chatDeleted"))
  } catch (error) {
    setError(extractError(error).message)
  }
}

function onChatDragStart(event: DragEvent, chatId: string) {
  draggedChatId.value = chatId
  dragOverChatId.value = null
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = "move"
    event.dataTransfer.setData("text/plain", chatId)
  }
}

function onChatDragOver(event: DragEvent, chatId: string) {
  if (!draggedChatId.value || draggedChatId.value === chatId) {
    return
  }
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = "move"
  }
  dragOverChatId.value = chatId
}

function onChatDrop(chatId: string) {
  const sourceId = draggedChatId.value
  draggedChatId.value = null
  dragOverChatId.value = null

  if (!sourceId || sourceId === chatId) {
    return
  }

  const nextList = [...chats.value]
  const sourceIndex = nextList.findIndex((chat) => chat.id === sourceId)
  const targetIndex = nextList.findIndex((chat) => chat.id === chatId)

  if (sourceIndex < 0 || targetIndex < 0) {
    return
  }

  const [moved] = nextList.splice(sourceIndex, 1)
  nextList.splice(targetIndex, 0, moved)
  chats.value = nextList
  persistChatOrder(chats.value)

  if (activeChat.value) {
    const matched = chats.value.find((chat) => chat.id === activeChat.value?.id)
    if (matched) {
      activeChat.value = matched
    }
  }
}

function onChatDragEnd() {
  draggedChatId.value = null
  dragOverChatId.value = null
}

async function openChat(chat: ChatRoom) {
  if (!user.value) {
    return
  }

  settingsOpen.value = false

  activeChat.value = chat
  clearUnread(chat.id)
  chatLoading.value = true
  messages.value = []
  const includeDeleted = canViewDeletedMessages()

  try {
    const history = await apiGet<ChatMessage[]>(
      `/chats/${encodeURIComponent(chat.id)}/messages?userId=${encodeURIComponent(user.value.id)}${
        includeDeleted ? "&includeDeleted=1" : ""
      }`
    )
    const seen = new Set<string>()
    const uniqueHistory: ChatMessage[] = []

    for (const item of history) {
      if (item.isDeleted && !includeDeleted) {
        continue
      }

      const idKey = String(item.id || "")
      const dedupeKey = idKey || messageFingerprint(item)
      if (seen.has(dedupeKey)) {
        continue
      }

      seen.add(dedupeKey)
      uniqueHistory.push(item)
    }

    messages.value = uniqueHistory
    clearUnread(chat.id)
    syncSocketRooms()
    void scrollMessagesToBottomStable()
  } catch (error) {
    setError(extractError(error).message)
  } finally {
    chatLoading.value = false
  }
}

function closeChat() {
  activeChat.value = null
  messages.value = []
}

function messageFingerprint(message: ChatMessage): string {
  let rawTime = ""
  if (message.createdAt) {
    const parsed = new Date(message.createdAt)
    rawTime = Number.isNaN(parsed.getTime()) ? String(message.createdAt) : parsed.toISOString()
  }
  return [
    message.chatId,
    message.userId,
    String(message.type || "").toUpperCase(),
    message.text || "",
    message.fileUrl || "",
    message.fileName || "",
    rawTime
  ].join("|")
}

function appendMessage(message: ChatMessage) {
  if (message.isDeleted) {
    return
  }

  const incomingId = String(message.id || "")
  const incomingFingerprint = messageFingerprint(message)

  if (
    messages.value.some((item) => {
      const existingId = String(item.id || "")
      if (incomingId && existingId && existingId === incomingId) {
        return true
      }
      return messageFingerprint(item) === incomingFingerprint
    })
  ) {
    return
  }

  messages.value.push(message)
  void scrollMessagesToBottom()
}

function removeMessage(messageId: string) {
  const nextId = String(messageId || "")
  messages.value = messages.value.filter((item) => String(item.id || "") !== nextId)
}

function markMessageDeleted(messageId: string) {
  const nextId = String(messageId || "")
  const canViewDeleted = canViewDeletedMessages()
  if (!canViewDeleted) {
    removeMessage(nextId)
    return
  }

  messages.value = messages.value.map((item) => {
    if (String(item.id || "") !== nextId) {
      return item
    }
    return {
      ...item,
      isDeleted: true
    }
  })
}

function canDeleteMessage(message: ChatMessage): boolean {
  if (!user.value) {
    return false
  }
  if (message.isDeleted) {
    return false
  }
  return message.userId === user.value.id || isSuperUser.value
}

async function deleteMessage(message: ChatMessage) {
  if (!user.value || !canDeleteMessage(message)) {
    return
  }

  try {
    await apiDelete(`/messages/${encodeURIComponent(message.id)}?userId=${encodeURIComponent(user.value.id)}`)
    if (activeChat.value?.id === message.chatId && canViewDeletedMessages()) {
      markMessageDeleted(message.id)
    } else {
      removeMessage(message.id)
    }
  } catch (error) {
    setError(extractError(error).message)
  }
}

async function sendTextMessage() {
  if (!user.value || !activeChat.value) {
    return
  }

  if (activeChat.value.isDeleted) {
    setError(t("deletedChatReadonly"))
    return
  }

  const text = textMessage.value.trim()
  if (!text || sendingMessage.value) {
    return
  }

  if (!reserveClientMessageSlot()) {
    setError(t("rateLimitExceeded"))
    return
  }

  sendingMessage.value = true

  try {
    const created = await apiPost<ChatMessage>("/messages", {
      chatId: activeChat.value.id,
      userId: user.value.id,
      type: "TEXT",
      text,
      fileUrl: null,
      fileName: null
    })

    textMessage.value = ""
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      appendMessage(created)
    }
  } catch (error) {
    setError(extractError(error).message)
  } finally {
    sendingMessage.value = false
  }
}

function onComposerKeydown(event: KeyboardEvent) {
  if (event.key === "Enter" && !event.shiftKey) {
    event.preventDefault()
    void sendTextMessage()
  }
}

async function uploadFile(file: File): Promise<UploadResponse> {
  const formData = new FormData()
  formData.append("file", file)

  return await $fetch<UploadResponse>(`${apiBase.value}/upload`, {
    method: "POST",
    body: formData
  })
}

async function sendUploadedFile(file: File, forcedType?: MessageType) {
  if (!user.value || !activeChat.value) {
    return
  }

  if (activeChat.value.isDeleted) {
    setError(t("deletedChatReadonly"))
    return
  }

  if (!reserveClientMessageSlot()) {
    setError(t("rateLimitExceeded"))
    return
  }

  sendingMessage.value = true

  try {
    const uploaded = await uploadFile(file)
    const created = await apiPost<ChatMessage>("/messages", {
      chatId: activeChat.value.id,
      userId: user.value.id,
      type: forcedType || uploaded.messageType,
      text: null,
      fileUrl: uploaded.url,
      fileName: uploaded.fileName
    })

    if (!socket || socket.readyState !== WebSocket.OPEN) {
      appendMessage(created)
    }
  } catch (error) {
    setError(extractError(error).message)
  } finally {
    sendingMessage.value = false
  }
}

function openNativeFilePicker(input: HTMLInputElement | null, pendingState: { value: boolean }) {
  if (!input || pendingState.value) {
    return
  }

  pendingState.value = true
  let settled = false

  const settle = () => {
    if (settled) {
      return
    }
    settled = true
    pendingState.value = false
  }

  const onWindowFocus = () => {
    setTimeout(settle, 80)
  }

  window.addEventListener("focus", onWindowFocus, { once: true })
  setTimeout(settle, 4200)
  input.click()
}

function pickMediaFile() {
  openNativeFilePicker(mediaInputRef.value, mediaPickerPending)
}

function onMediaFileSelected(event: Event) {
  mediaPickerPending.value = false
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    void sendUploadedFile(file)
  }
  target.value = ""
}

function pickAvatarFile() {
  openNativeFilePicker(avatarInputRef.value, avatarPickerPending)
}

function onAvatarFileSelected(event: Event) {
  avatarPickerPending.value = false
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    void openImageCropper(file, "avatar")
  }
  target.value = ""
}

function pickCreateChatIconFile() {
  openNativeFilePicker(createChatIconInputRef.value, createChatIconPickerPending)
}

function onCreateChatIconSelected(event: Event) {
  createChatIconPickerPending.value = false
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    void openImageCropper(file, "create_chat_icon")
  }
  target.value = ""
}

function applyUpdatedChat(updated: ChatRoom) {
  chats.value = chats.value.map((item) => (item.id === updated.id ? updated : item))
  if (activeChat.value?.id === updated.id) {
    activeChat.value = updated
  }
  persistChatOrder(chats.value)
}

function pickActiveChatIconFile() {
  if (!canEditActiveChatIcon.value || chatIconUpdating.value || activeChatIconPickerPending.value) {
    return
  }
  openNativeFilePicker(activeChatIconInputRef.value, activeChatIconPickerPending)
}

async function onActiveChatIconSelected(event: Event) {
  activeChatIconPickerPending.value = false
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  target.value = ""

  if (!file || !user.value || !activeChat.value || !canEditActiveChatIcon.value) {
    return
  }

  void openImageCropper(file, "active_chat_icon")
}

function queueProfileSave(delayMs = 650) {
  if (profileSaveTimer) {
    clearTimeout(profileSaveTimer)
  }
  profileSaveTimer = setTimeout(() => {
    profileSaveTimer = null
    void saveProfile()
  }, delayMs)
}

async function saveProfile(options: { immediate?: boolean } = {}) {
  if (!user.value) {
    return
  }

  if (profileSaving.value) {
    if (options.immediate) {
      queueProfileSave(180)
    }
    return
  }

  const nextNickname = profileForm.nickname.trim() || user.value.nickname
  const hasNicknameChange = nextNickname !== user.value.nickname
  const hasAvatarChange = Boolean(profileForm.avatarFile)

  if (!hasNicknameChange && !hasAvatarChange) {
    return
  }

  profileSaving.value = true

  try {
    let avatarUrl = user.value.avatarUrl

    if (profileForm.avatarFile) {
      const uploaded = await uploadFile(profileForm.avatarFile)
      avatarUrl = uploaded.url
    }

    const updated = await apiPut<UserProfile>(`/users/${encodeURIComponent(user.value.id)}`, {
      nickname: nextNickname,
      avatarUrl
    })

    user.value = updated
    suppressProfileWatch = true
    profileForm.nickname = updated.nickname
    suppressProfileWatch = false
    profileForm.avatarFile = null
    cleanupAvatarPreview()
    persistUserSession(updated)
  } catch (error) {
    setError(extractError(error).message)
  } finally {
    profileSaving.value = false

    if (user.value && profileForm.nickname.trim() && profileForm.nickname.trim() !== user.value.nickname) {
      queueProfileSave(300)
    }
  }
}

function connectSocket() {
  if (!user.value) {
    return
  }

  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
    return
  }

  reconnecting.value = true
  socketConnected.value = false
  socket = new WebSocket(wsBase.value)

  socket.onopen = () => {
    socketConnected.value = true
    reconnecting.value = false
    backendHealthy.value = true
    syncSocketRooms()
  }

  socket.onmessage = (event: MessageEvent<string>) => {
    try {
      backendHealthy.value = true
      const envelope = JSON.parse(event.data) as SocketEnvelope
      if (envelope.type === "message") {
        const chatId = envelope.payload.chatId
        if (chatId === activeChat.value?.id) {
          appendMessage(envelope.payload)
          clearUnread(chatId)
        } else if (envelope.payload.userId !== user.value?.id) {
          incrementUnread(chatId)
        }
        notifyIncomingMessage(envelope.payload)
        return
      }

      if (envelope.type === "message_deleted") {
        if (envelope.payload.chatId === activeChat.value?.id) {
          if (canViewDeletedMessages()) {
            markMessageDeleted(envelope.payload.id)
          } else {
            removeMessage(envelope.payload.id)
          }
        }
        return
      }

      if (envelope.type === "chat_deleted") {
        const chatId = envelope.payload.chatId
        const existing = chats.value.find((item) => item.id === chatId)
        const shouldKeepDeleted = canViewDeletedInChat(existing)

        if (existing && shouldKeepDeleted) {
          const updated = { ...existing, isDeleted: true }
          applyUpdatedChat(updated)
          if (activeChat.value?.id === chatId) {
            activeChat.value = updated
          }
          clearUnread(chatId)
          joinedRoomIds.delete(chatId)
          syncSocketRooms()
          setInfo(t("chatDeleted"))
        } else {
          chats.value = chats.value.filter((item) => item.id !== chatId)
          clearUnread(chatId)
          persistChatOrder(chats.value)
          syncSocketRooms()
          if (activeChat.value?.id === chatId) {
            closeChat()
            setInfo(t("chatDeleted"))
            if (chats.value.length > 0) {
              void openChat(chats.value[0])
            }
          }
        }
      }
    } catch {
      // Ignore malformed ws payload.
    }
  }

  socket.onerror = () => {
    socketConnected.value = false
    reconnecting.value = true
  }

  socket.onclose = () => {
    socketConnected.value = false
    joinedRoomIds.clear()
    if (!allowReconnect) {
      reconnecting.value = false
      return
    }
    reconnecting.value = true
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
    }
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connectSocket()
    }, 1200)
  }
}

function syncSocketRooms() {
  if (!socket || socket.readyState !== WebSocket.OPEN || !user.value) {
    return
  }

  const targetRoomIds = new Set(chats.value.map((chat) => chat.id))

  for (const chatId of targetRoomIds) {
    joinSocketRoom(chatId)
  }

  for (const chatId of [...joinedRoomIds]) {
    if (!targetRoomIds.has(chatId)) {
      leaveSocketRoom(chatId)
    }
  }
}

function joinSocketRoom(chatId: string) {
  if (!socket || socket.readyState !== WebSocket.OPEN || !user.value) {
    return
  }

  if (joinedRoomIds.has(chatId)) {
    return
  }

  socket.send(JSON.stringify({ type: "join", chatId, userId: user.value.id }))
  joinedRoomIds.add(chatId)
}

function leaveSocketRoom(chatId: string) {
  if (!joinedRoomIds.has(chatId)) {
    return
  }

  if (!socket || socket.readyState !== WebSocket.OPEN) {
    joinedRoomIds.delete(chatId)
    return
  }

  socket.send(JSON.stringify({ type: "leave", chatId }))
  joinedRoomIds.delete(chatId)
}

function stopStream(stream: MediaStream | null) {
  stream?.getTracks().forEach((track) => track.stop())
}

function startAudioTimer() {
  stopAudioTimer()
  audioStartedAt = Date.now()
  audioElapsed.value = 0
  audioTicker = setInterval(() => {
    audioElapsed.value = Math.floor((Date.now() - audioStartedAt) / 1000)
  }, 200)
}

function stopAudioTimer() {
  if (!audioTicker) {
    return
  }
  clearInterval(audioTicker)
  audioTicker = null
}

function startVideoTimer() {
  stopVideoTimer()
  videoStartedAt = Date.now()
  videoElapsed.value = 0
  videoTicker = setInterval(() => {
    videoElapsed.value = Math.floor((Date.now() - videoStartedAt) / 1000)
  }, 200)
}

function stopVideoTimer() {
  if (!videoTicker) {
    return
  }
  clearInterval(videoTicker)
  videoTicker = null
}

async function sendRecordedBlob(blob: Blob, baseName: string, forcedType: MessageType) {
  const extension = blob.type.includes("mp4") ? "mp4" : forcedType === "AUDIO" ? "webm" : "webm"
  const mimeType = blob.type || (forcedType === "AUDIO" ? "audio/webm" : "video/webm")
  const file = new File([blob], `${baseName}.${extension}`, { type: mimeType })
  await sendUploadedFile(file, forcedType)
}

async function startAudioRecording() {
  if (!activeChat.value) {
    setError(t("chatNotSelected"))
    return
  }

  if (activeChat.value.isDeleted) {
    setError(t("deletedChatReadonly"))
    return
  }

  if (audioRecording.value) {
    return
  }

  try {
    audioStream = await navigator.mediaDevices.getUserMedia({ audio: true })

    const preferredMime = "audio/webm;codecs=opus"
    const canUsePreferred = MediaRecorder.isTypeSupported(preferredMime)
    audioRecorder = canUsePreferred
      ? new MediaRecorder(audioStream, { mimeType: preferredMime })
      : new MediaRecorder(audioStream)

    audioChunks = []

    audioRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        audioChunks.push(event.data)
      }
    }

    audioRecorder.onstop = () => {
      const blob = new Blob(audioChunks, {
        type: audioRecorder?.mimeType || "audio/webm"
      })
      stopAudioTimer()
      stopStream(audioStream)
      audioStream = null
      audioRecorder = null
      audioRecording.value = false
      void sendRecordedBlob(blob, `audio-${Date.now()}`, "AUDIO")
    }

    audioRecorder.start()
    audioRecording.value = true
    startAudioTimer()
  } catch {
    setError(t("microphoneDenied"))
  }
}

function stopAudioRecordingAndSend() {
  if (!audioRecorder || !audioRecording.value) {
    return
  }
  audioRecorder.stop()
}

async function startVideoRecording() {
  if (!activeChat.value) {
    setError(t("chatNotSelected"))
    return
  }

  if (activeChat.value.isDeleted) {
    setError(t("deletedChatReadonly"))
    return
  }

  if (videoRecording.value) {
    return
  }

  try {
    videoStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
    videoRecorder = new MediaRecorder(videoStream)
    videoChunks = []

    videoRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        videoChunks.push(event.data)
      }
    }

    videoRecorder.onstop = () => {
      const blob = new Blob(videoChunks, {
        type: videoRecorder?.mimeType || "video/webm"
      })
      stopVideoTimer()
      stopStream(videoStream)
      videoStream = null
      videoRecorder = null
      videoRecording.value = false
      void sendRecordedBlob(blob, `video-${Date.now()}`, "VIDEO")
    }

    videoRecorder.start()
    videoRecording.value = true
    startVideoTimer()
  } catch {
    setError(t("cameraDenied"))
  }
}

function stopVideoRecordingAndSend() {
  if (!videoRecorder || !videoRecording.value) {
    return
  }
  videoRecorder.stop()
}

function formatDuration(value: number): string {
  const total = Math.max(0, Math.floor(value))
  const minutes = Math.floor(total / 60)
  const seconds = total % 60
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`
}

function notificationPreview(message: ChatMessage): string {
  if (isTextMessage(message)) {
    return message.text || ""
  }
  if (isAudioMessage(message)) {
    return t("notificationAudio")
  }
  if (isVideoMessage(message)) {
    return t("notificationVideo")
  }
  if (hasFilePayload(message)) {
    return message.fileName || t("notificationFile")
  }
  return t("newMessage")
}

function shouldNotifyAboutMessage(message: ChatMessage): boolean {
  if (typeof window === "undefined" || !("Notification" in window)) {
    return false
  }

  if (Notification.permission !== "granted") {
    return false
  }

  if (!user.value || message.userId === user.value.id) {
    return false
  }

  const activeChatId = activeChat.value?.id
  const isActiveChat = activeChatId === message.chatId
  const tabFocused = document.visibilityState === "visible" && document.hasFocus()
  return !(isActiveChat && tabFocused)
}

function notifyIncomingMessage(message: ChatMessage) {
  if (!shouldNotifyAboutMessage(message)) {
    return
  }

  const chat = chats.value.find((item) => item.id === message.chatId)
  const title = chat?.name || t("newMessage")
  const bodyPrefix = message.senderNickname ? `${message.senderNickname}: ` : ""
  const body = `${bodyPrefix}${notificationPreview(message)}`.trim()

  try {
    const notification = new Notification(title, {
      body,
      icon: message.senderAvatarUrl || "/default-profile.svg",
      tag: `chat-${message.chatId}`
    })

    notification.onclick = () => {
      window.focus()
      if (chat) {
        void openChat(chat)
      }
      notification.close()
    }

    setTimeout(() => {
      notification.close()
    }, 6000)
  } catch {
    // Ignore notification API runtime errors.
  }
}

function formatTime(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
}

function messageDayKey(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`
}

function shouldShowMessageDate(index: number): boolean {
  if (index <= 0) {
    return true
  }
  const current = messages.value[index]
  const previous = messages.value[index - 1]
  if (!current || !previous) {
    return false
  }
  return messageDayKey(current.createdAt) !== messageDayKey(previous.createdAt)
}

function formatMessageDateLabel(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const targetStart = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
  const dayMs = 24 * 60 * 60 * 1000
  if (targetStart === todayStart) {
    return t("today")
  }
  if (targetStart === todayStart - dayMs) {
    return t("yesterday")
  }

  const localeName = locale.value === "ru" ? "ru-RU" : "en-US"
  return date.toLocaleDateString(localeName, {
    day: "2-digit",
    month: "long",
    year: "numeric"
  })
}

function normalizedMessageType(message: ChatMessage): string {
  return String(message.type || "").trim().toUpperCase()
}

function hasTextPayload(message: ChatMessage): boolean {
  return Boolean(message.text && message.text.trim().length > 0)
}

function hasFilePayload(message: ChatMessage): boolean {
  return Boolean(message.fileUrl || message.fileName)
}

function isAudioMessage(message: ChatMessage): boolean {
  if (normalizedMessageType(message) === "AUDIO") {
    return true
  }
  const source = `${message.fileName || ""} ${message.fileUrl || ""}`.toLowerCase()
  return /\.(mp3|m4a|aac|ogg|oga|opus|wav|flac|webm)(\?.*)?$/i.test(source)
}

function isVideoMessage(message: ChatMessage): boolean {
  if (normalizedMessageType(message) === "VIDEO") {
    return true
  }
  const source = `${message.fileName || ""} ${message.fileUrl || ""}`.toLowerCase()
  return /\.(mp4|mov|m4v|webm|mkv|avi|wmv)(\?.*)?$/i.test(source)
}

function isTextMessage(message: ChatMessage): boolean {
  return hasTextPayload(message)
}

function mediaLabel(message: ChatMessage): string {
  if (isAudioMessage(message)) {
    return "Audio"
  }
  if (isVideoMessage(message)) {
    return "Video"
  }
  return "File"
}

function isImageAttachment(message: ChatMessage): boolean {
  if (!hasFilePayload(message)) {
    return false
  }

  const source = `${message.fileName || ""} ${message.fileUrl || ""}`.toLowerCase()
  return /\.(png|jpe?g|gif|webp|bmp|svg|avif|heic|heif|jfif)(\?.*)?$/i.test(source)
}

function messageAvatarUrl(message: ChatMessage): string {
  return message.senderAvatarUrl || "/default-profile.svg"
}

function onMessageAvatarError(event: Event) {
  const image = event.target as HTMLImageElement
  if (image.src.endsWith("/default-profile.svg")) {
    return
  }
  image.src = "/default-profile.svg"
}

async function scrollMessagesToBottom() {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function scrollMessagesToBottomStable() {
  await scrollMessagesToBottom()
  for (const delay of [70, 180, 360]) {
    await sleep(delay)
    await scrollMessagesToBottom()
  }
}

function resizeComposerInput() {
  const textarea = composerInputRef.value
  if (!textarea) {
    return
  }

  textarea.style.height = "34px"
  const nextHeight = Math.min(textarea.scrollHeight, 136)
  textarea.style.height = `${Math.max(34, nextHeight)}px`
}

watch(
  () => profileForm.nickname,
  () => {
    if (suppressProfileWatch || !user.value) {
      return
    }
    queueProfileSave(600)
  }
)

watch(
  () => joinForm.name,
  () => {
    joinPasswordRequired.value = false
    pendingJoinName.value = ""
    joinForm.password = ""
  }
)

watch(
  () => textMessage.value,
  () => {
    void nextTick(() => {
      resizeComposerInput()
    })
  }
)

watch(
  () => imageCropZoom.value,
  () => {
    clampImageCropOffsets()
  }
)

watch(
  () => imageCropViewSize.value,
  () => {
    clampImageCropOffsets()
  }
)

onMounted(async () => {
  appLoading.value = true
  startViewportWatcher()
  capturePendingAdminKeyFromUrl()

  const savedLocale = getStorage().getItem(LS_LOCALE)
  if (savedLocale === "ru" || savedLocale === "en") {
    locale.value = savedLocale
  } else {
    setLocale("en")
  }

  const savedTheme = getStorage().getItem(LS_THEME)
  if (savedTheme === "dark" || savedTheme === "light") {
    theme.value = savedTheme
  } else {
    setTheme("light")
  }

  unreadByChat.value = readUnreadMap()

  try {
    startHealthChecks()
    const restored = await restoreUserFromStorage()
    if (restored) {
      onboardingVisible.value = false
      await initializeSessionAfterAuth()
    } else {
      onboardingVisible.value = true
    }
    await nextTick()
    resizeComposerInput()
  } catch (error) {
    setError(extractError(error).message)
  } finally {
    appLoading.value = false
  }
})

onUnmounted(() => {
  stopViewportWatcher()
  allowReconnect = false
  reconnecting.value = false
  socketConnected.value = false
  stopHealthChecks()
  stopQrPolling()
  void stopQrScanner()

  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (profileSaveTimer) {
    clearTimeout(profileSaveTimer)
    profileSaveTimer = null
  }

  closeRealtimeConnection()

  if (audioRecorder && audioRecording.value) {
    audioRecorder.stop()
  }
  if (videoRecorder && videoRecording.value) {
    videoRecorder.stop()
  }

  stopAudioTimer()
  stopVideoTimer()
  cleanupAvatarPreview()
  cleanupCreateChatIconPreview()
  resetImageCropState()
  stopStream(audioStream)
  stopStream(videoStream)
})
</script>

<template>
  <main class="messenger-root" :class="{ 'theme-dark': theme === 'dark', 'onboarding-mode': showOnboarding }">
    <aside class="sidebar">
      <header class="sidebar-header">
        <div class="sidebar-actions">
          <button
            v-if="isAuthenticated"
            class="icon-btn"
            :class="{ active: profilePanelOpen }"
            type="button"
            :title="t('profile')"
            @click="toggleProfilePanel"
          >
            <AppIcon name="user" />
          </button>
          <button
            v-if="isAuthenticated"
            class="icon-btn"
            :class="{ active: createChatPanelOpen }"
            type="button"
            :title="t('createChat')"
            @click="toggleCreateChatPanel"
          >
            <AppIcon name="plus" />
          </button>
          <button class="icon-btn" type="button" :title="t('settings')" @click="settingsOpen = true">
            <AppIcon name="settings" />
          </button>
          <button
            v-if="isAuthenticated"
            class="icon-btn"
            type="button"
            :disabled="qrSessionLoading"
            :title="t('showQr')"
            @click="openQrModal"
          >
            <i class="fa-solid fa-qrcode" aria-hidden="true" />
          </button>
        </div>
      </header>

      <div v-if="errorMessage" class="banner banner-error">{{ errorMessage }}</div>
      <div v-if="infoMessage" class="banner banner-info">{{ infoMessage }}</div>

      <div class="sidebar-content" :class="{ 'panel-open': sidebarPanelOpen }">
        <section v-if="appLoading" class="card loading-card">{{ t("loading") }}</section>

        <template v-else>
          <section v-if="showOnboarding" class="card onboarding-card">
            <h2>{{ t("onboardingTitle") }}</h2>
            <p class="onboarding-description">{{ t("onboardingDescription") }}</p>
            <div class="field-col">
              <button class="btn btn-light" type="button" :disabled="qrScannerLoading" @click="openQrScannerModal">
                {{ qrScannerLoading ? t("linkInProgress") : t("scanQr") }}
              </button>
              <button class="btn" type="button" :disabled="onboardingCreatingProfile" @click="createNewProfile">
                {{ onboardingCreatingProfile ? t("createProfileInProgress") : t("createNewProfile") }}
              </button>
            </div>
          </section>

          <template v-else>
            <section v-if="profilePanelOpen" class="card profile-card">
            <h2>{{ t("profile") }}</h2>
            <div class="profile-row">
              <button class="avatar-tap" :class="{ 'picker-pending': avatarPickerPending }" type="button" :disabled="avatarPickerPending" @click="pickAvatarFile">
                <img class="avatar" :src="profileAvatar" alt="avatar" @error="onProfileAvatarError" />
                <span class="avatar-badge">
                  <AppIcon :name="avatarPickerPending ? 'refresh' : 'camera'" :class="{ spin: avatarPickerPending }" :size="14" />
                </span>
              </button>
              <div>
                <div class="profile-name-row">
                  <div class="profile-name">{{ user?.nickname }}</div>
                  <span v-if="profileIdTail" class="profile-id-tail">{{ profileIdTail }}</span>
                </div>
                <div class="profile-id">ID: {{ user?.id }}</div>
              </div>
            </div>

            <div class="field-col">
              <input v-model="profileForm.nickname" type="text" :placeholder="t('nickname')" />
              <p class="profile-sync" :class="{ saving: profileSaving }">
                {{ profileSaving ? t("profileSyncing") : t("changeAvatarHint") }}
              </p>
              <input
                ref="avatarInputRef"
                type="file"
                accept="image/*"
                style="display: none"
                @change="onAvatarFileSelected"
              />
            </div>
          </section>

            <section v-if="createChatPanelOpen" class="card">
            <h2>{{ t("createChat") }}</h2>
            <div class="field-col">
              <div class="chat-icon-picker-row">
                <button
                  class="avatar-tap chat-icon-tap"
                  :class="{ 'picker-pending': createChatIconPickerPending }"
                  type="button"
                  :title="t('chatIcon')"
                  :disabled="createChatIconPickerPending"
                  @click="pickCreateChatIconFile"
                >
                  <img class="chat-avatar chat-avatar-large" :src="createChatIconPreview" alt="chat icon" @error="onChatIconError" />
                  <span class="avatar-badge">
                    <AppIcon :name="createChatIconPickerPending ? 'refresh' : 'camera'" :class="{ spin: createChatIconPickerPending }" :size="14" />
                  </span>
                </button>
                <p class="chat-icon-hint">{{ t("chatIconHint") }}</p>
              </div>
              <input v-model="createForm.name" type="text" :placeholder="t('chatNameMin')" />
              <input
                v-model="createForm.password"
                type="text"
                :placeholder="t('optionalPassword')"
                autocomplete="new-password"
                name="chat-create-password"
                spellcheck="false"
                data-lpignore="true"
                data-1p-ignore="true"
              />
              <input
                ref="createChatIconInputRef"
                type="file"
                accept="image/*"
                style="display: none"
                @change="onCreateChatIconSelected"
              />
              <button class="btn" type="button" :disabled="!canCreateChat" @click="createChat">
                {{ t("createChatBtn") }}
              </button>
            </div>
          </section>

            <section class="join-panel">
            <div class="join-panel-body">
              <div class="join-name-field">
                <input v-model="joinForm.name" type="text" :placeholder="t('chatName')" />
                <span class="join-name-icon">
                  <AppIcon name="search" :size="14" />
                </span>
              </div>
              <input
                v-if="joinPasswordStep"
                v-model="joinForm.password"
                type="text"
                :placeholder="t('chatPassword')"
                autocomplete="new-password"
                name="chat-join-password"
                spellcheck="false"
                data-lpignore="true"
                data-1p-ignore="true"
              />
              <button v-if="showJoinConnectButton" class="join-connect-btn" type="button" :disabled="!canJoinChat" @click="joinChat">
                {{ joiningChat ? t("connecting") : t("connect") }}
              </button>
            </div>
          </section>

            <section class="chats-panel">
            <div class="chat-list">
              <article
                v-for="chat in chats"
                :key="chat.id"
                class="chat-item"
                :class="{
                  active: activeChat?.id === chat.id,
                  unread: unreadCount(chat.id) > 0,
                  deleted: chat.isDeleted,
                  dragging: draggedChatId === chat.id,
                  'drag-over': dragOverChatId === chat.id
                }"
                draggable="true"
                @click="openChat(chat)"
                @dragstart="onChatDragStart($event, chat.id)"
                @dragover="onChatDragOver($event, chat.id)"
                @drop.prevent="onChatDrop(chat.id)"
                @dragend="onChatDragEnd"
              >
                <div class="chat-item-head">
                  <div class="chat-title-wrap">
                    <img class="chat-avatar chat-avatar-small" :src="chatIconUrl(chat)" alt="chat icon" loading="lazy" @error="onChatIconError" />
                    <p class="chat-title">{{ chat.name }}</p>
                  </div>
                  <div class="chat-item-side">
                    <span v-if="unreadCount(chat.id) > 0" class="chat-unread-badge">{{ unreadCount(chat.id) }}</span>
                    <button
                      v-if="canManageChat(chat) && !chat.isDeleted"
                      class="msg-delete chat-delete"
                      type="button"
                      :title="t('deleteChat')"
                      @click.stop="deleteChat(chat)"
                    >
                      <AppIcon name="trash" :size="15" />
                    </button>
                  </div>
                </div>
                <p class="chat-meta">
                  {{ chat.isDeleted ? t("deletedChat") : (chat.hasPassword ? t("privateChat") : t("publicChat")) }}
                </p>
              </article>
              <p v-if="chats.length === 0" class="empty-hint">{{ t("noChats") }}</p>
            </div>
          </section>
          </template>
        </template>
      </div>
    </aside>

    <section v-if="!hideChatBoardInOnboarding" class="chat-board">
      <header class="chat-header">
        <div class="chat-header-main">
          <template v-if="settingsOpen">
            <h2>{{ t("settingsTitle") }}</h2>
          </template>
          <template v-else-if="activeChat">
            <button
              class="chat-header-icon-btn"
              type="button"
                :disabled="!canEditActiveChatIcon || chatIconUpdating || activeChatIconPickerPending"
                :class="{ 'picker-pending': activeChatIconPickerPending }"
                :title="canEditActiveChatIcon ? t('chatIcon') : ''"
                @click="pickActiveChatIconFile"
              >
                <img class="chat-avatar chat-avatar-header" :src="chatIconUrl(activeChat)" alt="chat icon" @error="onChatIconError" />
                <span v-if="canEditActiveChatIcon" class="avatar-badge">
                <AppIcon :name="activeChatIconPickerPending ? 'refresh' : 'camera'" :class="{ spin: activeChatIconPickerPending }" :size="13" />
                </span>
              </button>
            <h2>{{ activeChat.name }}</h2>
            <input
              ref="activeChatIconInputRef"
              type="file"
              accept="image/*"
              style="display: none"
              @change="onActiveChatIconSelected"
            />
          </template>
          <template v-else>
            <h2>{{ t("selectChat") }}</h2>
          </template>
        </div>
        <button v-if="settingsOpen" class="btn btn-light" type="button" @click="settingsOpen = false">
          {{ t("close") }}
        </button>
        <button
          v-else
          class="icon-btn chat-close-btn"
          type="button"
          :disabled="!activeChat"
          :title="t('closeChat')"
          @click="closeChat"
        >
          <AppIcon name="close" :size="15" />
        </button>
      </header>

      <template v-if="showOnboarding && !settingsOpen">
        <section class="onboarding-main">
          <h3>{{ t("onboardingTitle") }}</h3>
          <p>{{ t("onboardingDescription") }}</p>
          <div class="onboarding-main-actions">
            <button class="btn btn-light" type="button" :disabled="qrScannerLoading" @click="openQrScannerModal">
              {{ qrScannerLoading ? t("linkInProgress") : t("scanQr") }}
            </button>
            <button class="btn" type="button" :disabled="onboardingCreatingProfile" @click="createNewProfile">
              {{ onboardingCreatingProfile ? t("createProfileInProgress") : t("createNewProfile") }}
            </button>
          </div>
        </section>
      </template>

      <template v-else-if="settingsOpen">
        <section class="settings-pane">
          <article class="settings-section">
            <p class="settings-label">{{ t("language") }}</p>
            <div class="language-row">
              <button class="btn btn-light" :class="{ active: locale === 'en' }" type="button" @click="setLocale('en')">
                {{ t("english") }}
              </button>
              <button class="btn btn-light" :class="{ active: locale === 'ru' }" type="button" @click="setLocale('ru')">
                {{ t("russian") }}
              </button>
            </div>
          </article>

          <article class="settings-section">
            <p class="settings-label">{{ t("theme") }}</p>
            <div class="theme-row">
              <button class="btn btn-light theme-btn" :class="{ active: theme === 'light' }" type="button" @click="setTheme('light')">
                <AppIcon name="sun" :size="16" />
                <span>{{ t("lightTheme") }}</span>
              </button>
              <button class="btn btn-light theme-btn" :class="{ active: theme === 'dark' }" type="button" @click="setTheme('dark')">
                <AppIcon name="moon" :size="16" />
                <span>{{ t("darkTheme") }}</span>
              </button>
            </div>
          </article>

          <article v-if="isAuthenticated" class="settings-section">
            <button class="btn btn-light settings-logout-btn" type="button" @click="logout">
              <i class="fa-solid fa-right-from-bracket" aria-hidden="true" />
              <span>{{ t("logout") }}</span>
            </button>
          </article>
        </section>
      </template>

      <template v-else>
        <div v-if="activeChat" class="connection-indicator" :class="`state-${connectionStateKey}`">
          <span class="connection-dot" />
          <span>{{ t(connectionStateKey) }}</span>
        </div>

        <section ref="messageListRef" class="message-list">
          <div v-if="chatLoading" class="state-line">{{ t("loadingMessages") }}</div>
          <div v-else-if="!activeChat" class="state-line">{{ t("selectChat") }}</div>
          <div v-else-if="messages.length === 0" class="state-line">{{ t("noMessages") }}</div>

          <template v-else>
            <template v-for="(message, index) in messages" :key="`message-${message.id || index}`">
              <div v-if="shouldShowMessageDate(index)" class="message-date-divider">
                {{ formatMessageDateLabel(message.createdAt) }}
              </div>
              <article
                class="message-item"
                :class="{ own: message.userId === user?.id, audio: !message.isDeleted && isAudioMessage(message), deleted: message.isDeleted }"
              >
            <div v-if="!isAudioMessage(message)" class="msg-head">
              <span class="msg-author">
                <img
                  class="msg-author-avatar"
                  :src="messageAvatarUrl(message)"
                  alt="avatar"
                  loading="lazy"
                  @error="onMessageAvatarError"
                />
                <span>{{ message.senderNickname }}</span>
                <span v-if="message.userId" class="msg-author-id-tail">{{ String(message.userId).slice(-12) }}</span>
              </span>
              <button
                v-if="canDeleteMessage(message)"
                class="msg-delete"
                type="button"
                :title="t('deleteMessage')"
                @click="deleteMessage(message)"
              >
                <AppIcon name="trash" :size="14" />
              </button>
            </div>

            <p v-if="isTextMessage(message)" class="msg-text">{{ message.text }}</p>

            <AudioMessagePlayer
              v-else-if="isAudioMessage(message)"
              :src="message.fileUrl || ''"
              :avatar-url="message.senderAvatarUrl"
              :nickname="message.senderNickname"
            />

            <VideoMessagePlayer
              v-else-if="isVideoMessage(message)"
              :src="message.fileUrl || ''"
            />

            <a
              v-else-if="isImageAttachment(message)"
              class="msg-image-link"
              :href="message.fileUrl || '#'"
              target="_blank"
              rel="noopener noreferrer"
            >
              <img class="msg-image" :src="message.fileUrl || ''" :alt="message.fileName || 'image'" loading="lazy" />
            </a>

            <a
              v-else-if="hasFilePayload(message)"
              class="msg-file"
              :href="message.fileUrl || '#'"
              target="_blank"
              rel="noopener noreferrer"
            >
              <AppIcon name="paperclip" :size="14" />
              {{ message.fileName ? `${mediaLabel(message)}: ${message.fileName}` : mediaLabel(message) }}
            </a>

            <p v-else class="msg-text">{{ message.text || "" }}</p>

            <div v-if="isAudioMessage(message)" class="audio-inline-meta">
              <span v-if="message.userId" class="msg-author-id-tail audio-id-tail">{{ String(message.userId).slice(-12) }}</span>
              <span class="msg-time in-audio">{{ formatTime(message.createdAt) }}</span>
              <button
                v-if="canDeleteMessage(message)"
                class="msg-delete audio-delete"
                type="button"
                :title="t('deleteMessage')"
                @click="deleteMessage(message)"
              >
                <AppIcon name="trash" :size="14" />
              </button>
            </div>

            <div v-if="!isAudioMessage(message)" class="msg-time">{{ formatTime(message.createdAt) }}</div>
              </article>
            </template>
          </template>
        </section>

        <footer class="composer">
          <div class="composer-inline">
            <button
              class="composer-action"
              type="button"
              :disabled="!activeChat || activeChatDeleted || sendingMessage || mediaPickerPending"
              :class="{ 'picker-pending': mediaPickerPending }"
              :title="t('attachFile')"
              @click="pickMediaFile"
            >
              <AppIcon :name="mediaPickerPending ? 'refresh' : 'plus'" :class="{ spin: mediaPickerPending }" :size="19" :stroke-width="2.2" />
            </button>

            <textarea
              ref="composerInputRef"
              v-model="textMessage"
              rows="1"
              :disabled="!activeChat || activeChatDeleted"
              :placeholder="t('typeMessage')"
              @input="resizeComposerInput"
              @keydown="onComposerKeydown"
            />

            <button
              v-if="hasDraftMessage"
              class="composer-send-btn"
              type="button"
              :disabled="!activeChat || activeChatDeleted || sendingMessage"
              :title="t('send')"
              @click="sendTextMessage"
            >
              <AppIcon name="send" :size="18" :stroke-width="2.4" />
            </button>

            <button
              v-else
              class="composer-action"
              :class="{ recording: audioRecording }"
              type="button"
              :disabled="!activeChat || activeChatDeleted || sendingMessage"
              :title="t('microphone')"
              @click="audioRecording ? stopAudioRecordingAndSend() : startAudioRecording()"
            >
              <AppIcon :name="audioRecording ? 'pause' : 'mic'" :size="19" :stroke-width="2.1" />
            </button>
          </div>

          <input ref="mediaInputRef" type="file" style="display: none" @change="onMediaFileSelected" />

          <div class="composer-bottom">
            <span class="status">{{
              activeChatDeleted
                ? t("deletedChatReadonly")
                : (videoRecording ? `${t("videoRecording")} ${formatDuration(videoElapsed)}` : (sendingMessage ? t("statusSending") : ""))
            }}</span>
          </div>
        </footer>
      </template>
    </section>

    <div v-if="audioRecording" class="record-overlay">
      <div class="record-card">
        <div class="record-pulse" />
        <p class="record-title">{{ t("recording") }}</p>
        <p class="record-time">{{ formatDuration(audioElapsed) }}</p>
        <p class="record-hint">{{ t("recOverlayHint") }}</p>
        <button class="btn" type="button" @click="stopAudioRecordingAndSend">{{ t("pauseSend") }}</button>
      </div>
    </div>

    <div v-if="imageCropModalOpen" class="crop-modal-backdrop" @click.self="closeImageCropper">
      <div class="crop-modal">
        <div class="qr-modal-head">
          <h3>{{ t("imageCropTitle") }}</h3>
          <button
            class="icon-btn small"
            type="button"
            :title="t('close')"
            :disabled="imageCropApplying"
            @click="closeImageCropper"
          >
            <AppIcon name="close" :size="14" />
          </button>
        </div>

        <p class="qr-modal-subtitle">{{ t("imageCropHint") }}</p>

        <div
          ref="imageCropAreaRef"
          class="crop-area"
          :class="{ dragging: imageCropDragging }"
          @pointerdown="onImageCropPointerDown"
          @pointermove="onImageCropPointerMove"
          @pointerup="onImageCropPointerEnd"
          @pointercancel="onImageCropPointerEnd"
        >
          <img
            v-if="imageCropDataUrl"
            class="crop-image"
            :src="imageCropDataUrl"
            :style="imageCropImageStyle"
            alt="crop preview"
            draggable="false"
          />
          <div class="crop-mask" />
        </div>

        <label class="crop-zoom-row">
          <span>{{ t("imageCropZoom") }}</span>
          <input v-model.number="imageCropZoom" type="range" min="1" max="3.5" step="0.01" />
        </label>

        <div class="qr-actions">
          <button class="btn btn-light" type="button" :disabled="imageCropApplying" @click="closeImageCropper">
            {{ t("close") }}
          </button>
          <button class="btn" type="button" :disabled="imageCropApplying" @click="applyImageCrop">
            {{ t("imageCropApply") }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="qrScannerModalOpen" class="qr-modal-backdrop" @click.self="closeQrScannerModal">
      <div class="qr-modal">
        <div class="qr-modal-head">
          <h3>{{ t("scanQr") }}</h3>
          <button class="icon-btn small" type="button" :title="t('close')" @click="closeQrScannerModal">
            <AppIcon name="close" :size="14" />
          </button>
        </div>

        <p class="qr-modal-subtitle">{{ t("scanToConnect") }}</p>

        <div class="qr-scan-frame">
          <div :id="qrReaderElementId" class="qr-reader" />
          <p v-if="qrScannerLoading" class="qr-reader-status">{{ t("linkInProgress") }}</p>
        </div>

        <p v-if="qrScannerError" class="qr-expire expired">{{ qrScannerError }}</p>

        <div class="qr-actions">
          <button class="btn btn-light" type="button" @click="restartQrScanner">
            {{ t("scanQr") }}
          </button>
          <button class="btn" type="button" @click="closeQrScannerModal">
            {{ t("close") }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="qrModalOpen" class="qr-modal-backdrop" @click.self="closeQrModal">
      <div class="qr-modal">
        <div class="qr-modal-head">
          <h3>{{ t("showQr") }}</h3>
          <button class="icon-btn small" type="button" :title="t('close')" @click="closeQrModal">
            <AppIcon name="close" :size="14" />
          </button>
        </div>

        <p class="qr-modal-subtitle">{{ t("scanToLink") }}</p>

        <div class="qr-frame">
          <p v-if="qrSessionLoading">{{ t("qrGenerating") }}</p>
          <img v-else-if="qrImageDataUrl" :src="qrImageDataUrl" alt="QR" />
          <p v-else>{{ t("networkError") }}</p>
        </div>

        <p v-if="qrSession && !qrIsExpired" class="qr-expire">
          {{ t("qrExpiresAt") }}: {{ formatTime(qrSession.expiresAt) }}
        </p>
        <p v-if="qrIsExpired" class="qr-expire expired">{{ t("qrExpired") }}</p>

        <div class="qr-actions">
          <button class="btn btn-light" type="button" @click="openQrModal">
            {{ t("newQr") }}
          </button>
          <button class="btn" type="button" @click="closeQrModal">
            {{ t("close") }}
          </button>
        </div>
      </div>
    </div>

  </main>
</template>
