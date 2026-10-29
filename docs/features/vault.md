# Feature: Secret Vault

Native Compose page; the web app (`#page-vault`) and `docs/reference/images.pdf` page 42 were the reference. Entry: Home "Vault" tile → `Routes.Vault`. Code: `feature/vault/`. The bottom bar stays visible.

## What it has
- **Lock screen**: 🔐, "Secret Vault", 4 dots, 3×4 key pad (⌫, 0, gold ✓), "Forgot PIN?". First time: "Set your Vault PIN", then "Confirm your PIN". Weak PINs (1111, 1234, 8765) are refused with the web's rules.
- **Forgot PIN**: sign-in password + new PIN twice.
- **Open**: "🔐 Vault · Private & secure" card with + Photo, + Video, 🔒 Lock; 3-column grid; tap for a full-screen swipe viewer (photos, video player); trash on each tile with confirm. Petals on unlock.
- The page blocks screenshots / recents preview (`FLAG_SECURE`). Leaving the page locks it again (the state is dropped with the ViewModel).

## Server calls
`GET api/auth/vault-status/:coupleId`, `POST api/auth/verify-pin`, `set-vault-pin`, `reset-vault-pin`; `POST api/media/upload` (multipart, `type=vault`, streamed from the phone, no storage permission: system photo picker); `DELETE api/media/delete` (`bucket=vault-media`); list in the shared state key `vault` (`{id,url,path,type,date}`, same as the web).

## Decisions
- The server judges every PIN (no offline unlock, nothing stored on the phone). Wrong PIN = 401, "no PIN yet" = 409 → setup, "already set" = 403 → enter.
- Delete removes the file first, then the list entry (as the web). Items are shown in saved order.
- Videos show a play tile (no thumbnail decoding library).

## Not done / known gaps
- Not compiled here (no Android SDK). `VaultLogic` is unit tested and was run with kotlinc.
- Change-PIN lives in Settings (not built yet).
- Signed photo links can expire; Sync reloads the list.
- No pinch-zoom in the viewer.
