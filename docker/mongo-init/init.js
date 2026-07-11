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