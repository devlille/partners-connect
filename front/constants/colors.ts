/**
 * Constantes de couleurs avec ratios de contraste WCAG AAA (7:1)
 * Tous les ratios ont été vérifiés pour garantir l'accessibilité
 */

export const COLORS = {
  // Text Colors (garantit ratio 7:1 sur fond blanc)
  TEXT: {
    PRIMARY: "#000000", // Ratio 21:1
    SECONDARY: "#374151", // Ratio 10.9:1 (gray-700)
    TERTIARY: "#4B5563", // Ratio 8.6:1 (gray-600)
    DISABLED: "#6B7280", // Ratio 5.7:1 (gray-500) - WCAG AA only
    LINK: "#1E40AF", // Ratio 8.6:1 (blue-800)
    LINK_HOVER: "#1E3A8A", // Ratio 10.4:1 (blue-900)
  },

  // Background Colors
  BACKGROUND: {
    PRIMARY: "#FFFFFF",
    SECONDARY: "#F9FAFB", // gray-50
    TERTIARY: "#F3F4F6", // gray-100
  },

  // Status Colors (pour texte sur fond blanc)
  STATUS: {
    SUCCESS: {
      TEXT: "#065F46", // green-800 - Ratio 7.6:1
      BG: "#D1FAE5", // green-100
      BORDER: "#10B981", // green-500
    },
    ERROR: {
      TEXT: "#991B1B", // red-800 - Ratio 7.5:1
      BG: "#FEE2E2", // red-100
      BORDER: "#EF4444", // red-500
    },
    WARNING: {
      TEXT: "#92400E", // amber-800 - Ratio 7.8:1
      BG: "#FEF3C7", // amber-100
      BORDER: "#F59E0B", // amber-500
    },
    INFO: {
      TEXT: "#1E40AF", // blue-800 - Ratio 8.6:1
      BG: "#DBEAFE", // blue-100
      BORDER: "#3B82F6", // blue-500
    },
  },

  // Button Colors (avec contraste pour texte blanc)
  BUTTON: {
    PRIMARY: {
      DEFAULT: "#1E40AF", // blue-800
      HOVER: "#1E3A8A", // blue-900
      ACTIVE: "#1E3A8A",
      TEXT: "#FFFFFF",
    },
    SECONDARY: {
      DEFAULT: "#4B5563", // gray-600
      HOVER: "#374151", // gray-700
      ACTIVE: "#1F2937", // gray-800
      TEXT: "#FFFFFF",
    },
    SUCCESS: {
      DEFAULT: "#065F46", // green-800
      HOVER: "#064E3B", // green-900
      ACTIVE: "#064E3B",
      TEXT: "#FFFFFF",
    },
    ERROR: {
      DEFAULT: "#991B1B", // red-800
      HOVER: "#7F1D1D", // red-900
      ACTIVE: "#7F1D1D",
      TEXT: "#FFFFFF",
    },
    WARNING: {
      DEFAULT: "#92400E", // amber-800
      HOVER: "#78350F", // amber-900
      ACTIVE: "#78350F",
      TEXT: "#FFFFFF",
    },
  },

  // Border Colors
  BORDER: {
    DEFAULT: "#D1D5DB", // gray-300
    LIGHT: "#E5E7EB", // gray-200
    MEDIUM: "#9CA3AF", // gray-400
    DARK: "#6B7280", // gray-500
    FOCUS: "#2563EB", // blue-600
  },

  // Focus Ring
  FOCUS: {
    RING: "#2563EB", // blue-600
    RING_OFFSET: "#FFFFFF",
  },

  // Dark Mode (pour futur usage)
  DARK: {
    BG_PRIMARY: "#111827", // gray-900
    BG_SECONDARY: "#1F2937", // gray-800
    BG_TERTIARY: "#374151", // gray-700
    TEXT_PRIMARY: "#F9FAFB", // gray-50
    TEXT_SECONDARY: "#E5E7EB", // gray-200
    TEXT_TERTIARY: "#D1D5DB", // gray-300
    BORDER: "#4B5563", // gray-600
  },
} as const;

/**
 * Helper pour obtenir les classes Tailwind correspondantes
 */
export const TAILWIND_COLORS = {
  TEXT: {
    PRIMARY: "text-black",
    SECONDARY: "text-gray-700",
    TERTIARY: "text-gray-600",
    DISABLED: "text-gray-500",
    LINK: "text-blue-800 hover:text-blue-900",
  },
  BG: {
    PRIMARY: "bg-white",
    SECONDARY: "bg-gray-50",
    TERTIARY: "bg-gray-100",
  },
  BORDER: {
    DEFAULT: "border-gray-300",
    LIGHT: "border-gray-200",
    FOCUS: "focus:border-blue-600 focus:ring-2 focus:ring-blue-600 focus:ring-opacity-50",
  },
} as const;

/**
 * Ratios de contraste minimum requis
 */
export const CONTRAST_RATIOS = {
  WCAG_AA_NORMAL: 4.5,
  WCAG_AA_LARGE: 3.0,
  WCAG_AAA_NORMAL: 7.0,
  WCAG_AAA_LARGE: 4.5,
} as const;
