export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event);
  const baseUrl = config.public.apiBaseUrl || 'http://localhost:8080';
  const path = getRouterParam(event, 'path');
  const target = `${baseUrl}/${path}`;

  return proxyRequest(event, target);
});
