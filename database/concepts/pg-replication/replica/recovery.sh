#!/bin/bash

# Wait for master
until pg_isready -h master -p 5432; do
    echo "Waiting for master..."
    sleep 2
done

echo "Cloning data from master..."
rm -rf /var/lib/postgresql/data/*
PGPASSWORD=postgres pg_basebackup -h master -D /var/lib/postgresql/data -U postgres -Fp -Xs -P -R -W
EOF

echo "Starting replica..."
exec docker-entrypoint.sh postgres
