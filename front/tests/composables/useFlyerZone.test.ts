import { describe, it, expect } from "vitest";
import {
  toTemplatePixels,
  fromTemplatePixels,
  zoneFitsInsideTemplate,
} from "~/composables/useFlyerZone";

describe("useFlyerZone", () => {
  describe("toTemplatePixels", () => {
    it("scales a display-pixel rectangle to template-pixel coords", () => {
      const result = toTemplatePixels(
        { x: 50, y: 25, width: 100, height: 50 },
        { displayWidth: 600, displayHeight: 400, naturalWidth: 1200, naturalHeight: 800 },
      );
      expect(result).toEqual({ x: 100, y: 50, width: 200, height: 100 });
    });

    it("returns identity when display dimensions match natural", () => {
      const result = toTemplatePixels(
        { x: 10, y: 20, width: 30, height: 40 },
        { displayWidth: 100, displayHeight: 100, naturalWidth: 100, naturalHeight: 100 },
      );
      expect(result).toEqual({ x: 10, y: 20, width: 30, height: 40 });
    });
  });

  describe("fromTemplatePixels", () => {
    it("scales a template-pixel rectangle to display-pixel coords", () => {
      const result = fromTemplatePixels(
        { x: 100, y: 50, width: 200, height: 100 },
        { displayWidth: 600, displayHeight: 400, naturalWidth: 1200, naturalHeight: 800 },
      );
      expect(result).toEqual({ x: 50, y: 25, width: 100, height: 50 });
    });
  });

  describe("zoneFitsInsideTemplate", () => {
    it("returns true for a zone fully inside the template", () => {
      expect(
        zoneFitsInsideTemplate(
          { x: 100, y: 200, width: 800, height: 500 },
          { naturalWidth: 1200, naturalHeight: 800 },
        ),
      ).toBe(true);
    });

    it("returns false when zone extends past the right edge", () => {
      expect(
        zoneFitsInsideTemplate(
          { x: 900, y: 0, width: 400, height: 100 },
          { naturalWidth: 1200, naturalHeight: 800 },
        ),
      ).toBe(false);
    });

    it("returns false when width is zero", () => {
      expect(
        zoneFitsInsideTemplate(
          { x: 0, y: 0, width: 0, height: 100 },
          { naturalWidth: 1200, naturalHeight: 800 },
        ),
      ).toBe(false);
    });

    it("returns false when x is negative", () => {
      expect(
        zoneFitsInsideTemplate(
          { x: -1, y: 0, width: 100, height: 100 },
          { naturalWidth: 1200, naturalHeight: 800 },
        ),
      ).toBe(false);
    });
  });
});
