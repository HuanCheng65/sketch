# 下载图片到磁盘

使用 [DownloadRequest] 可以将图片下载到磁盘，如下：

```kotlin
DownloadRequest(context, "https://www.sample.com/image.jpg") {
    listener(
        onSuccess = { request: DownloadRequest, result: DownloadResult.Success ->
            val input = result.data.newInputStream()
            // ...
        }, onSuccess = { request: DownloadRequest, result: DownloadResult.Error ->
            val exception: SketchException = result.exception
            // ...
        }
    )
}.enqueue()
```

当你需要同步获取下载结果时您可以使用 execute 方法，如下：

```kotlin
coroutineScope.launch(Dispatchers.Main) {
    val result: DownloadResult = DownloadRequest(context, "https://www.sample.com/image.jpg").execute()
    if (result is DownloadResult.Success) {
        val input = result.data.newInputStream()
        // ...
    } else if (result is DownloadResult.Error) {
        val exception: SketchException = result.exception
        // ...
    }
}
```

[DownloadRequest]: ../../sketch/src/main/java/com/github/panpf/sketch/request/DownloadRequest.kt