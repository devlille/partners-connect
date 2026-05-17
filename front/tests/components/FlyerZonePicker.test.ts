import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import FlyerZonePicker from "~/components/FlyerZonePicker.vue";

const translations: Record<string, string> = {
  "flyer.zone.errors.outOfBounds": "La zone est hors des limites du gabarit",
  "flyer.zone.widthLabel": "Largeur",
  "flyer.zone.heightLabel": "Hauteur",
};

const mountOptions = {
  global: {
    mocks: {
      $t: (key: string) => translations[key] ?? key,
    },
  },
};

describe("FlyerZonePicker", () => {
  it("emits update:zone when numeric inputs change", async () => {
    const wrapper = mount(FlyerZonePicker, {
      props: {
        templateUrl: "data:image/png;base64,iVBORw0KGgo=",
        naturalWidth: 1200,
        naturalHeight: 800,
        zone: { x: 0, y: 0, width: 100, height: 100 },
      },
      ...mountOptions,
    });

    const inputs = wrapper.findAll("input[type=number]");
    expect(inputs).toHaveLength(4);

    await inputs[2]!.setValue(200);
    await inputs[2]!.trigger("change");

    const events = wrapper.emitted("update:zone");
    expect(events).toBeTruthy();
    const lastEvent = events![events!.length - 1]![0] as { width: number };
    expect(lastEvent.width).toBe(200);
  });

  it("shows an out-of-bounds error when zone exceeds the template", async () => {
    const wrapper = mount(FlyerZonePicker, {
      props: {
        templateUrl: "data:image/png;base64,iVBORw0KGgo=",
        naturalWidth: 100,
        naturalHeight: 100,
        zone: { x: 50, y: 50, width: 80, height: 80 },
      },
      ...mountOptions,
    });

    const alert = wrapper.find('[role="alert"]');
    expect(alert.exists()).toBe(true);
    expect(alert.text()).toBe(translations["flyer.zone.errors.outOfBounds"]);
  });
});
