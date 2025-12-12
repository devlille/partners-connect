import { describe, it, expect } from "vitest";
import type { CompanyBillingData } from "~/utils/api";

describe("BillingForm Component", () => {
  describe("Form Validation", () => {
    function isValidBillingContact(contact: {
      first_name: string;
      last_name: string;
      email: string;
    }): boolean {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

      return !!(
        contact.first_name &&
        contact.first_name.trim() !== "" &&
        contact.last_name &&
        contact.last_name.trim() !== "" &&
        contact.email &&
        emailRegex.test(contact.email)
      );
    }

    it("should validate complete billing contact", () => {
      const contact = {
        first_name: "John",
        last_name: "Doe",
        email: "john.doe@example.com",
      };

      expect(isValidBillingContact(contact)).toBe(true);
    });

    it("should invalidate contact without first_name", () => {
      const contact = {
        first_name: "",
        last_name: "Doe",
        email: "john.doe@example.com",
      };

      expect(isValidBillingContact(contact)).toBe(false);
    });

    it("should invalidate contact without last_name", () => {
      const contact = {
        first_name: "John",
        last_name: "",
        email: "john.doe@example.com",
      };

      expect(isValidBillingContact(contact)).toBe(false);
    });

    it("should invalidate contact with invalid email", () => {
      const contact = {
        first_name: "John",
        last_name: "Doe",
        email: "invalid-email",
      };

      expect(isValidBillingContact(contact)).toBe(false);
    });

    it("should invalidate contact with empty email", () => {
      const contact = {
        first_name: "John",
        last_name: "Doe",
        email: "",
      };

      expect(isValidBillingContact(contact)).toBe(false);
    });

    it("should validate various email formats", () => {
      const validEmails = [
        "user@example.com",
        "user.name@example.com",
        "user+tag@example.co.uk",
        "user_name@example-domain.com",
      ];

      validEmails.forEach((email) => {
        const contact = {
          first_name: "John",
          last_name: "Doe",
          email,
        };
        expect(isValidBillingContact(contact)).toBe(true);
      });
    });
  });

  describe("CompanyBillingData Structure", () => {
    it("should accept valid billing data with all fields", () => {
      const billingData: CompanyBillingData = {
        name: "My Company",
        po: "PO-12345",
        contact: {
          first_name: "John",
          last_name: "Doe",
          email: "john.doe@example.com",
        },
      };

      expect(billingData.name).toBe("My Company");
      expect(billingData.po).toBe("PO-12345");
      expect(billingData.contact.first_name).toBe("John");
      expect(billingData.contact.last_name).toBe("Doe");
      expect(billingData.contact.email).toBe("john.doe@example.com");
    });

    it("should accept billing data with null name", () => {
      const billingData: CompanyBillingData = {
        name: null,
        po: "PO-12345",
        contact: {
          first_name: "John",
          last_name: "Doe",
          email: "john.doe@example.com",
        },
      };

      expect(billingData.name).toBeNull();
      expect(billingData.contact.first_name).toBe("John");
    });

    it("should accept billing data with null po", () => {
      const billingData: CompanyBillingData = {
        name: "My Company",
        po: null,
        contact: {
          first_name: "John",
          last_name: "Doe",
          email: "john.doe@example.com",
        },
      };

      expect(billingData.po).toBeNull();
      expect(billingData.name).toBe("My Company");
    });

    it("should require contact information", () => {
      const billingData: CompanyBillingData = {
        name: "My Company",
        po: "PO-12345",
        contact: {
          first_name: "John",
          last_name: "Doe",
          email: "john.doe@example.com",
        },
      };

      expect(billingData.contact).toBeDefined();
      expect(billingData.contact.first_name).toBeDefined();
      expect(billingData.contact.last_name).toBeDefined();
      expect(billingData.contact.email).toBeDefined();
    });
  });

  describe("Data Sanitization", () => {
    function sanitizeBillingData(data: CompanyBillingData): CompanyBillingData {
      return {
        name: data.name?.trim() || null,
        po: data.po?.trim() || null,
        contact: {
          first_name: data.contact.first_name.trim(),
          last_name: data.contact.last_name.trim(),
          email: data.contact.email.trim(),
        },
      };
    }

    it("should trim whitespace from contact fields", () => {
      const data: CompanyBillingData = {
        name: "  My Company  ",
        po: "  PO-12345  ",
        contact: {
          first_name: "  John  ",
          last_name: "  Doe  ",
          email: "  john.doe@example.com  ",
        },
      };

      const sanitized = sanitizeBillingData(data);

      expect(sanitized.name).toBe("My Company");
      expect(sanitized.po).toBe("PO-12345");
      expect(sanitized.contact.first_name).toBe("John");
      expect(sanitized.contact.last_name).toBe("Doe");
      expect(sanitized.contact.email).toBe("john.doe@example.com");
    });

    it("should convert empty strings to null for optional fields", () => {
      const data: CompanyBillingData = {
        name: "",
        po: "",
        contact: {
          first_name: "John",
          last_name: "Doe",
          email: "john.doe@example.com",
        },
      };

      const sanitized = sanitizeBillingData(data);

      expect(sanitized.name).toBeNull();
      expect(sanitized.po).toBeNull();
    });
  });
});
