/**
 * Composable pour gérer le cache avec TTL
 * Permet de mettre en cache des données API avec expiration automatique
 */

import type { CacheEntry, CacheOptions, CacheKey } from "~/types/cache";
import { UI_CONSTANTS } from "~/constants/ui";
import { getCacheKey, getCacheTimestampKey } from "~/constants/storage-keys";

export function useCache() {
  const cacheStore = new Map<string, CacheEntry>();

  /**
   * Vérifie si une entrée de cache est toujours valide
   */
  function isValid(entry: CacheEntry): boolean {
    const now = Date.now();
    return now - entry.timestamp < entry.ttl;
  }

  /**
   * Obtenir une entrée du cache
   */
  function get<T>(key: CacheKey, options?: CacheOptions): T | null {
    const cacheKey = String(key);
    const entry = cacheStore.get(cacheKey);

    if (!entry) {
      options?.onCacheMiss?.();
      return null;
    }

    if (options?.force || !isValid(entry)) {
      cacheStore.delete(cacheKey);
      options?.onCacheMiss?.();
      return null;
    }

    options?.onCacheHit?.();
    return entry.data as T;
  }

  /**
   * Définir une entrée dans le cache
   */
  function set<T>(key: CacheKey, data: T, ttl: number = UI_CONSTANTS.CACHE_TTL.MEDIUM): void {
    const cacheKey = String(key);
    const entry: CacheEntry<T> = {
      data,
      timestamp: Date.now(),
      ttl,
    };

    cacheStore.set(cacheKey, entry);

    // Persister dans localStorage si souhaité
    if (import.meta.client) {
      try {
        localStorage.setItem(getCacheKey(cacheKey), JSON.stringify(data));
        localStorage.setItem(getCacheTimestampKey(cacheKey), String(entry.timestamp));
      } catch (e) {
        // Silently fail si localStorage est plein
        console.warn("Failed to persist cache to localStorage:", e);
      }
    }
  }

  /**
   * Invalider une ou plusieurs entrées de cache
   */
  function invalidate(pattern: string | RegExp): void {
    if (typeof pattern === "string") {
      // Invalidation exacte
      cacheStore.delete(pattern);
      if (import.meta.client) {
        localStorage.removeItem(getCacheKey(pattern));
        localStorage.removeItem(getCacheTimestampKey(pattern));
      }
    } else {
      // Invalidation par pattern
      const keysToDelete: string[] = [];
      for (const key of cacheStore.keys()) {
        if (pattern.test(key)) {
          keysToDelete.push(key);
        }
      }
      keysToDelete.forEach((key) => {
        cacheStore.delete(key);
        if (import.meta.client) {
          localStorage.removeItem(getCacheKey(key));
          localStorage.removeItem(getCacheTimestampKey(key));
        }
      });
    }
  }

  /**
   * Vider tout le cache
   */
  function clear(): void {
    cacheStore.clear();
    if (import.meta.client) {
      // Clear all cache entries from localStorage
      const keysToRemove: string[] = [];
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (key && (key.startsWith("cache_") || key.startsWith("cache_ts_"))) {
          keysToRemove.push(key);
        }
      }
      keysToRemove.forEach((key) => localStorage.removeItem(key));
    }
  }

  /**
   * Obtenir les statistiques du cache
   */
  function getStats() {
    return {
      size: cacheStore.size,
      keys: Array.from(cacheStore.keys()),
    };
  }

  /**
   * Restaurer le cache depuis localStorage au démarrage
   */
  function restore(): void {
    if (!import.meta.client) return;

    const keysToCheck: string[] = [];

    // Identifier toutes les clés de cache
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key && key.startsWith("cache_") && !key.startsWith("cache_ts_")) {
        keysToCheck.push(key.replace("cache_", ""));
      }
    }

    // Restaurer les entrées valides
    keysToCheck.forEach((key) => {
      try {
        const data = localStorage.getItem(getCacheKey(key));
        const timestamp = localStorage.getItem(getCacheTimestampKey(key));

        if (data && timestamp) {
          const entry: CacheEntry = {
            data: JSON.parse(data),
            timestamp: parseInt(timestamp, 10),
            ttl: UI_CONSTANTS.CACHE_TTL.MEDIUM, // TTL par défaut
          };

          // Ne restaurer que si toujours valide
          if (isValid(entry)) {
            cacheStore.set(key, entry);
          } else {
            // Nettoyer les entrées expirées
            localStorage.removeItem(getCacheKey(key));
            localStorage.removeItem(getCacheTimestampKey(key));
          }
        }
      } catch (e) {
        console.warn(`Failed to restore cache for key ${key}:`, e);
      }
    });
  }

  /**
   * Wrapper pour les appels API avec cache
   */
  async function fetchWithCache<T>(
    key: CacheKey,
    fetchFn: () => Promise<T>,
    options?: CacheOptions,
  ): Promise<T> {
    // Vérifier le cache d'abord
    const cached = get<T>(key, options);
    if (cached !== null && !options?.force) {
      return cached;
    }

    // Charger depuis l'API
    const data = await fetchFn();

    // Mettre en cache
    set(key, data, options?.ttl);

    return data;
  }

  // Restaurer le cache au montage
  if (import.meta.client) {
    onMounted(() => {
      restore();
    });
  }

  return {
    get,
    set,
    invalidate,
    clear,
    getStats,
    restore,
    fetchWithCache,
  };
}
