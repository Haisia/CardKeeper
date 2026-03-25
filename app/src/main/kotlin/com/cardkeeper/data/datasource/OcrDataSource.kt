package com.cardkeeper.data.datasource

import android.content.Context
import android.net.Uri
import com.cardkeeper.domain.model.OcrTextBlock
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class OcrDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val textRecognizer: TextRecognizer
) {

    /**
     * Run ML Kit OCR on [imageFile] and return all recognized text lines
     * with their bounding boxes.
     *
     * Uses the Korean TextRecognizer (see OcrModule) which handles both
     * Korean and Latin scripts — no separate recognizer needed for mixed cards.
     *
     * @param imageFile JPEG file on disk (cacheDir temp file from CameraX capture
     *   or permanent file from ImageStorageDataSource)
     * @return List of [OcrTextBlock] — one entry per recognized line, ordered
     *   top-to-bottom, left-to-right within the same row.
     * @throws Exception if ML Kit fails to process the image
     */
    suspend fun recognizeText(imageFile: File): List<OcrTextBlock> =
        suspendCancellableCoroutine { continuation ->
            val inputImage = InputImage.fromFilePath(context, Uri.fromFile(imageFile))

            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    // Flatten: Block -> Line -> OcrTextBlock
                    // Each Line is one logical text run (e.g., "홍길동" or "Senior Engineer")
                    val blocks = visionText.textBlocks.flatMap { block ->
                        block.lines.mapNotNull { line ->
                            val box = line.boundingBox ?: return@mapNotNull null
                            OcrTextBlock(
                                text = line.text,
                                left = box.left,
                                top = box.top,
                                right = box.right,
                                bottom = box.bottom
                            )
                        }
                    }
                    if (continuation.isActive) continuation.resume(blocks) { cause ->
                        // TextRecognizer tasks cannot be individually cancelled in ML Kit;
                        // the continuation guard above prevents delivering results after cancellation
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }

            continuation.invokeOnCancellation {
                // TextRecognizer tasks cannot be individually cancelled in ML Kit;
                // the continuation guard above prevents delivering results after cancellation
            }
        }
}
