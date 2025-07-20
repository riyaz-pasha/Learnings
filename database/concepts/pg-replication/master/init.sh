#!/bin/bash
set -e

# Copy custom pg_hba.conf after init
if [ -f /custom/pg_hba.conf ]; then
  cp /custom/pg_hba.conf /var/lib/postgresql/data/pg_hba.conf
  chown postgres:postgres /var/lib/postgresql/data/pg_hba.conf
fi

exec postgres -c config_file=/etc/postgresql/postgresql.conf
