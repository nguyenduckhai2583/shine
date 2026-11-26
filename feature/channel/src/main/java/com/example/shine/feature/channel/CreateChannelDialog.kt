package com.example.shine.feature.channel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun CreateChannelDialog(
    onDismiss: () -> Unit,
    onCreateChannel: (name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val channelNameState = rememberTextFieldState()
    val isCreateEnabled by remember {
        derivedStateOf { channelNameState.text.isNotBlank() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Channel",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Channel is where conversations happen around a topic. Use a name that’s easy to find and understand.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    state = channelNameState,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Channel name") },
                    placeholder = { Text("e.g. plan-launch") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Tag,
                            contentDescription = null,
                        )
                    },
                    inputTransformation = InputTransformation.byValue { _, proposed ->
                        proposed.toString().replace(' ', '-').lowercase()
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                    ),
                    onKeyboardAction = {
                        if (isCreateEnabled) {
                            onCreateChannel(channelNameState.text.toString().trim())
                        }
                    },
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreateChannel(channelNameState.text.toString().trim()) },
                enabled = isCreateEnabled,
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier,
    )
}
