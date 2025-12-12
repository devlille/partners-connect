import { describe, it, expect } from "vitest";
import { useOptionTranslation } from "~/composables/useOptionTranslation";
import type { SponsoringOption } from "~/utils/api";

describe("useOptionTranslation", () => {
  const { getOptionName, getOptionDescription } = useOptionTranslation();

  describe("getOptionName", () => {
    it("should return the name from a text option", () => {
      const option: SponsoringOption = {
        type: "text",
        id: "1",
        name: "Option texte",
        description: "Description de l'option",
        price: 100,
      };

      const name = getOptionName(option);
      expect(name).toBe("Option texte");
    });

    it("should return the name from a typed_number option", () => {
      const option: SponsoringOption = {
        type: "typed_number",
        id: "2",
        name: "Billets",
        description: "10 billets",
        price: 500,
        type_descriptor: "nb_ticket",
        fixed_quantity: 10,
      };

      const name = getOptionName(option);
      expect(name).toBe("Billets");
    });

    it("should return the name from a typed_quantitative option", () => {
      const option: SponsoringOption = {
        type: "typed_quantitative",
        id: "3",
        name: "Offres d'emploi",
        price: 200,
        type_descriptor: "job_offer",
      };

      const name = getOptionName(option);
      expect(name).toBe("Offres d'emploi");
    });

    it("should return default text when no name is available", () => {
      const option: SponsoringOption = {
        type: "text",
        id: "4",
        name: "",
        price: 100,
      };

      const name = getOptionName(option);
      expect(name).toBe("Option sans nom");
    });
  });

  describe("getOptionDescription", () => {
    it("should return the description from an option", () => {
      const option: SponsoringOption = {
        type: "text",
        id: "1",
        name: "Option FR",
        description: "Description de l'option",
        price: 100,
      };

      const description = getOptionDescription(option);
      expect(description).toBe("Description de l'option");
    });

    it("should return description when available", () => {
      const option: SponsoringOption = {
        type: "typed_selectable",
        id: "2",
        name: "Stand",
        description: "Choisissez votre stand",
        price: 1000,
        type_descriptor: "booth",
        selectable_values: ["3x3", "6x3"],
      };

      const description = getOptionDescription(option);
      expect(description).toBe("Choisissez votre stand");
    });

    it("should return null when no description is available", () => {
      const option: SponsoringOption = {
        type: "text",
        id: "3",
        name: "Test",
        price: 100,
      };

      const description = getOptionDescription(option);
      expect(description).toBeNull();
    });
  });
});
