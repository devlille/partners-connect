// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: "2025-05-15",
  devtools: { enabled: true },
  modules: ["@nuxt/ui", "@nuxt/test-utils", "@nuxtjs/i18n", "@pinia/nuxt", "@sentry/nuxt/module"],

  app: {
    head: {
      htmlAttrs: {
        lang: "fr",
      },
    },
  },

  i18n: {
    locales: ["fr", "en", "es"],
    defaultLocale: "fr",
    vueI18n: "./i18n.config.ts",
    strategy: "no_prefix",
    detectBrowserLanguage: {
      useCookie: true,
      cookieKey: "i18n_redirected",
      redirectOn: "root",
    },
    compilation: {
      strictMessage: false,
      escapeHtml: false,
    },
    datetimeFormats: {
      fr: {
        short: {
          year: "numeric",
          month: "2-digit",
          day: "2-digit",
        },
        long: {
          year: "numeric",
          month: "long",
          day: "numeric",
        },
        full: {
          year: "numeric",
          month: "long",
          day: "numeric",
          weekday: "long",
        },
      },
      en: {
        short: {
          year: "numeric",
          month: "2-digit",
          day: "2-digit",
        },
        long: {
          year: "numeric",
          month: "long",
          day: "numeric",
        },
        full: {
          year: "numeric",
          month: "long",
          day: "numeric",
          weekday: "long",
        },
      },
      es: {
        short: {
          year: "numeric",
          month: "2-digit",
          day: "2-digit",
        },
        long: {
          year: "numeric",
          month: "long",
          day: "numeric",
        },
        full: {
          year: "numeric",
          month: "long",
          day: "numeric",
          weekday: "long",
        },
      },
    },
    numberFormats: {
      fr: {
        currency: {
          style: "currency",
          currency: "EUR",
          notation: "standard",
        },
        decimal: {
          style: "decimal",
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        },
        percent: {
          style: "percent",
          useGrouping: false,
        },
      },
      en: {
        currency: {
          style: "currency",
          currency: "USD",
          notation: "standard",
        },
        decimal: {
          style: "decimal",
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        },
        percent: {
          style: "percent",
          useGrouping: false,
        },
      },
      es: {
        currency: {
          style: "currency",
          currency: "EUR",
          notation: "standard",
        },
        decimal: {
          style: "decimal",
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        },
        percent: {
          style: "percent",
          useGrouping: false,
        },
      },
    },
  },

  runtimeConfig: {
    API_BASE_URL: "http://localhost:8080",
    public: {
      apiBaseUrl: process.env.NUXT_PUBLIC_API_BASE_URL,
      defaultOrgSlug: process.env.NUXT_PUBLIC_DEFAULT_ORG_SLUG || "devlille",
      defaultEventSlug: process.env.NUXT_PUBLIC_DEFAULT_EVENT_SLUG || "devlille-2026",
    },
  },

  sentry: {
    sourceMapsUploadOptions: {
      org: "philibert-consulting",
      project: "partners-connect",
    },
  },

  sourcemap: {
    client: "hidden",
  },
});
