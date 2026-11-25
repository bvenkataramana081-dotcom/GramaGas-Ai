package com.example.ai

import android.util.Log
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val DIAG_TAG = "GramaGasAI"

enum class AiErrorType {
    HIGH_DEMAND_OR_RATE_LIMIT,
    TIMEOUT,
    NETWORK,
    SERVICE_UNAVAILABLE,
    EMPTY_RESPONSE
}

data class AiErrorClassification(
    val type: AiErrorType,
    val isRetryable: Boolean,
    val httpStatusCode: Int? = null,
    val rootCauseCategory: String = "UNKNOWN"
)

/**
 * Classifies Gemini / Firebase AI and network exceptions into user-safe categories
 * while extracting HTTP status codes and root-cause diagnostics for the developer console.
 */
fun classifyAiError(throwable: Throwable): AiErrorClassification {
    val chain = generateSequence(throwable) { it.cause }.toList()
    val combinedText = chain
        .joinToString(" ") { "${it::class.java.simpleName} ${it.message.orEmpty()}" }
        .lowercase()

    val statusCode = extractHttpStatusCode(combinedText)

    if (chain.any {
            it is TimeoutCancellationException ||
                it is SocketTimeoutException ||
                it is InterruptedIOException
        } || combinedText.contains("deadline_exceeded") || combinedText.contains("timed out")
    ) {
        return AiErrorClassification(
            type = AiErrorType.TIMEOUT,
            isRetryable = true,
            httpStatusCode = statusCode ?: 504,
            rootCauseCategory = "TIMEOUT_OR_DEADLINE_EXCEEDED"
        )
    }

    if (statusCode == 404 ||
        combinedText.contains("not_found") ||
        combinedText.contains("not found") ||
        combinedText.contains("unknown model")
    ) {
        return AiErrorClassification(
            type = AiErrorType.SERVICE_UNAVAILABLE,
            isRetryable = false,
            httpStatusCode = 404,
            rootCauseCategory = "MODEL_NOT_FOUND_404"
        )
    }

    if (statusCode == 403 ||
        combinedText.contains("permission_denied") ||
        combinedText.contains("app check") ||
        combinedText.contains("appcheck") ||
        combinedText.contains("attestation") ||
        combinedText.contains("forbidden")
    ) {
        return AiErrorClassification(
            type = AiErrorType.SERVICE_UNAVAILABLE,
            isRetryable = false,
            httpStatusCode = 403,
            rootCauseCategory = "PERMISSION_OR_APP_CHECK_403"
        )
    }

    val isRateLimitOrOverloaded =
        statusCode == 429 ||
            statusCode == 503 ||
            combinedText.contains("resource_exhausted") ||
            combinedText.contains("quota") ||
            combinedText.contains("rate limit") ||
            combinedText.contains("rate_limit") ||
            combinedText.contains("too many requests") ||
            combinedText.contains("overloaded") ||
            combinedText.contains("high demand") ||
            combinedText.contains("service unavailable") ||
            combinedText.contains("unavailable")

    if (isRateLimitOrOverloaded) {
        val category = if (statusCode == 429 || combinedText.contains("quota") || combinedText.contains("resource_exhausted")) {
            "QUOTA_OR_RATE_LIMIT_429"
        } else {
            "HIGH_DEMAND_OR_OVERLOADED_503"
        }
        return AiErrorClassification(
            type = AiErrorType.HIGH_DEMAND_OR_RATE_LIMIT,
            isRetryable = true,
            httpStatusCode = statusCode ?: if (category.endsWith("429")) 429 else 503,
            rootCauseCategory = category
        )
    }

    val isTransientServerError =
        statusCode == 500 ||
            statusCode == 502 ||
            statusCode == 504 ||
            combinedText.contains("internal") ||
            combinedText.contains("bad gateway") ||
            combinedText.contains("temporarily")

    if (isTransientServerError) {
        return AiErrorClassification(
            type = AiErrorType.SERVICE_UNAVAILABLE,
            isRetryable = true,
            httpStatusCode = statusCode ?: 500,
            rootCauseCategory = "TRANSIENT_SERVER_ERROR_${statusCode ?: 500}"
        )
    }

    if (chain.any {
            it is UnknownHostException ||
                it is ConnectException ||
                it is IOException
        } || combinedText.contains("unable to resolve host") ||
        combinedText.contains("failed to connect") ||
        combinedText.contains("network") ||
        combinedText.contains("connection reset") ||
        combinedText.contains("no address associated")
    ) {
        return AiErrorClassification(
            type = AiErrorType.NETWORK,
            isRetryable = true,
            httpStatusCode = statusCode,
            rootCauseCategory = "NETWORK_CONNECTIVITY_ERROR"
        )
    }

    return AiErrorClassification(
        type = AiErrorType.SERVICE_UNAVAILABLE,
        isRetryable = false,
        httpStatusCode = statusCode,
        rootCauseCategory = if (statusCode != null) "HTTP_ERROR_$statusCode" else "UNCLASSIFIED_API_ERROR"
    )
}

fun isLikelyGroundingRejection(throwable: Throwable): Boolean {
    val combined = generateSequence(throwable) { it.cause }
        .joinToString(" ") { "${it::class.java.simpleName} ${it.message.orEmpty()}" }
        .lowercase()

    val statusCode = extractHttpStatusCode(combined)
    return statusCode == 400 ||
        statusCode == 403 ||
        statusCode == 429 ||
        combined.contains("search") ||
        combined.contains("grounding") ||
        combined.contains("tool") ||
        combined.contains("quota") ||
        combined.contains("billing") ||
        combined.contains("resource_exhausted") ||
        combined.contains("invalid_argument")
}

fun extractHttpStatusCode(message: String): Int? {
    val regex = Regex("""\b(400|401|403|404|408|429|500|502|503|504)\b""")
    return regex.find(message)?.groupValues?.getOrNull(1)?.toIntOrNull()
}

fun logAiDiagnosticFailure(
    operation: String,
    modelName: String,
    groundingEnabled: Boolean,
    attempt: Int,
    classification: AiErrorClassification,
    throwable: Throwable
) {
    val causeSummary = generateSequence(throwable) { it.cause }
        .joinToString(" -> ") { "${it::class.java.simpleName}: ${it.message}" }
    runCatching {
        Log.e(
            DIAG_TAG,
            "[$operation] FAILED attempt=$attempt model=$modelName grounding=$groundingEnabled " +
                "httpStatus=${classification.httpStatusCode ?: "unknown"} " +
                "rootCause=${classification.rootCauseCategory} retryable=${classification.isRetryable} " +
                "details=$causeSummary",
            throwable
        )
    }
}
