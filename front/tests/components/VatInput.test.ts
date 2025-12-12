import { describe, it, expect, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import VatInput from "~/components/VatInput.vue";
import { VALIDATION_MESSAGES } from "~/constants/validation";

describe("VatInput", () => {
  let wrapper: any;

  beforeEach(() => {
    wrapper = mount(VatInput, {
      props: {
        modelValue: "",
      },
    });
  });

  describe("Format validation", () => {
    it("should accept valid French VAT number", async () => {
      const input = wrapper.find("input");
      await input.setValue("FR12345678901");
      await input.trigger("blur");

      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["FR12345678901"]);
      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.VAT_INVALID);

      const validationEvents = wrapper.emitted("validation");
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([true]);
    });

    it("should accept other EU country VAT numbers", async () => {
      const validVatNumbers = [
        "DE123456789", // Allemagne (9 chiffres)
        "BE0123456789", // Belgique (10 chiffres)
        "IT12345678901", // Italie (11 chiffres)
        "ES12345678901", // Espagne (11 chiffres)
      ];

      for (const vatNumber of validVatNumbers) {
        const input = wrapper.find("input");
        await input.setValue(vatNumber);
        await input.trigger("blur");

        expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.VAT_INVALID);
      }
    });

    it("should reject VAT with incorrect format", async () => {
      const input = wrapper.find("input");
      await input.setValue("FR123");
      await input.trigger("blur");

      expect(wrapper.text()).toContain(VALIDATION_MESSAGES.VAT_INVALID);

      const validationEvents = wrapper.emitted("validation");
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([false]);
    });

    it("should reject VAT without country code", async () => {
      const input = wrapper.find("input");
      await input.setValue("12345678901");
      await input.trigger("blur");

      expect(wrapper.text()).toContain(VALIDATION_MESSAGES.VAT_INVALID);

      const validationEvents = wrapper.emitted("validation");
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([false]);
    });

    it("should convert to uppercase", async () => {
      const input = wrapper.find("input");
      await input.setValue("fr12345678901");

      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["FR12345678901"]);
    });

    it("should display error message on blur when invalid", async () => {
      const input = wrapper.find("input");
      await input.setValue("FR123");

      // Pas d'erreur pendant la saisie
      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.VAT_INVALID);

      await input.trigger("blur");

      // Erreur affichée après le blur
      expect(wrapper.text()).toContain(VALIDATION_MESSAGES.VAT_INVALID);
    });

    it("should show required error when field is required and empty", async () => {
      wrapper = mount(VatInput, {
        props: {
          modelValue: "",
          required: true,
        },
      });

      const input = wrapper.find("input");
      await input.trigger("blur");

      expect(wrapper.text()).toContain(VALIDATION_MESSAGES.REQUIRED);

      const validationEvents = wrapper.emitted("validation");
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([false]);
    });

    it("should accept empty value when not required", async () => {
      const input = wrapper.find("input");
      await input.trigger("blur");

      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.REQUIRED);

      const validationEvents = wrapper.emitted("validation");
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([true]);
    });
  });

  describe("Input sanitization", () => {
    it("should remove all non-alphanumeric characters", async () => {
      const input = wrapper.find("input");
      await input.setValue("FR 12-345 678.901");

      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["FR12345678901"]);
    });

    it("should limit input to 13 characters", async () => {
      const input = wrapper.find("input");
      await input.setValue("FR123456789012345");

      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["FR12345678901"]);
    });

    it("should convert lowercase to uppercase", async () => {
      const input = wrapper.find("input");
      await input.setValue("fr12345678901");

      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["FR12345678901"]);
    });
  });

  describe("EU VAT numbers", () => {
    const validVatNumbers = [
      { country: "France", vat: "FR12345678901" },
      { country: "Germany", vat: "DE123456789" },
      { country: "Belgium", vat: "BE0123456789" },
      { country: "Italy", vat: "IT12345678901" },
      { country: "Spain", vat: "ES12345678901" },
      { country: "Netherlands", vat: "NL123456789B01" },
      { country: "Portugal", vat: "PT123456789" },
      { country: "Austria", vat: "ATU12345678" },
      { country: "Sweden", vat: "SE123456789001" },
      { country: "Poland", vat: "PL1234567890" },
    ];

    validVatNumbers.forEach(({ country, vat }) => {
      it(`should accept valid ${country} VAT: ${vat}`, async () => {
        const input = wrapper.find("input");
        await input.setValue(vat);
        await input.trigger("blur");

        expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.VAT_INVALID);
      });
    });
  });

  describe("Props", () => {
    it("should display custom label", () => {
      wrapper = mount(VatInput, {
        props: {
          modelValue: "",
          label: "Numéro de TVA",
        },
      });

      expect(wrapper.text()).toContain("Numéro de TVA");
    });

    it("should display custom placeholder", () => {
      wrapper = mount(VatInput, {
        props: {
          modelValue: "",
          placeholder: "DE123456789",
        },
      });

      const input = wrapper.find("input");
      expect(input.attributes("placeholder")).toBe("DE123456789");
    });

    it("should display hint text when provided", () => {
      wrapper = mount(VatInput, {
        props: {
          modelValue: "",
          hint: "Format: 2 lettres (pays) + chiffres",
        },
      });

      expect(wrapper.text()).toContain("Format: 2 lettres (pays) + chiffres");
    });

    it("should disable input when disabled prop is true", () => {
      wrapper = mount(VatInput, {
        props: {
          modelValue: "",
          disabled: true,
        },
      });

      const input = wrapper.find("input");
      expect(input.attributes("disabled")).toBeDefined();
    });

    it("should show errors during input when showErrorOnInput is true", async () => {
      wrapper = mount(VatInput, {
        props: {
          modelValue: "",
          showErrorOnInput: true,
        },
      });

      const input = wrapper.find("input");
      await input.setValue("FR123");

      // Avec showErrorOnInput: true, l'erreur s'affiche pendant la saisie
      expect(wrapper.text()).toContain(VALIDATION_MESSAGES.VAT_INVALID);
    });

    it("should skip validation when validate is false", async () => {
      wrapper = mount(VatInput, {
        props: {
          modelValue: "",
          validate: false,
        },
      });

      const input = wrapper.find("input");
      await input.setValue("INVALID");
      await input.trigger("blur");

      // Pas d'erreur car validation désactivée
      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.VAT_INVALID);
    });
  });

  describe("Validation events", () => {
    it("should emit validation event with true for valid VAT", async () => {
      const input = wrapper.find("input");
      await input.setValue("FR12345678901");

      const validationEvents = wrapper.emitted("validation");
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([true]);
    });

    it("should emit validation event with false for invalid VAT", async () => {
      const input = wrapper.find("input");
      await input.setValue("FR123");

      const validationEvents = wrapper.emitted("validation");
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([false]);
    });
  });

  describe("Visual feedback", () => {
    it("should display error message in red", async () => {
      const input = wrapper.find("input");
      await input.setValue("FR123");
      await input.trigger("blur");

      const errorText = wrapper.find(".text-red-600");
      expect(errorText.exists()).toBe(true);
      expect(errorText.text()).toBe(VALIDATION_MESSAGES.VAT_INVALID);
    });
  });

  describe("HTML attributes", () => {
    it('should have maxlength="13"', () => {
      const input = wrapper.find("input");
      expect(input.attributes("maxlength")).toBe("13");
    });

    it('should have type="text"', () => {
      const input = wrapper.find("input");
      expect(input.attributes("type")).toBe("text");
    });
  });
});
