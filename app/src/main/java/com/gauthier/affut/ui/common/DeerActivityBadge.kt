package com.gauthier.affut.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gauthier.affut.util.DeerActivityLevel
import com.gauthier.affut.util.DeerActivityResult

private fun colorFor(level: DeerActivityLevel): Color = when (level) {
    DeerActivityLevel.FAVORABLE -> Color(0xFF2E7D32)
    DeerActivityLevel.MOYEN -> Color(0xFFEF6C00)
    DeerActivityLevel.DEFAVORABLE -> Color(0xFFC62828)
}

@Composable
fun DeerActivityBadge(result: DeerActivityResult, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🦌", style = MaterialTheme.typography.titleMedium)
            Text(
                result.level.label,
                color = colorFor(result.level),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text(result.summary(), style = MaterialTheme.typography.bodySmall)
    }
}
