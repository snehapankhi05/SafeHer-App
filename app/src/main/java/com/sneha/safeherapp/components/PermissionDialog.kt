package com.sneha.safeherapp.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.sneha.safeherapp.utils.PermissionManager

@Composable
fun PermissionDialog(
    feature: PermissionManager.Feature,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onGoToSettings: () -> Unit,
    isPermanentlyDenied: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "${feature.displayName} Permission Needed") },
        text = {
            Text(
                text = if (isPermanentlyDenied) {
                    "${feature.description}\n\nYou have permanently denied this permission. Please enable it in the app settings to use this feature."
                } else {
                    feature.description
                }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isPermanentlyDenied) {
                        onGoToSettings()
                    } else {
                        onConfirm()
                    }
                }
            ) {
                Text(text = if (isPermanentlyDenied) "Open Settings" else "Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}
