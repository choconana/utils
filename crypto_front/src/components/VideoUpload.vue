<template>
  <div>
    <el-upload
      ref="videoUpload"
      action="自定义上传"
      list-type="picture-card"
      accept=".mp4"
      :http-request="videoUpload"
      :limit="limit"
      :before-upload="beforeUpload"
      :before-remove="beforeRemove"
      :on-preview="isPreview && handlePreview"
      :on-exceed="handleExceed"
      :on-progress="handleProgress"
      :on-success="handleSuccess"
      :file-list="fileList"
      :disabled="disabled"
      :class="{hide: !canUpload}"
      :key="key"
    >
      <i class="el-icon-plus"></i>
      <div slot="tip" class="el-upload__tip t-tip">{{ tipMsg }}</div>
    </el-upload>
    <el-dialog :visible.sync="dialogVisible" append-to-body v-if="isPreview">
      <video width="100%" :src="dialogUrl" controls />
    </el-dialog>
    <div v-show="videoList[0]" style="display: none; position:absolute; right: 999999px">
      <canvas id="coverCanvas" />
      <video id="coverVideo" :src="videoList[0] && videoList[0].url" controls="controls" />
    </div>
  </div>
</template>
<script>
import { uploadApi } from '@/api/upload'
import { compressImage, deepClone } from '@/utils/util'
import base from '@/api/base'
export default {
  name: 'VideoUpload',
  props: {
      // 上传数量限制
      limit: {
          type: Number,
          required: false,
          default: () => {
              return 1
          }
      },
      // 接口请求参数
      params: {
          type: Object,
          require: false,
          default: () => {
              return {}
          }
      },
      // 文件大小限制
      fileSize: {
          type: Number,
          required: false,
          default: () => {
              return 100
          }
      },
      // 是否支持预览
      isPreview: {
          type: Boolean,
          required: false,
          default: () => {
              return true
          }
      },
      defaultVideos: {
          type: Array,
          required: false,
          default: () => {
              return []
          }
      },
      disabled: {
          type: Boolean,
          required: false,
          default: () => {
              return false
          }
      },
      // 是否允许上传
      canUpload: {
          type: Boolean,
          required: false,
          default: () => {
              return true
          }
      },
      tipMsg: {
          type: String,
          required: false,
          default: () => {
              return 'aaaaa'
          }
      },
      mode: {
          type: String,
          required: false,
          default: () => {
              return ''
          }
      }
  },
  data () {
      return {
        tmpVideoList: this.defaultVideos, 
        fileList: [],
        dialogVisible: false,
        coverUrlList: [],
        coverList: [],
        isAllowUpload: true,
        videoList: [],
        videoUrlList: [],
        dialogUrl: '',
        coverFile: '',
        imgsrc: '',
        relativeCoverPathList: [], // 视频封面相对路径
        key: 0
      }
  },
  computed: {
  },
  watch: {
    // videoList(val) {
    //   this.tmpVideoList = val
    // }
  },
  methods: {
    // 初始化视频组件的值
    init () {
      this.fileList = []
      this.dialogUrl = ''
      this.videoList = []
      // this.tmpVideoList = []
    },
    // 上传前
    beforeUpload (file) {
      const isLimitSize = (file.size / Math.pow(1024, 2)) > this.fileSize
      if (isLimitSize) {
          this.isAllowUpload = false
          this.$message.warning(`上传视频大小不能超过 ${this.fileSize}MB!`)
          return false
      }
      return file
    },
    // 自定义上传行为
    videoUpload (params) {
      const file = params.file
      const formData = new FormData()
      formData.append('uploadFile', file)
      uploadApi.videoUpload(this.params, formData, progressEvent => {
          this.uploadProgress(progressEvent, params)
      }).then(
        data => {
          const { respCode, respInfo } = data
          if (respCode === this.$cons.respCode.normal) {
            // 上传成功
            this.$message({
                message: this.$cons.message.uploadSuccess,
                type: 'success',
                center: true
            })
            if (this.limit > 1) {
              this.videoUrlList.push({ url: data.videoUrl })
              this.fileList.push({ url: data.videoUrl })
            } else {
              this.videoUrlList[0] = { url: data.videoUrl }
              this.fileList[0] = { url: data.videoUrl }
            }
            params.onSuccess()
          } else {
            // 上传失败
            const message = respInfo || this.$cons.message.uploadFail
            this.$message({
              message,
              type: 'error',
              center: true
            })
            params.onError(file)
          }
        },
        () => {
            params.onError(file)
            this.$message({
                message: this.$cons.respInfo.requestFail,
                type: 'error',
                center: true
            })
        }
      )
    },
    uploadProgress (progressEvent, params) {
        const complete = (progressEvent.loaded / progressEvent.total * 100 | 0)
        params.file.progressFlag = true
        params.file.successFlag = false
        params.onProgress({ percent: complete })
    },
    // 删除前
    beforeRemove (file) {
      const k = this.fileList.findIndex(x => x.url === file.url)
      const k1 = this.relativeCoverPathList.findIndex(x => x.url === file.url)
      const k2 = this.videoUrlList.findIndex(x => x.url === file.url)
      // const k3 = this.tmpVideoList.findIndex(x => x.url === file.url)
      if (!this.isAllowUpload) {
          this.isAllowUpload = true
          return true
      }
      this.fileList.splice(k, 1)
      this.relativeCoverPathList.splice(k1, 1)
      this.videoUrlList.splice(k2, 1)
      // this.tmpVideoList.splice(k3, 1)
      // this.$emit('postVideos', this.fileList, this.relativeCoverPathList, this.videoUrlList)
      this.$emit('postVideos', this.fileList, this.relativeCoverPathList, this.videoUrlList, this.videoList)
    },
    handleProgress (event, file, fileList) {
        this.fileList = fileList
        const idx = this.fileList.findIndex(item => {
            return item.name === file.name
        })
        if (idx >= 0) {
            this.fileList[idx].progressFlag = true
            this.fileList[idx].successFlag = false
        }
        if (event.percent !== 100) {
            this.fileList[idx].progressPersent = event.percent
        }
    },
    // 预览
    handlePreview (file) {
      if (this.mode === 'add') {
        this.dialogUrl = this.videoList[0].url
      } else if (this.mode === 'edit') {
        // this.dialogUrl = this.tmpVideoList[0].url
        this.dialogUrl = this.defaultVideos[0].url
      }
      
      this.dialogVisible = true
    },
    handleExceed () {
      this.$message.warning(`当前限制选择 ${this.limit} 个视频`)
    },
    handleSuccess (res, file, fileList) {
      // 防止多次删除重新上传视频后，视频列表数据不正确
      if (this.limit > 1) {
       this.videoList.push(file)
      } else {
       this.videoList[0] = file
      }
      // this.tmpVideoList = this.videoList
      this.fileList = fileList
      const idx = this.fileList.findIndex(item => {
          return item.name === file.name
      })
      if (idx >= 0) {
          this.fileList[idx].progressFlag = true
          if (file.status === 'success') {
              this.fileList[idx].successFlag = true
          }
      }
      this.$nextTick(() => {
        this.getCover(file.url)
      })
      this.fileList = []
    },
    // 截取视频第一帧作为缩略图
    getCover(url, file) {
      const video = document.querySelector('#coverVideo')
      video.src = url 
      var canvas = document.getElementById('coverCanvas')
      const ctx = canvas.getContext('2d') 
      video.crossOrigin = 'anonymous'
      video.currentTime = 1 
      video.oncanplay = () => {
        canvas.width = video.videoWidth 
        canvas.height = video.videoHeight 
        ctx.drawImage(video, 0, 0, video.videoWidth, video.videoHeight)
        this.imgsrc = canvas.toDataURL('image/png') 
        const res = this.convert2File(this.imgsrc, 'cover.png')
        this.getVideoImgUrl(res)
      }
    },
    convert2File(url, filename) {
      const arr = url.split(',')
      const mime = arr[0].match(/:(.*?);/)[1]
      const bstr = atob(arr[1])
      let n = bstr.length
      const u8arr = new Uint8Array(n)
      while (n--) {
        u8arr[n] = bstr.charCodeAt(n)
      }
      return new File([u8arr], filename, { type: mime })
    },
    getVideoImgUrl(res) {
      this.coverImgUpload(res)
    },
    coverImgUpload (params) {
      const file = params
      const formData = new FormData()
      formData.append('uploadFile', file)
      return uploadApi.upload(this.params, formData, progressEvent => {
      }).then(
        data => {
          const { respCode, respInfo } = data
          if (respCode === this.$cons.respCode.normal) {
            // 上传成功
            if (this.limit > 1) {
              this.relativeCoverPathList.push({
                url: data.picUrl
              })
              this.fileList.push({ 
                url: encodeURI(`${base.url}/fileDownload/sys/picDownload?picUrl=${data.picUrl}`)
              })
            } else {
              this.relativeCoverPathList[0] = { url: data.picUrl }
              this.fileList[0] = { url: encodeURI(`${base.url}/fileDownload/sys/picDownload?picUrl=${data.picUrl}`) }
            }
            this.key++
            this.$emit('postVideos', this.fileList, this.relativeCoverPathList, this.videoUrlList, this.videoList)
          } else {
            // 上传失败
            const message = respInfo || this.$cons.message.uploadFail
            this.$message({
                message,
                type: 'error',
                center: true
            })
          }
        },
        () => {
          params.onError(file)
          this.$message({
              message: this.$cons.respInfo.requestFail,
              type: 'error',
              center: true
          })
        }
      )
    },
  }
}
</script>
<style lang="less" scoped>
/deep/.hide .el-upload--picture-card {
  display: none;
}
</style>
