#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

./build_all.py -v --arch=riscv32 --chip generic --board vadl-clang --clean "$@"
