package com.example.shine.domain.model

sealed class AuthException(message: String) : Exception(message) {
    class Network : AuthException("No internet connection")

    class TwoFactorRequired : AuthException("Two-factor verification is not supported yet")

    class Server(message: String) : AuthException(message)

    class Unknown(message: String?) : AuthException(message ?: "Something went wrong")
}
