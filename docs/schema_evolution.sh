#!/bin/bash

cd /pulsar/bin

# Tenant and namespace
./pulsar-admin tenants create rtk
./pulsar-admin namespaces create rtk/example
./pulsar-admin namespaces set-is-allow-auto-update-schema --disable rtk/example
./pulsar-admin namespaces set-schema-validation-enforce --enable rtk/example
./pulsar-admin namespaces set-schema-compatibility-strategy --compatibility FULL_TRANSITIVE rtk/example

# Topics
./pulsar-admin topics delete-partitioned-topic persistent://rtk/example/input
./pulsar-admin topics create-partitioned-topic --partitions 1 persistent://rtk/example/input
# infinite retention - keep forever even if acknowledged
./pulsar-admin topics set-retention -s -1 -t -1 persistent://rtk/example/input
./pulsar-admin topics create-subscription persistent://rtk/example/input -s input-function

# Topics
./pulsar-admin topics delete-partitioned-topic persistent://rtk/example/output
./pulsar-admin topics create-partitioned-topic --partitions 1 persistent://rtk/example/output
# infinite retention - keep forever even if acknowledged
./pulsar-admin topics set-retention -s -1 -t -1 persistent://rtk/example/output

# Schemas
./pulsar-admin schemas delete persistent://rtk/example/input
./pulsar-admin schemas delete persistent://rtk/example/output
./pulsar-admin schemas upload \
    --filename /pulsar/schemas/internal/User1.json \
    persistent://rtk/example/input
./pulsar-admin schemas upload \
    --filename /pulsar/schemas/internal/User1.json \
    persistent://rtk/example/output

# Schemas to Update
./pulsar-admin schemas upload \
    --filename /pulsar/schemas/internal/User2.json \
    persistent://rtk/example/input
./pulsar-admin schemas upload \
    --filename /pulsar/schemas/internal/User2.json \
    persistent://rtk/example/output
