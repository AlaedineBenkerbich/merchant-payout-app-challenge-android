package com.example.androidinterview.domain.validation

import com.example.androidinterview.domain.model.Currency
import com.example.androidinterview.domain.model.Money
import java.math.BigDecimal
import java.math.BigInteger

sealed interface AmountValidation {
    /** Nothing meaningful typed yet: an empty field, or only a sign or a separator. */
    data object Empty : AmountValidation
    data object NotANumber : AmountValidation
    data object NotPositive : AmountValidation
    data object TooManyDecimals : AmountValidation
    data object TooLarge : AmountValidation
    data class Valid(val money: Money) : AmountValidation
}

/**
 * Turns what the user typed into a [Money] in minor units, without ever going through a float.
 *
 * Separators are interpreted, never silently guessed. Accepted spellings of one thousand two
 * hundred and fifty pounds and fifty pence are `1250.5`, `1,250.50` (commas group the thousands,
 * UK style) and `1250,5` (a lone comma followed by one or two digits is a decimal comma).
 * Anything ambiguous, such as `1,2500` or `1.250,50`, is rejected so the merchant can fix it
 * instead of paying a different amount from the one they meant.
 */
object PayoutAmountValidator {

    /** The payout API carries amounts as 32-bit integers, so anything larger would be rejected. */
    const val MAX_MINOR_UNITS: Long = Int.MAX_VALUE.toLong()

    private val GROUPED_THOUSANDS = Regex("""\d{1,3}(,\d{3})+(\.\d*)?""")
    private val DOT_DECIMAL = Regex("""\d+\.?\d*|\.\d+""")
    private val COMMA_DECIMAL = Regex("""\d*,\d{0,2}""")

    fun validate(input: String, currency: Currency): AmountValidation {
        val trimmed = input.trim()
        val isNegative = trimmed.startsWith('-')
        val unsigned = trimmed.removePrefix("-")

        if (unsigned.none(Char::isDigit) && unsigned.all { it == '.' || it == ',' }) return AmountValidation.Empty

        val plain = when {
            GROUPED_THOUSANDS.matches(unsigned) -> unsigned.replace(",", "")
            DOT_DECIMAL.matches(unsigned) -> unsigned
            COMMA_DECIMAL.matches(unsigned) -> unsigned.replace(',', '.')
            else -> return AmountValidation.NotANumber
        }

        val amount = BigDecimal(plain).stripTrailingZeros()
        return when {
            isNegative || amount.signum() == 0 -> AmountValidation.NotPositive
            amount.scale() > currency.fractionDigits -> AmountValidation.TooManyDecimals
            else -> {
                val minorUnits = amount.setScale(currency.fractionDigits).unscaledValue()
                if (minorUnits > BigInteger.valueOf(MAX_MINOR_UNITS)) {
                    AmountValidation.TooLarge
                } else {
                    AmountValidation.Valid(Money(minorUnits.toLong(), currency))
                }
            }
        }
    }
}
