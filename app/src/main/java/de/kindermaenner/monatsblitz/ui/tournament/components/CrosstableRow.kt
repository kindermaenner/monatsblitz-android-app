package de.kindermaenner.monatsblitz.ui.tournament.components

import android.R
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.kindermaenner.monatsblitz.domain.model.Tournament


@Composable
fun CrosstableRow(
    tournament: Tournament,
    rowIndex: Int,
    playerName : String,
    horizontalScrollState: ScrollState,
    results : List<String>,
    setResult : (rowIndex: Int, columnIndex: Int, result: de.kindermaenner.monatsblitz.domain.model.GameResult) -> Unit
) {
    val NumberWidth = 40.dp
    val NameWidth = 140.dp

    Row(
        modifier = Modifier.height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Nummer
        Box(
            modifier = Modifier.width(NumberWidth),
            contentAlignment = Alignment.Center
        ) {
            Text((rowIndex + 1).toString())
        }

        // Spielername
        Box(
            modifier = Modifier.width(NameWidth),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = playerName,
                maxLines = 1
            )
        }

        // Ergebnisspalten
        Row(
            modifier = Modifier.horizontalScroll(horizontalScrollState)
        ) {
            tournament.playerIds.forEachIndexed { columnIndex, _ ->

                ResultCell(
                    value = results[columnIndex],
                    enabled = rowIndex != columnIndex,
                    onResultSelected = { result ->
                        setResult(rowIndex, columnIndex, result)
                    }
                )
            }
        }
    }
}
