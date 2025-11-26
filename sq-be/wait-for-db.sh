#!/bin/sh
# Wait for MySQL to be ready
until mysqladmin ping -h"$DB_HOST" -u"$DB_USER" -p"$DB_PASSWORD" --silent; do
  echo "Waiting for MySQL..."
  sleep 2
done
echo "MySQL is ready!"

