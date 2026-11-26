const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_hamada_01";
const BOB_UID = "bob_kolwezi_02";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
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

test("Unauthenticated users cannot read or write synced KASA NOVA collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("customers").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("products").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("sales").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("payments").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("expenses").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("cash_drops").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("chat_messages").get());
});

test("Owner can sync V5 customers, products, sales, payments, expenses, cash_drops, and chat_messages; cross-user access denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 4000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").set({
      userId: ALICE_UID,
      localId: 1,
      name: "Dépôt Mwamba",
      phone: "+243992120772",
      address: "Manika, Kolwezi",
      gpsLat: -10.7235,
      gpsLng: 25.4920,
      source: "TikTok",
      loyaltyPoints: 25,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1").set({
      userId: ALICE_UID,
      localId: 1,
      name: "Mixeur Professionnel 2L",
      category: "Cuisine",
      costUsd: 24.0,
      costCdf: 68000,
      priceUsd: 38.0,
      priceCdf: 108000,
      stockQuantity: 14,
      videoUrl: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject disallowed category
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_bad").set({
      userId: ALICE_UID,
      localId: 2,
      name: "Invalid Category Item",
      category: "Other",
      costUsd: 10,
      costCdf: 28000,
      priceUsd: 15,
      priceCdf: 42000,
      stockQuantity: 5,
      videoUrl: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sales").doc("sale_1").set({
      userId: ALICE_UID,
      localId: 1,
      customerId: 1,
      productId: 1,
      customerName: "Dépôt Mwamba",
      productName: "Ciment Gris 50kg",
      customerSource: "TikTok",
      type: "INSTALLMENT",
      quantity: 10,
      totalPrice: 120.0,
      profitAmount: 35.0,
      currency: "USD",
      paidAmount: 40.0,
      remainingAmount: 80.0,
      dueDate: 1800000000000,
      signaturePath: "/data/user/0/com.aistudio.kasanova/files/signatures/sig_1.png",
      isGiveawayEntry: true,
      dayKey: "2026-11-26",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sales").doc("sale_1").update({
      paidAmount: 80.0,
      remainingAmount: 40.0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("payments").doc("pay_1").set({
      userId: ALICE_UID,
      localId: 1,
      saleId: 1,
      customerName: "Dépôt Mwamba",
      amount: 38.0,
      discountApplied: 2.0,
      currency: "USD",
      dayKey: "2026-11-26",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1").set({
      userId: ALICE_UID,
      localId: 1,
      title: "Carburant Kolwezi",
      amount: 10.0,
      currency: "USD",
      dayKey: "2026-11-26",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cash Vault Drop: Agent creates PENDING, Owner confirms COMPLETED
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cash_drops").doc("drop_1").set({
      userId: ALICE_UID,
      amount: 150.0,
      currency: "USD",
      otp: "4821",
      status: "PENDING",
      agentName: "Agent Kolwezi",
      note: "Dépôt fin de tournée",
      createdAt: now,
      updatedAt: now,
    })
  );

  const confirmTime = new Date(Date.now() - 1000);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cash_drops").doc("drop_1").update({
      status: "COMPLETED",
      confirmedAt: confirmTime,
      updatedAt: confirmTime,
    })
  );

  // Cannot re-open completed drop
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("cash_drops").doc("drop_1").update({
      status: "PENDING",
      confirmedAt: confirmTime,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Real-time chat messages
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chat_messages").doc("msg_1").set({
      userId: ALICE_UID,
      senderRole: "AGENT",
      senderName: "Agent Kolwezi",
      text: "Client Musompo prêt pour encaissement.",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject cross-user reads/writes
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cash_drops").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("chat_messages").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("sales").doc("sale_1").update({
      remainingAmount: 0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject invalid source or invalid OTP
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("cash_drops").doc("drop_bad").set({
      userId: ALICE_UID,
      amount: 50.0,
      currency: "USD",
      otp: "12AB",
      status: "PENDING",
      agentName: "Agent",
      note: "",
      createdAt: now,
      updatedAt: now,
    })
  );
});
