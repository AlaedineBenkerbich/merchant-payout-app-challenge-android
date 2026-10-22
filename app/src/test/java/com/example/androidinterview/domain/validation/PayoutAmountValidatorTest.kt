package com.example.androidinterview.domain.validation

import com.example.androidinterview.domain.model.Currency
import com.example.androidinterview.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals

class PayoutAmountValidatorTest {

    private fun validate(input: String, currency: Currency = Currency.GBP) =
        PayoutAmountValidator.validate(input, currency)

    private fun gbp(minorUnits: Long) = AmountValidation.Valid(Money(minorUnits, Currency.GBP))

    @Test
    fun `should convert typed amount to minor units when input is a valid decimal`() {
        // Given
        val inputs = listOf("400", "400.5", "0.01", ".5", "7.")

        // When
        val results = inputs.map { validate(it) }

        // Then
        assertEquals(listOf(gbp(40_000), gbp(40_050), gbp(1), gbp(50), gbp(700)), results)
    }

    @Test
    fun `should read commas as thousands separators when they group digits in threes`() {
        // Given
        val inputs = listOf("1,000", "5,000.00", "1,234,567.89", "999,999")

        // When
        val results = inputs.map { validate(it) }

        // Then
        assertEquals(listOf(gbp(100_000), gbp(500_000), gbp(123_456_789), gbp(99_999_900)), results)
    }

    @Test
    fun `should read a lone comma followed by one or two digits as a decimal comma`() {
        // Given
        val inputs = listOf("12,34", "12,5", "1,00", "12,")

        // When
        val results = inputs.map { validate(it) }

        // Then
        assertEquals(listOf(gbp(1_234), gbp(1_250), gbp(100), gbp(1_200)), results)
    }

    @Test
    fun `should keep the selected currency when amount is valid`() {
        // Given
        val input = "25"

        // When
        val result = validate(input, Currency.EUR)

        // Then
        assertEquals(AmountValidation.Valid(Money(2_500, Currency.EUR)), result)
    }

    @Test
    fun `should report empty when there is nothing to read yet`() {
        // Given
        val inputs = listOf("", "  ", "-", ".", ",", "-.")

        // When
        val results = inputs.map { validate(it) }

        // Then
        assertEquals(List(inputs.size) { AmountValidation.Empty }, results)
    }

    @Test
    fun `should reject amount when it is zero or negative`() {
        // Given
        val inputs = listOf("0", "0.00", "-0", "-5", "-12.50", "-1,000")

        // When
        val results = inputs.map { validate(it) }

        // Then
        assertEquals(List(inputs.size) { AmountValidation.NotPositive }, results)
    }

    @Test
    fun `should refuse to guess when the separators are ambiguous`() {
        // Given: a comma before four digits, mixed styles, repeated separators.
        val inputs = listOf("1,2500", "12,345,67", "1.000,50", "1.2.3", "1,2,3", "1,000,5")

        // When
        val results = inputs.map { validate(it) }

        // Then
        assertEquals(List(inputs.size) { AmountValidation.NotANumber }, results)
    }

    @Test
    fun `should reject amount when input is not a number`() {
        // Given
        val inputs = listOf("abc", "1e3", "--5", "5-", "12a", "£5")

        // When
        val results = inputs.map { validate(it) }

        // Then
        assertEquals(List(inputs.size) { AmountValidation.NotANumber }, results)
    }

    @Test
    fun `should reject amount when it has more decimals than the currency allows`() {
        // Given
        val inputs = listOf("12.345", "1,000.001")

        // When
        val results = inputs.map { validate(it) }

        // Then
        assertEquals(List(inputs.size) { AmountValidation.TooManyDecimals }, results)
    }

    @Test
    fun `should ignore trailing zeros when counting decimals`() {
        // Given
        val input = "12.500"

        // When
        val result = validate(input)

        // Then
        assertEquals(gbp(1_250), result)
    }

    @Test
    fun `should reject amount when it exceeds what the api can carry`() {
        // Given
        val largestAccepted = "21474836.47"
        val oneMinorUnitTooMany = "21474836.48"

        // When
        val accepted = validate(largestAccepted)
        val rejected = validate(oneMinorUnitTooMany)

        // Then
        assertEquals(gbp(Int.MAX_VALUE.toLong()), accepted)
        assertEquals(AmountValidation.TooLarge, rejected)
    }
}
