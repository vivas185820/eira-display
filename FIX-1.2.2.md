# Eira Display 1.2.2 — Stability Fix

Corrección:
- se retiró del arranque el registro dinámico del DownloadManager;
- se retiró el comprobador automático de actualización al iniciar;
- se conserva el arranque rápido/caché de la 1.1;
- el código del actualizador queda inactivo hasta reimplementarlo de forma aislada.

Objetivo: recuperar primero una app estable que abra siempre.
