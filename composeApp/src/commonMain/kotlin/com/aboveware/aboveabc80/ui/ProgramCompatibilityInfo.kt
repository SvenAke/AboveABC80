package com.aboveware.aboveabc80.ui

import aboveabc80.composeapp.generated.resources.Res
import aboveabc80.composeapp.generated.resources.program_compatibility_info_button
import aboveabc80.composeapp.generated.resources.program_compatibility_info_disclaimer
import aboveabc80.composeapp.generated.resources.program_compatibility_info_title
import aboveabc80.composeapp.generated.resources.program_compatibility_reason_basic_ii
import aboveabc80.composeapp.generated.resources.program_compatibility_reason_commands
import aboveabc80.composeapp.generated.resources.program_compatibility_reason_extension_800
import aboveabc80.composeapp.generated.resources.program_compatibility_reason_extension_806
import aboveabc80.composeapp.generated.resources.program_compatibility_reason_unknown
import aboveabc80.composeapp.generated.resources.program_compatibility_tag_abc800
import aboveabc80.composeapp.generated.resources.program_compatibility_tag_abc800_806
import aboveabc80.composeapp.generated.resources.program_compatibility_tag_abc806
import aboveabc80.composeapp.generated.resources.program_compatibility_tag_unknown
import com.aboveware.aboveabc80.ProgramCompatibilityEvidence
import com.aboveware.aboveabc80.ProgramCompatibilityHint
import com.aboveware.aboveabc80.ProgramModelHint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProgramCompatibilityTag(hint: ProgramCompatibilityHint) {
    if (hint.model == ProgramModelHint.UNKNOWN) return

    val label = when (hint.model) {
        ProgramModelHint.ABC800 -> stringResource(Res.string.program_compatibility_tag_abc800)
        ProgramModelHint.ABC806 -> stringResource(Res.string.program_compatibility_tag_abc806)
        ProgramModelHint.ABC800_806 -> stringResource(Res.string.program_compatibility_tag_abc800_806)
        ProgramModelHint.UNKNOWN -> stringResource(Res.string.program_compatibility_tag_unknown)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun ProgramCompatibilityInfoDialog(
    fileName: String,
    hint: ProgramCompatibilityHint,
    onDismiss: () -> Unit
) {
    val reason = when (hint.evidence) {
        ProgramCompatibilityEvidence.EXTENSION_800 ->
            stringResource(Res.string.program_compatibility_reason_extension_800)
        ProgramCompatibilityEvidence.EXTENSION_806 ->
            stringResource(Res.string.program_compatibility_reason_extension_806)
        ProgramCompatibilityEvidence.BASIC_II_RECORD ->
            stringResource(Res.string.program_compatibility_reason_basic_ii)
        ProgramCompatibilityEvidence.HIGH_RESOLUTION_COMMANDS ->
            stringResource(Res.string.program_compatibility_reason_commands)
        ProgramCompatibilityEvidence.INSUFFICIENT ->
            stringResource(Res.string.program_compatibility_reason_unknown)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.program_compatibility_info_title, fileName)) },
        text = {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hint.model == ProgramModelHint.UNKNOWN) {
                        Text(stringResource(Res.string.program_compatibility_tag_unknown))
                    } else {
                        ProgramCompatibilityTag(hint)
                    }
                }
                Text(reason)
                Text(
                    stringResource(Res.string.program_compatibility_info_disclaimer),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.program_compatibility_info_button))
            }
        }
    )
}
