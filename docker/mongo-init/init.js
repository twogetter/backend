
db = db.getSiblingDB('productdb');

db.createUser({
  user: 'product_user',
  pwd: 'product_pass',
  roles: [
    { role: 'readWrite', db: 'productdb' }
  ]
});

print('MongoDB product_user 생성 완료');