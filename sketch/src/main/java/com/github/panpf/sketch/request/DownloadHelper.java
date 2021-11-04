/*
 * Copyright (C) 2019 panpf <panpfpanpf@outlook.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.panpf.sketch.request;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.panpf.sketch.SLog;
import com.github.panpf.sketch.Sketch;
import com.github.panpf.sketch.cache.DiskCache;
import com.github.panpf.sketch.uri.UriModel;
import com.github.panpf.sketch.util.SketchUtils;

public class DownloadHelper {

    private static final String NAME = "DownloadHelper";

    @NonNull
    private final DownloadOptions downloadOptions;
    @NonNull
    private Sketch sketch;
    @NonNull
    private String uri;
    @Nullable
    private DownloadListener downloadListener;
    private boolean sync;
    @Nullable
    private DownloadProgressListener downloadProgressListener;

    public DownloadHelper(@NonNull Sketch sketch, @NonNull String uri, @Nullable DownloadListener downloadListener) {
        this.sketch = sketch;
        this.uri = uri;
        this.downloadListener = downloadListener;
        this.downloadOptions = new DisplayOptions();
    }

    // todo 补充测试 options

    /**
     * Limit request processing depth
     */
    @NonNull
    public DownloadHelper requestLevel(@Nullable RequestLevel requestLevel) {
        if (requestLevel != null) {
            downloadOptions.setRequestLevel(requestLevel);
        }
        return this;
    }

    @NonNull
    public DownloadHelper disableCacheInDisk() {
        downloadOptions.setCacheInDiskDisabled(true);
        return this;
    }

    /**
     * Batch setting download parameters, all reset
     */
    @NonNull
    public DownloadHelper options(@Nullable DownloadOptions newOptions) {
        downloadOptions.copy(newOptions);
        return this;
    }

    @NonNull
    public DownloadHelper downloadProgressListener(@Nullable DownloadProgressListener downloadProgressListener) {
        this.downloadProgressListener = downloadProgressListener;
        return this;
    }

    /**
     * Synchronous execution
     *
     * @deprecated Use the {@link #execute()} method instead
     */
    @NonNull
//    @Deprecated
    public DownloadHelper sync() {
        this.sync = true;
        return this;
    }

    /**
     * @deprecated Use the {@link #execute()} or {@link #enqueue(DownloadListener)} method instead
     */
    @Nullable
//    @Deprecated
    public DownloadRequest commit() {
        // Cannot run on UI threads
        if (sync && SketchUtils.isMainThread()) {
            throw new IllegalStateException("Cannot sync perform the download in the UI thread ");
        }

        // Uri cannot is empty
        if (TextUtils.isEmpty(uri)) {
            SLog.em(NAME, "Uri is empty");
            CallbackHandler.postCallbackError(downloadListener, ErrorCause.URI_INVALID, sync);
            return null;
        }

        // Uri type must be supported
        final UriModel uriModel = UriModel.match(sketch, uri);
        if (uriModel == null) {
            SLog.emf(NAME, "Unsupported uri type. %s", uri);
            CallbackHandler.postCallbackError(downloadListener, ErrorCause.URI_NO_SUPPORT, sync);
            return null;
        }

        // Only support http ot https
        if (!uriModel.isFromNet()) {
            SLog.emf(NAME, "Only support http ot https. %s", uri);
            CallbackHandler.postCallbackError(downloadListener, ErrorCause.URI_NO_SUPPORT, sync);
            return null;
        }

        processOptions();

        final String key = SketchUtils.makeRequestKey(uri, uriModel, downloadOptions.makeKey());
        if (!checkDiskCache(key, uriModel)) {
            return null;
        }

        return submitRequest(key, uriModel);
    }

    private void processOptions() {
        sketch.getConfiguration().getOptionsFilterManager().filter(downloadOptions);
    }

    private boolean checkDiskCache(@NonNull String key, @NonNull UriModel uriModel) {
        if (!downloadOptions.isCacheInDiskDisabled()) {
            DiskCache diskCache = sketch.getConfiguration().getDiskCache();
            DiskCache.Entry diskCacheEntry = diskCache.get(uriModel.getDiskCacheKey(uri));
            if (diskCacheEntry != null) {
                if (SLog.isLoggable(SLog.DEBUG)) {
                    SLog.dmf(NAME, "Download image completed. %s", key);
                }
                if (downloadListener != null) {
                    downloadListener.onCompleted(new CacheDownloadResult(diskCacheEntry, ImageFrom.DISK_CACHE));
                }
                return false;
            }
        }

        return true;
    }

    @NonNull
    private DownloadRequest submitRequest(@NonNull String key, @NonNull UriModel uriModel) {
        CallbackHandler.postCallbackStarted(downloadListener, sync);

        DownloadRequest request = new DownloadRequest(sketch, uri, uriModel, key,
                downloadOptions, downloadListener, downloadProgressListener);
        request.setSync(sync);

        if (SLog.isLoggable(SLog.DEBUG)) {
            SLog.dmf(NAME, "Run dispatch submitted. %s", key);
        }
        request.submitDispatch();

        return request;
    }

//    // todo 优化流程
//    public void enqueue(@NonNull DownloadListener listener) {
//        this.downloadListener = listener;
//        this.sync = false;
//        commit();
//    }
//
//    @NonNull
//    public DownloadResponse execute() {
//        this.sync = true;
//        final DownloadResponse[] response = new DownloadResponse[1];
//        this.downloadListener = new DownloadListener() {
//            @Override
//            public void onStarted() {
//
//            }
//
//            @Override
//            public void onCompleted(@NonNull DownloadResult result) {
//                response[0] = new DownloadSuccessResponse(result);
//            }
//
//            @Override
//            public void onError(@NonNull ErrorCause cause) {
//                response[0] = new DownloadErrorResponse(cause);
//            }
//
//            @Override
//            public void onCanceled(@NonNull CancelCause cause) {
//                response[0] = new DownloadCancelResponse(cause);
//            }
//        };
//        commit();
//        return response[0];
//    }
}