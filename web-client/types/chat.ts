export type MessageType = "TEXT" | "FILE" | "AUDIO" | "VIDEO"

export interface UserProfile {
  id: string
  nickname: string
  avatarUrl: string | null
  createdAt: string
  isSuperAdmin?: boolean
}

export interface ChatRoom {
  id: string
  name: string
  iconUrl: string | null
  hasPassword: boolean
  createdBy: string
  createdAt: string
  membersCount: number
  isDeleted?: boolean
}

export interface ChatMessage {
  id: string
  chatId: string
  userId: string
  senderNickname: string
  senderAvatarUrl: string | null
  type: MessageType
  text: string | null
  fileUrl: string | null
  fileName: string | null
  createdAt: string
  isDeleted?: boolean
  deletedAt?: string | null
  deletedBy?: string | null
}

export interface UploadResponse {
  url: string
  fileName: string
  mimeType: string
  messageType: MessageType
}

export interface ApiError {
  message?: string
}
