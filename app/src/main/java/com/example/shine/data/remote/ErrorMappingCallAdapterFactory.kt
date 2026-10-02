package com.example.shine.data.remote

import com.example.shine.data.remote.dto.ApiErrorResponse
import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException
import kotlinx.serialization.json.Json
import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.Type

class ErrorMappingCallAdapterFactory(private val json: Json) : CallAdapter.Factory() {

    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): CallAdapter<*, *>? {
        if (getRawType(returnType) != Call::class.java) return null

        @Suppress("UNCHECKED_CAST")
        val delegate = retrofit.nextCallAdapter(this, returnType, annotations) as CallAdapter<Any, Any>
        return object : CallAdapter<Any, Any> {
            override fun responseType(): Type = delegate.responseType()

            override fun adapt(call: Call<Any>): Any = delegate.adapt(ErrorMappingCall(call))
        }
    }

    private inner class ErrorMappingCall<T>(private val delegate: Call<T>) : Call<T> {

        override fun enqueue(callback: Callback<T>) {
            delegate.enqueue(object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    if (response.isSuccessful) {
                        callback.onResponse(this@ErrorMappingCall, response)
                    } else {
                        callback.onFailure(this@ErrorMappingCall, HttpException(response).toAppException())
                    }
                }

                override fun onFailure(call: Call<T>, t: Throwable) {
                    callback.onFailure(this@ErrorMappingCall, t.toAppException())
                }
            })
        }

        override fun execute(): Response<T> {
            val response = try {
                delegate.execute()
            } catch (e: Exception) {
                throw e.toAppException()
            }
            if (!response.isSuccessful) throw HttpException(response).toAppException()
            return response
        }

        override fun clone(): Call<T> = ErrorMappingCall(delegate.clone())

        override fun isExecuted(): Boolean = delegate.isExecuted

        override fun cancel() = delegate.cancel()

        override fun isCanceled(): Boolean = delegate.isCanceled

        override fun request(): Request = delegate.request()

        override fun timeout(): Timeout = delegate.timeout()
    }

    private fun Throwable.toAppException(): AppException = when (this) {
        is AppException -> this
        is HttpException -> AppException(AppError.SERVER, serverMessage = parseMessage(), cause = this)
        is IOException -> AppException(AppError.NO_INTERNET, cause = this)
        else -> AppException(AppError.UNKNOWN, cause = this)
    }

    private fun HttpException.parseMessage(): String? {
        val body = response()?.errorBody()?.string() ?: return null
        return runCatching { json.decodeFromString<ApiErrorResponse>(body).message }.getOrNull()
    }
}
