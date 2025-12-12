/**
 * Types pour le système de cache
 */

export interface CacheEntry<T = any> {
  data: T;
  timestamp: number;
  ttl: number;
}

export interface CacheOptions {
  /** Time to live en millisecondes */
  ttl?: number;
  /** Forcer le rafraîchissement même si le cache est valide */
  force?: boolean;
  /** Callback appelé quand les données sont chargées depuis le cache */
  onCacheHit?: () => void;
  /** Callback appelé quand les données sont chargées depuis l'API */
  onCacheMiss?: () => void;
}

export interface CacheConfig {
  /** TTL par défaut en millisecondes */
  defaultTTL: number;
  /** Activer la persistence en localStorage */
  persist?: boolean;
  /** Préfixe pour les clés de cache */
  prefix?: string;
  /** Taille maximale du cache (nombre d'entrées) */
  maxSize?: number;
}

export type CacheKey = string | number;

export type CacheInvalidationStrategy =
  | "exact" // Invalider uniquement la clé exacte
  | "prefix" // Invalider toutes les clés commençant par le pattern
  | "all"; // Invalider tout le cache

export interface CacheStats {
  /** Nombre total d'entrées dans le cache */
  size: number;
  /** Nombre de hits */
  hits: number;
  /** Nombre de miss */
  misses: number;
  /** Taux de hit (%) */
  hitRate: number;
  /** Mémoire utilisée (estimée en bytes) */
  memoryUsage: number;
}
