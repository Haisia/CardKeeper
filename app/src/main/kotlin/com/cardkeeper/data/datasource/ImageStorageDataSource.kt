package com.cardkeeper.data.datasource

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageStorageDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Compress and copy [sourceFile] to permanent internal storage.
     *
     * Scales the image so that the longest side is at most [maxDimension] px,
     * then saves as JPEG at [quality]% compression.
     *
     * @param sourceFile Temp JPEG from cacheDir (camera capture or gallery copy)
     * @return Relative path of the saved file (e.g. "cards/abc123.jpg").
     *   Store this in [CardEntity.imagePath]. Reconstruct absolute path with:
     *   `File(context.filesDir, relativePath).absolutePath`
     * @throws Exception if the source file cannot be decoded or the destination
     *   directory cannot be created
     */
    suspend fun saveImage(
        sourceFile: File,
        maxDimension: Int = 1024,
        quality: Int = 85
    ): String = withContext(Dispatchers.IO) {
        val uuid = UUID.randomUUID().toString()
        val relativePath = "cards/$uuid.jpg"
        val destFile = File(context.filesDir, relativePath)

        destFile.parentFile?.mkdirs()

        val originalBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
            ?: throw IllegalArgumentException("Cannot decode image: ${sourceFile.absolutePath}")

        val scaled = scaleBitmap(originalBitmap, maxDimension)

        destFile.outputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }

        if (scaled !== originalBitmap) {
            scaled.recycle()
        }
        originalBitmap.recycle()

        relativePath
    }

    /**
     * Delete the image file for the given relative path.
     * Safe to call if the file does not exist.
     */
    suspend fun deleteImage(relativePath: String): Unit = withContext(Dispatchers.IO) {
        File(context.filesDir, relativePath).delete()
    }

    /**
     * Return the absolute File for a stored relative path.
     * Use this when loading the image for display (e.g., Coil AsyncImage).
     */
    fun getImageFile(relativePath: String): File =
        File(context.filesDir, relativePath)

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private fun scaleBitmap(source: Bitmap, maxDimension: Int): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxDimension && h <= maxDimension) return source

        val scaleFactor = maxDimension.toFloat() / maxOf(w, h)
        val newW = (w * scaleFactor).toInt()
        val newH = (h * scaleFactor).toInt()
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }
}
