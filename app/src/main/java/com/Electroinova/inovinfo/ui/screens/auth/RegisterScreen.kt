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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var nombre   by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(nombre, email, password) { viewModel.limpiarError() }

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
            // Top bar
            TopAppBar(
                title = { Text("Registro", color = ElectroOnPrimary) },
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
                Spacer(Modifier.height(8.dp))

                Text(
                    text       = "Crear cuenta",
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color      = ElectroGold
                )
                Text(
                    text      = "El primer usuario registrado será coordinadora",
                    fontSize  = 13.sp,
                    color     = ElectroOnPrimary.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(top = 4.dp, bottom = 32.dp)
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
                        Text(
                            text       = "Datos de la coordinadora",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = ElectroNavyBlue
                        )

                        // Nombre
                        OutlinedTextField(
                            value         = nombre,
                            onValueChange = { nombre = it },
                            label         = { Text("Nombre completo") },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = authTextFieldColors()
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
                                    viewModel.solicitarRegistro(nombre, email, password)
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

                        // Botón registrar
                        Button(
                            onClick  = { viewModel.solicitarRegistro(nombre, email, password) },
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
                                    text       = "Registrarse y verificar email",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 15.sp
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
