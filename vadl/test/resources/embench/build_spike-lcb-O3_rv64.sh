#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

./build_all.py -v --arch riscv64 --chip generic --board spike-lcb-O3 --clean "$@"
