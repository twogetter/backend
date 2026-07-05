#!/bin/bash

set -e

create_user_and_db() {
  local db=$1
  local user=$2
  local pass=$3

  echo "Creating user '$user' and database '$db'..."

  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE USER ${user} WITH PASSWORD '${pass}';
    CREATE DATABASE ${db} OWNER ${user};
    GRANT ALL PRIVILEGES ON DATABASE ${db} TO ${user};
EOSQL

  echo "Done: $db / $user"
}

# docker-compose env_file에서 주입되면 그 값 사용
create_user_and_db "authdb"         "auth_user"         "${AUTH_DB_PASSWORD:-auth_pass}"
create_user_and_db "userdb"         "user_user"         "${USER_DB_PASSWORD:-user_pass}"
create_user_and_db "paymentdb"      "payment_user"      "${PAYMENT_DB_PASSWORD:-payment_pass}"
create_user_and_db "orderdb"        "order_user"        "${ORDER_DB_PASSWORD:-order_pass}"
create_user_and_db "notificationdb" "notification_user" "${NOTIFICATION_DB_PASSWORD:-notification_pass}"
create_user_and_db "chatdb"         "chat_user"         "${CHAT_DB_PASSWORD:-chat_pass}"

echo "All databases and users created successfully"