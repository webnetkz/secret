require("dotenv").config();

const express = require("express");
const cors = require("cors");
const multer = require("multer");
const bcrypt = require("bcryptjs");
const http = require("http");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");
const mysql = require("mysql2/promise");
const { WebSocketServer } = require("ws");

const PORT = Number(process.env.PORT || 3100);
const HOST = process.env.HOST || "0.0.0.0";

const DB_HOST = process.env.DB_HOST || "127.0.0.1";
const DB_PORT = Number(process.env.DB_PORT || 3306);
const DB_USER = process.env.DB_USER || "root";
const DB_PASSWORD = process.env.DB_PASSWORD || "";
const DB_NAME = process.env.DB_NAME || "secret_chat";
const DB_CONNECTION_LIMIT = Number(process.env.DB_CONNECTION_LIMIT || 10);
const DB_AUTO_CREATE = process.env.DB_AUTO_CREATE !== "false";

const DATA_DIR = path.join(__dirname, "..", "data");
const STORE_PATH = path.join(DATA_DIR, "store.json");
const UPLOAD_DIR = path.join(__dirname, "..", "uploads");

const MESSAGE_TYPE = {
  TEXT: "TEXT",
  FILE: "FILE",
  AUDIO: "AUDIO",
  VIDEO: "VIDEO"
};

const MESSAGE_RATE_LIMIT_PER_SECOND = 5;
const MESSAGE_RATE_WINDOW_MS = 1000;
const HTML_LIKE_EXTENSIONS = new Set([".html", ".htm", ".xhtml", ".shtml", ".mhtml"]);
const PROFILE_LINK_SESSION_TTL_MS = Math.max(
  60_000,
  Number(process.env.PROFILE_LINK_SESSION_TTL_MS || 5 * 60 * 1000)
);
const PROFILE_LINK_STATUS = {
  PENDING: "PENDING",
  COMPLETED: "COMPLETED",
  EXPIRED: "EXPIRED"
};

let pool = null;

function nowIso() {
  return new Date().toISOString();
}

function nowMs() {
  return Date.now();
}

function addMsToIso(ms) {
  return new Date(nowMs() + ms).toISOString();
}

function randomIdentifier() {
  const randomPart = crypto.randomBytes(3).toString("hex");
  return `guest-${randomPart}`;
}

function safeTrim(value) {
  if (typeof value !== "string") {
    return "";
  }
  return value.trim();
}

const SUPER_USER_IDS = new Set(
  safeTrim(process.env.SUPER_USER_IDS)
    .split(",")
    .map((value) => safeTrim(value))
    .filter(Boolean)
);
const ADMIN_ACTIVATION_KEY = safeTrim(process.env.KEY_ADMIN || process.env.KEY_AMIND);

function isSuperUser(userId) {
  return SUPER_USER_IDS.has(safeTrim(userId));
}

function detectMessageTypeByMime(mimeType) {
  if (typeof mimeType !== "string") {
    return MESSAGE_TYPE.FILE;
  }
  if (mimeType.startsWith("audio/")) {
    return MESSAGE_TYPE.AUDIO;
  }
  if (mimeType.startsWith("video/")) {
    return MESSAGE_TYPE.VIDEO;
  }
  return MESSAGE_TYPE.FILE;
}

function isHtmlLikeUpload(file) {
  const ext = path.extname(file?.originalname || "").toLowerCase();
  const mime = safeTrim(file?.mimetype).toLowerCase();
  return (
    HTML_LIKE_EXTENSIONS.has(ext) ||
    mime === "text/html" ||
    mime === "application/xhtml+xml"
  );
}

function buildSafeUploadExtension(file) {
  const ext = path.extname(file?.originalname || "").toLowerCase();
  if (!isHtmlLikeUpload(file)) {
    return ext;
  }
  return ext ? `${ext}.txt` : ".html.txt";
}

function buildSafeUploadDisplayName(file) {
  const original = safeTrim(file?.originalname);
  if (!isHtmlLikeUpload(file)) {
    return original || "file";
  }
  if (!original) {
    return "file.html.txt";
  }
  return original.toLowerCase().endsWith(".txt") ? original : `${original}.txt`;
}

function isLegacyHtmlUploadFile(filePath) {
  const lowerName = path.basename(filePath || "").toLowerCase();
  return (
    lowerName.endsWith(".html") ||
    lowerName.endsWith(".htm") ||
    lowerName.endsWith(".xhtml") ||
    lowerName.endsWith(".shtml") ||
    lowerName.endsWith(".mhtml")
  );
}

function buildProfileLinkQrText(sessionId) {
  return `secretchat://profile-link?session=${encodeURIComponent(sessionId)}`;
}

function isIsoExpired(isoValue) {
  const expiresAt = new Date(isoValue);
  if (Number.isNaN(expiresAt.getTime())) {
    return true;
  }
  return expiresAt.getTime() <= nowMs();
}

function mapChatResponse(chat) {
  return {
    id: chat.id,
    name: chat.name,
    iconUrl: safeTrim(chat.iconUrl) || null,
    hasPassword: Boolean(chat.passwordHash),
    createdBy: chat.createdBy,
    createdAt: chat.createdAt,
    membersCount: Number(chat.membersCount || 0),
    isDeleted: Boolean(chat.isDeleted)
  };
}

function mapUserResponse(user) {
  return {
    id: user.id,
    nickname: user.nickname,
    avatarUrl: user.avatarUrl || null,
    createdAt: user.createdAt,
    isSuperAdmin: Boolean(user.isSuperAdmin) || isSuperUser(user.id)
  };
}

function mapMessageResponse(message) {
  return {
    id: message.id,
    chatId: message.chatId,
    userId: message.userId,
    senderNickname: message.senderNickname,
    senderAvatarUrl: message.senderAvatarUrl,
    type: message.type,
    text: message.text,
    fileUrl: message.fileUrl,
    fileName: message.fileName,
    createdAt: message.createdAt,
    isDeleted: Boolean(message.isDeleted),
    deletedAt: message.deletedAt || null,
    deletedBy: message.deletedBy || null
  };
}

function asyncHandler(handler) {
  return (req, res) => {
    Promise.resolve(handler(req, res)).catch((error) => {
      console.error("Unhandled API error", error);
      if (!res.headersSent) {
        res.status(500).json({ message: "Internal server error" });
      }
    });
  };
}

function createFileStorage() {
  if (!fs.existsSync(UPLOAD_DIR)) {
    fs.mkdirSync(UPLOAD_DIR, { recursive: true });
  }

  return multer.diskStorage({
    destination: (_req, _file, callback) => {
      callback(null, UPLOAD_DIR);
    },
    filename: (_req, file, callback) => {
      const ext = buildSafeUploadExtension(file);
      const uniqueName = `${Date.now()}-${crypto.randomUUID()}${ext}`;
      callback(null, uniqueName);
    }
  });
}

async function initializeDatabase() {
  if (DB_AUTO_CREATE) {
    const bootstrapConnection = await mysql.createConnection({
      host: DB_HOST,
      port: DB_PORT,
      user: DB_USER,
      password: DB_PASSWORD
    });

    try {
      const escapedDbName = DB_NAME.replace(/`/g, "``");
      await bootstrapConnection.query(
        `CREATE DATABASE IF NOT EXISTS \`${escapedDbName}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`
      );
    } finally {
      await bootstrapConnection.end();
    }
  }

  pool = mysql.createPool({
    host: DB_HOST,
    port: DB_PORT,
    user: DB_USER,
    password: DB_PASSWORD,
    database: DB_NAME,
    connectionLimit: DB_CONNECTION_LIMIT,
    waitForConnections: true,
    charset: "utf8mb4"
  });

  await createSchema();
  await migrateFromStoreIfNeeded();
  await hydrateSuperUsersFromDb();
}

async function createSchema() {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS users (
      id CHAR(36) NOT NULL,
      nickname VARCHAR(120) NOT NULL,
      avatar_url TEXT NULL,
      created_at VARCHAR(40) NOT NULL,
      is_super_admin TINYINT(1) NOT NULL DEFAULT 0,
      PRIMARY KEY (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  `);

  await pool.query(`
    CREATE TABLE IF NOT EXISTS chats (
      id CHAR(36) NOT NULL,
      name VARCHAR(255) NOT NULL,
      icon_url TEXT NULL,
      password_hash VARCHAR(255) NULL,
      created_by CHAR(36) NOT NULL,
      created_at VARCHAR(40) NOT NULL,
      is_deleted TINYINT(1) NOT NULL DEFAULT 0,
      active_name VARCHAR(255) GENERATED ALWAYS AS (
        CASE WHEN is_deleted = 0 THEN name ELSE NULL END
      ) STORED,
      deleted_at VARCHAR(40) NULL,
      deleted_by CHAR(36) NULL,
      PRIMARY KEY (id),
      UNIQUE KEY uq_chats_active_name (active_name),
      KEY idx_chats_created_by (created_by),
      CONSTRAINT fk_chats_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
      CONSTRAINT fk_chats_deleted_by FOREIGN KEY (deleted_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  `);

  await pool.query(`
    CREATE TABLE IF NOT EXISTS chat_members (
      chat_id CHAR(36) NOT NULL,
      user_id CHAR(36) NOT NULL,
      joined_at VARCHAR(40) NOT NULL,
      PRIMARY KEY (chat_id, user_id),
      KEY idx_chat_members_user (user_id),
      CONSTRAINT fk_chat_members_chat FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE ON UPDATE CASCADE,
      CONSTRAINT fk_chat_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  `);

  await pool.query(`
    CREATE TABLE IF NOT EXISTS messages (
      id CHAR(36) NOT NULL,
      chat_id CHAR(36) NOT NULL,
      user_id CHAR(36) NOT NULL,
      sender_nickname VARCHAR(120) NOT NULL,
      sender_avatar_url TEXT NULL,
      type ENUM('TEXT', 'FILE', 'AUDIO', 'VIDEO') NOT NULL,
      message_text TEXT NULL,
      file_url TEXT NULL,
      file_name VARCHAR(512) NULL,
      created_at VARCHAR(40) NOT NULL,
      is_deleted TINYINT(1) NOT NULL DEFAULT 0,
      deleted_at VARCHAR(40) NULL,
      deleted_by CHAR(36) NULL,
      PRIMARY KEY (id),
      KEY idx_messages_chat_created (chat_id, created_at),
      KEY idx_messages_user (user_id),
      CONSTRAINT fk_messages_chat FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE ON UPDATE CASCADE,
      CONSTRAINT fk_messages_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
      CONSTRAINT fk_messages_deleted_by FOREIGN KEY (deleted_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  `);

  await pool.query(`
    CREATE TABLE IF NOT EXISTS profile_link_sessions (
      id CHAR(36) NOT NULL,
      initiator_user_id CHAR(36) NULL,
      resolved_user_id CHAR(36) NULL,
      status ENUM('PENDING', 'COMPLETED', 'EXPIRED') NOT NULL DEFAULT 'PENDING',
      source VARCHAR(16) NOT NULL DEFAULT 'WEB',
      created_at VARCHAR(40) NOT NULL,
      expires_at VARCHAR(40) NOT NULL,
      completed_at VARCHAR(40) NULL,
      PRIMARY KEY (id),
      KEY idx_profile_link_status_expires (status, expires_at),
      KEY idx_profile_link_initiator (initiator_user_id),
      KEY idx_profile_link_resolved (resolved_user_id),
      CONSTRAINT fk_profile_link_initiator FOREIGN KEY (initiator_user_id) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE,
      CONSTRAINT fk_profile_link_resolved FOREIGN KEY (resolved_user_id) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  `);

  // Backward compatible migration for existing installations.
  try {
    await pool.query(`
      ALTER TABLE chats
      ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0
    `);
  } catch (error) {
    if (!error || error.code !== "ER_DUP_FIELDNAME") {
      throw error;
    }
  }

  try {
    await pool.query(`
      ALTER TABLE chats
      ADD COLUMN deleted_at VARCHAR(40) NULL
    `);
  } catch (error) {
    if (!error || error.code !== "ER_DUP_FIELDNAME") {
      throw error;
    }
  }

  try {
    await pool.query(`
      ALTER TABLE chats
      ADD COLUMN deleted_by CHAR(36) NULL
    `);
  } catch (error) {
    if (!error || error.code !== "ER_DUP_FIELDNAME") {
      throw error;
    }
  }

  try {
    await pool.query(`
      ALTER TABLE chats
      ADD CONSTRAINT fk_chats_deleted_by
      FOREIGN KEY (deleted_by) REFERENCES users(id)
      ON DELETE SET NULL
      ON UPDATE CASCADE
    `);
  } catch (error) {
    if (
      !error ||
      (error.code !== "ER_DUP_KEYNAME" &&
        error.code !== "ER_FK_DUP_NAME" &&
        error.code !== "ER_CANT_CREATE_TABLE")
    ) {
      throw error;
    }
  }

  try {
    await pool.query(`
      ALTER TABLE users
      ADD COLUMN is_super_admin TINYINT(1) NOT NULL DEFAULT 0
    `);
  } catch (error) {
    if (!error || error.code !== "ER_DUP_FIELDNAME") {
      throw error;
    }
  }

  // Legacy index kept chat names globally unique, including deleted chats.
  // New behavior keeps names unique only among active chats.
  try {
    await pool.query(`
      ALTER TABLE chats
      DROP INDEX uq_chats_name
    `);
  } catch (error) {
    if (!error || error.code !== "ER_CANT_DROP_FIELD_OR_KEY") {
      throw error;
    }
  }

  try {
    await pool.query(`
      ALTER TABLE chats
      ADD COLUMN active_name VARCHAR(255) GENERATED ALWAYS AS (
        CASE WHEN is_deleted = 0 THEN name ELSE NULL END
      ) STORED
    `);
  } catch (error) {
    if (!error || error.code !== "ER_DUP_FIELDNAME") {
      throw error;
    }
  }

  try {
    await pool.query(`
      CREATE UNIQUE INDEX uq_chats_active_name ON chats (active_name)
    `);
  } catch (error) {
    if (!error || error.code !== "ER_DUP_KEYNAME") {
      throw error;
    }
  }
}

async function migrateFromStoreIfNeeded() {
  if (!fs.existsSync(STORE_PATH)) {
    return;
  }

  const [[userStats]] = await pool.query("SELECT COUNT(*) AS count FROM users");
  const [[chatStats]] = await pool.query("SELECT COUNT(*) AS count FROM chats");
  const [[messageStats]] = await pool.query("SELECT COUNT(*) AS count FROM messages");

  const totalRecords = Number(userStats.count || 0) + Number(chatStats.count || 0) + Number(messageStats.count || 0);
  if (totalRecords > 0) {
    return;
  }

  let parsed = null;
  try {
    parsed = JSON.parse(fs.readFileSync(STORE_PATH, "utf-8"));
  } catch (error) {
    console.error("Failed to parse store.json during migration", error);
    return;
  }

  const rawUsers = Array.isArray(parsed?.users) ? parsed.users : [];
  const rawChats = Array.isArray(parsed?.chats) ? parsed.chats : [];
  const rawMessages = Array.isArray(parsed?.messages) ? parsed.messages : [];

  if (!rawUsers.length && !rawChats.length && !rawMessages.length) {
    return;
  }

  const users = rawUsers
    .filter((user) => safeTrim(user?.id))
    .map((user) => ({
      id: safeTrim(user.id),
      nickname: safeTrim(user.nickname) || randomIdentifier(),
      avatarUrl: safeTrim(user.avatarUrl) || null,
      createdAt: safeTrim(user.createdAt) || nowIso()
    }));

  const userIds = new Set(users.map((user) => user.id));
  const userById = new Map(users.map((user) => [user.id, user]));

  const chats = rawChats
    .filter((chat) => safeTrim(chat?.id) && userIds.has(safeTrim(chat?.createdBy)))
    .map((chat) => ({
      id: safeTrim(chat.id),
      name: safeTrim(chat.name),
      iconUrl: safeTrim(chat.iconUrl) || null,
      passwordHash: safeTrim(chat.passwordHash) || null,
      createdBy: safeTrim(chat.createdBy),
      createdAt: safeTrim(chat.createdAt) || nowIso(),
      isDeleted: Boolean(chat.isDeleted),
      deletedAt: safeTrim(chat.deletedAt) || null,
      deletedBy: userIds.has(safeTrim(chat.deletedBy)) ? safeTrim(chat.deletedBy) : null,
      members: Array.isArray(chat.members) ? chat.members.map((member) => safeTrim(member)).filter(Boolean) : []
    }))
    .filter((chat) => chat.name.length >= 4);

  const chatIds = new Set(chats.map((chat) => chat.id));
  const validTypes = new Set(Object.values(MESSAGE_TYPE));

  const messages = rawMessages
    .filter((message) => {
      const messageId = safeTrim(message?.id);
      const chatId = safeTrim(message?.chatId);
      const userId = safeTrim(message?.userId);
      return messageId && chatIds.has(chatId) && userIds.has(userId);
    })
    .map((message) => {
      const type = safeTrim(message.type).toUpperCase();
      const user = userById.get(safeTrim(message.userId));
      return {
        id: safeTrim(message.id),
        chatId: safeTrim(message.chatId),
        userId: safeTrim(message.userId),
        senderNickname: safeTrim(message.senderNickname) || user?.nickname || randomIdentifier(),
        senderAvatarUrl: safeTrim(message.senderAvatarUrl) || user?.avatarUrl || null,
        type: validTypes.has(type) ? type : MESSAGE_TYPE.FILE,
        text: safeTrim(message.text) || null,
        fileUrl: safeTrim(message.fileUrl) || null,
        fileName: safeTrim(message.fileName) || null,
        createdAt: safeTrim(message.createdAt) || nowIso(),
        isDeleted: Boolean(message.isDeleted),
        deletedAt: safeTrim(message.deletedAt) || null,
        deletedBy: userIds.has(safeTrim(message.deletedBy)) ? safeTrim(message.deletedBy) : null
      };
    });

  const connection = await pool.getConnection();
  try {
    await connection.beginTransaction();

    for (const user of users) {
      await connection.query(
        `
          INSERT INTO users (id, nickname, avatar_url, created_at)
          VALUES (?, ?, ?, ?)
          ON DUPLICATE KEY UPDATE
            nickname = VALUES(nickname),
            avatar_url = VALUES(avatar_url)
        `,
        [user.id, user.nickname, user.avatarUrl, user.createdAt]
      );
    }

    for (const chat of chats) {
      await connection.query(
        `
          INSERT INTO chats (
            id,
            name,
            icon_url,
            password_hash,
            created_by,
            created_at,
            is_deleted,
            deleted_at,
            deleted_by
          )
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        `,
        [
          chat.id,
          chat.name,
          chat.iconUrl,
          chat.passwordHash,
          chat.createdBy,
          chat.createdAt,
          chat.isDeleted ? 1 : 0,
          chat.deletedAt,
          chat.deletedBy
        ]
      );

      await connection.query(
        `
          INSERT IGNORE INTO chat_members (chat_id, user_id, joined_at)
          VALUES (?, ?, ?)
        `,
        [chat.id, chat.createdBy, chat.createdAt]
      );

      for (const memberId of chat.members) {
        if (!userIds.has(memberId)) {
          continue;
        }

        await connection.query(
          `
            INSERT IGNORE INTO chat_members (chat_id, user_id, joined_at)
            VALUES (?, ?, ?)
          `,
          [chat.id, memberId, chat.createdAt]
        );
      }
    }

    for (const message of messages) {
      await connection.query(
        `
          INSERT INTO messages (
            id,
            chat_id,
            user_id,
            sender_nickname,
            sender_avatar_url,
            type,
            message_text,
            file_url,
            file_name,
            created_at,
            is_deleted,
            deleted_at,
            deleted_by
          )
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          ON DUPLICATE KEY UPDATE
            sender_nickname = VALUES(sender_nickname),
            sender_avatar_url = VALUES(sender_avatar_url),
            type = VALUES(type),
            message_text = VALUES(message_text),
            file_url = VALUES(file_url),
            file_name = VALUES(file_name),
            created_at = VALUES(created_at),
            is_deleted = VALUES(is_deleted),
            deleted_at = VALUES(deleted_at),
            deleted_by = VALUES(deleted_by)
        `,
        [
          message.id,
          message.chatId,
          message.userId,
          message.senderNickname,
          message.senderAvatarUrl,
          message.type,
          message.text,
          message.fileUrl,
          message.fileName,
          message.createdAt,
          message.isDeleted ? 1 : 0,
          message.deletedAt,
          message.deletedBy
        ]
      );
    }

    await connection.commit();
    console.log(`Migrated store.json to MySQL: users=${users.length}, chats=${chats.length}, messages=${messages.length}`);
  } catch (error) {
    await connection.rollback();
    console.error("Failed to migrate store.json to MySQL", error);
    throw error;
  } finally {
    connection.release();
  }
}

async function findUser(userId) {
  const [rows] = await pool.query(
    `
      SELECT
        id,
        nickname,
        avatar_url AS avatarUrl,
        created_at AS createdAt,
        is_super_admin AS isSuperAdmin
      FROM users
      WHERE id = ?
      LIMIT 1
    `,
    [userId]
  );
  if (!rows[0]) {
    return null;
  }
  return mapUserResponse(rows[0]);
}

async function hydrateSuperUsersFromDb() {
  const [rows] = await pool.query(
    `
      SELECT id
      FROM users
      WHERE is_super_admin = 1
    `
  );

  for (const row of rows) {
    const id = safeTrim(row.id);
    if (id) {
      SUPER_USER_IDS.add(id);
    }
  }
}

async function findChatById(chatId) {
  const [rows] = await pool.query(
    `
      SELECT
        c.id,
        c.name,
        c.icon_url AS iconUrl,
        c.password_hash AS passwordHash,
        c.created_by AS createdBy,
        c.created_at AS createdAt,
        c.is_deleted AS isDeleted,
        c.deleted_at AS deletedAt,
        c.deleted_by AS deletedBy,
        (
          SELECT COUNT(*)
          FROM chat_members cm
          WHERE cm.chat_id = c.id
        ) AS membersCount
      FROM chats c
      WHERE c.id = ?
      LIMIT 1
    `,
    [chatId]
  );
  return rows[0] || null;
}

async function findChatByName(name, options = {}) {
  const includeDeleted = options.includeDeleted !== false;
  const [rows] = await pool.query(
    `
      SELECT
        c.id,
        c.name,
        c.icon_url AS iconUrl,
        c.password_hash AS passwordHash,
        c.created_by AS createdBy,
        c.created_at AS createdAt,
        c.is_deleted AS isDeleted,
        c.deleted_at AS deletedAt,
        c.deleted_by AS deletedBy,
        (
          SELECT COUNT(*)
          FROM chat_members cm
          WHERE cm.chat_id = c.id
        ) AS membersCount
      FROM chats c
      WHERE LOWER(c.name) = LOWER(?)
        ${includeDeleted ? "" : "AND c.is_deleted = 0"}
      ORDER BY c.is_deleted ASC, c.created_at DESC
      LIMIT 1
    `,
    [name]
  );
  return rows[0] || null;
}

async function isMember(chatId, userId) {
  if (isSuperUser(userId)) {
    return true;
  }

  const [rows] = await pool.query(
    `
      SELECT 1
      FROM chat_members
      WHERE chat_id = ? AND user_id = ?
      LIMIT 1
    `,
    [chatId, userId]
  );
  return rows.length > 0;
}

function isChatAdmin(chat, userId) {
  if (!chat || !userId) {
    return false;
  }
  return chat.createdBy === userId || isSuperUser(userId);
}

function canViewDeletedMessages(userId) {
  return isSuperUser(userId);
}

async function findMessageById(messageId) {
  const [rows] = await pool.query(
    `
      SELECT
        m.id,
        m.chat_id AS chatId,
        m.user_id AS userId,
        m.sender_nickname AS senderNickname,
        m.sender_avatar_url AS senderAvatarUrl,
        m.type,
        m.message_text AS text,
        m.file_url AS fileUrl,
        m.file_name AS fileName,
        m.created_at AS createdAt,
        m.is_deleted AS isDeleted,
        m.deleted_at AS deletedAt,
        m.deleted_by AS deletedBy
      FROM messages m
      WHERE m.id = ?
      LIMIT 1
    `,
    [messageId]
  );
  return rows[0] || null;
}

async function findProfileLinkSession(sessionId) {
  const [rows] = await pool.query(
    `
      SELECT
        s.id,
        s.initiator_user_id AS initiatorUserId,
        s.resolved_user_id AS resolvedUserId,
        s.status,
        s.source,
        s.created_at AS createdAt,
        s.expires_at AS expiresAt,
        s.completed_at AS completedAt
      FROM profile_link_sessions s
      WHERE s.id = ?
      LIMIT 1
    `,
    [sessionId]
  );
  return rows[0] || null;
}

async function expireProfileLinkSessionIfNeeded(session) {
  if (!session) {
    return null;
  }
  if (session.status !== PROFILE_LINK_STATUS.PENDING) {
    return session;
  }
  if (!isIsoExpired(session.expiresAt)) {
    return session;
  }

  await pool.query(
    `
      UPDATE profile_link_sessions
      SET status = ?
      WHERE id = ? AND status = ?
    `,
    [PROFILE_LINK_STATUS.EXPIRED, session.id, PROFILE_LINK_STATUS.PENDING]
  );

  return await findProfileLinkSession(session.id);
}

async function mapProfileLinkSessionResponse(session) {
  if (!session) {
    return null;
  }
  const resolvedUser = session.resolvedUserId ? await findUser(session.resolvedUserId) : null;
  return {
    sessionId: session.id,
    status: session.status,
    source: session.source,
    createdAt: session.createdAt,
    expiresAt: session.expiresAt,
    completedAt: session.completedAt || null,
    initiatorUserId: session.initiatorUserId || null,
    resolvedUserId: session.resolvedUserId || null,
    qrText: buildProfileLinkQrText(session.id),
    user: resolvedUser || null
  };
}

async function listUserChats(userId) {
  if (isSuperUser(userId)) {
    const [rows] = await pool.query(
      `
        SELECT
          c.id,
          c.name,
          c.icon_url AS iconUrl,
          c.password_hash AS passwordHash,
          c.created_by AS createdBy,
          c.created_at AS createdAt,
          c.is_deleted AS isDeleted,
          c.deleted_at AS deletedAt,
          c.deleted_by AS deletedBy,
          COUNT(cm_all.user_id) AS membersCount
        FROM chats c
        LEFT JOIN chat_members cm_all
          ON cm_all.chat_id = c.id
        GROUP BY
          c.id,
          c.name,
          c.icon_url,
          c.password_hash,
          c.created_by,
          c.created_at,
          c.is_deleted,
          c.deleted_at,
          c.deleted_by
        ORDER BY c.created_at ASC
      `
    );

    return rows.map((chat) => mapChatResponse(chat));
  }

  const [rows] = await pool.query(
    `
      SELECT
        c.id,
        c.name,
        c.icon_url AS iconUrl,
        c.password_hash AS passwordHash,
        c.created_by AS createdBy,
        c.created_at AS createdAt,
        c.is_deleted AS isDeleted,
        c.deleted_at AS deletedAt,
        c.deleted_by AS deletedBy,
        COUNT(cm_all.user_id) AS membersCount
      FROM chats c
      INNER JOIN chat_members cm_user
        ON cm_user.chat_id = c.id
       AND cm_user.user_id = ?
      LEFT JOIN chat_members cm_all
        ON cm_all.chat_id = c.id
      WHERE c.is_deleted = 0 OR c.created_by = ?
      GROUP BY
        c.id,
        c.name,
        c.icon_url,
        c.password_hash,
        c.created_by,
        c.created_at,
        c.is_deleted,
        c.deleted_at,
        c.deleted_by
      ORDER BY c.created_at ASC
    `,
    [userId, userId]
  );

  return rows.map((chat) => mapChatResponse(chat));
}

async function listChatMessages(chatId, options = {}) {
  const includeDeleted = Boolean(options.includeDeleted);
  const [rows] = await pool.query(
    `
      SELECT
        m.id,
        m.chat_id AS chatId,
        m.user_id AS userId,
        m.sender_nickname AS senderNickname,
        m.sender_avatar_url AS senderAvatarUrl,
        m.type,
        m.message_text AS text,
        m.file_url AS fileUrl,
        m.file_name AS fileName,
        m.created_at AS createdAt,
        m.is_deleted AS isDeleted,
        m.deleted_at AS deletedAt,
        m.deleted_by AS deletedBy
      FROM messages m
      WHERE m.chat_id = ? ${includeDeleted ? "" : "AND m.is_deleted = 0"}
      ORDER BY m.created_at DESC
      LIMIT 200
    `,
    [chatId]
  );

  rows.reverse();
  return rows.map((message) => mapMessageResponse(message));
}

const app = express();
const server = http.createServer(app);
const wss = new WebSocketServer({ server, path: "/ws" });

const socketRooms = new Map();
const messageRateBuckets = new Map();

function reserveMessageRateSlot(userId) {
  const now = Date.now();
  const windowStart = now - MESSAGE_RATE_WINDOW_MS;
  const current = messageRateBuckets.get(userId) || [];
  const recent = current.filter((timestamp) => timestamp > windowStart);

  if (recent.length >= MESSAGE_RATE_LIMIT_PER_SECOND) {
    messageRateBuckets.set(userId, recent);
    const retryAfterMs = Math.max(1, MESSAGE_RATE_WINDOW_MS - (now - recent[0]));
    return { allowed: false, retryAfterMs };
  }

  recent.push(now);
  messageRateBuckets.set(userId, recent);
  return { allowed: true, retryAfterMs: 0 };
}

function broadcastToRoom(chatId, payload) {
  const encoded = JSON.stringify(payload);
  for (const [socket, roomSet] of socketRooms.entries()) {
    if (socket.readyState !== socket.OPEN) {
      continue;
    }
    if (roomSet.has(chatId)) {
      socket.send(encoded);
    }
  }
}

function clearRoom(chatId) {
  for (const roomSet of socketRooms.values()) {
    roomSet.delete(chatId);
  }
}

wss.on("connection", (socket) => {
  socketRooms.set(socket, new Set());

  socket.on("message", async (rawMessage) => {
    try {
      const message = JSON.parse(String(rawMessage));
      const roomSet = socketRooms.get(socket);
      if (!roomSet) {
        return;
      }

      if (
        message.type === "join" &&
        typeof message.chatId === "string" &&
        typeof message.userId === "string"
      ) {
        const allowed = await isMember(message.chatId, message.userId);
        if (allowed) {
          roomSet.add(message.chatId);
        }
      }

      if (message.type === "leave" && typeof message.chatId === "string") {
        roomSet.delete(message.chatId);
      }
    } catch (_error) {
      // Ignore malformed websocket messages.
    }
  });

  socket.on("close", () => {
    socketRooms.delete(socket);
  });
});

app.use(cors());
app.use(express.json({ limit: "10mb" }));
app.use(
  "/uploads",
  express.static(UPLOAD_DIR, {
    setHeaders: (res, filePath) => {
      res.setHeader("X-Content-Type-Options", "nosniff");
      if (isLegacyHtmlUploadFile(filePath)) {
        res.setHeader("Content-Type", "text/plain; charset=utf-8");
      }
    }
  })
);

const upload = multer({ storage: createFileStorage() });

app.get(
  "/api/health",
  asyncHandler(async (_req, res) => {
    await pool.query("SELECT 1");
    res.json({ status: "ok", now: nowIso() });
  })
);

app.post(
  "/api/auth/logout",
  asyncHandler(async (req, res) => {
    const userId = safeTrim(req.body?.userId);
    if (userId) {
      messageRateBuckets.delete(userId);
    }
    res.json({ status: "ok" });
  })
);

app.post(
  "/api/admin/activate",
  asyncHandler(async (req, res) => {
    const userId = safeTrim(req.body?.userId);
    const adminKey = safeTrim(req.body?.key);

    if (!userId || !adminKey) {
      res.status(400).json({ message: "userId and key are required" });
      return;
    }

    if (!ADMIN_ACTIVATION_KEY) {
      res.status(503).json({ message: "Admin key is not configured" });
      return;
    }

    if (adminKey !== ADMIN_ACTIVATION_KEY) {
      res.status(401).json({ message: "Invalid admin key" });
      return;
    }

    const user = await findUser(userId);
    if (!user) {
      res.status(404).json({ message: "User not found" });
      return;
    }

    if (!user.isSuperAdmin) {
      await pool.query(
        `
          UPDATE users
          SET is_super_admin = 1
          WHERE id = ?
        `,
        [user.id]
      );
      SUPER_USER_IDS.add(user.id);
    }

    const updated = await findUser(user.id);
    res.json(updated);
  })
);

app.post(
  "/api/users/register",
  asyncHandler(async (req, res) => {
    const nickname = safeTrim(req.body?.nickname) || randomIdentifier();
    const avatarUrl = safeTrim(req.body?.avatarUrl) || null;

    const user = {
      id: crypto.randomUUID(),
      nickname,
      avatarUrl,
      createdAt: nowIso(),
      isSuperAdmin: false
    };

    await pool.query(
      `
        INSERT INTO users (id, nickname, avatar_url, created_at, is_super_admin)
        VALUES (?, ?, ?, ?, ?)
      `,
      [user.id, user.nickname, user.avatarUrl, user.createdAt, 0]
    );

    res.status(201).json(mapUserResponse(user));
  })
);

app.get(
  "/api/users/:userId",
  asyncHandler(async (req, res) => {
    const user = await findUser(req.params.userId);
    if (!user) {
      res.status(404).json({ message: "User not found" });
      return;
    }

    res.json(user);
  })
);

app.put(
  "/api/users/:userId",
  asyncHandler(async (req, res) => {
    const user = await findUser(req.params.userId);
    if (!user) {
      res.status(404).json({ message: "User not found" });
      return;
    }

    const nickname = safeTrim(req.body?.nickname);
    const avatarUrl = safeTrim(req.body?.avatarUrl);
    const hasAvatarUrlField = Object.prototype.hasOwnProperty.call(req.body || {}, "avatarUrl");

    const fields = [];
    const values = [];

    if (nickname) {
      fields.push("nickname = ?");
      values.push(nickname);
    }

    if (hasAvatarUrlField) {
      fields.push("avatar_url = ?");
      values.push(avatarUrl || null);
    }

    if (fields.length > 0) {
      values.push(user.id);
      await pool.query(
        `
          UPDATE users
          SET ${fields.join(", ")}
          WHERE id = ?
        `,
        values
      );
    }

    const updated = await findUser(user.id);
    res.json(updated);
  })
);

app.post(
  "/api/profile-link/sessions",
  asyncHandler(async (req, res) => {
    const userId = safeTrim(req.body?.userId);
    const sourceRaw = safeTrim(req.body?.source).toUpperCase();
    const source = sourceRaw === "ANDROID" ? "ANDROID" : "WEB";

    let initiatorUserId = null;
    if (userId) {
      const user = await findUser(userId);
      if (!user) {
        res.status(404).json({ message: "User not found" });
        return;
      }
      initiatorUserId = user.id;
    }

    const sessionId = crypto.randomUUID();
    const createdAt = nowIso();
    const expiresAt = addMsToIso(PROFILE_LINK_SESSION_TTL_MS);

    await pool.query(
      `
        INSERT INTO profile_link_sessions (
          id,
          initiator_user_id,
          resolved_user_id,
          status,
          source,
          created_at,
          expires_at,
          completed_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      `,
      [sessionId, initiatorUserId, null, PROFILE_LINK_STATUS.PENDING, source, createdAt, expiresAt, null]
    );

    const session = await findProfileLinkSession(sessionId);
    res.status(201).json(await mapProfileLinkSessionResponse(session));
  })
);

app.get(
  "/api/profile-link/sessions/:sessionId",
  asyncHandler(async (req, res) => {
    const sessionId = safeTrim(req.params.sessionId);
    const session = await findProfileLinkSession(sessionId);
    if (!session) {
      res.status(404).json({ message: "Link session not found" });
      return;
    }

    const normalized = await expireProfileLinkSessionIfNeeded(session);
    res.json(await mapProfileLinkSessionResponse(normalized));
  })
);

app.post(
  "/api/profile-link/sessions/:sessionId/complete",
  asyncHandler(async (req, res) => {
    const sessionId = safeTrim(req.params.sessionId);
    const preferredUserId = safeTrim(req.body?.userId);

    const session = await findProfileLinkSession(sessionId);
    if (!session) {
      res.status(404).json({ message: "Link session not found" });
      return;
    }

    const normalized = await expireProfileLinkSessionIfNeeded(session);
    if (!normalized) {
      res.status(404).json({ message: "Link session not found" });
      return;
    }

    if (normalized.status === PROFILE_LINK_STATUS.EXPIRED) {
      res.status(410).json({ message: "Link session expired" });
      return;
    }

    if (normalized.status === PROFILE_LINK_STATUS.COMPLETED) {
      res.json(await mapProfileLinkSessionResponse(normalized));
      return;
    }

    let resolvedUserId = null;
    if (preferredUserId) {
      const preferredUser = await findUser(preferredUserId);
      if (!preferredUser) {
        res.status(404).json({ message: "User not found" });
        return;
      }
      resolvedUserId = preferredUser.id;
    } else if (normalized.initiatorUserId) {
      resolvedUserId = normalized.initiatorUserId;
    }

    if (!resolvedUserId) {
      res.status(400).json({ message: "No profile to link. Create profile on one device first" });
      return;
    }

    const resolvedUser = await findUser(resolvedUserId);
    if (!resolvedUser) {
      res.status(404).json({ message: "User not found" });
      return;
    }

    await pool.query(
      `
        UPDATE profile_link_sessions
        SET
          status = ?,
          resolved_user_id = ?,
          completed_at = ?
        WHERE id = ? AND status = ?
      `,
      [PROFILE_LINK_STATUS.COMPLETED, resolvedUser.id, nowIso(), sessionId, PROFILE_LINK_STATUS.PENDING]
    );

    const updated = await findProfileLinkSession(sessionId);
    res.json(await mapProfileLinkSessionResponse(updated));
  })
);

app.get(
  "/api/chats",
  asyncHandler(async (req, res) => {
    const userId = safeTrim(req.query.userId);
    if (!userId) {
      res.status(400).json({ message: "userId is required" });
      return;
    }

    if (!(await findUser(userId))) {
      res.status(404).json({ message: "User not found" });
      return;
    }

    const chats = await listUserChats(userId);
    res.json(chats);
  })
);

app.post(
  "/api/chats/create",
  asyncHandler(async (req, res) => {
    const name = safeTrim(req.body?.name);
    const password = safeTrim(req.body?.password);
    const iconUrl = safeTrim(req.body?.iconUrl);
    const userId = safeTrim(req.body?.userId);

    if (!userId) {
      res.status(400).json({ message: "userId is required" });
      return;
    }

    if (!(await findUser(userId))) {
      res.status(404).json({ message: "User not found" });
      return;
    }

    if (name.length < 4) {
      res.status(400).json({ message: "Chat name must be longer than 3 characters" });
      return;
    }

    const existingActiveChat = await findChatByName(name, { includeDeleted: false });
    if (existingActiveChat) {
      res.status(409).json({ message: "Chat with this name already exists" });
      return;
    }

    const chatId = crypto.randomUUID();
    const chatCreatedAt = nowIso();

    try {
      await pool.query(
        `
          INSERT INTO chats (
            id,
            name,
            icon_url,
            password_hash,
            created_by,
            created_at,
            is_deleted,
            deleted_at,
            deleted_by
          )
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        `,
        [
          chatId,
          name,
          iconUrl || null,
          password ? bcrypt.hashSync(password, 10) : null,
          userId,
          chatCreatedAt,
          0,
          null,
          null
        ]
      );
    } catch (error) {
      if (error && error.code === "ER_DUP_ENTRY") {
        res.status(409).json({ message: "Chat with this name already exists" });
        return;
      }
      throw error;
    }

    await pool.query(
      `
        INSERT INTO chat_members (chat_id, user_id, joined_at)
        VALUES (?, ?, ?)
      `,
      [chatId, userId, chatCreatedAt]
    );

    const chat = await findChatById(chatId);
    res.status(201).json(mapChatResponse(chat));
  })
);

app.post(
  "/api/chats/join",
  asyncHandler(async (req, res) => {
    const name = safeTrim(req.body?.name);
    const password = safeTrim(req.body?.password);
    const userId = safeTrim(req.body?.userId);

    if (!userId) {
      res.status(400).json({ message: "userId is required" });
      return;
    }

    if (!(await findUser(userId))) {
      res.status(404).json({ message: "User not found" });
      return;
    }

    const chat = await findChatByName(name);

    if (!chat) {
      res.status(404).json({ message: "Chat not found" });
      return;
    }

    if (chat.isDeleted && !isChatAdmin(chat, userId)) {
      res.status(404).json({ message: "Chat not found" });
      return;
    }

    if (!isSuperUser(userId) && chat.passwordHash && !bcrypt.compareSync(password || "", chat.passwordHash)) {
      res.status(401).json({ message: "Wrong password" });
      return;
    }

    await pool.query(
      `
        INSERT IGNORE INTO chat_members (chat_id, user_id, joined_at)
        VALUES (?, ?, ?)
      `,
      [chat.id, userId, nowIso()]
    );

    const updatedChat = await findChatById(chat.id);
    res.json(mapChatResponse(updatedChat));
  })
);

app.put(
  "/api/chats/:chatId/icon",
  asyncHandler(async (req, res) => {
    const chatId = safeTrim(req.params.chatId);
    const userId = safeTrim(req.body?.userId);
    const iconUrl = safeTrim(req.body?.iconUrl);

    if (!userId) {
      res.status(400).json({ message: "userId is required" });
      return;
    }

    const user = await findUser(userId);
    if (!user) {
      res.status(404).json({ message: "User not found" });
      return;
    }

    const chat = await findChatById(chatId);
    if (!chat) {
      res.status(404).json({ message: "Chat not found" });
      return;
    }

    if (chat.createdBy !== user.id && !isSuperUser(user.id)) {
      res.status(403).json({ message: "Only chat creator can update chat icon" });
      return;
    }

    await pool.query(
      `
        UPDATE chats
        SET icon_url = ?
        WHERE id = ?
      `,
      [iconUrl || null, chatId]
    );

    const updatedChat = await findChatById(chatId);
    res.json(mapChatResponse(updatedChat));
  })
);

app.delete(
  "/api/chats/:chatId",
  asyncHandler(async (req, res) => {
    const chatId = safeTrim(req.params.chatId);
    const userId = safeTrim(req.query.userId);

    if (!userId) {
      res.status(400).json({ message: "userId is required" });
      return;
    }

    const user = await findUser(userId);
    if (!user) {
      res.status(404).json({ message: "User not found" });
      return;
    }

    const chat = await findChatById(chatId);
    if (!chat) {
      res.status(404).json({ message: "Chat not found" });
      return;
    }

    if (chat.createdBy !== user.id && !isSuperUser(user.id)) {
      res.status(403).json({ message: "Only chat creator can delete chat" });
      return;
    }

    const [[messageStats]] = await pool.query(
      `
        SELECT COUNT(*) AS count
        FROM messages
        WHERE chat_id = ?
      `,
      [chatId]
    );
    const removedMessagesCount = Number(messageStats.count || 0);

    if (!chat.isDeleted) {
      await pool.query(
        `
          UPDATE chats
          SET
            is_deleted = 1,
            deleted_at = ?,
            deleted_by = ?
          WHERE id = ?
        `,
        [nowIso(), user.id, chatId]
      );
    }

    const updatedChat = await findChatById(chatId);

    broadcastToRoom(chatId, {
      type: "chat_deleted",
      payload: {
        chatId,
        isDeleted: true
      }
    });
    clearRoom(chatId);

    res.json({
      chatId,
      removedMessagesCount,
      isDeleted: true,
      chat: updatedChat ? mapChatResponse(updatedChat) : null
    });
  })
);

app.get(
  "/api/chats/:chatId/messages",
  asyncHandler(async (req, res) => {
    const chatId = safeTrim(req.params.chatId);
    const userId = safeTrim(req.query.userId);
    const includeDeletedParam = safeTrim(req.query.includeDeleted).toLowerCase();
    const includeDeletedRequested = includeDeletedParam === "1" || includeDeletedParam === "true";

    if (!userId) {
      res.status(400).json({ message: "userId is required" });
      return;
    }

    const chat = await findChatById(chatId);
    if (!chat) {
      res.status(404).json({ message: "Chat not found" });
      return;
    }

    if (!(await isMember(chatId, userId))) {
      res.status(403).json({ message: "You are not a member of this chat" });
      return;
    }

    const canAccessDeletedChat = isChatAdmin(chat, userId);
    if (chat.isDeleted && !canAccessDeletedChat) {
      res.status(404).json({ message: "Chat not found" });
      return;
    }

    const canViewDeleted = canViewDeletedMessages(userId);
    const messages = await listChatMessages(chatId, {
      includeDeleted: canViewDeleted && includeDeletedRequested
    });
    res.json(messages);
  })
);

app.post(
  "/api/messages",
  asyncHandler(async (req, res) => {
    const chatId = safeTrim(req.body?.chatId);
    const userId = safeTrim(req.body?.userId);
    const type = safeTrim(req.body?.type).toUpperCase();
    const text = safeTrim(req.body?.text);
    const fileUrl = safeTrim(req.body?.fileUrl);
    const fileName = safeTrim(req.body?.fileName);

    if (!chatId || !userId || !type) {
      res.status(400).json({ message: "chatId, userId and type are required" });
      return;
    }

    const chat = await findChatById(chatId);
    if (!chat) {
      res.status(404).json({ message: "Chat not found" });
      return;
    }

    const user = await findUser(userId);
    if (!user) {
      res.status(404).json({ message: "User not found" });
      return;
    }

    if (!(await isMember(chatId, userId))) {
      res.status(403).json({ message: "You are not a member of this chat" });
      return;
    }

    if (chat.isDeleted) {
      res.status(410).json({ message: "Chat is deleted" });
      return;
    }

    const validTypes = Object.values(MESSAGE_TYPE);
    if (!validTypes.includes(type)) {
      res.status(400).json({ message: "Invalid message type" });
      return;
    }

    if (type === MESSAGE_TYPE.TEXT && !text) {
      res.status(400).json({ message: "Text is required for TEXT messages" });
      return;
    }

    if (type !== MESSAGE_TYPE.TEXT && !fileUrl) {
      res.status(400).json({ message: "fileUrl is required for media messages" });
      return;
    }

    const rateCheck = reserveMessageRateSlot(userId);
    if (!rateCheck.allowed) {
      res.set("Retry-After", String(Math.ceil(rateCheck.retryAfterMs / 1000)));
      res.status(429).json({
        message: "Too many messages. Max 5 per second",
        retryAfterMs: rateCheck.retryAfterMs
      });
      return;
    }

    const message = {
      id: crypto.randomUUID(),
      chatId,
      userId,
      senderNickname: user.nickname,
      senderAvatarUrl: user.avatarUrl,
      type,
      text: type === MESSAGE_TYPE.TEXT ? text : null,
      fileUrl: type === MESSAGE_TYPE.TEXT ? null : fileUrl,
      fileName: fileName || null,
      createdAt: nowIso(),
      isDeleted: false
    };

    await pool.query(
      `
        INSERT INTO messages (
          id,
          chat_id,
          user_id,
          sender_nickname,
          sender_avatar_url,
          type,
          message_text,
          file_url,
          file_name,
          created_at,
          is_deleted,
          deleted_at,
          deleted_by
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      `,
      [
        message.id,
        message.chatId,
        message.userId,
        message.senderNickname,
        message.senderAvatarUrl,
        message.type,
        message.text,
        message.fileUrl,
        message.fileName,
        message.createdAt,
        0,
        null,
        null
      ]
    );

    const response = mapMessageResponse(message);
    broadcastToRoom(chatId, {
      type: "message",
      payload: response
    });

    res.status(201).json(response);
  })
);

app.delete(
  "/api/messages/:messageId",
  asyncHandler(async (req, res) => {
    const messageId = safeTrim(req.params.messageId);
    const userId = safeTrim(req.query.userId);

    if (!userId) {
      res.status(400).json({ message: "userId is required" });
      return;
    }

    const user = await findUser(userId);
    if (!user) {
      res.status(404).json({ message: "User not found" });
      return;
    }

    const message = await findMessageById(messageId);
    if (!message) {
      res.status(404).json({ message: "Message not found" });
      return;
    }

    const chat = await findChatById(message.chatId);
    if (!chat) {
      res.status(404).json({ message: "Chat not found" });
      return;
    }

    if (message.userId !== user.id && !isSuperUser(user.id)) {
      res.status(403).json({ message: "You can delete only your own messages" });
      return;
    }

    if (message.isDeleted) {
      res.json({ id: message.id, chatId: message.chatId, isDeleted: true });
      return;
    }

    await pool.query(
      `
        UPDATE messages
        SET
          is_deleted = 1,
          deleted_at = ?,
          deleted_by = ?
        WHERE id = ?
      `,
      [nowIso(), user.id, message.id]
    );

    broadcastToRoom(message.chatId, {
      type: "message_deleted",
      payload: {
        id: message.id,
        chatId: message.chatId
      }
    });

    res.json({ id: message.id, chatId: message.chatId, isDeleted: true });
  })
);

app.post(
  "/api/upload",
  upload.single("file"),
  asyncHandler(async (req, res) => {
    if (!req.file) {
      res.status(400).json({ message: "file is required" });
      return;
    }

    const baseUrl = `${req.protocol}://${req.get("host")}`;
    const url = `${baseUrl}/uploads/${req.file.filename}`;
    const detectedType = detectMessageTypeByMime(req.file.mimetype);

    res.status(201).json({
      url,
      fileName: buildSafeUploadDisplayName(req.file),
      mimeType: req.file.mimetype,
      messageType: detectedType
    });
  })
);

async function startServer() {
  if (!fs.existsSync(DATA_DIR)) {
    fs.mkdirSync(DATA_DIR, { recursive: true });
  }

  if (!fs.existsSync(UPLOAD_DIR)) {
    fs.mkdirSync(UPLOAD_DIR, { recursive: true });
  }

  await initializeDatabase();

  server.listen(PORT, HOST, () => {
    console.log(`Secret backend started on http://${HOST}:${PORT}`);
    console.log(
      `MySQL connected: mysql://${DB_USER}@${DB_HOST}:${DB_PORT}/${DB_NAME} (autoCreate=${DB_AUTO_CREATE})`
    );
  });
}

startServer().catch((error) => {
  console.error("Failed to start backend", error);
  process.exit(1);
});
