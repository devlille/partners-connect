export default {
  api: {
    input:
      "https://app-9603f7fd-d8c8-45ce-80f3-1f1d9f3fc367.cleverapps.io/swagger/documentation.yaml",
    output: {
      target: "./utils/api.ts",
      override: {
        mutator: {
          path: "./custom-instance.ts",
          name: "customFetch",
        },
      },
    },
  },
};
