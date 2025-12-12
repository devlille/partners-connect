import { z } from "zod";

/**
 * Schema de validation pour les traductions d'options
 */
export const optionTranslationSchema = z.object({
  name: z.string().min(1, "errors.validation"),
  description: z.string().optional(),
});

/**
 * Schema de validation pour une option de sponsoring
 */
export const sponsoringOptionSchema = z.object({
  translations: z
    .record(z.string(), optionTranslationSchema)
    .refine((translations) => Object.keys(translations).length > 0, {
      message: "errors.validation",
    }),
  price: z.number().min(0, "errors.validation").optional(),
  is_free: z.boolean().optional(),
});

/**
 * Schema de validation pour un pack de sponsoring
 */
export const sponsoringPackSchema = z.object({
  name: z.string().min(1, "errors.validation"),
  base_price: z.number().min(0, "errors.validation"),
  max_quantity: z.number().int().positive("errors.validation").optional(),
  nb_tickets: z.number().int().min(0, "errors.validation").optional(),
  with_booth: z.boolean().optional(),
});

/**
 * Schema de validation pour l'association d'options à un pack
 */
export const packOptionsSchema = z
  .object({
    requiredOptions: z.array(z.string()),
    optionalOptions: z.array(z.string()),
  })
  .refine(
    (data) => {
      // Vérifier qu'aucune option n'est à la fois requise et optionnelle
      const requiredSet = new Set(data.requiredOptions);
      const optionalSet = new Set(data.optionalOptions);
      const intersection = [...requiredSet].filter((id) => optionalSet.has(id));
      return intersection.length === 0;
    },
    { message: "Une option ne peut pas être à la fois requise et optionnelle" },
  );

/**
 * Schema de validation pour la création d'un sponsor
 */
export const sponsorSchema = z.object({
  company_name: z.string().min(1, "errors.validation"),
  contact_name: z.string().min(1, "errors.validation"),
  contact_role: z.string().optional(),
  email: z.string().email("errors.validation"),
  phone: z.string().optional(),
  pack_id: z.string().min(1, "errors.validation"),
  option_ids: z.array(z.string()).optional(),
});

/**
 * Types TypeScript générés à partir des schemas Zod
 */
export type OptionTranslation = z.infer<typeof optionTranslationSchema>;
export type SponsoringOptionInput = z.infer<typeof sponsoringOptionSchema>;
export type SponsoringPackInput = z.infer<typeof sponsoringPackSchema>;
export type PackOptionsInput = z.infer<typeof packOptionsSchema>;
export type SponsorInput = z.infer<typeof sponsorSchema>;
