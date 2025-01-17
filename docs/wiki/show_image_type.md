# 显示图片类型角标

`需要导入 sketch-extensions 模块`

sketch-extensions 模块提供的 [SketchImageView] 支持在 View 右下角显示图片类型角标，如下：

```kotlin
sketchImageView.showMimeTypeLogoWithRes(
    mimeTypeIconMap = mapOf(
        "image/gif" to R.drawable.image_type_gif,
        "image/png" to R.drawable.image_type_png,
        "image/jpeg" to R.drawable.image_type_jpeg,
        "image/webp" to R.drawable.image_type_webp,
        "image/bmp" to R.drawable.image_type_bmp,
        "image/svg+xml" to R.drawable.image_type_svg,
        "image/heif" to R.drawable.image_type_heif,
    ),
    margin = 4.dp2px
)
```

> 图片类型角标功能由 [MimeTypeLogoAbility] 实现

[SketchImageView]: ../../sketch-extensions/src/main/java/com/github/panpf/sketch/SketchImageView.kt

[MimeTypeLogoAbility]: ../../sketch-extensions/src/main/java/com/github/panpf/sketch/viewability/MimeTypeLogoAbility.kt