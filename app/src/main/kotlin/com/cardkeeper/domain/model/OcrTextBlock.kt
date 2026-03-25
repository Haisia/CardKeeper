package com.cardkeeper.domain.model

/**
 * A single line of OCR-recognized text with its bounding-box coordinates.
 *
 * Coordinates are pixel values in the original image coordinate space.
 * left < right, top < bottom (origin at top-left of image).
 *
 * This is a pure Kotlin data class — zero Android framework imports.
 * ML Kit types are converted to this model in OcrDataSource before
 * leaving the data layer.
 */
data class OcrTextBlock(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    /** Horizontal center X of the bounding box. */
    val centerX: Int get() = (left + right) / 2

    /** Vertical center Y of the bounding box. */
    val centerY: Int get() = (top + bottom) / 2

    /** Approximate line height in pixels. */
    val height: Int get() = bottom - top

    /** Approximate line width in pixels. */
    val width: Int get() = right - left
}
