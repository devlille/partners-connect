export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event);
  const baseUrl = (config.public.apiBaseUrl || 'http://localhost:8080').replace(/\/$/, '');
  const path = getRouterParam(event, 'path');
  const { search } = getRequestURL(event);
  const target = `${baseUrl}/${path}${search}`;

  return proxyRequest(event, target);
});
