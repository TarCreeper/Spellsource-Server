#!/usr/bin/env bash

# Executes the fat jar of the network server using the Embedded application by default
java -javaagent:/data/quasar-core-0.7.9-jdk8.jar=mb -cp /data/net-1.3.0-all.jar com.hiddenswitch.spellsource.applications.Remote >>/var/log/java.log 2>&1