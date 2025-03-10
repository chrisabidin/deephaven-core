Debezium - Kafka Demo
=====================

The docker compose file in this directory starts a compose with
images for mysql, Debezium, Redpanda (kafka implementation) and Deephaven,
plus an additional image to generate an initial mysql schema and
then generate updates to the tables over time for a simple e-commerce demo.

The demo follows closely the one defined for Materialize here:
https://github.com/MaterializeInc/ecommerce-demo/blob/main/README_RPM.md

The load generation script is in `loadgen/generate_load.py`.
At the beginning of the script the constant `purchaseGenEveryMS`
controls how frequently purchases are triggered.


How to run
==========

First, if you are building from sources, follow the instructions
in https://deephaven.io/core/docs/how-to-guides/launch-build,
and ensure you can launch a regular version of Deephaven
as per the instructions there.  Once that works, stop that
Deephaven instance, and continue below.

Start docker-compose with the compose file in this
directory:

```
cd debezium-demo
docker-compose up
```

Then start a Deephaven web console (will be in python mode
by default per the command above) by navigating to

```
http://localhost:10000/ide
```

and cut & paste to it from `demo.py`.  If Deephaven is running
on a different host from your browser, you can replace `localhost`
with the right hostname.

Suggesting that you cut&paste instead of automatically setting
the script to run is intentional, so that you can see tables
as they are created and populated and watch them update
before you execute the next command.


Attributions
============

Files in this directory are based on demo code by
Debezium, Redpanda, and Materialize

* Debezium https://github.com/debezium/debezium
* Redpanda https://github.com/vectorizedio/redpanda
* Materialize https://github.com/MaterializeInc/materialize
