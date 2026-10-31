// routes/notifications.js  (add to the Express server "us-app")
//
// Does the real push sending for the Android app. The app only (1) uploads its phone token and
// (2) asks for a push to the partner. All pushes are FCM "data" messages: the app draws the
// notification itself, so it looks the same with the app open, in the background or closed.
//
//   app.use('/api/notifications', require('./routes/notifications')(supabase));
//
// Needs:  npm i firebase-admin   and env FIREBASE_SERVICE_ACCOUNT (the JSON text of the service account).
const express = require('express');
const admin = require('firebase-admin');

if (!admin.apps.length) {
  admin.initializeApp({ credential: admin.credential.cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)) });
}

// Must match NotificationType.key in the app. Unknown types are shown as a plain "system" notification.
const TYPES = new Set([
  'chat_message', 'chat_reaction', 'call_incoming', 'call_missed',
  'partner_hug', 'partner_miss', 'partner_touch', 'surprise_new', 'surprise_unlocked', 'anniversary',
  'milestone', 'memory_added', 'memory_throwback', 'bucket_added', 'bucket_done',
  'location_arrived', 'location_left', 'location_offline', 'location_sharing', 'location_sos', 'meet_planner',
  'calendar_reminder', 'calendar_event', 'study_reminder', 'routine_reminder', 'period_reminder', 'money_update',
  'game_invite', 'movie_invite', 'music_shared', 'fight_logged', 'fight_resolved', 'vault_activity',
  'partner_request', 'partner_accepted', 'partner_removed', 'level_up', 'pro_update', 'system',
]);

// FCM data values must all be strings.
const strings = (obj) => Object.fromEntries(Object.entries(obj || {}).map(([k, v]) => [k, String(v)]));

/** Push to every phone of the OTHER person in the couple. Reused by the other routes (see README). */
async function pushToPartner(supabase, { coupleId, fromRole, type, title, body, data }) {
  const partnerRole = fromRole === 'user1' ? 'user2' : 'user1';
  const { data: rows, error } = await supabase
    .from('fcm_tokens').select('token').eq('couple_id', coupleId).eq('role', partnerRole);
  if (error || !rows || rows.length === 0) return { sent: 0 };

  const tokens = rows.map((r) => r.token);
  const message = {
    tokens,
    data: strings({ ...data, type: TYPES.has(type) ? type : 'system', title: title || '', body: body || '' }),
    android: { priority: 'high', ttl: type === 'call_incoming' ? 45 * 1000 : 24 * 3600 * 1000 },
  };
  const res = await admin.messaging().sendEachForMulticast(message);

  // Forget phones that no longer exist.
  const dead = [];
  res.responses.forEach((r, i) => {
    const code = r.error && r.error.code;
    if (code === 'messaging/registration-token-not-registered' || code === 'messaging/invalid-registration-token') dead.push(tokens[i]);
  });
  if (dead.length) await supabase.from('fcm_tokens').delete().in('token', dead);
  return { sent: res.successCount };
}

module.exports = (supabase) => {
  const router = express.Router();

  // POST /api/notifications/token   { userId, coupleId, role, token, platform }
  router.post('/token', async (req, res) => {
    const { userId, coupleId, role, token, platform } = req.body || {};
    if (!userId || !coupleId || !token) return res.status(400).json({ error: 'missing fields' });
    // One phone token belongs to one person: a new sign-in on the same phone takes it over.
    await supabase.from('fcm_tokens').delete().eq('token', token);
    const { error } = await supabase.from('fcm_tokens').insert({ user_id: userId, couple_id: coupleId, role, token, platform: platform || 'android' });
    if (error) return res.status(500).json({ error: error.message });
    res.json({ ok: true });
  });

  // POST /api/notifications/token/remove   { userId, token }   (log out)
  router.post('/token/remove', async (req, res) => {
    const { userId, token } = req.body || {};
    if (!userId || !token) return res.status(400).json({ error: 'missing fields' });
    await supabase.from('fcm_tokens').delete().eq('user_id', userId).eq('token', token);
    res.json({ ok: true });
  });

  // POST /api/notifications/send   { coupleId, fromRole, type, title?, body?, data? }  -> push to the partner
  router.post('/send', async (req, res) => {
    const { coupleId, fromRole, type, title, body, data } = req.body || {};
    if (!coupleId || !fromRole || !type) return res.status(400).json({ error: 'missing fields' });
    try {
      res.json({ ok: true, ...(await pushToPartner(supabase, { coupleId, fromRole, type, title, body, data })) });
    } catch (e) {
      res.status(500).json({ error: String(e.message || e) });
    }
  });

  return router;
};

module.exports.pushToPartner = pushToPartner;
