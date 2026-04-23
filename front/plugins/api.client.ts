import { setApiBaseUrl } from '~/custom-instance';

export default defineNuxtPlugin(() => {
  setApiBaseUrl('/api-proxy');
});
