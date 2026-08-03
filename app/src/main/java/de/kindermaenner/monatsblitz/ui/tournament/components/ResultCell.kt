package de.kindermaenner.monatsblitz.ui.tournament.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.kindermaenner.monatsblitz.domain.model.GameResult


@Composable
fun ResultCell(
    value: String,
    enabled: Boolean,
    onResultSelected: (GameResult) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(40.dp)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline
                )
                .clickable(enabled = enabled) {
                    expanded = true
                },
            contentAlignment = Alignment.Center
        ) {
            Text(value)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(GameResult.Win.displayName) },
                onClick = {
                    onResultSelected(GameResult.Win)
                    expanded = false
                }
            )

            DropdownMenuItem(
                text = { Text(GameResult.Remis.displayName) },
                onClick = {
                    onResultSelected(GameResult.Remis)
                    expanded = false
                }
            )

            DropdownMenuItem(
                text = { Text(GameResult.Loss.displayName) },
                onClick = {
                    onResultSelected(GameResult.Loss)
                    expanded = false
                }
            )

            DropdownMenuItem(
                text = { Text(GameResult.ForfeitWin.displayName) },
                onClick = {
                    onResultSelected(GameResult.ForfeitWin)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(GameResult.ForfeitLoss.displayName) },
                onClick = {
                    onResultSelected(GameResult.ForfeitLoss)
                    expanded = false
                }
            )
        }
    }
}
