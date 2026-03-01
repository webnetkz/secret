<script setup lang="ts">
import { computed } from "vue"

const props = withDefaults(
  defineProps<{
    name:
      | "refresh"
      | "settings"
      | "trash"
      | "paperclip"
      | "video"
      | "mic"
      | "pause"
      | "send"
      | "camera"
      | "user"
      | "close"
      | "plus"
      | "moon"
      | "sun"
      | "search"
      | "qr"
    size?: number
    strokeWidth?: number
  }>(),
  {
    size: 20,
    strokeWidth: 1.9
  }
)

const isSettingsIcon = computed(() => props.name === "settings")
const viewBox = computed(() => (isSettingsIcon.value ? "0 0 512 512" : "0 0 24 24"))
</script>

<template>
  <svg
    :width="props.size"
    :height="props.size"
    :viewBox="viewBox"
    :fill="isSettingsIcon ? 'currentColor' : 'none'"
    :stroke="isSettingsIcon ? 'none' : 'currentColor'"
    :stroke-width="isSettingsIcon ? 0 : props.strokeWidth"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <template v-if="props.name === 'refresh'">
      <path d="M21 12a9 9 0 1 1-2.64-6.36" />
      <path d="M21 3v6h-6" />
    </template>

    <template v-else-if="props.name === 'settings'">
      <path d="M495.9 166.6c3.2 8.7 .5 18.4-6.4 24.6l-43.3 39.4c1.1 8.3 1.7 16.8 1.7 25.4s-.6 17.1-1.7 25.4l43.3 39.4c6.9 6.2 9.6 15.9 6.4 24.6c-4.4 11.9-9.7 23.3-15.8 34.3l-4.7 8.1c-6.6 11-14 21.4-22.1 31.2c-5.9 7.2-15.7 9.6-24.5 6.8l-55.7-17.7c-13.4 10.3-28.2 18.9-44 25.4l-12.5 57.1c-2 9.1-9 16.3-18.2 17.8c-13.8 2.3-28 3.5-42.5 3.5s-28.7-1.2-42.5-3.5c-9.2-1.5-16.2-8.7-18.2-17.8l-12.5-57.1c-15.8-6.5-30.6-15.1-44-25.4L83.1 425.9c-8.8 2.8-18.6 .3-24.5-6.8c-8.1-9.8-15.5-20.2-22.1-31.2l-4.7-8.1c-6.1-11-11.4-22.4-15.8-34.3c-3.2-8.7-.5-18.4 6.4-24.6l43.3-39.4C64.6 273.1 64 264.6 64 256s.6-17.1 1.7-25.4L22.4 191.2c-6.9-6.2-9.6-15.9-6.4-24.6c4.4-11.9 9.7-23.3 15.8-34.3l4.7-8.1c6.6-11 14-21.4 22.1-31.2c5.9-7.2 15.7-9.6 24.5-6.8l55.7 17.7c13.4-10.3 28.2-18.9 44-25.4l12.5-57.1c2-9.1 9-16.3 18.2-17.8C227.3 1.2 241.5 0 256 0s28.7 1.2 42.5 3.5c9.2 1.5 16.2 8.7 18.2 17.8l12.5 57.1c15.8 6.5 30.6 15.1 44 25.4l55.7-17.7c8.8-2.8 18.6-.3 24.5 6.8c8.1 9.8 15.5 20.2 22.1 31.2l4.7 8.1c6.1 11 11.4 22.4 15.8 34.3zM256 336a80 80 0 1 0 0-160 80 80 0 1 0 0 160z" />
    </template>

    <template v-else-if="props.name === 'trash'">
      <path d="M3 6h18" />
      <path d="M8 6V4h8v2" />
      <path d="M6 6l1 14h10l1-14" />
      <path d="M10 11v6" />
      <path d="M14 11v6" />
    </template>

    <template v-else-if="props.name === 'paperclip'">
      <path d="M21.4 11.3L12 20.7a6 6 0 0 1-8.5-8.5l9.2-9.2a4 4 0 1 1 5.7 5.6l-9.2 9.2a2 2 0 1 1-2.8-2.8l8.5-8.5" />
    </template>

    <template v-else-if="props.name === 'video'">
      <rect x="3" y="6" width="13" height="12" rx="2" />
      <path d="M16 10.5l5-3v9l-5-3" />
    </template>

    <template v-else-if="props.name === 'mic'">
      <rect x="9" y="3" width="6" height="11" rx="3" />
      <path d="M5 11a7 7 0 0 0 14 0" />
      <path d="M12 18v3" />
      <path d="M9 21h6" />
    </template>

    <template v-else-if="props.name === 'pause'">
      <rect x="6" y="5" width="4" height="14" rx="1" />
      <rect x="14" y="5" width="4" height="14" rx="1" />
    </template>

    <template v-else-if="props.name === 'send'">
      <path d="M22 2L11 13" />
      <path d="M22 2l-7 20-4-9-9-4 20-7Z" />
    </template>

    <template v-else-if="props.name === 'camera'">
      <path d="M4 7h4l1.6-2h4.8L16 7h4a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2Z" />
      <circle cx="12" cy="13" r="3.2" />
    </template>

    <template v-else-if="props.name === 'user'">
      <circle cx="12" cy="8" r="3.4" />
      <path d="M4 20c.8-3.8 3.9-6 8-6s7.2 2.2 8 6" />
    </template>

    <template v-else-if="props.name === 'close'">
      <path d="M6 6l12 12" />
      <path d="M18 6L6 18" />
    </template>

    <template v-else-if="props.name === 'plus'">
      <path d="M12 5v14" />
      <path d="M5 12h14" />
    </template>

    <template v-else-if="props.name === 'moon'">
      <path d="M21 14.5A8.5 8.5 0 1 1 9.5 3A7 7 0 1 0 21 14.5Z" />
    </template>

    <template v-else-if="props.name === 'sun'">
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2.2" />
      <path d="M12 19.8V22" />
      <path d="M2 12h2.2" />
      <path d="M19.8 12H22" />
      <path d="M4.9 4.9l1.6 1.6" />
      <path d="M17.5 17.5l1.6 1.6" />
      <path d="M19.1 4.9l-1.6 1.6" />
      <path d="M6.5 17.5l-1.6 1.6" />
    </template>

    <template v-else-if="props.name === 'search'">
      <circle cx="11" cy="11" r="6.5" />
      <path d="M20 20l-3.6-3.6" />
    </template>

    <template v-else-if="props.name === 'qr'">
      <rect x="3" y="3" width="6" height="6" rx="1" />
      <rect x="15" y="3" width="6" height="6" rx="1" />
      <rect x="3" y="15" width="6" height="6" rx="1" />
      <path d="M13 13h2v2h-2z" />
      <path d="M17 13h4v4h-2v-2h-2z" />
      <path d="M13 17h2v4h-4v-2h2z" />
      <path d="M17 19h2v2h-2z" />
    </template>
  </svg>
</template>
