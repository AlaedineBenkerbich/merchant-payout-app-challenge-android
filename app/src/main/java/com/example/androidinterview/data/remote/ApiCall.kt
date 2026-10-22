package com.example.androidinterview.data.remote

import com.example.androidinterview.data.remote.dto.ApiErrorDto
import com.example.androidinterview.domain.model.DomainError
import com.example.androidinterview.domain.model.Outcome
import java.io.IOException
import java.time.DateTimeException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

private const val INSUFFICIENT_FUNDS_CODE = "INSUFFICIENT_FUNDS"
private const val FIRST_CLIENT_ERROR = 400
private const val FIRST_SERVER_ERROR = 500

/** Runs an API call and turns every expected failure into an [Outcome.Failure] instead of an exception. */
internal suspend fun <T> apiCall(block: suspend () -> T): Outcome<T> = try {
    Outcome.Success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (exception: HttpException) {
    Outcome.Failure(exception.toDomainError())
} catch (exception: IOException) {
    Outcome.Failure(DomainError.NoConnection)
} catch (exception: SerializationException) {
    Outcome.Failure(DomainError.Unexpected)
} catch (exception: DateTimeException) {
    Outcome.Failure(DomainError.Unexpected)
}

private fun HttpException.toDomainError(): DomainError {
    val apiError = readApiError()
    return when {
        apiError?.code == INSUFFICIENT_FUNDS_CODE -> DomainError.InsufficientFunds
        code() >= FIRST_SERVER_ERROR -> DomainError.ServiceUnavailable
        code() >= FIRST_CLIENT_ERROR -> DomainError.Rejected(apiError?.error)
        else -> DomainError.Unexpected
    }
}

private fun HttpException.readApiError(): ApiErrorDto? {
    val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
    if (body.isNullOrBlank()) return null
    return runCatching { ApiJson.decodeFromString<ApiErrorDto>(body) }.getOrNull()
}
