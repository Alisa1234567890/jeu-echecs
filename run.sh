t#!/bin/bash
cd "$(dirname "$0")"
java -cp "target/classes:target/dependency/*" org.Main
