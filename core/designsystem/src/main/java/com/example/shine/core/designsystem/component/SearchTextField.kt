package com.example.shine.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.shine.core.designsystem.R

@Composable
fun SearchTextField(
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    state: TextFieldState = rememberTextFieldState(),
    enabled: Boolean = true,
    onSearch: (() -> Unit)? = null,
    clearContentDescription: String = stringResource(R.string.clear_search),
) {
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)

    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .collect { currentOnQueryChange(it) }
    }

    SearchTextField(
        state = state,
        placeholder = placeholder,
        modifier = modifier,
        enabled = enabled,
        onSearch = onSearch,
        clearContentDescription = clearContentDescription,
    )
}

@Composable
fun SearchTextField(
    state: TextFieldState,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSearch: (() -> Unit)? = null,
    clearContentDescription: String = stringResource(R.string.clear_search),
) {
    val focusManager = LocalFocusManager.current
    val hasQuery by remember(state) {
        derivedStateOf { state.text.isNotEmpty() }
    }

    OutlinedTextField(
        state = state,
        modifier = modifier,
        enabled = enabled,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (hasQuery) {
                IconButton(onClick = { state.clearText() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = clearContentDescription,
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search,
        ),
        onKeyboardAction = {
            focusManager.clearFocus()
            onSearch?.invoke()
        },
        lineLimits = TextFieldLineLimits.SingleLine,
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchTextFieldEmptyPreview() {
    MaterialTheme {
        SearchTextField(
            state = rememberTextFieldState(),
            placeholder = "Search…",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchTextFieldFilledPreview() {
    MaterialTheme {
        SearchTextField(
            state = rememberTextFieldState(initialText = "Query"),
            placeholder = "Search…",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}
