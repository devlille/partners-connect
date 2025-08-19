import type { Event, Organization } from "~/types/partner";

const generateApiSdk = (token: string, baseUrl: string) => {
  const request = async <T>(path: string, options: any = {}): Promise<T> => {
    return await $fetch<T>(`${baseUrl}${path}`, {
      ...options,
      headers: {
        ...(options.headers || {}),
      },
    });
  };

  return {
    getUserEvents: () => request<Event[]>("/users/me/events"),
    getUserOrgs: () => request<Organization[]>("/users/me/orgs"),
    getOrg: (slug: string) => request<Organization>("/orgs/" + slug),
    getOrgEvents: (slug: string) =>
      request<Event[]>("/orgs/" + slug + "/events"),
    updateOrg: (slug: string, organization: Organization) =>
      request<Event[]>("/orgs/" + slug, {
        body: organization,
        method: "PUT",
      }),
  };
};

let sdk: ReturnType<typeof generateApiSdk> | undefined;

export const getSdk = (token: string, baseUrl: string) => {
  if (sdk) {
    return sdk;
  }
  sdk = generateApiSdk(token, baseUrl);

  return sdk;
};
