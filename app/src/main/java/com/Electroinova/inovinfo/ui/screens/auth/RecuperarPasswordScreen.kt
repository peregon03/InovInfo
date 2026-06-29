package com.Electroinova.inovinfo.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Electroinova.inovinfo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecuperarPasswordScreen(
    viewModel: AuthViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }

    LaunchedEffect(email) { viewModel.limpiarError() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ElectroNavyBlueDark, ElectroNavyBlue)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            TopAppBar(
                title = { Text("Recuperar contraseña", color = ElectroOnPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = ElectroOnPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))

                Icon(
                    imageVector        = Icons.Default.LockReset,
                    contentDescription = null,
                    modifier           = Modifier.size(64.dp),
                    tint               = ElectroGold
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text       = "¿Olvidaste tu contraseña?",
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color      = ElectroOnPrimary
                )
                Text(
                    text     = "Ingresa tu correo y te enviaremos un código para restablecerla",
                    fontSize = 14.sp,
                    color    = ElectroOnPrimary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(containerColor = ElectroBackground)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value         = email,
                            onValueChange = { email = it },
                            label         = { Text("Correo electrónico") },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction    = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.solicitarRecuperacion(email)
                                }
                            ),
                            colors = authTextFieldColors()
                        )

                        // Error
                        if (uiState.error != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ElectroError.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text     = uiState.error!!,
                                    color    = ElectroError,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Button(
                            onClick  = { viewModel.solicitarRecuperacion(email) },
                            enabled  = !uiState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape  = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectroNavyBlue)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(22.dp),
                                    color       = ElectroOnPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text       = "Enviar código",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(48.dp))
            }
        }
    }
}
