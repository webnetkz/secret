<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"

const props = withDefaults(
  defineProps<{
    src: string
    avatarUrl?: string | null
    nickname?: string | null
  }>(),
  {
    avatarUrl: null,
    nickname: "user"
  }
)

const audioRef = ref<HTMLAudioElement | null>(null)
const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const instanceId = `audio-${Math.random().toString(36).slice(2, 10)}-${Date.now().toString(36)}`
const AUDIO_PLAY_EVENT = "secret-audio-play"
const isDesktopWave = ref(false)

const speedSteps = [1, 1.5, 2]
const speedIndex = ref(0)
const mobileBarHeights = [6, 10, 14, 11, 8, 16, 12, 9, 17, 13, 8, 15, 11, 7, 14, 10, 6, 12, 18, 11, 8, 16, 12, 7, 14, 9, 6, 13, 17, 11, 8, 14, 10, 7]
const desktopBarHeights = mobileBarHeights.flatMap((height, index, bars) => {
  if (index === bars.length - 1) {
    return [height]
  }
  const next = bars[index + 1]
  return [height, Math.round((height + next) / 2)]
})

let desktopWaveQuery: MediaQueryList | null = null
let desktopWaveListener: ((event: MediaQueryListEvent) => void) | null = null

const speedLabel = computed(() => `${speedSteps[speedIndex.value]}x`)
const progress = computed(() => (duration.value > 0 ? Math.min(1, currentTime.value / duration.value) : 0))
const barHeights = computed(() => (isDesktopWave.value ? desktopBarHeights : mobileBarHeights))
const activeBarCount = computed(() => Math.max(1, Math.round(progress.value * barHeights.value.length)))
const displayAvatar = computed(() => props.avatarUrl || "/default-profile.svg")
const avatarAlt = computed(() => `${props.nickname || "user"} avatar`)

function togglePlay() {
  const audio = audioRef.value
  if (!audio) {
    return
  }

  if (isPlaying.value) {
    audio.pause()
    isPlaying.value = false
    return
  }

  void audio.play()
  isPlaying.value = true
}

function onPlay() {
  isPlaying.value = true
  window.dispatchEvent(
    new CustomEvent<{ id: string }>(AUDIO_PLAY_EVENT, {
      detail: { id: instanceId }
    })
  )
}

function onExternalAudioPlay(event: Event) {
  const customEvent = event as CustomEvent<{ id: string }>
  if (customEvent.detail?.id === instanceId) {
    return
  }

  const audio = audioRef.value
  if (!audio) {
    return
  }

  if (!audio.paused) {
    audio.pause()
  }
  isPlaying.value = false
}

function onTimeUpdate() {
  const audio = audioRef.value
  if (!audio) {
    return
  }
  currentTime.value = audio.currentTime
}

function onLoadedMetadata() {
  const audio = audioRef.value
  if (!audio) {
    return
  }
  duration.value = Number.isFinite(audio.duration) ? audio.duration : 0
  audio.playbackRate = speedSteps[speedIndex.value]
}

function onEnded() {
  isPlaying.value = false
  currentTime.value = duration.value
}

function seek(event: Event) {
  const audio = audioRef.value
  if (!audio) {
    return
  }

  const input = event.target as HTMLInputElement
  const nextValue = Number(input.value)
  audio.currentTime = nextValue
  currentTime.value = nextValue
}

function toggleSpeed() {
  const audio = audioRef.value
  if (!audio) {
    return
  }

  speedIndex.value = (speedIndex.value + 1) % speedSteps.length
  audio.playbackRate = speedSteps[speedIndex.value]
}

function onAvatarError(event: Event) {
  const image = event.target as HTMLImageElement
  if (image.src.endsWith("/default-profile.svg")) {
    return
  }
  image.src = "/default-profile.svg"
}

function formatTime(value: number): string {
  const total = Math.max(0, Math.floor(value))
  const minutes = Math.floor(total / 60)
  const seconds = total % 60
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`
}

watch(
  () => props.src,
  () => {
    const audio = audioRef.value
    if (!audio) {
      return
    }

    audio.pause()
    audio.currentTime = 0
    audio.playbackRate = speedSteps[0]
    speedIndex.value = 0
    currentTime.value = 0
    duration.value = 0
    isPlaying.value = false
  }
)

onMounted(() => {
  window.addEventListener(AUDIO_PLAY_EVENT, onExternalAudioPlay as EventListener)

  desktopWaveQuery = window.matchMedia("(min-width: 1024px)")
  isDesktopWave.value = desktopWaveQuery.matches
  desktopWaveListener = (event: MediaQueryListEvent) => {
    isDesktopWave.value = event.matches
  }
  desktopWaveQuery.addEventListener("change", desktopWaveListener)
})

onBeforeUnmount(() => {
  window.removeEventListener(AUDIO_PLAY_EVENT, onExternalAudioPlay as EventListener)
  if (desktopWaveQuery && desktopWaveListener) {
    desktopWaveQuery.removeEventListener("change", desktopWaveListener)
  }

  const audio = audioRef.value
  if (!audio) {
    return
  }
  audio.pause()
})
</script>

<template>
  <div class="audio-player">
    <audio
      ref="audioRef"
      :src="props.src"
      preload="metadata"
      @timeupdate="onTimeUpdate"
      @loadedmetadata="onLoadedMetadata"
      @ended="onEnded"
      @pause="isPlaying = false"
      @play="onPlay"
    />

    <button class="audio-play" type="button" @click="togglePlay">
      <svg
        v-if="!isPlaying"
        viewBox="0 0 24 24"
        width="16"
        height="16"
        fill="currentColor"
        aria-hidden="true"
      >
        <path d="M8 6.6l10.5 5.4L8 17.4V6.6z" />
      </svg>
      <svg
        v-else
        viewBox="0 0 24 24"
        width="16"
        height="16"
        fill="currentColor"
        aria-hidden="true"
      >
        <rect x="7" y="6" width="3.5" height="12" rx="1" />
        <rect x="13.5" y="6" width="3.5" height="12" rx="1" />
      </svg>
    </button>

    <div class="audio-center">
      <div class="audio-wave-wrap">
        <div
          class="audio-wave"
          :style="{ gridTemplateColumns: `repeat(${barHeights.length}, minmax(0, 1fr))` }"
        >
          <span
            v-for="(barHeight, index) in barHeights"
            :key="index"
            class="audio-bar"
            :class="{
              active: index < activeBarCount,
              moving: isPlaying && index < activeBarCount
            }"
            :style="{
              height: `${barHeight}px`,
              animationDelay: `${index * 0.03}s`
            }"
          />
        </div>

        <input
          class="audio-range"
          type="range"
          min="0"
          :max="duration || 0"
          step="0.05"
          :value="currentTime"
          @input="seek"
        />
      </div>

      <div class="audio-meta">
        <span>{{ formatTime(currentTime) }}</span>
        <button class="audio-speed" type="button" @click="toggleSpeed">{{ speedLabel }}</button>
        <span>{{ formatTime(duration) }}</span>
      </div>
    </div>

    <img class="audio-avatar" :src="displayAvatar" :alt="avatarAlt" loading="lazy" @error="onAvatarError" />
  </div>
</template>

<style scoped>
.audio-player {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) 44px;
  gap: 10px;
  align-items: start;
  width: 100%;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 32px;
  padding: 8px 9px;
  background: linear-gradient(180deg, #20252d 0%, #191e26 100%);
  color: #dce6f8;
}

.audio-play {
  margin-top: 2px;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.14);
  color: #f2f6ff;
  cursor: pointer;
  display: grid;
  place-items: center;
}

.audio-play:hover {
  background: rgba(255, 255, 255, 0.23);
}

.audio-center {
  min-width: 0;
  padding-top: 3px;
}

.audio-wave-wrap {
  position: relative;
  height: 24px;
}

.audio-wave {
  height: 100%;
  display: grid;
  grid-template-columns: repeat(34, minmax(0, 1fr));
  align-items: center;
  gap: 2px;
}

.audio-bar {
  display: block;
  width: 100%;
  border-radius: 999px;
  background: rgba(171, 185, 210, 0.36);
  transform-origin: center;
}

.audio-bar.active {
  background: #66c3ff;
}

.audio-bar.moving {
  animation: pulse 1.1s ease-in-out infinite;
}

.audio-range {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  margin: 0;
  opacity: 0;
  cursor: pointer;
}

.audio-meta {
  margin-top: 5px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  line-height: 1;
  color: #a3b4d2;
}

.audio-speed {
  border: none;
  border-radius: 999px;
  background: rgba(105, 199, 255, 0.2);
  color: #8fddff;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  cursor: pointer;
}

.audio-speed:hover {
  background: rgba(105, 199, 255, 0.3);
}

.audio-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.25);
  background: #24344a;
}

@keyframes pulse {
  0%,
  100% {
    transform: scaleY(1);
  }
  50% {
    transform: scaleY(0.62);
  }
}
</style>
