# Backend changes for Watch Together (Android v4)
Apply in this order. The app works without them; only the listed extras are missing until you do.

1. **SQL** (Supabase SQL editor): run `add-scene-column.sql`. Adds the favourite-scene column.
2. **`routes/movie-favorites.js`**: replace with `movie-favorites.js` here. Adds `scene` and lets Edit Movie change title, year, genre, poster, review, scene and owner.
3. **`routes/routes-movie.js`**: replace with `routes-movie.js` here (copy of your current file plus the two reaction routes; replaces the older `reactions.js.txt` paste).
4. **`public/movie.js`** (web, optional): apply `web-movie.js.diff` so web reactions also reach the Android partner.
5. Redeploy Render (and Vercel for step 4).

Without 1-2: Edit / scene are not saved. Without 3: no reactions between phones. Without 4: the web page's reactions do not reach the app.
Poster upload uses the existing `/api/media/upload` route: nothing to change.
