package com.cardkeeper.domain.model

/**
 * Business card fields parsed from OCR output.
 *
 * All fields default to empty string so the correction form can display
 * blank inputs for unrecognized fields without null checks.
 *
 * Pure Kotlin — no Android imports.
 */
data class ParsedCard(
    val name: String = "",
    val company: String = "",
    val jobTitle: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = ""
)
