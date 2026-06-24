package com.arisamtunes.data.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import javax.inject.Inject

class AuthApi @Inject constructor(private val client: HttpClient) {
    suspend fun login(email: String, password: String) = client.post("auth/login") { json(LoginDto(email, password)) }.token()
    suspend fun register(email: String, password: String, name: String) = client.post("auth/register") { json(RegisterDto(email, password, name)) }.token()
    suspend fun me(): UserDto = client.get("auth/me").successBody()

    private suspend fun HttpResponse.token(): TokenDto = successBody()
    private suspend inline fun <reified T> HttpResponse.successBody(): T {
        if (status.isSuccess()) return body()
        val error = runCatching { body<ErrorEnvelopeDto>().error.code }.getOrDefault("INTERNAL_ERROR")
        throw AuthFailure(error)
    }
    private inline fun <reified T> io.ktor.client.request.HttpRequestBuilder.json(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }
}

class AuthFailure(val code: String) : RuntimeException(code)
