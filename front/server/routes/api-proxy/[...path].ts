export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event);
  const path = getRouterParam(event, 'path');
  const target = `${config.API_BASE_URL}/${path}`;

  return proxyRequest(event, target);
});
