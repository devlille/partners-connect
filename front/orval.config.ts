export default {
  pearl: {
    input: "http://localhost:8080/swagger/documentation.yaml",
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
