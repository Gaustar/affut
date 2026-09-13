package com.gauthier.affut.ui.friends

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsGroupsScreen(
    onBack: () -> Unit,
    viewModel: FriendsGroupsViewModel = viewModel(),
) {
    val myProfile by viewModel.myProfile.collectAsState()
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    val profileLoadFailed by viewModel.profileLoadFailed.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val message by viewModel.message.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var friendCodeInput by remember { mutableStateOf("") }
    var groupNameInput by remember { mutableStateOf("") }
    var groupCodeInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Amis et groupes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualiser")
                    }
                },
            )
        },
    ) { padding ->
        if (!isSignedIn) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Non connecté : les amis et groupes nécessitent une connexion Google " +
                        "active. Connecte-toi depuis Paramètres puis reviens ici.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Mon code", style = MaterialTheme.typography.titleMedium)
                if (profileLoadFailed) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Impossible de récupérer ton code (réseau ?).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = viewModel::retryProfile) { Text("Réessayer") }
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            myProfile?.friendCode ?: "…",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        myProfile?.let { profile ->
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(profile.friendCode))
                            }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copier le code")
                            }
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Ajoute-moi sur Affût avec mon code ami : ${profile.friendCode}",
                                    )
                                }
                                context.startActivity(Intent.createChooser(intent, "Partager mon code"))
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = "Partager le code")
                            }
                        }
                    }
                    Text(
                        "Communique ce code à quelqu'un pour qu'il t'ajoute comme ami.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            message?.let { msg ->
                item {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(msg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            TextButton(onClick = viewModel::dismissMessage) { Text("OK") }
                        }
                    }
                    LaunchedEffect(msg) {
                        friendCodeInput = ""
                        groupNameInput = ""
                        groupCodeInput = ""
                    }
                }
            }

            item {
                HorizontalDivider()
                Text("Ajouter un ami", style = MaterialTheme.typography.titleMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = friendCodeInput,
                        onValueChange = { friendCodeInput = it },
                        label = { Text("Code ami") },
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = { viewModel.addFriendByCode(friendCodeInput) }) { Text("Ajouter") }
                }
            }

            item {
                Text("Mes amis (${friends.size})", style = MaterialTheme.typography.titleMedium)
                if (friends.isEmpty()) {
                    Text("Aucun ami pour l'instant.", style = MaterialTheme.typography.bodySmall)
                }
            }

            items(friends, key = { it.uid }) { friend ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(friend.displayName)
                    TextButton(onClick = { viewModel.removeFriend(friend.uid) }) { Text("Retirer") }
                }
            }

            item {
                HorizontalDivider()
                Text("Créer un groupe", style = MaterialTheme.typography.titleMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = groupNameInput,
                        onValueChange = { groupNameInput = it },
                        label = { Text("Nom du groupe") },
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = { viewModel.createGroup(groupNameInput) }) { Text("Créer") }
                }
            }

            item {
                HorizontalDivider()
                Text("Rejoindre un groupe", style = MaterialTheme.typography.titleMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = groupCodeInput,
                        onValueChange = { groupCodeInput = it },
                        label = { Text("Code groupe") },
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = { viewModel.joinGroupByCode(groupCodeInput) }) { Text("Rejoindre") }
                }
            }

            item {
                Text("Mes groupes (${groups.size})", style = MaterialTheme.typography.titleMedium)
                if (groups.isEmpty()) {
                    Text("Aucun groupe pour l'instant.", style = MaterialTheme.typography.bodySmall)
                }
            }

            items(groups, key = { it.id }) { group ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(group.name)
                        Text(
                            "Code ${group.code} · ${group.memberCount} membre(s)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(onClick = { viewModel.leaveGroup(group.id) }) { Text("Quitter") }
                }
            }
        }
    }
}
