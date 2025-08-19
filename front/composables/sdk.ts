export const useSdk = () => {
  const runtimeConfig = useRuntimeConfig();
  return getSdk(process.env.TOKEN!, runtimeConfig.public.apiBaseUrl);
};
