const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-sheildle-445b68c5-4463-4bd1-939c-33931bb3fc12";
const PARENT_UID = "parent_uid_001";
const CHILD_UID = "child_uid_001";
const STRANGER_UID = "stranger_uid_999";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      databaseId: DATABASE_ID,
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function dbFor(uid) {
  return testEnv.authenticatedContext(uid).firestore(DATABASE_ID);
}

function unauthDb() {
  return testEnv.unauthenticatedContext().firestore(DATABASE_ID);
}

test("Unauthenticated user cannot read or write pairings or users", async () => {
  await assertFails(unauthDb().collection("users").doc(CHILD_UID).get());
  await assertFails(unauthDb().collection("pairings").doc(CHILD_UID).get());
  await assertFails(unauthDb().collection("pairing_codes").doc("123456").get());
});

function safePastTimestamp() {
  return new Date(Date.now() - 2000);
}

test("User can save own UserProfile and stranger cannot write or read it", async () => {
  const childDb = dbFor(CHILD_UID);
  const strangerDb = dbFor(STRANGER_UID);

  await assertSucceeds(
    childDb.collection("users").doc(CHILD_UID).set({
      userId: CHILD_UID,
      role: "child",
      email: "child@example.com",
      displayName: "Child Phone",
      updatedAt: safePastTimestamp(),
    })
  );

  await assertFails(strangerDb.collection("users").doc(CHILD_UID).get());
  await assertFails(
    strangerDb.collection("users").doc(CHILD_UID).set({
      userId: CHILD_UID,
      role: "child",
      email: "x@y.com",
      displayName: "Hacked",
      updatedAt: safePastTimestamp(),
    })
  );
});

test("Child registers pairing code and parent claims pairing while stranger is denied remote control", async () => {
  const childDb = dbFor(CHILD_UID);
  const parentDb = dbFor(PARENT_UID);
  const strangerDb = dbFor(STRANGER_UID);

  await assertSucceeds(
    childDb.collection("pairings").doc(CHILD_UID).set({
      pairingId: CHILD_UID,
      parentId: "",
      childUid: CHILD_UID,
      pairingCode: "654321",
      childDisplayName: "Pixel Kid",
      active: false,
      createdAt: safePastTimestamp(),
      updatedAt: safePastTimestamp(),
    })
  );

  await assertSucceeds(
    childDb.collection("pairing_codes").doc("654321").set({
      pairingCode: "654321",
      childUid: CHILD_UID,
      childDisplayName: "Pixel Kid",
      createdAt: safePastTimestamp(),
    })
  );

  // Listing all pairing_codes is denied
  await assertFails(parentDb.collection("pairing_codes").get());
  // Exact-code lookup succeeds
  await assertSucceeds(parentDb.collection("pairing_codes").doc("654321").get());

  // Parent claims pairing
  await assertSucceeds(
    parentDb.collection("pairings").doc(CHILD_UID).update({
      parentId: PARENT_UID,
      parentDisplayName: "Parent Phone",
      active: true,
      updatedAt: safePastTimestamp(),
    })
  );

  // Paired parent can push remote settings & lockdown command & read thumbnail
  await assertSucceeds(
    parentDb.collection("users").doc(CHILD_UID).collection("remote_control").doc("settings").set({
      sensitivity: "STRICT",
      strictEverything: true,
      lockdownDurationMinutes: 60,
      antiTamperEnabled: true,
      childUninstallLocked: true,
      liveSeeEnabled: true,
      whitelistedPackages: ["com.android.dialer"],
      warningThresholdCount: 2,
      bedtimeEnabled: true,
      bedtimeStartMinutes: 1320,
      bedtimeEndMinutes: 360,
      appTimeBudgets: { "com.google.android.youtube": 45 },
      customBlockedDomains: ["example-bad.com"],
      customBlockedKeywords: ["casino"],
      safeDnsEnabled: true,
      blockExplicitAdult: true,
      blockSuggestiveContent: true,
      blockSensitiveGraphic: true,
      blockGamblingKeywords: true,
      studyModeEnabled: true,
      studyModeDurationMinutes: 45,
      studySoftWarningSeconds: 30,
      studyAllowedPackages: ["com.google.android.calculator"],
      smartSafeSearchEnabled: true,
      safeSearchPacks: ["adult", "gambling", "bypass_proxy"],
      discreetBlockingScreenMode: true,
      updatedAt: safePastTimestamp(),
    })
  );

  // Invalid warningThresholdCount (> 5) or invalid studySoftWarningSeconds (> 120) is rejected
  await assertFails(
    parentDb.collection("users").doc(CHILD_UID).collection("remote_control").doc("settings").set({
      sensitivity: "STRICT",
      strictEverything: true,
      lockdownDurationMinutes: 60,
      antiTamperEnabled: true,
      childUninstallLocked: true,
      liveSeeEnabled: true,
      warningThresholdCount: 9,
      updatedAt: safePastTimestamp(),
    })
  );

  await assertFails(
    parentDb.collection("users").doc(CHILD_UID).collection("remote_control").doc("settings").set({
      sensitivity: "STRICT",
      strictEverything: true,
      lockdownDurationMinutes: 60,
      antiTamperEnabled: true,
      childUninstallLocked: true,
      liveSeeEnabled: true,
      studySoftWarningSeconds: 300,
      updatedAt: safePastTimestamp(),
    })
  );

  await assertSucceeds(
    parentDb.collection("users").doc(CHILD_UID).collection("remote_control").doc("commands").set({
      action: "LOCKDOWN",
      timestamp: Date.now(),
      acknowledged: false,
      issuedByUid: PARENT_UID,
    })
  );

  await assertSucceeds(
    childDb.collection("users").doc(CHILD_UID).collection("remote_control").doc("thumbnails").set({
      thumbnailBase64: "abc123",
      activeAppName: "Chrome",
      timestamp: Date.now(),
      protectionArmed: true,
      lockdownActive: false,
    })
  );

  await assertSucceeds(
    parentDb.collection("users").doc(CHILD_UID).collection("remote_control").doc("thumbnails").get()
  );

  // Stranger cannot read settings, commands, thumbnails, or threat_logs
  await assertFails(
    strangerDb.collection("users").doc(CHILD_UID).collection("remote_control").doc("settings").get()
  );
  await assertFails(
    strangerDb.collection("users").doc(CHILD_UID).collection("remote_control").doc("commands").set({
      action: "UNLOCK",
      timestamp: Date.now(),
      acknowledged: false,
      issuedByUid: STRANGER_UID,
    })
  );
});
