/**
 * Clés de stockage standardisées pour localStorage et sessionStorage
 * Centralise toutes les clés pour éviter les typos et faciliter la maintenance
 */

export const STORAGE_KEYS = {
  // Authentication
  AUTH_TOKEN: 'auth_token',
  USER_INFO: 'user_info',

  // UI Preferences
  THEME: 'theme',
  LANGUAGE: 'i18n_redirected',
  SIDEBAR_COLLAPSED: 'sidebar_collapsed',

  // Cache
  CACHE_PREFIX: 'cache_',
  CACHE_TIMESTAMP_PREFIX: 'cache_ts_',

  // Filters & Search
  TABLE_FILTERS_PREFIX: 'filters_',
  SEARCH_HISTORY: 'search_history',

  // Forms
  DRAFT_PREFIX: 'draft_',
} as const;

export type StorageKey = typeof STORAGE_KEYS[keyof typeof STORAGE_KEYS];

/**
 * Helpers pour construire des clés dynamiques
 */
export const getCacheKey = (key: string) => `${STORAGE_KEYS.CACHE_PREFIX}${key}`;
export const getCacheTimestampKey = (key: string) => `${STORAGE_KEYS.CACHE_TIMESTAMP_PREFIX}${key}`;
export const getTableFiltersKey = (tableId: string) => `${STORAGE_KEYS.TABLE_FILTERS_PREFIX}${tableId}`;
export const getDraftKey = (formId: string) => `${STORAGE_KEYS.DRAFT_PREFIX}${formId}`;
