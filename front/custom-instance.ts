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
    const accessToken =
      "ya29.a0AS3H6NwbB_My3BJujABm9tGRUSY71nborkyuwVtao-TO6gtpUMBFt7DgnGHGKDpSGoMZm3MMPlNbtjySNJjKmdJgGRtB-heFkt9_byQMAn1GwfwHvBY9hxOWYp-TH9-b3-zcNdgMabKOl2AGh61WD-Tk5HvCfAdN5ZyzMG_WnQaCgYKAWoSARUSFQHGX2MiUKkqGZ6EVet2Nl08pzmg2A0177";
    config.headers.Authorization = `Bearer ${accessToken}`;
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
        throw new Error(`HTTP error! Status: ${error.response.status}`);
      }
      throw error;
    }
  );

  return instance;
};

const axiosPearl = createCustomAxiosInstance("http://localhost:8080");

export const customFetch = <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig
): Promise<AxiosResponse<T> & { error: boolean; ok: boolean }> => {
  return axiosPearl({ ...config, ...options });
};

export type ErrorType<Error> = AxiosError<Error>;
