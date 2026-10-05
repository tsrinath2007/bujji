package com.pupil.app.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.pupil.app.BuildConfig
import com.pupil.app.R
import com.pupil.app.core.AppConstants
import com.pupil.app.core.creature.CreatureDialogue
import com.pupil.app.ui.components.CreatureState
import com.pupil.app.ui.components.CreatureWidget
import com.pupil.app.ui.theme.PupilPrimaryLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun LoginScreen(
    onLoginSuccess: (userName: String, userEmail: String) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Branding Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Bujji Logo
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_monochrome),
                    contentDescription = "Bujji Logo",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = AppConstants.APP_NAME,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Teach it. It grows.\nYour personal AI study companion",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Friendly Creature Mascot (Curious)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                CreatureWidget(
                    state = CreatureState.CURIOUS,
                    level = 1,
                    totalXp = 0,
                    streakDays = 1,
                    speechBubbleText = "Welcome! Let's study together!",
                    showProgressBar = false,
                    compact = false
                )
            }

            // Authentication Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                // Google Sign In Button
                val context = LocalContext.current
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            try {
                                val serverClientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID
                                if (serverClientId.isBlank()) {
                                    errorMessage = "Google Client ID not configured"
                                    isLoading = false
                                    return@launch
                                }

                                val credentialManager = CredentialManager.create(context)
                                val googleIdOption = GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(serverClientId)
                                    .setAutoSelectEnabled(false)
                                    .build()

                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()

                                val result = credentialManager.getCredential(
                                    request = request,
                                    context = context
                                )

                                val credential = result.credential
                                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                    val idToken = googleIdTokenCredential.idToken
                                    val displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id.substringBefore("@")
                                    val email = googleIdTokenCredential.id

                                    // Hand off ID token to Supabase /auth/v1/token?grant_type=id_token
                                    val supabaseUrl = BuildConfig.SUPABASE_URL
                                    val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY
                                    if (supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()) {
                                        withContext(Dispatchers.IO) {
                                            try {
                                                val endpoint = URL("$supabaseUrl/auth/v1/token?grant_type=id_token")
                                                val conn = endpoint.openConnection() as HttpURLConnection
                                                conn.requestMethod = "POST"
                                                conn.setRequestProperty("apikey", supabaseAnonKey)
                                                conn.setRequestProperty("Content-Type", "application/json")
                                                conn.doOutput = true

                                                val jsonBody = JSONObject().apply {
                                                    put("provider", "google")
                                                    put("id_token", idToken)
                                                }
                                                conn.outputStream.use { os ->
                                                    os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                                                }

                                                val responseCode = conn.responseCode
                                                val respText = if (responseCode in 200..299) {
                                                    conn.inputStream.bufferedReader().use { it.readText() }
                                                } else {
                                                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                                                }

                                                if (responseCode in 200..299) {
                                                    val respJson = JSONObject(respText)
                                                    val userObj = respJson.optJSONObject("user")
                                                    val userId = userObj?.optString("id")
                                                    val accessToken = respJson.optString("access_token")

                                                    // Upsert into Supabase Table Editor: public.profiles table
                                                    if (!userId.isNullOrBlank()) {
                                                        try {
                                                            val profileUrl = URL("$supabaseUrl/rest/v1/profiles")
                                                            val profileConn = profileUrl.openConnection() as HttpURLConnection
                                                            profileConn.requestMethod = "POST"
                                                            profileConn.setRequestProperty("apikey", supabaseAnonKey)
                                                            profileConn.setRequestProperty("Authorization", "Bearer ${if (accessToken.isNotBlank()) accessToken else supabaseAnonKey}")
                                                            profileConn.setRequestProperty("Content-Type", "application/json")
                                                            profileConn.setRequestProperty("Prefer", "resolution=merge-duplicates")
                                                            profileConn.doOutput = true

                                                            val profileBody = JSONObject().apply {
                                                                put("id", userId)
                                                                put("email", email)
                                                                put("name", displayName)
                                                                put("photo_url", googleIdTokenCredential.profilePictureUri?.toString() ?: "")
                                                                put("xp", 0)
                                                                put("level", 1)
                                                                put("streak", 1)
                                                            }

                                                            profileConn.outputStream.use { pos ->
                                                                pos.write(profileBody.toString().toByteArray(Charsets.UTF_8))
                                                            }
                                                            val pCode = profileConn.responseCode
                                                            android.util.Log.i("SupabaseAuth", "Profiles upsert status: $pCode")
                                                        } catch (pe: Exception) {
                                                            android.util.Log.w("SupabaseAuth", "Profile sync warning: ${pe.message}")
                                                        }
                                                    }
                                                } else {
                                                    android.util.Log.w("SupabaseAuth", "Supabase token exchange HTTP $responseCode: $respText")
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("SupabaseAuth", "Error contacting Supabase", e)
                                            }
                                        }
                                    }

                                    isLoading = false
                                    onLoginSuccess(displayName, email)
                                } else {
                                    errorMessage = "Unexpected credential format received"
                                    isLoading = false
                                }
                            } catch (e: GetCredentialCancellationException) {
                                // User cancelled the Google picker sheet
                                isLoading = false
                            } catch (e: GetCredentialException) {
                                isLoading = false
                                errorMessage = "Google sign-in error: ${e.localizedMessage ?: "Unknown error"}"
                                android.util.Log.e("GoogleAuth", "CredentialManager error", e)
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = "Authentication failed: ${e.localizedMessage ?: "Please try again"}"
                                android.util.Log.e("GoogleAuth", "General error", e)
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PupilPrimaryLight,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Connecting to Google...", fontWeight = FontWeight.Bold)
                    } else {
                        // Google "G" Badge
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("G", fontWeight = FontWeight.Bold, color = PupilPrimaryLight, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Continue as Guest / Skip
                TextButton(
                    onClick = onSkip,
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Continue as guest",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
