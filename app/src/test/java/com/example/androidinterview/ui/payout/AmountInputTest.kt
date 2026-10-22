package com.example.androidinterview.ui.payout

import kotlin.test.Test
import kotlin.test.assertEquals

class AmountInputTest {

    @Test
    fun `should leave an amount untouched when it is already written with digits and separators`() {
        // Given
        val inputs = listOf("1250.75", "1,250.75", "12,5", ".5", "7.")

        // When
        val sanitized = inputs.map(::sanitizeAmountInput)

        // Then
        assertEquals(inputs, sanitized)
    }

    @Test
    fun `should drop currency symbols spaces and letters when they are typed or pasted`() {
        // Given
        val pasted = "£ 5,000.00 GBP"

        // When
        val sanitized = sanitizeAmountInput(pasted)

        // Then
        assertEquals("5,000.00", sanitized)
    }

    @Test
    fun `should keep a leading minus so that the validator can reject the negative amount`() {
        // Given
        val input = "-500"

        // When
        val sanitized = sanitizeAmountInput(input)

        // Then
        assertEquals("-500", sanitized)
    }

    @Test
    fun `should drop a minus sign when it is not the first character`() {
        // Given
        val input = "5-00"

        // When
        val sanitized = sanitizeAmountInput(input)

        // Then
        assertEquals("500", sanitized)
    }

    @Test
    fun `should not reinterpret separators when the amount contains several of them`() {
        // Given: the validator, not the input filter, decides what these mean.
        val input = "1.2.3,4"

        // When
        val sanitized = sanitizeAmountInput(input)

        // Then
        assertEquals("1.2.3,4", sanitized)
    }

    @Test
    fun `should return an empty text when nothing in the input can be part of an amount`() {
        // Given
        val input = "abc £€"

        // When
        val sanitized = sanitizeAmountInput(input)

        // Then
        assertEquals("", sanitized)
    }
}
