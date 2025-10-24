import axios, {
  type AxiosError,
  type AxiosResponse,
  type AxiosInstance,
  type AxiosRequestConfig,
} from "axios";

// Function to create a custom Axios instance
const createCustomAxiosInstance = (baseUrl: string): AxiosInstance => {
  const instance = axios.create({ baseURL: baseUrl });

  instance.interceptors.request.use(async (config) => {
    // Get token from localStorage if available
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
        // Redirect to login only if token is invalid (401 Unauthorized)
        if (error.response.status === 401) {
          if (typeof window !== 'undefined') {
            localStorage.removeItem('auth_token');
            localStorage.removeItem('user_info');
            
            const config = useRuntimeConfig();
            const redirectUrl = encodeURIComponent(
              `${process.env.NODE_ENV === "development" ? "http://localhost:8080" : window.location.origin}${
                window.location.pathname
              }`
            );
            window.location.href = `${config.public.apiBaseUrl}/auth/login?redirectUrl=${redirectUrl}`;
          }
        }
        throw new Error(`HTTP error! Status: ${error.response.status}`);
      }
      throw error;
    }
  );

  return instance;
};

// Function to get axios instance with runtime config
export const getAxiosInstance = () => {
  const config = useRuntimeConfig();
  return createCustomAxiosInstance(config.public.apiBaseUrl);
};

export const customFetch = <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig
): Promise<AxiosResponse<T> & { error: boolean; ok: boolean }> => {
  const axiosInstance = getAxiosInstance();
  return axiosInstance({ ...config, ...options });
};

export type ErrorType<Error> = AxiosError<Error>;
