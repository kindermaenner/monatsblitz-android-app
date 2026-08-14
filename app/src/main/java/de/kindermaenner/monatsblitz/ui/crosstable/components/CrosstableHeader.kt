package de.kindermaenner.monatsblitz.ui.crosstable.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.kindermaenner.monatsblitz.domain.model.Tournament

@Composable
fun CrosstableHeader(
    tournament: Tournament,
    horizontalScrollState: ScrollState
) {
    val NumberWidth = 40.dp
    val NameWidth = 140.dp
    val CellWidth = 40.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier.width(NumberWidth),
            contentAlignment = Alignment.Center
        ) {
            Text("Nr")
        }

        Box(
            modifier = Modifier.width(NameWidth),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Spieler")
        }

        Row(
            modifier = Modifier.horizontalScroll(horizontalScrollState)
        ) {
            tournament.playerIds.forEachIndexed { index, _ ->

                Box(
                    modifier = Modifier.width(CellWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Text((index+1).toString())
                }
            }
        }
    }
}
