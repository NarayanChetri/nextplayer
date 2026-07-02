package dev.anilbeesetti.nextplayer.core.domain

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.anilbeesetti.nextplayer.core.common.Dispatcher
import dev.anilbeesetti.nextplayer.core.common.NextDispatchers
import dev.anilbeesetti.nextplayer.core.common.extensions.getStorageVolumes
import dev.anilbeesetti.nextplayer.core.common.extensions.prettyName
import dev.anilbeesetti.nextplayer.core.model.Folder
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Lists the real folders on disk for browsing a move destination, file-manager style.
 *
 * Unlike [GetFolderTreeMediaUseCase] and [GetSortedFoldersUseCase], which are both derived from
 * the videos MediaStore already knows about, this walks the actual filesystem so every folder
 * shows up and can be navigated into — including empty ones and ones with no videos yet.
 */
class GetFileSystemFoldersUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(NextDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * @param path Folder whose immediate subfolders should be listed. When null, lists the
     *             available storage volumes (internal storage, SD card, USB OTG, …) instead.
     */
    suspend operator fun invoke(path: String? = null): List<Folder> = withContext(ioDispatcher) {
        val children = try {
            if (path != null) {
                File(path).listFiles()?.toList().orEmpty()
            } else {
                context.getStorageVolumes()
            }
        } catch (e: SecurityException) {
            emptyList()
        }

        children
            .filter { it.isDirectory && !it.isHidden && !it.name.startsWith(".") }
            .map { it.toFolder() }
            .sortedBy { it.name.lowercase() }
    }

    private fun File.toFolder(): Folder {
        val subFolderCount = try {
            listFiles()?.count { it.isDirectory && !it.isHidden && !it.name.startsWith(".") } ?: 0
        } catch (e: SecurityException) {
            0
        }
        return Folder(
            name = prettyName,
            path = path,
            parentPath = parent,
            dateModified = lastModified(),
            foldersCount = subFolderCount,
        )
    }
}