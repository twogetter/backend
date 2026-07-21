// init-local-data.js

db = db.getSiblingDB('productdb');

function generateProductName(artistName, groupName) {
  const SUFFIX = " 구독권";
  const displayName = (groupName && groupName.trim().length > 0)
      ? groupName + " " + artistName
      : artistName;
  return "[" + displayName + "]" + SUFFIX;
}

const now = new Date();
const createdDate = new Date("2026-07-21T00:00:00Z");

const sampleProducts = [
  {
    artistId: NumberLong(1001),
    artistName: "카리나",
    groupName: "aespa",
    price: NumberLong(4900)
  },
  {
    artistId: NumberLong(1002),
    artistName: "태연",
    groupName: "소녀시대",
    price: NumberLong(4900)
  },
  {
    artistId: NumberLong(1003),
    artistName: "아이유",
    groupName: null,
    price: NumberLong(5900)
  },
  {
    artistId: NumberLong(1004),
    artistName: "원영",
    groupName: "IVE",
    price: NumberLong(5900)
  },
  {
    artistId: NumberLong(1005),
    artistName: "유진",
    groupName: "IVE",
    price: NumberLong(5900)
  },
  {
    artistId: NumberLong(1006),
    artistName: "미나미",
    groupName: "RESCENE",
    price: NumberLong(4900)
  },
  {
    artistId: NumberLong(1007),
    artistName: "원이",
    groupName: "RESCENE",
    price: NumberLong(4900)
  },
  {
    artistId: NumberLong(1008),
    artistName: "제나",
    groupName: "RESCENE",
    price: NumberLong(4900)
  },
];

print("🔄 샘플 상품 데이터 적재 시작...");

let inserted = 0;
let skipped = 0;

sampleProducts.forEach(function (data) {
  const exists = db.products.findOne({artistId: data.artistId, deleted: false});

  if (exists) {
    print(
        `ℹ️  스킵 (이미 존재) - artistId: ${data.artistId}, artistName: ${data.artistName}`);
    skipped++;
    return;
  }

  const product = {
    pid: data.artistId,
    artistId: data.artistId,
    artistName: data.artistName,
    groupName: data.groupName,
    name: generateProductName(data.artistName, data.groupName),
    description: `${data.artistName}의 프리미엄 구독 상품입니다.`,
    imageUrl: `https://example.com/images/${data.artistId}.jpg`,
    status: "ACTIVE",
    price: data.price,
    openDate: now,
    deleted: false,
    statusChangedAt: now,
    createdAt: createdDate,
    updatedAt: now
  };

  db.products.insertOne(product);
  print(`✅ 적재 완료 - name: ${product.name}`);
  inserted++;
});

print(`\n📊 결과: ${inserted}건 적재, ${skipped}건 스킵`);