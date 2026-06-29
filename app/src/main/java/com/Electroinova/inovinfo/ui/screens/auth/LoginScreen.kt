package com.Electroinova.inovinfo.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Electroinova.inovinfo.ui.navigation.Screen
import com.Electroinova.inovinfo.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Limpiar error al cambiar campos
    LaunchedEffect(email, password) { viewModel.limpiarError() }

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
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(48.dp))

            // Logo / marca
            Text(
                text       = "InovInfo",
                fontSize   = 40.sp,
                fontWeight = FontWeight.Bold,
                color      = ElectroGold
            )
            Text(
                text      = "Electroinova Soluciones",
                fontSize  = 14.sp,
                color     = ElectroOnPrimary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // Card con formulario
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(containerColor = ElectroBackground)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text       = "Iniciar sesión",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = ElectroNavyBlue
                    )

                    // Email
                    OutlinedTextField(
                        value         = email,
                        onValueChange = { email = it },
                        label         = { Text("Correo electrónico") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction    = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = authTextFieldColors()
                    )

                    // Contraseña
                    OutlinedTextField(
                        value         = password,
                        onValueChange = { password = it },
                        label         = { Text("Contraseña") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        visualTransformation = if (showPassword) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.login(email, password)
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector        = if (showPassword) Icons.Default.VisibilityOff
                                                        else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Ocultar" else "Mostrar"
                                )
                            }
                        },
                        colors = authTextFieldColors()
                    )

                    // Mensaje de error
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

                    // Botón ingresar
                    Button(
                        onClick  = { viewModel.login(email, password) },
                        enabled  = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape  = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectroNavyBlue)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color    = ElectroOnPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text       = "Ingresar",
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 16.sp
                            )
                        }
                    }

                    // Recuperar contraseña
                    TextButton(
                        onClick  = { navController.navigate(Screen.RecuperarPassword.route) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text  = "¿Olvidaste tu contraseña?",
                            color = ElectroNavyBlueLight,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Registrar primer usuario (solo si aún no existe coordinadora)
            if (uiState.primerUsuario) {
                Spacer(Modifier.height(24.dp))
                OutlinedButton(
                    onClick  = { navController.navigate(Screen.Register.route) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = ElectroGold),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, ElectroGold)
                ) {
                    Text(
                        text       = "Crear cuenta de coordinadora",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text     = "Aún no hay usuarios registrados en el sistema",
                    color    = ElectroOnPrimary.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = ElectroNavyBlue,
    focusedLabelColor    = ElectroNavyBlue,
    cursorColor          = ElectroNavyBlue
)
