package com.Electroinova.inovinfo.data.repository

/**
 * Repositorio de Visitas.
 *
 * Patrón: Repository actúa como única fuente de verdad (Single Source of Truth).
 * - Lee/escribe primero en Room (modo offline).
 * - Sincroniza con Google Sheets en background cuando hay conexión.
 *
 * Implementación pendiente.
 */
interface VisitaRepository
