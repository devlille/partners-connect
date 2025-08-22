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
      "ya29.A0AS3H6Nyju28JTb_NWc11kwa3grC4B0IFXLngTsvHB8fn490Or5y4OF7FZwu3dQvO2k5chu0K1v6w04ik-HZkqbOu2XArxoqwaA_FaqfqXDvcLHkXsHKPuXQRemLvU3cEG0qcgSwdOjgPYudnWE61FUMcyPKX8vI2bFIJRkAIaWaZvPx2uhgsaIKWW8ZbK1bXooverLZXaCgYKAaISARUSFQHGX2MiAC4hwkr5xq-QfLTI0b62Nw0207";
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
