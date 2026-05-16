export interface FlyerZone {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface TemplateDisplay {
  displayWidth: number;
  displayHeight: number;
  naturalWidth: number;
  naturalHeight: number;
}

export interface TemplateNatural {
  naturalWidth: number;
  naturalHeight: number;
}

export function toTemplatePixels(zone: FlyerZone, display: TemplateDisplay): FlyerZone {
  const xRatio = display.naturalWidth / display.displayWidth;
  const yRatio = display.naturalHeight / display.displayHeight;
  return {
    x: Math.round(zone.x * xRatio),
    y: Math.round(zone.y * yRatio),
    width: Math.round(zone.width * xRatio),
    height: Math.round(zone.height * yRatio),
  };
}

export function fromTemplatePixels(zone: FlyerZone, display: TemplateDisplay): FlyerZone {
  const xRatio = display.displayWidth / display.naturalWidth;
  const yRatio = display.displayHeight / display.naturalHeight;
  return {
    x: Math.round(zone.x * xRatio),
    y: Math.round(zone.y * yRatio),
    width: Math.round(zone.width * xRatio),
    height: Math.round(zone.height * yRatio),
  };
}

export function zoneFitsInsideTemplate(zone: FlyerZone, template: TemplateNatural): boolean {
  if (zone.x < 0 || zone.y < 0) return false;
  if (zone.width <= 0 || zone.height <= 0) return false;
  if (zone.x + zone.width > template.naturalWidth) return false;
  if (zone.y + zone.height > template.naturalHeight) return false;
  return true;
}
