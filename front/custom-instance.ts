import axios, {
  type AxiosResponse,
  type AxiosInstance,
  type AxiosRequestConfig,
} from "axios";

let _apiBaseUrl = '';
let _axiosInstance: AxiosInstance | null = null;

export function setApiBaseUrl(url: string) {
  _apiBaseUrl = url;
  _axiosInstance = null;
}

const createCustomAxiosInstance = (baseUrl: string): AxiosInstance => {
  const instance = axios.create({ baseURL: baseUrl });

  instance.interceptors.request.use(async (config) => {
    let accessToken = null;
    if (typeof window !== "undefined") {
      accessToken = localStorage.getItem("auth_token");
    }

    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }

    config.headers["Accept-Language"] = "fr";
    return config;
  });

  instance.interceptors.response.use(
    (response) => {
      return {
        ...response,
        error: response.status > 300,
        ok: response.status >= 200 && response.status <= 299,
      };
    },
    (error) => {
      if (error.response) {
        if (error.response.status === 401) {
          if (typeof window !== "undefined") {
            localStorage.removeItem("auth_token");
            localStorage.removeItem("user_info");

            const redirectUrl = encodeURIComponent(
              `${
                process.env.NODE_ENV === "development"
                  ? "http://localhost:3000"
                  : window.location.origin
              }/auth/callback`,
            );
            window.location.href = `${_apiBaseUrl}/auth/login?redirectUrl=${redirectUrl}`;
          }
        }

        const customError = new Error(`HTTP error! Status: ${error.response.status}`) as Error & {
          response?: typeof error.response;
        };
        customError.response = error.response;
        throw customError;
      }
      throw error;
    },
  );

  return instance;
};

const getAxiosInstance = (): AxiosInstance => {
  if (!_axiosInstance) {
    if (!_apiBaseUrl) {
      try {
        _apiBaseUrl = useRuntimeConfig().public.apiBaseUrl;
      } catch {
        // Nuxt context not available; URL will be set by the plugin before API calls
      }
    }
    _axiosInstance = createCustomAxiosInstance(_apiBaseUrl);
  }
  return _axiosInstance;
};

export const customFetch = <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig,
): Promise<AxiosResponse<T> & { error: boolean; ok: boolean }> => {
  const axiosInstance = getAxiosInstance();
  return axiosInstance({ ...config, ...options });
};
