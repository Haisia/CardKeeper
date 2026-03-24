package com.cardkeeper.domain.usecase

import com.cardkeeper.domain.model.OcrTextBlock
import com.cardkeeper.domain.model.ParsedCard
import javax.inject.Inject

class ParseOcrResultUseCase @Inject constructor() {

    fun invoke(blocks: List<OcrTextBlock>): ParsedCard {
        if (blocks.isEmpty()) return ParsedCard()

        val remaining = blocks.toMutableList()

        // --- Pass 1: Regex extraction for unambiguous fields ---
        val phone = extractByRegex(remaining, PHONE_REGEX)
        val email = extractByRegex(remaining, EMAIL_REGEX)

        // --- Pass 2: Korean/English label-value pairs ---
        val labelMatches = extractLabelValuePairs(remaining)

        // --- Pass 3: Positional heuristics for leftover blocks ---
        // Sort remaining blocks top-to-bottom, then left-to-right
        val sorted = remaining.sortedWith(compareBy({ it.top }, { it.left }))

        val name = labelMatches["name"]
            ?: extractName(sorted)

        val company = labelMatches["company"]
            ?: extractCompany(sorted)

        val jobTitle = labelMatches["jobTitle"]
            ?: extractJobTitle(sorted)

        val address = labelMatches["address"]
            ?: extractAddress(sorted)

        return ParsedCard(
            name = name,
            company = company,
            jobTitle = jobTitle,
            phone = phone,
            email = email,
            address = address
        )
    }

    // ----------------------------------------------------------------
    // Regex extraction
    // ----------------------------------------------------------------

    private fun extractByRegex(blocks: MutableList<OcrTextBlock>, regex: Regex): String {
        val iterator = blocks.iterator()
        while (iterator.hasNext()) {
            val block = iterator.next()
            val match = regex.find(block.text)
            if (match != null) {
                iterator.remove()
                return match.value.trim()
            }
        }
        return ""
    }

    // ----------------------------------------------------------------
    // Label-value pair detection
    // ----------------------------------------------------------------

    private fun extractLabelValuePairs(blocks: MutableList<OcrTextBlock>): Map<String, String> {
        val result = mutableMapOf<String, String>()

        for (block in blocks.toList()) {
            val fieldKey = matchLabel(block.text) ?: continue

            // Value is the text after the colon on the same block, OR the next block to the right / below
            val inlineValue = block.text
                .substringAfter(":", "")
                .substringAfter("：", "")  // full-width colon
                .trim()

            if (inlineValue.isNotEmpty()) {
                result[fieldKey] = inlineValue
            } else {
                // Find the closest block to the right on the same row
                val sameRowBlocks = blocks.filter { other ->
                    other !== block &&
                    kotlin.math.abs(other.centerY - block.centerY) < block.height &&
                    other.left > block.right
                }
                val valueBlock = sameRowBlocks.minByOrNull { it.left }
                if (valueBlock != null) {
                    result[fieldKey] = valueBlock.text.trim()
                }
            }
        }

        return result
    }

    private fun matchLabel(text: String): String? {
        val t = text.trim().lowercase()
        return when {
            t.startsWith("이름") || t.startsWith("성명") || t.startsWith("name") -> "name"
            t.startsWith("회사") || t.startsWith("소속") || t.startsWith("company") || t.startsWith("organization") -> "company"
            t.startsWith("직책") || t.startsWith("직위") || t.startsWith("title") || t.startsWith("position") -> "jobTitle"
            t.startsWith("주소") || t.startsWith("address") || t.startsWith("addr") -> "address"
            else -> null
        }
    }

    // ----------------------------------------------------------------
    // Positional heuristics
    // ----------------------------------------------------------------

    private fun extractName(sorted: List<OcrTextBlock>): String {
        if (sorted.isEmpty()) return ""
        // Name is typically the tallest (largest font) block in the top third of the card
        val topThirdHeight = sorted.maxOf { it.bottom } / 3
        val topBlocks = sorted.filter { it.top < topThirdHeight }
        val candidates = topBlocks.ifEmpty { sorted.take(3) }
        // Prefer: shortest text (names are short), tallest font
        return candidates
            .filter { it.text.length in 1..20 }
            .maxByOrNull { it.height }
            ?.text
            ?.trim()
            ?: sorted.first().text.trim()
    }

    private fun extractCompany(sorted: List<OcrTextBlock>): String {
        return sorted.firstOrNull { block ->
            val t = block.text
            COMPANY_KEYWORDS.any { t.contains(it, ignoreCase = true) }
        }?.text?.trim() ?: ""
    }

    private fun extractJobTitle(sorted: List<OcrTextBlock>): String {
        return sorted.firstOrNull { block ->
            val t = block.text
            TITLE_KEYWORDS.any { t.contains(it, ignoreCase = true) }
        }?.text?.trim() ?: ""
    }

    private fun extractAddress(sorted: List<OcrTextBlock>): String {
        // Address blocks tend to be near the bottom and are longer than other lines
        val avgHeight = if (sorted.isEmpty()) 0 else sorted.sumOf { it.top } / sorted.size
        val bottomBlocks = sorted.filter { it.top > avgHeight }
        return bottomBlocks
            .filter { it.text.length > 10 }  // addresses are long
            .joinToString(" ") { it.text.trim() }
            .trim()
    }

    // ----------------------------------------------------------------
    // Companion constants
    // ----------------------------------------------------------------

    companion object {
        // Korean phone: 010-XXXX-XXXX, 02-XXX-XXXX, +82-10-XXXX-XXXX, etc.
        val PHONE_REGEX = Regex(
            """(?:\+82[-\s]?)?(?:0\d{1,2}|02)[-\s.]?\d{3,4}[-\s.]?\d{4}"""
        )

        // Standard email
        val EMAIL_REGEX = Regex(
            """[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}"""
        )

        val COMPANY_KEYWORDS = listOf(
            "주식회사", "(주)", "㈜", "유한회사", "합명회사",
            "Corp", "Inc", "Ltd", "LLC", "Co.", "Group", "Holdings",
            "Technology", "Solutions", "Consulting"
        )

        val TITLE_KEYWORDS = listOf(
            "대표", "이사", "팀장", "부장", "차장", "과장", "대리", "사원",
            "수석", "선임", "책임",
            "CEO", "CTO", "COO", "CFO",
            "Director", "Manager", "Engineer", "Developer",
            "Senior", "Junior", "Lead", "Principal",
            "Head of", "VP of"
        )
    }
}
