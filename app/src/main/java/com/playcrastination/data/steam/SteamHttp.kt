package com.playcrastination.data.steam

import com.playcrastination.domain.model.SteamFailure
import com.playcrastination.domain.model.SteamProblem
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.delay
import kotlinx.serialization.SerializationException
import retrofit2.Response

internal class SteamHttp {
    private val retryAt = AtomicLong(0)

    suspend fun <T : Any> request(call: suspend () -> Response<T>): T {
        if (System.currentTimeMillis() < retryAt.get()) throw SteamFailure(SteamProblem.RATE_LIMITED)

        repeat(3) { attempt ->
            val response = try {
                call()
            } catch (_: IOException) {
                if (attempt == 2) throw SteamFailure(SteamProblem.TEMPORARY)
                delay(350L shl attempt)
                return@repeat
            } catch (_: SerializationException) {
                throw SteamFailure(SteamProblem.UNAVAILABLE)
            }
            if (response.isSuccessful) return response.body() ?: throw SteamFailure(SteamProblem.UNAVAILABLE)

            val status = response.code()
            response.errorBody()?.close()
            when (status) {
                401, 403 -> throw SteamFailure(SteamProblem.KEY_REJECTED)
                429 -> {
                    val now = System.currentTimeMillis()
                    val until = retryDeadline(response.headers()["Retry-After"], now)
                    retryAt.updateAndGet { previous -> maxOf(previous, until) }
                    throw SteamFailure(SteamProblem.RATE_LIMITED)
                }
                in 500..599 -> {
                    if (attempt == 2) throw SteamFailure(SteamProblem.TEMPORARY)
                    delay(350L shl attempt)
                }
                else -> throw SteamFailure(SteamProblem.UNAVAILABLE)
            }
        }
        throw SteamFailure(SteamProblem.TEMPORARY)
    }

    private fun retryDeadline(value: String?, now: Long): Long {
        value?.trim()?.toLongOrNull()?.let { seconds ->
            return now + seconds.coerceIn(1L, 2_592_000L) * 1000L
        }
        if (!value.isNullOrBlank()) {
            try {
                return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toInstant().toEpochMilli().coerceAtLeast(now + 1_000)
            } catch (_: DateTimeParseException) {
                // The conservative default below also covers an invalid header.
            }
        }
        return now + 60_000L
    }
}
