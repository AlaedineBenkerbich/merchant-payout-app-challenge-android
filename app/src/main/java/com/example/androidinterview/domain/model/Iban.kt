package com.example.androidinterview.domain.model

/** A syntactically valid IBAN: normalised (no spaces, upper case) and checked with ISO 13616 mod 97. */
@JvmInline
value class Iban private constructor(val value: String) {

    /** Keeps the country code, check digits and last four characters, and hides everything else. */
    val masked: String
        get() {
            val hiddenCount = (value.length - VISIBLE_PREFIX - VISIBLE_SUFFIX).coerceAtLeast(0)
            return value.take(VISIBLE_PREFIX) + "*".repeat(hiddenCount) + value.takeLast(VISIBLE_SUFFIX)
        }

    companion object {
        private const val VISIBLE_PREFIX = 4
        private const val VISIBLE_SUFFIX = 4
        private const val MIN_LENGTH = 15
        private const val MAX_LENGTH = 34
        private const val CHECKSUM_MODULUS = 97
        private const val VALID_REMAINDER = 1

        private val SHAPE = Regex("[A-Z]{2}[0-9]{2}[A-Z0-9]*")

        /** What the first characters of a valid IBAN can look like while the country code and check digits are typed. */
        private val SHAPE_IN_PROGRESS = Regex("[A-Z]{1,2}|[A-Z]{2}[0-9]")

        /**
         * Exact length per country for the SEPA area and the UK. Countries missing from this table
         * still go through the generic length bounds and the checksum.
         */
        private val LENGTH_BY_COUNTRY = mapOf(
            "AD" to 24, "AT" to 20, "BE" to 16, "BG" to 22, "CH" to 21, "CY" to 28, "CZ" to 24,
            "DE" to 22, "DK" to 18, "EE" to 20, "ES" to 24, "FI" to 18, "FR" to 27, "GB" to 22,
            "GI" to 23, "GR" to 27, "HR" to 21, "HU" to 28, "IE" to 22, "IS" to 26, "IT" to 27,
            "LI" to 21, "LT" to 20, "LU" to 20, "LV" to 21, "MC" to 27, "MT" to 31, "NL" to 18,
            "NO" to 15, "PL" to 28, "PT" to 25, "RO" to 24, "SE" to 24, "SI" to 19, "SK" to 24,
            "SM" to 27, "VA" to 22,
        )

        fun parse(raw: String): IbanValidation {
            val normalized = raw.filterNot(Char::isWhitespace).uppercase()
            return when {
                normalized.isEmpty() -> IbanValidation.Empty
                SHAPE.matches(normalized) -> validateStructure(normalized)
                SHAPE_IN_PROGRESS.matches(normalized) -> IbanValidation.Invalid(IbanError.TOO_SHORT)
                else -> IbanValidation.Invalid(IbanError.MALFORMED)
            }
        }

        private fun validateStructure(iban: String): IbanValidation {
            val problem = lengthProblem(iban)
            return when {
                problem != null -> IbanValidation.Invalid(problem)
                !hasValidChecksum(iban) -> IbanValidation.Invalid(IbanError.INVALID_CHECKSUM)
                else -> IbanValidation.Valid(Iban(iban))
            }
        }

        /** Telling "too short" from "too long" lets a form stay quiet while the merchant is still typing. */
        private fun lengthProblem(iban: String): IbanError? {
            val exactLength = LENGTH_BY_COUNTRY[iban.take(2)]
            val minimum = exactLength ?: MIN_LENGTH
            val maximum = exactLength ?: MAX_LENGTH
            return when {
                iban.length < minimum -> IbanError.TOO_SHORT
                iban.length > maximum -> IbanError.TOO_LONG
                else -> null
            }
        }

        /** Moves the first four characters to the end, maps letters to 10..35 and checks `mod 97 == 1`. */
        private fun hasValidChecksum(iban: String): Boolean {
            val rearranged = iban.substring(VISIBLE_PREFIX) + iban.substring(0, VISIBLE_PREFIX)
            var remainder = 0
            for (character in rearranged) {
                val digits = if (character.isDigit()) {
                    character.toString()
                } else {
                    (character - 'A' + 10).toString()
                }
                for (digit in digits) {
                    remainder = (remainder * 10 + digit.digitToInt()) % CHECKSUM_MODULUS
                }
            }
            return remainder == VALID_REMAINDER
        }
    }
}

sealed interface IbanValidation {
    data object Empty : IbanValidation
    data class Invalid(val reason: IbanError) : IbanValidation
    data class Valid(val iban: Iban) : IbanValidation
}

enum class IbanError {
    MALFORMED,

    /** Not wrong so far, just incomplete: the merchant may still be typing. */
    TOO_SHORT,
    TOO_LONG,
    INVALID_CHECKSUM,
}
