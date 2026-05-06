import { describe, it, expect } from "vitest";
import { isHighlightedPack, HIGHLIGHTED_PACK_NAMES } from "~/utils/booth/isHighlightedPack";

describe("isHighlightedPack", () => {
  it("returns true for the exact name 'Pack Silver'", () => {
    expect(isHighlightedPack("Pack Silver")).toBe(true);
  });

  it("returns true for the exact name 'Pack Gold'", () => {
    expect(isHighlightedPack("Pack Gold")).toBe(true);
  });

  it("returns false for any other pack name", () => {
    expect(isHighlightedPack("Pack Bronze")).toBe(false);
    expect(isHighlightedPack("Pack Platinum")).toBe(false);
  });

  it("is case-sensitive (lower-case variant rejected)", () => {
    expect(isHighlightedPack("pack silver")).toBe(false);
    expect(isHighlightedPack("pack gold")).toBe(false);
  });

  it("requires the 'Pack ' prefix (bare 'Silver'/'Gold' rejected)", () => {
    expect(isHighlightedPack("Silver")).toBe(false);
    expect(isHighlightedPack("Gold")).toBe(false);
  });

  it("rejects words in inverted order", () => {
    expect(isHighlightedPack("Silver Pack")).toBe(false);
    expect(isHighlightedPack("Gold Pack")).toBe(false);
  });

  it("rejects null, undefined, and empty string", () => {
    expect(isHighlightedPack(null)).toBe(false);
    expect(isHighlightedPack(undefined)).toBe(false);
    expect(isHighlightedPack("")).toBe(false);
  });

  it("exposes the highlighted pack names tuple", () => {
    expect(HIGHLIGHTED_PACK_NAMES).toEqual(["Pack Silver", "Pack Gold"]);
  });
});
