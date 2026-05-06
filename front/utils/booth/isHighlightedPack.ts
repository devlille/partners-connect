export const HIGHLIGHTED_PACK_NAMES = ["Pack Silver", "Pack Gold"] as const;

export type HighlightedPackName = (typeof HIGHLIGHTED_PACK_NAMES)[number];

export function isHighlightedPack(packName?: string | null): packName is HighlightedPackName {
  return packName === "Pack Silver" || packName === "Pack Gold";
}
