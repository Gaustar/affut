package com.gauthier.affut.ui.sharing

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gauthier.affut.domain.model.Group
import com.gauthier.affut.domain.model.UserProfile

/**
 * Sélecteur de portée de partage (amis + groupes), réutilisé par l'édition de spot et le
 * partage de position en direct. Ni amis ni groupes sélectionnés = rien n'est partagé.
 */
@Composable
fun ShareScopePicker(
    friends: List<UserProfile>,
    groups: List<Group>,
    selectedFriendIds: Set<String>,
    selectedGroupIds: Set<String>,
    onFriendToggle: (String) -> Unit,
    onGroupToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Partager avec", style = MaterialTheme.typography.labelLarge)

        if (friends.isEmpty() && groups.isEmpty()) {
            Text(
                "Aucun ami ni groupe pour l'instant — ajoute-en depuis \"Amis et groupes\".",
                style = MaterialTheme.typography.bodySmall,
            )
            return@Column
        }

        if (friends.isNotEmpty()) {
            Text("Amis", style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                friends.forEach { friend ->
                    FilterChip(
                        selected = friend.uid in selectedFriendIds,
                        onClick = { onFriendToggle(friend.uid) },
                        label = { Text(friend.displayName) },
                    )
                }
            }
        }

        if (groups.isNotEmpty()) {
            Text("Groupes", style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                groups.forEach { group ->
                    FilterChip(
                        selected = group.id in selectedGroupIds,
                        onClick = { onGroupToggle(group.id) },
                        label = { Text(group.name) },
                    )
                }
            }
        }
    }
}
