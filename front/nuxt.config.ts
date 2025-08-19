// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: "2025-05-15",
  devtools: { enabled: true },
  modules: ["@nuxt/ui", "@nuxt/test-utils", "@nuxt/eslint"],
  css: ["~/public/css/main.css"],
  runtimeConfig: {
    API_BASE_URL: "http://localhost:3000",
    public: {
      apiBaseUrl:
        process.env.NUXT_PUBLIC_API_BASE_URL || "http://localhost:8080",
    },
  },
});
