---
name: regenerate-front-api
description: Regenerate the front-end Orval-generated client (front/utils/api.ts) from the server's local bundled OpenAPI spec (server/application/src/main/resources/openapi/documentation.yaml) instead of the production URL. Use whenever the user wants to preview, sync, or update the front with API changes that have NOT yet been deployed to production — phrases like "update api.ts", "regenerate the front API", "run orval against the local spec", "sync the front client with my server changes", "preview the new endpoints in the front". Also use after editing openapi.yaml/schemas in this repo when the user wants the front to see the new shape before shipping.
---

# Regenerate the Front API Client from the Local OpenAPI Spec

You are helping an engineer iterate on API changes across both halves of `partners-connect`. The front-end consumes the API through an Orval-generated client (`front/utils/api.ts`). In normal use Orval reads the *production* spec from `https://app-be77cae5-4a57-4cca-b721-15f8ab1f76c4.cleverapps.io/swagger/documentation.yaml` — so freshly added server endpoints are invisible to the front until they ship.

This skill performs a temporary swap so the front can be regenerated against the **local** bundled spec, then restores the production input via git. The "swap → generate → restore" pattern keeps the committed `orval.config.ts` untouched in normal commits while still letting you iterate locally.

## Prerequisites — verify before swapping

Run these checks first. If any fails, stop and surface the problem to the user instead of plowing ahead.

1. **The local spec is up to date.** The Orval input file is the *bundled* `documentation.yaml`, not the source `openapi.yaml`. If the user has just edited `openapi.yaml` or any `*.schema.json` in `server/application/src/main/resources/`, the bundle is stale. Tell the user to run `npm run bundle` from `server/` first (or invoke the `openapi-schemas` skill which covers this). Do not auto-bundle — bundling failures should be surfaced in their own workflow.

2. **`front/orval.config.ts` has no uncommitted edits.** Run `git -C ../front status --porcelain orval.config.ts`. If the file is already modified, the restore step (`git checkout --`) would silently throw away the user's in-progress changes. Stop and ask the user how to proceed.

3. **The relative path resolves.** From `front/`, the local spec is at `../server/application/src/main/resources/openapi/documentation.yaml`. Confirm the file exists before swapping.

## The workflow

All paths assume the working directory is `server/` (where this skill lives). Use `cd ../front` once for the front-side commands, or pass `-C ../front` to git.

### 1. Swap the Orval input to the local spec

Edit `front/orval.config.ts` and replace the `input` value with the relative path to the bundled local spec:

```ts
// Before
input:
  "https://app-be77cae5-4a57-4cca-b721-15f8ab1f76c4.cleverapps.io/swagger/documentation.yaml",

// After
input:
  "../server/application/src/main/resources/openapi/documentation.yaml",
```

Use the `Edit` tool with a tight `old_string` so only the URL changes. Leave the rest of the config (output target, mutator) alone.

### 2. Run Orval

From the `front/` directory:

```bash
cd ../front && npx orval --config ./orval.config.ts
```

Orval rewrites `front/utils/api.ts` in place. If it fails (schema error, unresolved `$ref`, malformed YAML), do NOT proceed to the restore step yet — the failure usually points at a problem in the local spec the user will want to fix while the config is still pointed at the local file. Surface the Orval error to the user and let them decide.

### 3. Restore the production input via git

Once Orval succeeds and `front/utils/api.ts` has been updated, restore the committed config so the local checkout doesn't end up with a stray edit:

```bash
git -C ../front checkout -- orval.config.ts
```

Why git restore instead of re-editing? The committed URL is the source of truth — if it ever changes on `main`, re-editing back to a hard-coded string would drift. `git checkout --` always lands on whatever the repo currently considers production.

### 4. Verify

- `git -C ../front status orval.config.ts` should show no changes.
- `git -C ../front status utils/api.ts` should show the regenerated file as modified (or report no diff if the server spec hadn't drifted from production).
- Briefly mention to the user which endpoints/types appear new in the diff if it's small enough to summarize. If the diff is huge, just point them at `git -C ../front diff utils/api.ts`.

## Error recovery

If something goes wrong mid-flight and the user wants to abort, the safe reset is:

```bash
git -C ../front checkout -- orval.config.ts utils/api.ts
```

This brings both files back to their committed state. Only suggest this once the user explicitly asks to abandon — don't volunteer it, because it discards the regeneration work.

## What this skill does NOT do

- It does not run `npm run bundle` — that's part of the OpenAPI authoring workflow (see the `openapi-schemas` skill). Bundling and front-regeneration are separate concerns; conflating them hides bundling failures.
- It does not commit anything. The regenerated `api.ts` is left as an unstaged modification for the user to review and commit on their own terms.
- It does not start the front dev server. If the user wants to see the new client in a running app, mention `pnpm dev` from `front/` as a follow-up, but don't launch it automatically.

## Reference: relevant file locations

| File | Path (from repo root) | Role |
|------|-----------------------|------|
| Orval config | `front/orval.config.ts` | Holds the `input` URL that this skill temporarily swaps |
| Generated client | `front/utils/api.ts` | The output Orval rewrites |
| Local bundled spec | `server/application/src/main/resources/openapi/documentation.yaml` | The temporary input target |
| Local spec source | `server/application/src/main/resources/openapi/openapi.yaml` | Edited by hand; bundled into `documentation.yaml` via `npm run bundle` |
