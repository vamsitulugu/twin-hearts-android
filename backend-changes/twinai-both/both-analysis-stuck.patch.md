# REQUIRES BACKEND CHANGE: Both mode "analyzing" never finishes (us-app `routes/ai-both.js`)

## Why (5 lines)
After both partners submit, the server starts the AI in the background. If anything throws before the
Groq call (or Groq answers with a model/format problem) the round stays "analyzing" forever: no result,
no "failed", so the phone just waits. Fix: always end in done or failed, and let Retry work on a stuck round.

## 1. Always finish: wrap generateBothResult
Rename the existing `async function generateBothResult(roundId, coupleId) {` to
`async function generateBothResultInner(roundId, coupleId) {` and add above it:
```js
async function generateBothResult(roundId, coupleId) {
  try {
    await generateBothResultInner(roundId, coupleId);
  } catch (e) {
    console.error('[both-mode] generation crashed:', e);
    await failRound(roundId, coupleId, e.message); // the phone shows Retry instead of waiting forever
  }
}
```

## 2. Declare `json` (it is used without `let`, so concurrent rounds can overwrite each other)
Inside generateBothResultInner, right before `const GROQ_MODEL = ...` add: `let json;`

## 3. Use a real model and clean the answer
Set the env var `GROQ_MODEL` on Render to a model your Groq account lists (for example `llama-3.3-70b-versatile`).
The built-in fallback `qwen/qwen3.8-27b` may not exist. After `const raw = ...` add:
```js
const cleaned = raw.replace(/<think>[\s\S]*?<\/think>/g, '').trim();
json = JSON.parse(cleaned);
json.__raw = cleaned;
```
and delete the two old lines `json = JSON.parse(raw);` / `json.__raw = raw;`.

## 4. Retry also works on a stuck round (older than 60 s)
In `router.post('/rounds/:roundId/retry'` replace the `.update(...).eq('id', roundId).eq('status', 'failed')` chain with:
```js
const staleBefore = new Date(Date.now() - 60000).toISOString();
const { data: locked } = await supabase
  .from('both_rounds').update({ status: 'analyzing', locked_at: new Date().toISOString() })
  .eq('id', roundId)
  .or(`status.eq.failed,and(status.eq.analyzing,locked_at.lt.${staleBefore})`)
  .select().maybeSingle();
```
Redeploy on Render. No database change.
