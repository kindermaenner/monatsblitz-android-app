package de.kindermaenner.monatsblitz.ui.tournamentsetup.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.kindermaenner.monatsblitz.domain.model.GameMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameModeDropdownSelector(
    selectedMode: GameMode?,
    availableModes: List<GameMode> = emptyList(),
    onModeSelected: (GameMode) -> Unit,
    onAddNewMode: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    val displayModes = remember(availableModes, selectedMode) {
        val list = availableModes.toMutableList()
        if (selectedMode != null && list.none { it.name.equals(selectedMode.name, ignoreCase = true) }) {
            list.add(0, selectedMode)
        }
        list
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {

        OutlinedTextField(
            value = selectedMode?.displayName ?: "",
            placeholder = { Text("Modus auswählen...") },
            onValueChange = {},
            readOnly = true,
            label = { Text("Modus") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            modifier = Modifier
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true
                )
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            displayModes.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.displayName) },
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    }
                )
            }

            if (onAddNewMode != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Neuer Modus...",
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        expanded = false
                        onAddNewMode()
                    }
                )
            }
        }
    }
}
