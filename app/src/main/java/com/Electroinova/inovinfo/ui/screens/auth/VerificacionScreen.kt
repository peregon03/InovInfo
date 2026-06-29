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
import androidx.compose.material.icons.filled.MarkEmailRead
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
import com.Electroinova.inovinfo.ui.theme.*

/**
 * [modo] = "registro" → verifica código de registro de nuevo usuario
 * [modo] = "recuperacion" → verifica código + ingresa nueva contraseña
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificacionScreen(
    viewModel: AuthViewModel,
    navController: NavController,
    modo: String
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var codigo          by remember { mutableStateOf("") }
    var nuevaPassword   by remember { mutableStateOf("") }
    var showPassword    by remember { mutableStateOf(false) }

    val esRecuperacion = modo == "recuperacion"

    LaunchedEffect(codigo, nuevaPassword) { viewModel.limpiarError() }

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
                title = {
                    Text(
                        if (esRecuperacion) "Cambiar contraseña" else "Verificar email",
                        color = ElectroOnPrimary
                    )
                },
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
                Spacer(Modifier.height(16.dp))

                // Icono
                Icon(
                    imageVector        = Icons.Default.MarkEmailRead,
                    contentDescription = null,
                    modifier           = Modifier.size(64.dp),
                    tint               = ElectroGold
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text       = if (esRecuperacion) "Revisa tu correo" else "Verifica tu cuenta",
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color      = ElectroOnPrimary
                )

                Text(
                    text     = "Enviamos un código de 6 dígitos a\n${uiState.emailPendiente}",
                    fontSize = 14.sp,
                    color    = ElectroOnPrimary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                )

                // Mensaje informativo (ej. "Código enviado a...")
                if (uiState.mensaje != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ElectroGold.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text     = uiState.mensaje!!,
                            color    = ElectroGoldDark,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(containerColor = ElectroBackground)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Código
                        OutlinedTextField(
                            value         = codigo,
                            onValueChange = { if (it.length <= 6) codigo = it.filter(Char::isDigit) },
                            label         = { Text("Código de 6 dígitos") },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction    = if (esRecuperacion) ImeAction.Next else ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                onDone = {
                                    focusManager.clearFocus()
                                    if (!esRecuperacion) viewModel.verificarRegistro(codigo)
                                }
                            ),
                            colors = authTextFieldColors()
                        )

                        // Nueva contraseña (solo en recuperación)
                        if (esRecuperacion) {
                            OutlinedTextField(
                                value         = nuevaPassword,
                                onValueChange = { nuevaPassword = it },
                                label         = { Text("Nueva contraseña") },
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
                                        viewModel.verificarRecuperacion(codigo, nuevaPassword)
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
                        }

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

                        // Botón verificar
                        Button(
                            onClick = {
                                if (esRecuperacion) viewModel.verificarRecuperacion(codigo, nuevaPassword)
                                else viewModel.verificarRegistro(codigo)
                            },
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
                                    text       = if (esRecuperacion) "Cambiar contraseña" else "Verificar código",
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
