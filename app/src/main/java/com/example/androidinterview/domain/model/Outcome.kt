package com.example.androidinterview.domain.model

/** Everything that can go wrong when talking to the banking API, as seen by the rest of the app. */
sealed interface DomainError {
    data object NoConnection : DomainError
    data object ServiceUnavailable : DomainError
    data object InsufficientFunds : DomainError
    data class Rejected(val message: String?) : DomainError
    data object Unexpected : DomainError
}

/** Result of an operation that can fail with a [DomainError]; failures are values, not exceptions. */
sealed interface Outcome<out T> {
    data class Success<out T>(val value: T) : Outcome<T>
    data class Failure(val error: DomainError) : Outcome<Nothing>
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}
