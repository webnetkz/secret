<script setup lang="ts">
import { computed, ref } from "vue"

const props = defineProps<{
  src: string
}>()

const videoRef = ref<HTMLVideoElement | null>(null)
const speedSteps = [1, 1.5, 2]
const speedIndex = ref(0)

const speedLabel = computed(() => `${speedSteps[speedIndex.value]}x`)

function toggleSpeed() {
  const video = videoRef.value
  if (!video) {
    return
  }

  speedIndex.value = (speedIndex.value + 1) % speedSteps.length
  video.playbackRate = speedSteps[speedIndex.value]
}
</script>

<template>
  <div class="video-player">
    <video ref="videoRef" class="video-view" controls preload="metadata" :src="props.src" />
    <button class="video-speed" type="button" @click="toggleSpeed">{{ speedLabel }}</button>
  </div>
</template>

<style scoped>
.video-player {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: min(420px, 72vw);
}

.video-view {
  width: 100%;
  border-radius: 12px;
  border: 1px solid #d8deea;
  background: #000;
}

.video-speed {
  align-self: flex-end;
  border: 1px solid #cad3e6;
  background: #fff;
  color: #122036;
  border-radius: 9px;
  padding: 6px 10px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
}
</style>
