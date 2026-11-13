const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_EMAIL = "pabloalexander361@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

function validMedPayload() {
  return {
    codigo: "75010000010",
    nombre: "Panadol Extra",
    principio: "Paracetamol 500mg",
    categoria: "Analgésicos",
    caducidad: "2028-01-01",
    precioCompra: 8.5,
    precioVenta: 14.0,
    stock: 120,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

// --- SECURITY BOUNDS (Dirty Dozen) ---

test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("medicamentos").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("ventas").get());
});

test("User profile: non-admin cannot self-assign admin role", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      nombre: "Alice",
      email: "alice@example.com",
      role: "admin",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("User profile: standard user can create empleado profile, bootstrap admin can create admin profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      nombre: "Alice",
      email: "alice@example.com",
      role: "empleado",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Email spoofing test: unverified admin email must be rejected
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(
    spoofDb.collection("users").doc("spoof_uid").set({
      userId: "spoof_uid",
      nombre: "Spoofer",
      email: ADMIN_EMAIL,
      role: "admin",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Verified bootstrap admin succeeds
  const adminDb = testEnv.authenticatedContext("admin_uid", {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    adminDb.collection("users").doc("admin_uid").set({
      userId: "admin_uid",
      nombre: "Pablo Admin",
      email: ADMIN_EMAIL,
      role: "admin",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("Shadow update & privilege escalation: cannot inject ghost fields or escalate role", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      nombre: "Alice",
      email: "alice@example.com",
      role: "empleado",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("medicamentos").doc("MED-001").set({
      ...validMedPayload(),
      isVerified: true,
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's pharmacy collections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("medicamentos").doc("MED-001").set(validMedPayload())
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("medicamentos").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("medicamentos").doc("MED-001").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("medicamentos").doc("MED-002").set(validMedPayload())
  );
});

test("Resource poisoning: rejects oversized IDs and invalid field types", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const longId = "a".repeat(200);
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("medicamentos").doc(longId).set(validMedPayload())
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("medicamentos").doc("MED-002").set({
      ...validMedPayload(),
      stock: -5,
    })
  );
});

test("Compras terminal state locking & relational check, plus immutable ventas", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Compra referencing non-existent med fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("compras").doc("CMP-001").set({
      codigo: "CMP-001",
      fecha: Date.now(),
      proveedorId: "PRV-001",
      medId: "MED-MISSING",
      cantidad: 50,
      costo: 35.0,
      estado: "Pendiente",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Create med first, then create Pendiente compra
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("medicamentos").doc("MED-001").set(validMedPayload())
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("compras").doc("CMP-001").set({
      codigo: "CMP-001",
      fecha: Date.now(),
      proveedorId: "PRV-001",
      medId: "MED-001",
      cantidad: 50,
      costo: 8.5,
      estado: "Pendiente",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Transition Pendiente -> Completado succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("compras").doc("CMP-001").update({
      estado: "Completado",
      updatedAt: serverTimestamp(),
    })
  );

  // Subsequent update after Completado fails for standard user
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("compras").doc("CMP-001").update({
      estado: "Pendiente",
      updatedAt: serverTimestamp(),
    })
  );

  // Ventas create succeeds, update/delete fail
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ventas").doc("VTA-001").set({
      codigo: "VTA-1001",
      fecha: Date.now(),
      subtotal: 28.0,
      descuento: 0.0,
      descPct: 0.0,
      iva: 3.36,
      total: 31.36,
      vendedorNombre: "Alice",
      vendedorId: ALICE_UID,
      puntos: 0,
      items: [
        { medId: "MED-001", nombre: "Panadol Extra", cantidad: 2, precio: 14.0 },
      ],
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("ventas").doc("VTA-001").update({
      total: 0.0,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("ventas").doc("VTA-001").delete()
  );
});
