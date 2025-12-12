import { describe, it, expect } from "vitest";
import type { CompanySchema, UpdateCompanySchema, AddressSchema } from "~/utils/api";

describe("CompanyForm Component", () => {
  describe("Company Data Validation", () => {
    function isValidCompanyData(company: Partial<CompanySchema>): boolean {
      return !!(
        company.name &&
        company.name.trim() !== "" &&
        company.siret &&
        company.siret.trim() !== ""
      );
    }

    it("should validate complete company data", () => {
      const company: CompanySchema = {
        id: "123",
        name: "Test Company",
        head_office: {
          address: "123 Main St",
          city: "Paris",
          zip_code: "75001",
          country: "France",
        },
        siret: "12345678901234",
        vat: "FR12345678901",
        site_url: "https://example.com",
        description: "A test company",
      };

      expect(isValidCompanyData(company)).toBe(true);
    });

    it("should invalidate company without name", () => {
      const company: Partial<CompanySchema> = {
        siret: "12345678901234",
        vat: "FR12345678901",
      };

      expect(isValidCompanyData(company)).toBe(false);
    });

    it("should invalidate company without siret", () => {
      const company: Partial<CompanySchema> = {
        name: "Test Company",
        vat: "FR12345678901",
      };

      expect(isValidCompanyData(company)).toBe(false);
    });
  });

  describe("UpdateCompanySchema Structure", () => {
    it("should accept valid update data with all fields", () => {
      const updateData: UpdateCompanySchema = {
        name: "Updated Company",
        head_office: {
          address: "456 New St",
          city: "Lyon",
          zip_code: "69001",
          country: "France",
        },
        siret: "98765432109876",
        vat: "FR98765432109",
        description: "Updated description",
        site_url: "https://newsite.com",
      };

      expect(updateData.name).toBe("Updated Company");
      expect(updateData.siret).toBe("98765432109876");
      expect(updateData.vat).toBe("FR98765432109");
      expect(updateData.description).toBe("Updated description");
      expect(updateData.site_url).toBe("https://newsite.com");
      expect(updateData.head_office?.city).toBe("Lyon");
    });

    it("should accept partial update data", () => {
      const updateData: UpdateCompanySchema = {
        name: "Updated Company",
        siret: "98765432109876",
      };

      expect(updateData.name).toBe("Updated Company");
      expect(updateData.siret).toBe("98765432109876");
      expect(updateData.vat).toBeUndefined();
      expect(updateData.description).toBeUndefined();
    });

    it("should accept null values for optional fields", () => {
      const updateData: UpdateCompanySchema = {
        name: "Company",
        description: null,
        site_url: null,
      };

      expect(updateData.name).toBe("Company");
      expect(updateData.description).toBeNull();
      expect(updateData.site_url).toBeNull();
    });
  });

  describe("Address Validation", () => {
    function isValidAddress(address: AddressSchema): boolean {
      // Tous les champs sont obligatoires pour l'adresse du siège social
      return !!(
        address.address &&
        address.address.trim() !== "" &&
        address.city &&
        address.city.trim() !== "" &&
        address.zip_code &&
        address.zip_code.trim() !== "" &&
        address.country &&
        address.country.trim() !== ""
      );
    }

    it("should validate complete address with all required fields", () => {
      const address: AddressSchema = {
        address: "123 Main St",
        city: "Paris",
        zip_code: "75001",
        country: "France",
      };

      expect(isValidAddress(address)).toBe(true);
    });

    it("should invalidate address without street (required field)", () => {
      const address: AddressSchema = {
        address: "",
        city: "Paris",
        zip_code: "75001",
        country: "France",
      };

      expect(isValidAddress(address)).toBe(false);
    });

    it("should invalidate address without city (required field)", () => {
      const address: AddressSchema = {
        address: "123 Main St",
        city: "",
        zip_code: "75001",
        country: "France",
      };

      expect(isValidAddress(address)).toBe(false);
    });

    it("should invalidate address without zip code (required field)", () => {
      const address: AddressSchema = {
        address: "123 Main St",
        city: "Paris",
        zip_code: "",
        country: "France",
      };

      expect(isValidAddress(address)).toBe(false);
    });

    it("should invalidate address without country (required field)", () => {
      const address: AddressSchema = {
        address: "123 Main St",
        city: "Paris",
        zip_code: "75001",
        country: "",
      };

      expect(isValidAddress(address)).toBe(false);
    });

    it("should invalidate address with only whitespace in required fields", () => {
      const address: AddressSchema = {
        address: "   ",
        city: "   ",
        zip_code: "   ",
        country: "   ",
      };

      expect(isValidAddress(address)).toBe(false);
    });
  });

  describe("SIRET Validation", () => {
    function isValidSiret(siret: string): boolean {
      // SIRET doit être une chaîne de 14 chiffres
      const siretRegex = /^\d{14}$/;
      return siretRegex.test(siret);
    }

    it("should validate correct SIRET format", () => {
      expect(isValidSiret("12345678901234")).toBe(true);
      expect(isValidSiret("98765432109876")).toBe(true);
    });

    it("should invalidate SIRET with wrong length", () => {
      expect(isValidSiret("123456789")).toBe(false);
      expect(isValidSiret("123456789012345")).toBe(false);
    });

    it("should invalidate SIRET with non-numeric characters", () => {
      expect(isValidSiret("1234567890123A")).toBe(false);
      expect(isValidSiret("12345-67890123")).toBe(false);
    });
  });

  describe("VAT Number Validation", () => {
    function isValidVAT(vat: string): boolean {
      // Format VAT français : FR suivi de 11 chiffres
      const vatRegex = /^FR\d{11}$/;
      return vatRegex.test(vat);
    }

    it("should validate correct French VAT format", () => {
      expect(isValidVAT("FR12345678901")).toBe(true);
      expect(isValidVAT("FR98765432109")).toBe(true);
    });

    it("should invalidate VAT without FR prefix", () => {
      expect(isValidVAT("12345678901")).toBe(false);
    });

    it("should invalidate VAT with wrong number of digits", () => {
      expect(isValidVAT("FR123456789")).toBe(false);
      expect(isValidVAT("FR123456789012")).toBe(false);
    });
  });

  describe("URL Validation", () => {
    function isValidURL(url: string): boolean {
      try {
        const parsedUrl = new URL(url);
        return parsedUrl.protocol === "http:" || parsedUrl.protocol === "https:";
      } catch {
        return false;
      }
    }

    it("should validate correct URLs", () => {
      expect(isValidURL("https://example.com")).toBe(true);
      expect(isValidURL("http://example.com")).toBe(true);
      expect(isValidURL("https://www.example.com/path")).toBe(true);
    });

    it("should invalidate incorrect URLs", () => {
      expect(isValidURL("not-a-url")).toBe(false);
      expect(isValidURL("ftp://example.com")).toBe(false);
      expect(isValidURL("example.com")).toBe(false);
    });
  });

  describe("Data Sanitization", () => {
    function sanitizeUpdateData(data: UpdateCompanySchema): UpdateCompanySchema {
      return {
        name: data.name?.trim() || null,
        siret: data.siret?.trim() || null,
        vat: data.vat?.trim() || null,
        site_url: data.site_url?.trim() || null,
        description: data.description?.trim() || null,
        head_office: data.head_office
          ? {
              address: data.head_office.address?.trim() || "",
              city: data.head_office.city?.trim() || "",
              zip_code: data.head_office.zip_code?.trim() || "",
              country: data.head_office.country?.trim() || "",
            }
          : null,
      };
    }

    it("should trim whitespace from all string fields", () => {
      const data: UpdateCompanySchema = {
        name: "  Company Name  ",
        siret: "  12345678901234  ",
        vat: "  FR12345678901  ",
        description: "  Description  ",
      };

      const sanitized = sanitizeUpdateData(data);

      expect(sanitized.name).toBe("Company Name");
      expect(sanitized.siret).toBe("12345678901234");
      expect(sanitized.vat).toBe("FR12345678901");
      expect(sanitized.description).toBe("Description");
    });

    it("should convert empty strings to null for optional fields", () => {
      const data: UpdateCompanySchema = {
        name: "Company",
        description: "",
        site_url: "   ",
      };

      const sanitized = sanitizeUpdateData(data);

      expect(sanitized.name).toBe("Company");
      expect(sanitized.description).toBeNull();
      expect(sanitized.site_url).toBeNull();
    });
  });
});
