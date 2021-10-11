package me.panpf.sketch.sample.item

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.github.panpf.assemblyadapter.BindingItemFactory
import com.github.panpf.tools4a.activity.ktx.safeStartActivity
import me.panpf.sketch.sample.ImageOptions
import me.panpf.sketch.sample.bean.AppInfo
import me.panpf.sketch.sample.databinding.ListItemAppBinding
import me.panpf.sketch.sample.widget.SampleImageView
import me.panpf.sketch.uri.AppIconUriModel

class AppItemFactory : BindingItemFactory<AppInfo, ListItemAppBinding>(AppInfo::class) {

    override fun createItemViewBinding(
        context: Context,
        inflater: LayoutInflater,
        parent: ViewGroup
    ) = ListItemAppBinding.inflate(inflater, parent, false)

    override fun initItem(
        context: Context,
        binding: ListItemAppBinding,
        item: BindingItem<AppInfo, ListItemAppBinding>
    ) {
        binding.imageInstalledAppIcon.apply {
            setOptions(ImageOptions.ROUND_RECT)
            page = SampleImageView.Page.APP_LIST
        }

        binding.root.setOnClickListener {
            val data = item.dataOrThrow
            val intent = context.packageManager.getLaunchIntentForPackage(data.packageName!!)
            if (intent != null) {
                context.safeStartActivity(intent)
            }
        }
    }

    override fun bindItemData(
        context: Context,
        binding: ListItemAppBinding,
        item: BindingItem<AppInfo, ListItemAppBinding>,
        bindingAdapterPosition: Int,
        absoluteAdapterPosition: Int,
        data: AppInfo
    ) {
        binding.imageInstalledAppIcon.displayImage(
            AppIconUriModel.makeUri(data.packageName, data.versionCode)
        )
        binding.textInstalledAppName.text = data.name
        binding.textInstalledAppInfo.text = data.versionName
    }
}
