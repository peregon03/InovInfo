package com.Electroinova.inovinfo.core.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

// ── Estado de sincronización ──────────────────────────────────────────────────

enum class SyncStatus {
    PENDING,
    SYNCED,
    ERROR
}

// ── Observador de conectividad ────────────────────────────────────────────────

/**
 * Observa el estado de la red usando [ConnectivityManager.NetworkCallback].
 * Expone un [Flow] de [Boolean] que emite `true` cuando hay conexión activa.
 */
@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /** Emite `true` si hay red disponible con capacidad a Internet, `false` en caso contrario. */
    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Emitir el estado actual al suscribirse
        trySend(tieneConexionActual())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    /** Verificación síncrona del estado de red actual. */
    fun tieneConexionActual(): Boolean {
        val red = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(red) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
