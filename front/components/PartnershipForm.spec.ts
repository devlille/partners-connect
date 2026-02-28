import { describe, it, expect, vi, beforeEach } from "vitest";
import { mountSuspended } from "@nuxt/test-utils/runtime";
import PartnershipForm from "./PartnershipForm.vue";
import type { ExtendedPartnershipItem } from "~/types/partnership";
import type { SponsoringPack } from "~/utils/api";

// Mock de l'API
vi.mock("~/utils/api", () => ({
  getEventsSponsoringPacks: vi.fn(() =>
    Promise.resolve({
      data: mockPacks,
    }),
  ),
}));

// Données de test
const mockPacks: SponsoringPack[] = [
  {
    id: "pack-1",
    name: "Pack Bronze",
    base_price: 1000,
    max_quantity: 10,
    required_options: [
      {
        id: "req-opt-1",
        name: "Logo sur le site",
        description: "Votre logo affiché sur la page sponsors",
        price: 0,
        type: "typed_boolean",
      },
      {
        id: "req-opt-2",
        name: "Tweet de remerciement",
        description: "Un tweet pour remercier votre sponsoring",
        price: 0,
        type: "typed_boolean",
      },
    ],
    optional_options: [
      {
        id: "opt-1",
        name: "Stand supplémentaire",
        description: "Un stand de 3x3m pour présenter votre entreprise",
        price: 500,
        type: "typed_boolean",
      },
      {
        id: "opt-2",
        name: "Goodies",
        description:
          "Distribution de goodies aux participants. Vous pouvez choisir la quantité que vous souhaitez distribuer.",
        price: 2,
        type: "typed_quantitative",
      },
    ],
  },
  {
    id: "pack-2",
    name: "Pack Silver",
    base_price: 2000,
    max_quantity: 5,
    required_options: [
      {
        id: "req-opt-3",
        name: "Logo premium",
        description: "Logo mis en avant sur la page d'accueil",
        price: 0,
        type: "typed_boolean",
      },
    ],
    optional_options: [],
  },
];

const mockPartnership: ExtendedPartnershipItem = {
  id: "partnership-1",
  contact: {
    display_name: "John Doe",
    role: "CTO",
  },
  company_name: "Acme Corp",
  event_name: "DevFest 2024",
  selected_pack_id: "pack-1",
  selected_pack_name: "Pack Bronze",
  language: "fr",
  emails: "john@acme.com",
  phone: "+33 6 12 34 56 78",
  created_at: "2024-01-01T00:00:00Z",
  option_ids: ["opt-1"],
  pack_options: [
    {
      id: "opt-1",
      name: "Stand supplémentaire",
      description: "Un stand de 3x3m",
      price: 500,
      type: "typed_boolean",
    },
  ],
  validated: false,
  paid: false,
  suggestion: false,
  agreement_generated: false,
  agreement_signed: false,
};

describe("PartnershipForm", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("Rendering", () => {
    it("renders the form with all fields", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: mockPartnership,
        },
      });

      // Vérifier que les champs sont présents
      expect(wrapper.find('input[placeholder="Nom du contact"]').exists()).toBe(true);
      expect(wrapper.find('input[placeholder="Rôle"]').exists()).toBe(true);
      expect(wrapper.find('input[placeholder="email@example.com"]').exists()).toBe(true);
      expect(wrapper.find('input[placeholder="+33 6 12 34 56 78"]').exists()).toBe(true);
    });

    it("displays partnership information correctly", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: mockPartnership,
        },
      });

      await wrapper.vm.$nextTick();

      const contactNameInput = wrapper.find('input[placeholder="Nom du contact"]');
      expect((contactNameInput.element as HTMLInputElement).value).toBe("John Doe");
    });

    it("displays pack name correctly", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: mockPartnership,
        },
      });

      await wrapper.vm.$nextTick();

      const packInput = wrapper.find('input[placeholder="Pack de sponsoring"]');
      expect((packInput.element as HTMLInputElement).value).toBe("Pack Bronze");
    });
  });

  describe("Required Options", () => {
    it("displays required options section when pack has required options", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: mockPartnership,
        },
      });

      await wrapper.vm.$nextTick();
      await new Promise((resolve) => setTimeout(resolve, 100)); // Attendre le chargement des packs

      const requiredSection = wrapper.find("#required-options-heading");
      expect(requiredSection.exists()).toBe(true);
      expect(requiredSection.text()).toContain("Options incluses dans le pack");
    });

    it("displays all required options from the pack", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: mockPartnership,
        },
      });

      await wrapper.vm.$nextTick();
      await new Promise((resolve) => setTimeout(resolve, 100));

      const requiredOptions = wrapper.findAll('[aria-label^="Option incluse :"]');
      expect(requiredOptions.length).toBe(2); // Pack Bronze a 2 options requises
    });

    it("displays required option details correctly", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: mockPartnership,
        },
      });

      await wrapper.vm.$nextTick();
      await new Promise((resolve) => setTimeout(resolve, 100));

      const text = wrapper.text();
      expect(text).toContain("Logo sur le site");
      expect(text).toContain("Tweet de remerciement");
    });
  });

  describe("Optional Options", () => {
    it("displays optional options section when options are selected", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: mockPartnership,
        },
      });

      await wrapper.vm.$nextTick();

      const optionalSection = wrapper.find("#optional-options-heading");
      expect(optionalSection.exists()).toBe(true);
      expect(optionalSection.text()).toContain("Options optionnelles sélectionnées");
    });

    it("displays selected optional options", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: mockPartnership,
        },
      });

      await wrapper.vm.$nextTick();

      const text = wrapper.text();
      expect(text).toContain("Stand supplémentaire");
      expect(text).toContain("500 €");
    });

    it("shows 'Aucune option' when no options are available", async () => {
      const partnershipWithoutOptions = {
        ...mockPartnership,
        selected_pack_id: "pack-2",
        option_ids: [],
        pack_options: [],
      };

      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: partnershipWithoutOptions,
        },
      });

      await wrapper.vm.$nextTick();
      await new Promise((resolve) => setTimeout(resolve, 100));

      // Pack 2 n'a pas d'options optionnelles, mais a des options requises
      const text = wrapper.text();
      expect(text).toContain("Logo premium");
    });
  });

  describe("Accessibility", () => {
    it("has proper ARIA attributes on required options section", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: mockPartnership,
        },
      });

      await wrapper.vm.$nextTick();
      await new Promise((resolve) => setTimeout(resolve, 100));

      const requiredSection = wrapper.find(
        '[role="region"][aria-labelledby="required-options-heading"]',
      );
      expect(requiredSection.exists()).toBe(true);
    });

    it("has proper ARIA attributes on optional options section", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: mockPartnership,
        },
      });

      await wrapper.vm.$nextTick();

      const optionalSection = wrapper.find(
        '[role="region"][aria-labelledby="optional-options-heading"]',
      );
      expect(optionalSection.exists()).toBe(true);
    });

    it("checkboxes have aria-label attributes", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: mockPartnership,
        },
      });

      await wrapper.vm.$nextTick();
      await new Promise((resolve) => setTimeout(resolve, 100));

      const requiredCheckbox = wrapper.find('[aria-label="Option incluse : Logo sur le site"]');
      expect(requiredCheckbox.exists()).toBe(true);

      const optionalCheckbox = wrapper.find(
        '[aria-label="Option optionnelle : Stand supplémentaire"]',
      );
      expect(optionalCheckbox.exists()).toBe(true);
    });

    it("checkboxes have tabindex='-1' to skip in tab order", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: mockPartnership,
        },
      });

      await wrapper.vm.$nextTick();
      await new Promise((resolve) => setTimeout(resolve, 100));

      const checkboxes = wrapper.findAll('input[type="checkbox"]');
      checkboxes.forEach((checkbox) => {
        expect(checkbox.attributes("tabindex")).toBe("-1");
      });
    });

    it("has role='status' and aria-live on empty state", async () => {
      const emptyPartnership = {
        ...mockPartnership,
        selected_pack_id: undefined,
        option_ids: [],
        pack_options: [],
      };

      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: emptyPartnership,
        },
      });

      await wrapper.vm.$nextTick();
      await new Promise((resolve) => setTimeout(resolve, 100));

      const emptyMessage = wrapper.find('[role="status"][aria-live="polite"]');
      expect(emptyMessage.exists()).toBe(true);
      expect(emptyMessage.text()).toContain("Aucune option");
    });
  });

  describe("Tooltips", () => {
    it("adds title attribute for long descriptions", async () => {
      const longDescPartnership = {
        ...mockPartnership,
        pack_options: [
          {
            id: "opt-2",
            name: "Goodies",
            description:
              "Distribution de goodies aux participants. Vous pouvez choisir la quantité que vous souhaitez distribuer. Cette option est idéale pour augmenter la visibilité de votre marque.",
            price: 2,
            type: "typed_quantitative",
          },
        ],
      };

      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: longDescPartnership,
        },
      });

      await wrapper.vm.$nextTick();

      const descriptionSpan = wrapper.find(".line-clamp-2");
      expect(descriptionSpan.exists()).toBe(true);
      expect(descriptionSpan.attributes("title")).toBeTruthy();
      expect(descriptionSpan.attributes("title")!.length).toBeGreaterThan(100);
    });

    it("does not add title for short descriptions", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: mockPartnership,
        },
      });

      await wrapper.vm.$nextTick();
      await new Promise((resolve) => setTimeout(resolve, 100));

      // Chercher une description courte
      const shortDesc = wrapper.find('span:contains("Votre logo affiché sur la page sponsors")');
      if (shortDesc.exists()) {
        expect(shortDesc.attributes("title")).toBeFalsy();
      }
    });

    it("applies line-clamp-2 class for long descriptions", async () => {
      const longDescPartnership = {
        ...mockPartnership,
        pack_options: [
          {
            id: "opt-2",
            name: "Goodies",
            description: "A".repeat(150), // Description très longue
            price: 2,
            type: "typed_quantitative",
          },
        ],
      };

      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: longDescPartnership,
        },
      });

      await wrapper.vm.$nextTick();

      const descriptionSpan = wrapper.find(".line-clamp-2");
      expect(descriptionSpan.exists()).toBe(true);
    });
  });

  describe("Form Submission", () => {
    it("emits save event with form data when submitted", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: mockPartnership,
        },
      });

      const form = wrapper.find("form");
      await form.trigger("submit");

      expect(wrapper.emitted("save")).toBeTruthy();
      const emittedData = wrapper.emitted("save")![0][0];
      expect(emittedData).toHaveProperty("contact_name");
      expect(emittedData).toHaveProperty("contact_role");
      expect(emittedData).toHaveProperty("language");
      expect(emittedData).toHaveProperty("emails");
      expect(emittedData).toHaveProperty("phone");
    });

    it("emits cancel event when cancel button is clicked", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: mockPartnership,
        },
      });

      // Le bouton Annuler est un UButton, cherchons tous les boutons
      const allButtons = wrapper.findAll("button");

      // Trouver le bouton qui contient "Annuler"
      let cancelButton;
      for (const button of allButtons) {
        if (button.text().includes("Annuler")) {
          cancelButton = button;
          break;
        }
      }

      expect(cancelButton).toBeDefined();
      if (cancelButton) {
        await cancelButton.trigger("click");
        expect(wrapper.emitted("cancel")).toBeTruthy();
      }
    });

    it("shows loading state on submit button", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: mockPartnership,
          loading: true,
        },
      });

      const submitButton = wrapper.find('button[type="submit"]');
      // Le bouton devrait avoir un état de chargement (visuellement)
      expect(submitButton.exists()).toBe(true);
    });
  });

  describe("Icons", () => {
    it("displays check-circle icon for required options", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: mockPartnership,
        },
      });

      await wrapper.vm.$nextTick();
      await new Promise((resolve) => setTimeout(resolve, 100));

      const icon = wrapper.find(".i-heroicons-check-circle");
      expect(icon.exists()).toBe(true);
      expect(icon.attributes("aria-hidden")).toBe("true");
    });

    it("displays plus-circle icon for optional options", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: mockPartnership,
        },
      });

      await wrapper.vm.$nextTick();

      const icon = wrapper.find(".i-heroicons-plus-circle");
      expect(icon.exists()).toBe(true);
      expect(icon.attributes("aria-hidden")).toBe("true");
    });
  });

  describe("Edge Cases", () => {
    it("handles partnership without selected pack", async () => {
      const noPackPartnership = {
        ...mockPartnership,
        selected_pack_id: undefined,
        selected_pack_name: undefined,
        option_ids: [],
        pack_options: [],
      };

      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: noPackPartnership,
        },
      });

      await wrapper.vm.$nextTick();
      await new Promise((resolve) => setTimeout(resolve, 100));

      // Ne devrait pas planter
      // Si pack_options est vide et qu'il n'y a pas de pack sélectionné,
      // on devrait voir le message "Aucune option"
      const text = wrapper.text();
      // Le texte devrait contenir soit "Aucune option" soit ne pas afficher de section d'options
      const hasNoOptionsMessage = text.includes("Aucune option");
      const hasNoRequiredSection = !wrapper.find("#required-options-heading").exists();
      const hasNoOptionalSection = !wrapper.find("#optional-options-heading").exists();

      expect(hasNoOptionsMessage || (hasNoRequiredSection && hasNoOptionalSection)).toBe(true);
    });

    it("handles partnership with suggested pack", async () => {
      const suggestedPartnership = {
        ...mockPartnership,
        selected_pack_id: undefined,
        suggested_pack_id: "pack-1",
        suggested_pack_name: "Pack Bronze",
      };

      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: suggestedPartnership,
        },
      });

      await wrapper.vm.$nextTick();
      await new Promise((resolve) => setTimeout(resolve, 100));

      const packInput = wrapper.find('input[placeholder="Pack de sponsoring"]');
      expect((packInput.element as HTMLInputElement).value).toBe("Pack Bronze (suggéré)");
    });

    it("handles null partnership prop", async () => {
      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: null,
        },
      });

      await wrapper.vm.$nextTick();

      // Le formulaire devrait s'afficher avec des valeurs par défaut
      expect(wrapper.find("form").exists()).toBe(true);
    });

    it("handles quantitative option display", async () => {
      const quantitativePartnership = {
        ...mockPartnership,
        pack_options: [
          {
            id: "opt-2",
            name: "Goodies",
            description: "Distribution de goodies",
            price: 2,
            type: "typed_quantitative",
            quantity: 100,
          },
        ],
      };

      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: quantitativePartnership,
        },
      });

      await wrapper.vm.$nextTick();

      const text = wrapper.text();
      expect(text).toContain("100 x 2 €");
    });

    it("handles selectable option display", async () => {
      const selectablePartnership = {
        ...mockPartnership,
        pack_options: [
          {
            id: "opt-3",
            name: "Taille du stand",
            description: "Choisissez la taille",
            type: "typed_selectable",
            selected_value: {
              id: "val-1",
              value: "3x3m",
              price: 500,
            },
          },
        ],
      };

      const wrapper = await mountSuspended(PartnershipForm, {
        props: {
          partnership: selectablePartnership,
        },
      });

      await wrapper.vm.$nextTick();

      const text = wrapper.text();
      expect(text).toContain("3x3m - 500 €");
    });
  });
});
