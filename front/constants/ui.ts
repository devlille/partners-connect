/**
 * Constantes UI pour timeouts, limites, animations, etc.
 */

export const UI_CONSTANTS = {
  // Timeouts
  DEBOUNCE_DELAY: 300, // ms
  TOAST_DURATION: 5000, // ms
  TOOLTIP_DELAY: 500, // ms
  AUTO_SAVE_DELAY: 2000, // ms

  // Cache TTL
  CACHE_TTL: {
    SHORT: 5 * 60 * 1000, // 5 minutes
    MEDIUM: 15 * 60 * 1000, // 15 minutes
    LONG: 60 * 60 * 1000, // 1 hour
    DAY: 24 * 60 * 60 * 1000, // 24 hours
  },

  // Pagination
  DEFAULT_PAGE_SIZE: 20,
  PAGE_SIZE_OPTIONS: [10, 20, 50, 100],

  // Table
  MAX_TABLE_ROWS_BEFORE_PAGINATION: 50,
  SEARCH_MIN_CHARS: 2,

  // Forms
  MAX_FILE_SIZE: 5 * 1024 * 1024, // 5MB
  ALLOWED_IMAGE_TYPES: ['image/jpeg', 'image/png', 'image/webp'],
  ALLOWED_DOCUMENT_TYPES: ['application/pdf', 'application/msword'],

  // Validation Limits
  MAX_NAME_LENGTH: 200,
  MAX_DESCRIPTION_LENGTH: 1000,
  MAX_URL_LENGTH: 500,
  MAX_EMAIL_LENGTH: 255,
  MAX_PHONE_LENGTH: 20,
  MIN_PASSWORD_LENGTH: 8,

  // Animation Durations (ms)
  ANIMATION: {
    FAST: 150,
    NORMAL: 300,
    SLOW: 500,
  },

  // Breakpoints (px)
  BREAKPOINTS: {
    SM: 640,
    MD: 768,
    LG: 1024,
    XL: 1280,
    '2XL': 1536,
  },

  // Z-Index
  Z_INDEX: {
    DROPDOWN: 1000,
    STICKY: 1020,
    FIXED: 1030,
    MODAL_BACKDROP: 1040,
    MODAL: 1050,
    POPOVER: 1060,
    TOOLTIP: 1070,
  },

  // Touch Targets (px)
  MIN_TOUCH_TARGET: 44,

  // Infinite Scroll
  INFINITE_SCROLL_THRESHOLD: 200, // px from bottom

  // Retry
  MAX_RETRIES: 3,
  RETRY_DELAY: 1000, // ms

  // Loading States
  SKELETON_ANIMATION_DURATION: 1500, // ms
} as const;

/**
 * Constantes pour les modal sizes
 */
export const MODAL_SIZES = {
  SM: 'max-w-sm',
  MD: 'max-w-md',
  LG: 'max-w-lg',
  XL: 'max-w-xl',
  '2XL': 'max-w-2xl',
  '3XL': 'max-w-3xl',
  '4XL': 'max-w-4xl',
  FULL: 'max-w-full',
} as const;

/**
 * Constantes pour les variants
 */
export const VARIANTS = {
  PRIMARY: 'primary',
  SECONDARY: 'secondary',
  SUCCESS: 'success',
  ERROR: 'error',
  DANGER: 'danger',
  WARNING: 'warning',
  INFO: 'info',
  NEUTRAL: 'neutral',
} as const;

export type Variant = typeof VARIANTS[keyof typeof VARIANTS];
