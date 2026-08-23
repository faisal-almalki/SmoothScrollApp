package com.densitech.scrollsmooth.ui.downloader

import android.content.Context
import androidx.media3.common.util.UnstableApi
import com.densitech.scrollsmooth.ui.media.MediaHttpDataSource
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.offline.DownloadManager
import java.util.concurrent.Executors

@UnstableApi
object DownloadManagerSingleton {
    @Volatile
    private var instance: DownloadManager? = null

    fun getInstance(context: Context): DownloadManager {
        return instance ?: synchronized(this) {
            instance ?: buildDownloadManager(context)
                .also { instance = it }
        }
    }

    private fun buildDownloadManager(context: Context): DownloadManager {
        val cronetDataSourceFactory = MediaHttpDataSource.factory(context)

        return DownloadManager(
            context,
            StandaloneDatabaseProvider(context),
            DownloadVideoCache.getInstance(context),
            cronetDataSourceFactory,
            Executors.newSingleThreadExecutor()
        )
    }
}