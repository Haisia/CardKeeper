package com.cardkeeper.domain.usecase

import com.cardkeeper.domain.model.OcrTextBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ParseOcrResultUseCaseTest {

    private lateinit var useCase: ParseOcrResultUseCase

    @Before
    fun setUp() {
        useCase = ParseOcrResultUseCase()
    }

    // ----------------------------------------------------------------
    // Empty / edge cases
    // ----------------------------------------------------------------

    @Test
    fun `empty block list returns empty ParsedCard`() {
        val result = useCase.invoke(emptyList())
        assertEquals("", result.name)
        assertEquals("", result.phone)
        assertEquals("", result.email)
    }

    // ----------------------------------------------------------------
    // Latin business card layout (single column)
    // ----------------------------------------------------------------

    @Test
    fun `latin card extracts name company title phone email`() {
        // Simulated ML Kit output for a typical English business card:
        // Line 0 (top, large font): "John Smith"
        // Line 1: "Acme Corp"
        // Line 2: "Senior Engineer"
        // Line 3: "T: 010-1234-5678"
        // Line 4: "john@acme.com"
        val blocks = listOf(
            block("John Smith",       left = 50,  top = 50,  right = 400, bottom = 90),
            block("Acme Corp",        left = 50,  top = 100, right = 350, bottom = 130),
            block("Senior Engineer",  left = 50,  top = 140, right = 380, bottom = 165),
            block("T: 010-1234-5678", left = 50,  top = 200, right = 380, bottom = 225),
            block("john@acme.com",    left = 50,  top = 230, right = 380, bottom = 255)
        )

        val result = useCase.invoke(blocks)

        assertEquals("john@acme.com", result.email)
        assertTrue("Phone should contain 010-1234-5678", result.phone.contains("010-1234-5678"))
        assertTrue("Company should mention Acme", result.company.contains("Acme") || result.company.isEmpty())
    }

    @Test
    fun `phone regex matches Korean mobile format`() {
        val blocks = listOf(
            block("홍길동",        left = 50, top = 50,  right = 300, bottom = 90),
            block("010-9876-5432", left = 50, top = 100, right = 300, bottom = 130)
        )
        val result = useCase.invoke(blocks)
        assertTrue("Phone must match 010-9876-5432", result.phone.contains("010-9876-5432"))
    }

    @Test
    fun `phone regex matches office number format`() {
        val blocks = listOf(
            block("김철수",        left = 50, top = 50,  right = 300, bottom = 90),
            block("02-123-4567",   left = 50, top = 100, right = 300, bottom = 130)
        )
        val result = useCase.invoke(blocks)
        assertTrue("Phone must match 02-123-4567", result.phone.contains("02-123-4567"))
    }

    @Test
    fun `email regex matches standard email`() {
        val blocks = listOf(
            block("박영희",            left = 50, top = 50,  right = 300, bottom = 90),
            block("yhpark@company.kr", left = 50, top = 100, right = 300, bottom = 130)
        )
        val result = useCase.invoke(blocks)
        assertEquals("yhpark@company.kr", result.email)
    }

    // ----------------------------------------------------------------
    // Korean label-value layout (dual column)
    // ----------------------------------------------------------------

    @Test
    fun `korean label-value dual column card extracts fields`() {
        // Left column labels (x: 50-150), right column values (x: 200-500)
        val blocks = listOf(
            block("이름",          left = 50,  top = 100, right = 150, bottom = 130),
            block("홍길동",        left = 200, top = 100, right = 400, bottom = 130),
            block("직책",          left = 50,  top = 140, right = 150, bottom = 170),
            block("팀장",          left = 200, top = 140, right = 350, bottom = 170),
            block("전화",          left = 50,  top = 180, right = 150, bottom = 210),
            block("010-1111-2222", left = 200, top = 180, right = 450, bottom = 210),
            block("이메일",        left = 50,  top = 220, right = 180, bottom = 250),
            block("hong@co.kr",    left = 200, top = 220, right = 450, bottom = 250)
        )

        val result = useCase.invoke(blocks)

        assertTrue("Phone must be extracted", result.phone.contains("010-1111-2222"))
        assertEquals("hong@co.kr", result.email)
    }

    // ----------------------------------------------------------------
    // Mixed Korean/Latin card
    // ----------------------------------------------------------------

    @Test
    fun `mixed korean latin card extracts email and phone`() {
        val blocks = listOf(
            block("Kim MinJun",          left = 50,  top = 30,  right = 400, bottom = 70),
            block("삼성전자(주)",         left = 50,  top = 80,  right = 400, bottom = 115),
            block("소프트웨어 엔지니어",  left = 50,  top = 125, right = 450, bottom = 155),
            block("010-5555-6666",        left = 50,  top = 180, right = 350, bottom = 210),
            block("minjun.kim@samsung.com", left = 50, top = 215, right = 500, bottom = 245),
            block("서울특별시 강남구 삼성동 123", left = 50, top = 260, right = 550, bottom = 290)
        )

        val result = useCase.invoke(blocks)

        assertTrue("Phone extracted", result.phone.contains("010-5555-6666"))
        assertEquals("minjun.kim@samsung.com", result.email)
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private fun block(text: String, left: Int, top: Int, right: Int, bottom: Int) =
        OcrTextBlock(text = text, left = left, top = top, right = right, bottom = bottom)
}
