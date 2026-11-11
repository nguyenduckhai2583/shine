package com.example.shine.core.domain

class FakeSignOutUseCase(
    private val onSignOut: () -> Unit = {},
) : SignOutUseCase {
    var signOutCalled = false
        private set

    override suspend fun invoke() {
        signOutCalled = true
        onSignOut()
    }
}
