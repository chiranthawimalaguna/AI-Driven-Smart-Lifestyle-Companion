/**
 * OPTIONAL, UNTESTED add-on. This file has never been deployed or run - I can't
 * verify it end-to-end from this environment (no access to deploy Cloud Functions
 * or send real email). Treat this as a solid starting point, not a guarantee.
 *
 * What it does: once a day, checks every signed-up user's overdue routine tasks in
 * Firestore, and if they have any, emails them a short summary via Gmail SMTP.
 *
 * Requirements before this can work:
 *   1. Your Firebase project must be on the Blaze (pay-as-you-go) plan - Cloud
 *      Functions that make outbound network calls (like sending email) require it.
 *      Blaze still has a generous free tier for low usage.
 *   2. A Gmail account to send from, with an "App Password" generated (Google
 *      Account -> Security -> 2-Step Verification -> App passwords) - do NOT use
 *      your normal Gmail password here.
 *   3. Firebase CLI installed locally (`npm install -g firebase-tools`).
 *
 * Deployment (from the AI-Driven Smart Lifestyle Companion project root, NOT from inside functions/):
 *   firebase login
 *   firebase init functions        (select this existing functions/ folder if asked)
 *   firebase functions:config:set gmail.user="youraddress@gmail.com" gmail.pass="your16charapppassword"
 *   firebase deploy --only functions
 *
 * Note: `functions:config:set` is Firebase's older (v1) configuration approach.
 * Depending on which Firebase CLI version you have installed, you may instead need
 * to use a `.env` file in this functions/ folder (newer CLI versions prompt you
 * which to use, or see https://firebase.google.com/docs/functions/config-env).
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

admin.initializeApp();

const gmailUser = functions.config().gmail?.user;
const gmailPass = functions.config().gmail?.pass;

const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: { user: gmailUser, pass: gmailPass },
});

// Runs once a day at 08:00 server time (UTC by default - adjust the schedule
// string, e.g. "0 8 * * *" with a .timeZone() call, if you want a specific zone).
exports.dailyEmailSummary = functions.pubsub
  .schedule("0 8 * * *")
  .onRun(async () => {
    const db = admin.firestore();
    const now = Date.now();

    const listUsersResult = await admin.auth().listUsers(1000);

    for (const userRecord of listUsersResult.users) {
      const uid = userRecord.uid;
      const email = userRecord.email;
      if (!email) continue;

      const tasksSnapshot = await db
        .collection("users")
        .doc(uid)
        .collection("routine_tasks")
        .where("isCompleted", "==", false)
        .get();

      const overdueTasks = tasksSnapshot.docs.filter(
        (doc) => (doc.data().scheduledTimeMillis || 0) < now
      );

      if (overdueTasks.length === 0) continue;

      const taskListHtml = overdueTasks
        .map((doc) => `<li>${escapeHtml(doc.data().title || "Untitled task")}</li>`)
        .join("");

      try {
        await transporter.sendMail({
          from: `"AI-Driven Smart Lifestyle Companion" <${gmailUser}>`,
          to: email,
          subject: `AI-Driven Smart Lifestyle Companion: ${overdueTasks.length} overdue task(s)`,
          html: `
            <p>Hi,</p>
            <p>You have ${overdueTasks.length} overdue task(s) in AI-Driven Smart Lifestyle Companion:</p>
            <ul>${taskListHtml}</ul>
            <p>Open the app to update or complete them.</p>
          `,
        });
      } catch (err) {
        // Log and continue - one failed email shouldn't stop the rest of the batch.
        console.error(`Failed to email ${email}:`, err);
      }
    }

    return null;
  });

function escapeHtml(text) {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}
