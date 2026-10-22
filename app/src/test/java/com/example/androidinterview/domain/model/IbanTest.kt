package com.example.androidinterview.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class IbanTest {

    @Test
    fun `should accept iban when country length and checksum are valid`() {
        // Given
        val ibans = listOf(
            "GB29NWBK60161331926819",
            "DE89370400440532013000",
            "FR1420041010050500013M02606",
            "ES9121000418450200051332",
            "NL91ABNA0417164300",
            "IT60X0542811101000000123456",
            "BE68539007547034",
        )

        // When
        val results = ibans.map { Iban.parse(it) }

        // Then
        results.forEachIndexed { index, result ->
            assertIs<IbanValidation.Valid>(result, "expected ${ibans[index]} to be valid")
        }
    }

    @Test
    fun `should normalise spaces and lower case when iban is pasted from a document`() {
        // Given
        val pasted = "gb82 west 1234 5698 7654 32"

        // When
        val result = Iban.parse(pasted)

        // Then
        assertIs<IbanValidation.Valid>(result)
        assertEquals("GB82WEST12345698765432", result.iban.value)
    }

    @Test
    fun `should accept iban from a country outside the length table when checksum is valid`() {
        // Given
        val turkishIban = "TR330006100519786457841326"

        // When
        val result = Iban.parse(turkishIban)

        // Then
        assertIs<IbanValidation.Valid>(result)
    }

    @Test
    fun `should reject iban when it is the invalid example from the exercise`() {
        // Given
        val invalidExample = "FR1212345123451234567A12310131231231231"

        // When
        val result = Iban.parse(invalidExample)

        // Then
        assertEquals(IbanValidation.Invalid(IbanError.TOO_LONG), result)
    }

    @Test
    fun `should reject iban when checksum is wrong`() {
        // Given
        val mistyped = "GB29NWBK60161331926818"

        // When
        val result = Iban.parse(mistyped)

        // Then
        assertEquals(IbanValidation.Invalid(IbanError.INVALID_CHECKSUM), result)
    }

    @Test
    fun `should report too short when the iban is one character short for its country`() {
        // Given
        val oneCharacterShort = "GB29NWBK6016133192681"

        // When
        val result = Iban.parse(oneCharacterShort)

        // Then
        assertEquals(IbanValidation.Invalid(IbanError.TOO_SHORT), result)
    }

    @Test
    fun `should report too long when the iban has an extra character for its country`() {
        // Given
        val oneCharacterLong = "GB29NWBK601613319268190"

        // When
        val result = Iban.parse(oneCharacterLong)

        // Then
        assertEquals(IbanValidation.Invalid(IbanError.TOO_LONG), result)
    }

    @Test
    fun `should only report too short while an iban is being typed from its first character`() {
        // Given
        val typedSoFar = listOf("G", "GB", "GB2", "GB29", "GB29NWBK", "GB29NWBK6016133192681")

        // When
        val results = typedSoFar.map { Iban.parse(it) }

        // Then
        assertEquals(List(typedSoFar.size) { IbanValidation.Invalid(IbanError.TOO_SHORT) }, results)
    }

    @Test
    fun `should report malformed when the first characters can never form an iban`() {
        // Given
        val impossiblePrefixes = listOf("1", "G1", "12", "GBX", "G-")

        // When
        val results = impossiblePrefixes.map { Iban.parse(it) }

        // Then
        assertEquals(List(impossiblePrefixes.size) { IbanValidation.Invalid(IbanError.MALFORMED) }, results)
    }

    @Test
    fun `should reject iban when it contains forbidden characters`() {
        // Given
        val withDashes = "GB29-NWBK-6016-1331-9268-19"

        // When
        val result = Iban.parse(withDashes)

        // Then
        assertEquals(IbanValidation.Invalid(IbanError.MALFORMED), result)
    }

    @Test
    fun `should report empty when input is blank`() {
        // Given
        val blank = "   "

        // When
        val result = Iban.parse(blank)

        // Then
        assertEquals(IbanValidation.Empty, result)
    }

    @Test
    fun `should mask the middle characters when iban is displayed`() {
        // Given
        val iban = (Iban.parse("GB29NWBK60161331926819") as IbanValidation.Valid).iban

        // When
        val masked = iban.masked

        // Then
        assertEquals("GB29**************6819", masked)
    }
}
