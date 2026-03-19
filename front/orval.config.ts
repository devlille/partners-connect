export default {
  api: {
    input:
      "https://app-be77cae5-4a57-4cca-b721-15f8ab1f76c4.cleverapps.io/swagger/documentation.yaml",
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
