import { describe, it, expect, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import UrlInput from "~/components/UrlInput.vue";
import { VALIDATION_MESSAGES } from "~/constants/validation";

describe("UrlInput", () => {
  let wrapper: any;

  beforeEach(() => {
    wrapper = mount(UrlInput, {
      props: {
        modelValue: "",
      },
    });
  });

  describe("Format validation", () => {
    it("should accept valid HTTPS URL", async () => {
      const input = wrapper.find("input");
      await input.setValue("https://example.com");
      await input.trigger("blur");

      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["https://example.com"]);
      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.URL_INVALID);

      const validationEvents = wrapper.emitted("validation");
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([true]);
    });

    it("should accept valid HTTP URL by default", async () => {
      const input = wrapper.find("input");
      await input.setValue("http://example.com");
      await input.trigger("blur");

      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["http://example.com"]);
      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.URL_INVALID);

      const validationEvents = wrapper.emitted("validation");
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([true]);
    });

    it("should reject HTTP when allowHttp is false", async () => {
      wrapper = mount(UrlInput, {
        props: {
          modelValue: "",
          allowHttp: false,
        },
      });

      const input = wrapper.find("input");
      await input.setValue("http://example.com");
      await input.trigger("blur");

      expect(wrapper.text()).toContain("URL doit utiliser HTTPS");

      const validationEvents = wrapper.emitted("validation");
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([false]);
    });

    it("should accept HTTPS when allowHttp is false", async () => {
      wrapper = mount(UrlInput, {
        props: {
          modelValue: "",
          allowHttp: false,
        },
      });

      const input = wrapper.find("input");
      await input.setValue("https://example.com");
      await input.trigger("blur");

      expect(wrapper.text()).not.toContain("URL doit utiliser HTTPS");

      const validationEvents = wrapper.emitted("validation");
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([true]);
    });

    it("should reject URL without protocol", async () => {
      const input = wrapper.find("input");
      await input.setValue("example.com");
      await input.trigger("blur");

      expect(wrapper.text()).toContain(VALIDATION_MESSAGES.URL_INVALID);

      const validationEvents = wrapper.emitted("validation");
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([false]);
    });

    it("should reject invalid URL format", async () => {
      const input = wrapper.find("input");
      await input.setValue("not a url");
      await input.trigger("blur");

      expect(wrapper.text()).toContain(VALIDATION_MESSAGES.URL_INVALID);

      const validationEvents = wrapper.emitted("validation");
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([false]);
    });

    it("should display error message on blur when invalid", async () => {
      const input = wrapper.find("input");
      await input.setValue("invalid");

      // Pas d'erreur pendant la saisie
      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.URL_INVALID);

      await input.trigger("blur");

      // Erreur affichée après le blur
      expect(wrapper.text()).toContain(VALIDATION_MESSAGES.URL_INVALID);
    });

    it("should show required error when field is required and empty", async () => {
      wrapper = mount(UrlInput, {
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

  describe("URL examples", () => {
    it("should accept URL with path", async () => {
      const input = wrapper.find("input");
      await input.setValue("https://example.com/path/to/page");
      await input.trigger("blur");

      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.URL_INVALID);
    });

    it("should accept URL with query parameters", async () => {
      const input = wrapper.find("input");
      await input.setValue("https://example.com?param=value&other=123");
      await input.trigger("blur");

      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.URL_INVALID);
    });

    it("should accept URL with fragment", async () => {
      const input = wrapper.find("input");
      await input.setValue("https://example.com#section");
      await input.trigger("blur");

      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.URL_INVALID);
    });

    it("should accept URL with subdomain", async () => {
      const input = wrapper.find("input");
      await input.setValue("https://subdomain.example.com");
      await input.trigger("blur");

      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.URL_INVALID);
    });

    it("should accept URL with port", async () => {
      const input = wrapper.find("input");
      await input.setValue("https://example.com:8080");
      await input.trigger("blur");

      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.URL_INVALID);
    });
  });

  describe("Props", () => {
    it("should display custom label", () => {
      wrapper = mount(UrlInput, {
        props: {
          modelValue: "",
          label: "Site web personnalisé",
        },
      });

      expect(wrapper.text()).toContain("Site web personnalisé");
    });

    it("should display custom placeholder", () => {
      wrapper = mount(UrlInput, {
        props: {
          modelValue: "",
          placeholder: "https://votre-site.fr",
        },
      });

      const input = wrapper.find("input");
      expect(input.attributes("placeholder")).toBe("https://votre-site.fr");
    });

    it("should display hint text when provided", () => {
      wrapper = mount(UrlInput, {
        props: {
          modelValue: "",
          hint: "L'URL doit commencer par https://",
        },
      });

      expect(wrapper.text()).toContain("L'URL doit commencer par https://");
    });

    it("should disable input when disabled prop is true", () => {
      wrapper = mount(UrlInput, {
        props: {
          modelValue: "",
          disabled: true,
        },
      });

      const input = wrapper.find("input");
      expect(input.attributes("disabled")).toBeDefined();
    });

    it("should show errors during input when showErrorOnInput is true", async () => {
      wrapper = mount(UrlInput, {
        props: {
          modelValue: "",
          showErrorOnInput: true,
        },
      });

      const input = wrapper.find("input");
      await input.setValue("invalid");

      // Avec showErrorOnInput: true, l'erreur s'affiche pendant la saisie
      expect(wrapper.text()).toContain(VALIDATION_MESSAGES.URL_INVALID);
    });

    it("should skip validation when validate is false", async () => {
      wrapper = mount(UrlInput, {
        props: {
          modelValue: "",
          validate: false,
        },
      });

      const input = wrapper.find("input");
      await input.setValue("not a url");
      await input.trigger("blur");

      // Pas d'erreur car validation désactivée
      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.URL_INVALID);
    });
  });

  describe("Validation events", () => {
    it("should emit validation event with true for valid URL", async () => {
      const input = wrapper.find("input");
      await input.setValue("https://example.com");

      const validationEvents = wrapper.emitted("validation");
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([true]);
    });

    it("should emit validation event with false for invalid URL", async () => {
      const input = wrapper.find("input");
      await input.setValue("invalid");

      const validationEvents = wrapper.emitted("validation");
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([false]);
    });
  });

  describe("Visual feedback", () => {
    it("should display error message in red", async () => {
      const input = wrapper.find("input");
      await input.setValue("invalid");
      await input.trigger("blur");

      const errorText = wrapper.find(".text-red-600");
      expect(errorText.exists()).toBe(true);
      expect(errorText.text()).toBe(VALIDATION_MESSAGES.URL_INVALID);
    });
  });
});
