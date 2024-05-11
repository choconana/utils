<script setup>
defineProps({
  msg: {
    type: String,
    required: true
  }
})
</script>

<template>
  <div class="greetings">
    <h1 class="green">{{ msg }}</h1>
    <h3>
      You’ve successfully created a project with
      <a href="https://vitejs.dev/" target="_blank" rel="noopener">Vite</a> +
      <a href="https://vuejs.org/" target="_blank" rel="noopener">Vue 3</a>.
    </h3>
  </div>
  <div v-if="false">
    <VideoUpload
            ref="videoUploadRef"
            :disabled="isDisabled"
            :canUpload="!isDisabled"
            :limit="1"
            :params="params"
            :fileSize="100"
            @postVideos="postVideos"
            :defaultVideos="form.videoList"
            :mode="'edit'"
            tipMsg="只支持mp4格式,视频大小不能超过100MB">
    </VideoUpload>
  </div>
</template>

<script>
import VideoUpload from '@/components/VideoUpload'

export default {
  components: {
    VideoUpload // 视频上传
  }
}
</script>

<style scoped>
h1 {
  font-weight: 500;
  font-size: 2.6rem;
  position: relative;
  top: -10px;
}

h3 {
  font-size: 1.2rem;
}

.greetings h1,
.greetings h3 {
  text-align: center;
}

@media (min-width: 1024px) {
  .greetings h1,
  .greetings h3 {
    text-align: left;
  }
}
</style>
