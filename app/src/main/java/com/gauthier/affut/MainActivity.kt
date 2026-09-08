package com.gauthier.affut

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.gauthier.affut.data.repository.AuthRepository
import com.gauthier.affut.ui.navigation.AppNavGraph
import com.gauthier.affut.ui.theme.AffutTheme

private sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object Error : AuthUiState
    data object SignedIn : AuthUiState
    /** Connexion Google impossible, mais l'utilisateur a choisi de continuer quand même :
     *  carte, spots locaux, boussole, météo et navigation restent utilisables. Seul le
     *  partage avec l'autre utilisateur ne fonctionne pas tant que la connexion échoue. */
    data object Offline : AuthUiState
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AffutTheme {
                var authState by remember { mutableStateOf<AuthUiState>(AuthUiState.Loading) }
                var attempt by remember { mutableStateOf(0) }
                val repository = remember { AuthRepository() }
                val webClientId = stringResource(R.string.default_web_client_id)

                LaunchedEffect(attempt) {
                    authState = AuthUiState.Loading
                    val result = repository.ensureSignedIn(this@MainActivity, webClientId)
                    authState = if (result.isSuccess) AuthUiState.SignedIn else AuthUiState.Error
                }

                when (authState) {
                    AuthUiState.SignedIn, AuthUiState.Offline -> {
                        val navController = rememberNavController()
                        AppNavGraph(navController = navController)
                    }
                    AuthUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    AuthUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Text(
                                    "Connexion impossible. Vérifie ta connexion internet et qu'un compte Google est configuré sur l'appareil.",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Button(onClick = { attempt++ }) {
                                    Text("Réessayer")
                                }
                                // La carte, les spots déjà enregistrés, la boussole, la météo et la
                                // navigation restent utilisables sans connexion — seul le partage
                                // avec l'autre utilisateur est indisponible tant que la connexion échoue.
                                Button(onClick = { authState = AuthUiState.Offline }) {
                                    Text("Continuer hors-ligne")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
