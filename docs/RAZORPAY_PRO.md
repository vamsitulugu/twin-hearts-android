# Pro with Razorpay (Android)

- Plans: Monthly ₹99 (30 days), Yearly ₹499 (365 days), Lifetime ₹999. One-time payments.
- Prices and the Razorpay secret live on the server (us-app `routes/pro.js`). The app only gets the public key id per order.
- Flow: Pro page -> `POST api/pro/create-order` -> Razorpay window (native SDK) -> `MainActivity` listener -> `PaymentBridge` -> `POST api/pro/verify` -> Pro on.
- Locked features call `proGate.require("name") { ... }`: Twin AI BOTH tab, Globe "Pin a Memory", Music add songs, Movie cloud upload. A free couple lands on the Pro page.
- The server also answers 402 for these features when the couple is not Pro.
- Set up the server first (us-app `RAZORPAY_SETUP.md`).
