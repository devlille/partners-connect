export default {
  api: {
    input: "https://partners-connect-server-486924521070.europe-west1.run.app/swagger/documentation.yaml",
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
