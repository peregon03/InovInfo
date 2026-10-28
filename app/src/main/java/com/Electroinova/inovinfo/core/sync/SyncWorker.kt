package com.Electroinova.inovinfo.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.Electroinova.inovinfo.data.repository.CatalogoRepository
import com.Electroinova.inovinfo.data.repository.VisitaRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "SyncWorker"

/**
 * Worker que sincroniza datos pendientes con el servidor cuando hay conexión.
 *
 * Orden de operaciones:
 * 1. Subir visitas con estado PENDING
 * 2. Subir fotos pendientes de visitas ya sincronizadas
 * 3. Refrescar catálogos (sedes, unidades, estados, suministros)
 * 4. Refrescar visitas del día y pendientes de seguimiento
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context:          Context,
    @Assisted workerParams:     WorkerParameters,
    private val visitaRepo:     VisitaRepository,
    private val catalogoRepo:   CatalogoRepository,
    private val connectivity:   ConnectivityObserver
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // No ejecutar si no hay conexión
        if (!connectivity.tieneConexionActual()) {
            Log.d(TAG, "Sin conexión — work pospuesto")
            return Result.retry()
        }

        return try {
            Log.d(TAG, "Iniciando sincronización...")

            // 1. Visitas pendientes → servidor
            visitaRepo.syncPendingVisitas()

            // 2. Fotos pendientes → servidor
            visitaRepo.syncPendingFotos()

            // 3. Catálogos
            catalogoRepo.refreshCatalogos()

            // 4. Listas del día
            visitaRepo.refreshVisitasHoy()
            visitaRepo.refreshPendientes()

            Log.d(TAG, "Sincronización completada correctamente")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la sincronización: ${e.javaClass.simpleName} - ${e.message}", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
