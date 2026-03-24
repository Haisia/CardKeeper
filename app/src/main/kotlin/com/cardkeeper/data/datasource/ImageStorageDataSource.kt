package com.cardkeeper.data.datasource

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ImageStorageDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Phase 2 will implement: saveImage(), deleteImage(), getImageFile()
    // Images stored in context.filesDir/cards/ with UUID filenames
    // CardEntity.imagePath stores relative path: "cards/{uuid}.jpg"
}
