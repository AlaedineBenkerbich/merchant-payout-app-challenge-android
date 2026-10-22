package com.example.androidinterview.domain.security

import com.example.androidinterview.domain.model.Currency
import com.example.androidinterview.domain.model.Money
import com.example.androidinterview.testing.eur
import com.example.androidinterview.testing.gbp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiometricPolicyTest {

    private val policy = BiometricPolicy()

    @Test
    fun `should skip biometrics when amount is below the threshold`() {
        // Given
        val amount = gbp(99_999)

        // When
        val required = policy.requiresAuthentication(amount)

        // Then
        assertFalse(required)
    }

    @Test
    fun `should skip biometrics when amount equals the threshold`() {
        // Given
        val amount = gbp(100_000)

        // When
        val required = policy.requiresAuthentication(amount)

        // Then
        assertFalse(required)
    }

    @Test
    fun `should require biometrics when amount is above the threshold`() {
        // Given
        val amount = gbp(100_001)

        // When
        val required = policy.requiresAuthentication(amount)

        // Then
        assertTrue(required)
    }

    @Test
    fun `should apply the threshold in the selected currency when paying out in euros`() {
        // Given
        val justBelow = eur(100_000)
        val justAbove = eur(100_001)

        // When
        val belowRequired = policy.requiresAuthentication(justBelow)
        val aboveRequired = policy.requiresAuthentication(justAbove)

        // Then
        assertFalse(belowRequired)
        assertTrue(aboveRequired)
    }

    @Test
    fun `should expose the threshold in the requested currency when asked for it`() {
        // Given
        val currency = Currency.EUR

        // When
        val threshold = policy.threshold(currency)

        // Then
        assertEquals(Money(100_000, Currency.EUR), threshold)
    }
}
