# Backend change: movie reactions (optional)
Why: the web page sends reactions over Supabase Realtime. The app must not call Supabase (R-60), so it needs two tiny routes.
What: paste `reactions.js.txt` into `routes/routes-movie.js`, redeploy Render. No SQL.
Without it: the app still works; your own emoji float on your phone, the partner does not see them (the app stops asking after one 404).
Note: the web page does not read these routes, so reactions are app-to-app only unless the web page is changed later.
