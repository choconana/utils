<template>
  <div>
    <el-upload
      action="自定义上传"
      list-type="picture-card"
      accept=".jpg,.png,.jpeg"
      :http-request="httpRequest"
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
    >
      <i class="el-icon-plus"></i>
      <div slot="tip" class="el-upload__tip t-tip">{{ tipMsg }}</div>
    </el-upload>
    <el-dialog :visible.sync="dialogVisible" append-to-body v-if="isPreview">
      <img width="100%" :src="dialogImageUrl" alt="" />
    </el-dialog>
  </div>
</template>
<script>
import { uploadApi } from '@/api/upload'
import { compressImage } from '@/utils/util'
export default {
    name: 'ImageUpload',
    props: {
        // 图片上传数量限制
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
        // 图片大小限制
        fileSize: {
            type: Number,
            required: false,
            default: () => {
                return 1
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
        // 默认加载图片 [{ url: 'xxxxx' }]
        defaultImages: {
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
                return '只支持jpeg/jpg/png图片格式'
            }
        }
    },
    data () {
        return {
            dialogVisible: false,
            dialogImageUrl: '',
            isAllowUpload: true,
            picUrlArr: []
        }
    },
    computed: {
        fileList: {
            get () {
                return this.defaultImages
            },
            set () {
                this.picUrlArr = this.defaultImages
                // ...
            }
        }
    },
    methods: {
        // 上传前
        beforeUpload (file) {
            const isLimitSize = (file.size / Math.pow(1024, 2)) > this.fileSize
            console.log(isLimitSize)
            if (isLimitSize) {
                this.isAllowUpload = false
                this.$message.warning(`上传图片大小不能超过 ${this.fileSize}MB!`)
                return false
            }
            return compressImage(file)
        },
        // 自定义上传行为
        httpRequest (params) {
            const file = params.file
            const formData = new FormData()
            formData.append('uploadFile', file)
            uploadApi.upload(this.params, formData, progressEvent => {
                // console.log(progressEvent)
                this.uploadProgress(progressEvent, params)
            }).then(
                data => {
                    // console.log(data, 'data')
                    const { respCode, respInfo } = data
                    if (respCode === this.$cons.respCode.normal) {
                        // 上传成功
                        this.$message({
                            message: this.$cons.message.uploadSuccess,
                            type: 'success',
                            center: true
                        })
                        if (this.limit > 1) {
                            this.picUrlArr.push({ url: data.picUrl })
                            this.fileList.push({ url: data.picUrl })
                        } else {
                            this.picUrlArr[0] = { url: data.picUrl }
                            this.fileList[0] = { url: data.picUrl }
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
            // console.log(params)
            const complete = (progressEvent.loaded / progressEvent.total * 100 | 0)
            params.file.progressFlag = true
            params.file.successFlag = false
            params.onProgress({ percent: complete })
        },
        // 删除前
        beforeRemove (file) {
            const k = this.picUrlArr.findIndex(x => x.url === file.url)
            if (!this.isAllowUpload) {
                this.isAllowUpload = true
                return true
            }
            this.picUrlArr.splice(k, 1)
            this.$emit('postImages', this.picUrlArr)
        },
        handleProgress (event, file, fileList) {
            console.log(event, file, fileList)
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
            this.dialogImageUrl = file.url
            this.dialogVisible = true
        },
        handleExceed () {
            this.$message.warning(`当前限制选择 ${this.fileSize} 张图片`)
        },
        handleSuccess (res, file, fileList) {
            console.log(res, file, fileList)
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
            this.$emit('postImages', this.picUrlArr)
        }
    }
}
</script>
<style lang="less" scoped>
/deep/.hide .el-upload--picture-card {
  display: none;
}
</style>
