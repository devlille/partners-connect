#!/usr/bin/env tsx
/**
 * Script pour vérifier les traductions i18n
 *
 * Fonctionnalités:
 * 1. Détecte les clés manquantes entre les fichiers de traduction
 * 2. Trouve les textes hardcodés dans le code
 * 3. Vérifie la cohérence des clés
 *
 * Usage:
 *   npx tsx scripts/check-i18n.ts
 */

import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.resolve(__dirname, "..");

// Couleurs pour la console
const colors = {
  reset: "\x1b[0m",
  red: "\x1b[31m",
  green: "\x1b[32m",
  yellow: "\x1b[33m",
  blue: "\x1b[34m",
  magenta: "\x1b[35m",
  cyan: "\x1b[36m",
};

interface Translation {
  [key: string]: string | Translation;
}

interface FlatTranslation {
  [key: string]: string;
}

/**
 * Aplatit un objet de traductions imbriqué
 */
function flattenTranslations(obj: Translation, prefix = ""): FlatTranslation {
  const result: FlatTranslation = {};

  for (const [key, value] of Object.entries(obj)) {
    const newKey = prefix ? `${prefix}.${key}` : key;

    if (typeof value === "object" && value !== null) {
      Object.assign(result, flattenTranslations(value as Translation, newKey));
    } else {
      result[newKey] = value as string;
    }
  }

  return result;
}

/**
 * Charge un fichier de traduction
 */
function loadTranslationFile(locale: string): FlatTranslation {
  const filePath = path.join(rootDir, "locales", `${locale}.json`);

  if (!fs.existsSync(filePath)) {
    console.error(`${colors.red}✗${colors.reset} Fichier de traduction non trouvé: ${filePath}`);
    return {};
  }

  const content = fs.readFileSync(filePath, "utf-8");
  const translations = JSON.parse(content);

  return flattenTranslations(translations);
}

/**
 * Compare deux ensembles de traductions
 */
function compareTranslations(
  locale1: string,
  translations1: FlatTranslation,
  locale2: string,
  translations2: FlatTranslation,
): { missing: string[]; extra: string[] } {
  const keys1 = new Set(Object.keys(translations1));
  const keys2 = new Set(Object.keys(translations2));

  const missing = Array.from(keys1).filter((key) => !keys2.has(key));
  const extra = Array.from(keys2).filter((key) => !keys1.has(key));

  return { missing, extra };
}

/**
 * Recherche les textes hardcodés dans les fichiers Vue/TS
 */
function findHardcodedTexts(dirPath: string, extensions = [".vue", ".ts"]): Map<string, string[]> {
  const hardcodedTexts = new Map<string, string[]>();

  // Pattern pour détecter les textes français hardcodés (mots avec accents, etc.)
  const frenchTextPattern = /(["'`])([A-ZÀ-Ý][a-zà-ÿ\s,'()-]{3,})\1/g;

  // Exceptions: mots-clés, noms de fichiers, etc.
  const exceptions = [
    "localhost",
    "undefined",
    "function",
    "return",
    "import",
    "export",
    "const",
    "let",
    "var",
    "class",
    "interface",
    "type",
    "async",
    "await",
    "true",
    "false",
    "null",
    "this",
    "super",
    "new",
    "delete",
    "typeof",
    "instanceof",
    "in",
    "of",
    "extends",
    "implements",
    "public",
    "private",
    "protected",
    "static",
    "readonly",
    "get",
    "set",
  ];

  function scanFile(filePath: string) {
    const ext = path.extname(filePath);
    if (!extensions.includes(ext)) return;

    // Ignorer certains dossiers
    if (
      filePath.includes("node_modules") ||
      filePath.includes(".nuxt") ||
      filePath.includes("dist") ||
      filePath.includes("utils/api.ts")
    ) {
      // Fichier généré
      return;
    }

    const content = fs.readFileSync(filePath, "utf-8");
    const matches = content.matchAll(frenchTextPattern);
    const texts: string[] = [];

    for (const match of matches) {
      const text = match[2];

      // Ignorer les exceptions
      if (exceptions.includes(text.toLowerCase())) continue;

      // Ignorer les très courtes chaînes
      if (text.length < 5) continue;

      // Ignorer les chaînes qui ressemblent à du code
      if (/^[a-z_][a-zA-Z0-9_]*$/.test(text)) continue;

      texts.push(text);
    }

    if (texts.length > 0) {
      hardcodedTexts.set(path.relative(rootDir, filePath), texts);
    }
  }

  function scanDirectory(dir: string) {
    if (!fs.existsSync(dir)) return;

    const entries = fs.readdirSync(dir, { withFileTypes: true });

    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);

      if (entry.isDirectory()) {
        scanDirectory(fullPath);
      } else {
        scanFile(fullPath);
      }
    }
  }

  scanDirectory(dirPath);
  return hardcodedTexts;
}

/**
 * Génère un rapport de traductions manquantes
 */
function generateReport() {
  console.log(`${colors.cyan}=== Vérification des traductions i18n ===${colors.reset}\n`);

  // Charger les traductions
  const locales = ["fr-FR", "en-US", "es-ES"];
  const translations: Record<string, FlatTranslation> = {};

  for (const locale of locales) {
    translations[locale] = loadTranslationFile(locale);
    console.log(
      `${colors.green}✓${colors.reset} Chargé ${locale}: ${Object.keys(translations[locale]).length} clés`,
    );
  }

  console.log();

  // Comparer les traductions
  let hasIssues = false;

  // Comparer FR (référence) avec les autres langues
  const frKeys = translations["fr-FR"];

  for (const locale of locales) {
    if (locale === "fr-FR") continue;

    const { missing, extra } = compareTranslations("fr-FR", frKeys, locale, translations[locale]);

    if (missing.length > 0) {
      hasIssues = true;
      console.log(`${colors.yellow}⚠${colors.reset}  Traductions manquantes dans ${locale}:`);
      missing.slice(0, 10).forEach((key) => {
        console.log(`   ${colors.red}✗${colors.reset} ${key}`);
      });
      if (missing.length > 10) {
        console.log(`   ... et ${missing.length - 10} autres`);
      }
      console.log();
    }

    if (extra.length > 0) {
      hasIssues = true;
      console.log(`${colors.yellow}⚠${colors.reset}  Traductions en trop dans ${locale}:`);
      extra.slice(0, 5).forEach((key) => {
        console.log(`   ${colors.magenta}+${colors.reset} ${key}`);
      });
      if (extra.length > 5) {
        console.log(`   ... et ${extra.length - 5} autres`);
      }
      console.log();
    }

    if (missing.length === 0 && extra.length === 0) {
      console.log(`${colors.green}✓${colors.reset} ${locale} est synchronisé avec fr-FR`);
      console.log();
    }
  }

  // Rechercher les textes hardcodés
  console.log(`${colors.cyan}=== Recherche de textes hardcodés ===${colors.reset}\n`);

  const directories = [
    path.join(rootDir, "pages"),
    path.join(rootDir, "components"),
    path.join(rootDir, "composables"),
  ];

  let totalHardcoded = 0;

  for (const dir of directories) {
    const hardcoded = findHardcodedTexts(dir);

    if (hardcoded.size > 0) {
      hasIssues = true;
      console.log(
        `${colors.yellow}⚠${colors.reset}  Textes hardcodés trouvés dans ${path.basename(dir)}/:`,
      );

      let count = 0;
      for (const [file, texts] of hardcoded.entries()) {
        if (count >= 5) {
          console.log(`   ... et ${hardcoded.size - 5} fichiers supplémentaires`);
          break;
        }
        console.log(`   ${colors.blue}${file}${colors.reset}:`);
        texts.slice(0, 3).forEach((text) => {
          console.log(`     "${text}"`);
        });
        if (texts.length > 3) {
          console.log(`     ... et ${texts.length - 3} autres`);
        }
        totalHardcoded += texts.length;
        count++;
      }
      console.log();
    }
  }

  if (totalHardcoded > 0) {
    console.log(
      `${colors.red}✗${colors.reset} Total: ${totalHardcoded} textes hardcodés détectés\n`,
    );
  } else {
    console.log(`${colors.green}✓${colors.reset} Aucun texte hardcodé détecté\n`);
  }

  // Résumé final
  console.log(`${colors.cyan}=== Résumé ===${colors.reset}`);
  if (!hasIssues) {
    console.log(`${colors.green}✓ Tout est OK!${colors.reset}`);
  } else {
    console.log(`${colors.yellow}⚠ Des problèmes ont été détectés${colors.reset}`);
    console.log(`\nActions recommandées:`);
    console.log(`1. Ajouter les traductions manquantes`);
    console.log(`2. Supprimer les traductions inutilisées`);
    console.log(`3. Extraire les textes hardcodés vers i18n`);
  }

  process.exit(hasIssues ? 1 : 0);
}

// Exécution
generateReport();
