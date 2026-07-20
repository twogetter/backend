const appPassword = process.env.MONGO_APP_PASSWORD;

db = db.getSiblingDB('productdb');

try {
  db.createUser({
    user: 'product_user',
    pwd: appPassword,
    roles: [{role: 'readWrite', db: 'productdb'}]
  });
  print('✅ MongoDB product_user 생성 완료');
} catch (e) {
  if (e.codeName === 'Location51003' || e.message.includes('already exists')) {
    print('ℹ️ 유저가 이미 존재 - 스킵');
  } else {
    print(`❌ 유저 생성 실패: ${e.message}`);
    quit(1);
  }
}

try {
  print('🔄 productdb 인덱스 생성 시작');

  db.products.createIndex({artistId: 1},
      {unique: true, name: "idx_artist_unique"});

  db.product_outbox_events.createIndex({idempotencyKey: 1},
      {unique: true, name: "idx_idempotencyKey_unique"});
  db.product_outbox_events.createIndex({status: 1, nextAttemptAt: 1},
      {name: "idx_status_nextAttemptAt"});

  db.product_change_reservations.createIndex(
      {productId: 1, commandType: 1},
      {
        name: "idx_productId_commandType_pending",
        unique: true,
        partialFilterExpression: {status: "PENDING"}
      }
  );

  print('✅ MongoDB 수동 인덱스 생성 성공');
} catch (e) {
  print(`❌ 인덱스 생성 과정에서 오류 발생: ${e.message}`);
  quit(1);
}