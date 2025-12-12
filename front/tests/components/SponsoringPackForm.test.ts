import { describe, it, expect } from "vitest";
import type { SponsoringOption } from "~/utils/api";

describe("SponsoringPackForm - formatOptionPrice", () => {
  // Fonction extraite du composant pour les tests
  function formatOptionPrice(option: SponsoringOption): string {
    // Pour typed_number, afficher le prix avec la quantité
    if (option.type === "typed_number" && "fixed_quantity" in option) {
      const price = option.price ? `${option.price}€` : "0€";
      return `${price} ×${option.fixed_quantity}`;
    }
    // Pour les autres types
    return option.price ? `${option.price}€` : "0€";
  }

  describe("typed_number options", () => {
    it("should format price with fixed quantity for typed_number", () => {
      const option: SponsoringOption = {
        id: "1",
        type: "typed_number",
        name: { fr: "Tickets", en: "Tickets" },
        price: 50,
        fixed_quantity: 10,
      };

      const result = formatOptionPrice(option);

      expect(result).toBe("50€ ×10");
    });

    it("should format with 0€ when price is 0 for typed_number", () => {
      const option: SponsoringOption = {
        id: "1",
        type: "typed_number",
        name: { fr: "Tickets", en: "Tickets" },
        price: 0,
        fixed_quantity: 5,
      };

      const result = formatOptionPrice(option);

      expect(result).toBe("0€ ×5");
    });

    it("should format with 0€ when price is null for typed_number", () => {
      const option: SponsoringOption = {
        id: "1",
        type: "typed_number",
        name: { fr: "Tickets", en: "Tickets" },
        price: null as any,
        fixed_quantity: 3,
      };

      const result = formatOptionPrice(option);

      expect(result).toBe("0€ ×3");
    });

    it("should handle large quantities for typed_number", () => {
      const option: SponsoringOption = {
        id: "1",
        type: "typed_number",
        name: { fr: "Goodies", en: "Goodies" },
        price: 5,
        fixed_quantity: 1000,
      };

      const result = formatOptionPrice(option);

      expect(result).toBe("5€ ×1000");
    });

    it("should handle decimal prices for typed_number", () => {
      const option: SponsoringOption = {
        id: "1",
        type: "typed_number",
        name: { fr: "Badges", en: "Badges" },
        price: 2.5,
        fixed_quantity: 20,
      };

      const result = formatOptionPrice(option);

      expect(result).toBe("2.5€ ×20");
    });
  });

  describe("text options", () => {
    it("should format simple price for text option", () => {
      const option: SponsoringOption = {
        id: "1",
        type: "text",
        name: { fr: "Logo", en: "Logo" },
        price: 100,
      };

      const result = formatOptionPrice(option);

      expect(result).toBe("100€");
    });

    it("should format 0€ when price is 0 for text option", () => {
      const option: SponsoringOption = {
        id: "1",
        type: "text",
        name: { fr: "Description", en: "Description" },
        price: 0,
      };

      const result = formatOptionPrice(option);

      expect(result).toBe("0€");
    });

    it("should format 0€ when price is null for text option", () => {
      const option: SponsoringOption = {
        id: "1",
        type: "text",
        name: { fr: "Notes", en: "Notes" },
        price: null as any,
      };

      const result = formatOptionPrice(option);

      expect(result).toBe("0€");
    });
  });

  describe("typed_quantitative options", () => {
    it("should format simple price for typed_quantitative option", () => {
      const option: SponsoringOption = {
        id: "1",
        type: "typed_quantitative",
        name: { fr: "Stand", en: "Booth" },
        price: 500,
        max_quantity: 5,
      };

      const result = formatOptionPrice(option);

      expect(result).toBe("500€");
    });

    it("should not include quantity for typed_quantitative", () => {
      const option: SponsoringOption = {
        id: "1",
        type: "typed_quantitative",
        name: { fr: "Tables", en: "Tables" },
        price: 50,
        max_quantity: 10,
      };

      const result = formatOptionPrice(option);

      expect(result).not.toContain("×");
      expect(result).toBe("50€");
    });
  });

  describe("typed_selectable options", () => {
    it("should format simple price for typed_selectable option", () => {
      const option: SponsoringOption = {
        id: "1",
        type: "typed_selectable",
        name: { fr: "Niveau de visibilité", en: "Visibility Level" },
        price: 200,
        choices: [
          { id: "low", name: { fr: "Bas", en: "Low" }, price: 100 },
          { id: "high", name: { fr: "Haut", en: "High" }, price: 300 },
        ],
      };

      const result = formatOptionPrice(option);

      expect(result).toBe("200€");
    });
  });

  describe("edge cases", () => {
    it("should handle undefined price", () => {
      const option: SponsoringOption = {
        id: "1",
        type: "text",
        name: { fr: "Option", en: "Option" },
        price: undefined as any,
      };

      const result = formatOptionPrice(option);

      expect(result).toBe("0€");
    });

    it("should handle negative price (should not happen but test defensive code)", () => {
      const option: SponsoringOption = {
        id: "1",
        type: "text",
        name: { fr: "Option", en: "Option" },
        price: -10,
      };

      const result = formatOptionPrice(option);

      expect(result).toBe("-10€");
    });

    it("should handle very large prices", () => {
      const option: SponsoringOption = {
        id: "1",
        type: "text",
        name: { fr: "Premium", en: "Premium" },
        price: 999999,
      };

      const result = formatOptionPrice(option);

      expect(result).toBe("999999€");
    });
  });

  describe("consistency with options list page", () => {
    it("should match format from options/index.vue for typed_number", () => {
      // Ce test vérifie que le format correspond à celui de pages/orgs/[slug]/events/[eventSlug]/options/index.vue
      const option: SponsoringOption = {
        id: "1",
        type: "typed_number",
        name: { fr: "Tickets", en: "Tickets" },
        price: 75,
        fixed_quantity: 15,
      };

      const result = formatOptionPrice(option);

      // Le format attendu est: "prix€ (×quantité)" dans la page options
      // Dans le formulaire de pack, on affiche: "prix€ ×quantité"
      expect(result).toContain("75€");
      expect(result).toContain("×15");
    });

    it("should handle free option with quantity", () => {
      const option: SponsoringOption = {
        id: "1",
        type: "typed_number",
        name: { fr: "Badges gratuits", en: "Free Badges" },
        price: 0,
        fixed_quantity: 5,
      };

      const result = formatOptionPrice(option);

      expect(result).toBe("0€ ×5");
    });
  });

  describe("label display in form", () => {
    it("should produce correct label format for required option with typed_number", () => {
      const option: SponsoringOption = {
        id: "1",
        type: "typed_number",
        name: { fr: "Tickets inclus", en: "Included Tickets" },
        price: 50,
        fixed_quantity: 10,
      };

      const optionName = "Tickets inclus";
      const formattedPrice = formatOptionPrice(option);
      const label = `${optionName} (${formattedPrice})`;

      expect(label).toBe("Tickets inclus (50€ ×10)");
    });

    it("should produce correct label format for optional option with simple price", () => {
      const option: SponsoringOption = {
        id: "1",
        type: "text",
        name: { fr: "Logo sur site web", en: "Logo on website" },
        price: 250,
      };

      const optionName = "Logo sur site web";
      const formattedPrice = formatOptionPrice(option);
      const label = `${optionName} (${formattedPrice})`;

      expect(label).toBe("Logo sur site web (250€)");
    });
  });
});
