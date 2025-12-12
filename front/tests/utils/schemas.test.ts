import { describe, it, expect } from "vitest";
import {
  sponsoringPackSchema,
  sponsoringOptionSchema,
  packOptionsSchema,
  sponsorSchema,
} from "~/utils/validation/schemas";

describe("Validation Schemas", () => {
  describe("sponsoringPackSchema", () => {
    it("should validate a valid pack", () => {
      const validPack = {
        name: "Gold Pack",
        base_price: 1000,
        max_quantity: 10,
        nb_tickets: 5,
        with_booth: true,
      };

      const result = sponsoringPackSchema.safeParse(validPack);
      expect(result.success).toBe(true);
    });

    it("should reject pack with empty name", () => {
      const invalidPack = {
        name: "",
        base_price: 1000,
      };

      const result = sponsoringPackSchema.safeParse(invalidPack);
      expect(result.success).toBe(false);
    });

    it("should reject pack with negative price", () => {
      const invalidPack = {
        name: "Gold Pack",
        base_price: -100,
      };

      const result = sponsoringPackSchema.safeParse(invalidPack);
      expect(result.success).toBe(false);
    });

    it("should reject pack with invalid quantity", () => {
      const invalidPack = {
        name: "Gold Pack",
        base_price: 1000,
        max_quantity: -5,
      };

      const result = sponsoringPackSchema.safeParse(invalidPack);
      expect(result.success).toBe(false);
    });
  });

  describe("sponsoringOptionSchema", () => {
    it("should validate a valid option", () => {
      const validOption = {
        translations: {
          fr: { name: "Option FR", description: "Description FR" },
        },
        price: 100,
        is_free: false,
      };

      const result = sponsoringOptionSchema.safeParse(validOption);
      expect(result.success).toBe(true);
    });

    it("should reject option with empty translations", () => {
      const invalidOption = {
        translations: {},
        price: 100,
      };

      const result = sponsoringOptionSchema.safeParse(invalidOption);
      expect(result.success).toBe(false);
    });

    it("should reject option with invalid translation name", () => {
      const invalidOption = {
        translations: {
          fr: { name: "", description: "Description" },
        },
        price: 100,
      };

      const result = sponsoringOptionSchema.safeParse(invalidOption);
      expect(result.success).toBe(false);
    });

    it("should reject option with negative price", () => {
      const invalidOption = {
        translations: {
          fr: { name: "Option", description: "Description" },
        },
        price: -50,
      };

      const result = sponsoringOptionSchema.safeParse(invalidOption);
      expect(result.success).toBe(false);
    });
  });

  describe("packOptionsSchema", () => {
    it("should validate when no overlap between required and optional", () => {
      const valid = {
        requiredOptions: ["opt1", "opt2"],
        optionalOptions: ["opt3", "opt4"],
      };

      const result = packOptionsSchema.safeParse(valid);
      expect(result.success).toBe(true);
    });

    it("should reject when option is both required and optional", () => {
      const invalid = {
        requiredOptions: ["opt1", "opt2"],
        optionalOptions: ["opt2", "opt3"],
      };

      const result = packOptionsSchema.safeParse(invalid);
      expect(result.success).toBe(false);
    });

    it("should validate empty arrays", () => {
      const valid = {
        requiredOptions: [],
        optionalOptions: [],
      };

      const result = packOptionsSchema.safeParse(valid);
      expect(result.success).toBe(true);
    });
  });

  describe("sponsorSchema", () => {
    it("should validate a valid sponsor", () => {
      const validSponsor = {
        company_name: "Test Company",
        contact_name: "John Doe",
        contact_role: "CEO",
        email: "john@test.com",
        phone: "+33123456789",
        pack_id: "pack-1",
        option_ids: ["opt1", "opt2"],
      };

      const result = sponsorSchema.safeParse(validSponsor);
      expect(result.success).toBe(true);
    });

    it("should reject sponsor with empty company name", () => {
      const invalid = {
        company_name: "",
        contact_name: "John Doe",
        email: "john@test.com",
        pack_id: "pack-1",
      };

      const result = sponsorSchema.safeParse(invalid);
      expect(result.success).toBe(false);
    });

    it("should reject sponsor with invalid email", () => {
      const invalid = {
        company_name: "Test Company",
        contact_name: "John Doe",
        email: "invalid-email",
        pack_id: "pack-1",
      };

      const result = sponsorSchema.safeParse(invalid);
      expect(result.success).toBe(false);
    });

    it("should reject sponsor without pack_id", () => {
      const invalid = {
        company_name: "Test Company",
        contact_name: "John Doe",
        email: "john@test.com",
        pack_id: "",
      };

      const result = sponsorSchema.safeParse(invalid);
      expect(result.success).toBe(false);
    });
  });
});
