FROM debian:12 AS builder-riscv-toolchain
ARG num_jobs=16

ENV RISCV=/opt/riscv-cross
ENV PATH=$RISCV/bin:$PATH

RUN apt update -y && apt upgrade -y
RUN apt install -y build-essential autoconf automake autotools-dev python3 python3-pip libmpc-dev libmpfr-dev libgmp-dev gawk bison flex texinfo gperf libtool patchutils bc zlib1g-dev libexpat-dev ninja-build git cmake libglib2.0-dev curl device-tree-compiler curl libmpc-dev texinfo

WORKDIR /root
RUN git clone https://github.com/riscv-collab/riscv-gnu-toolchain
WORKDIR riscv-gnu-toolchain
RUN git checkout 9a28c809a5d355874def06a414aa5f272fae564d
RUN git submodule update --init --depth=1 -- gcc
RUN git submodule update --init --depth=1 -- glibc
RUN git submodule update --init --depth=1 -- binutils
RUN ./configure --with-arch=rv32iam --with-abi=ilp32 --prefix=/opt/riscv-cross && make linux -j $num_jobs && make clean

# Spike

FROM debian:12 AS builder-spike-toolchain
ARG num_jobs=16
RUN apt update -y
RUN apt install -y build-essential autoconf automake autotools-dev python3 python3-pip libmpc-dev libmpfr-dev libgmp-dev gawk bison flex texinfo gperf libtool patchutils bc zlib1g-dev libexpat-dev ninja-build git cmake libglib2.0-dev curl device-tree-compiler
WORKDIR /root
RUN git clone https://github.com/riscv-software-src/riscv-isa-sim
WORKDIR riscv-isa-sim
RUN ./configure --prefix=/opt/spike && make -j$num_jobs && make install && make clean

# Rust

FROM rust:latest AS builder-fail-if
RUN cargo install --git https://github.com/kper/fail_if fail_if

# LLVM
FROM ghcr.io/openvadl/llvm17-base@sha256:26659363db3bd278f54fb6d81581038d270aaaee859d839188063545d50d81ae
RUN apt update -y && apt-get update --fix-missing
RUN apt-get install -y device-tree-compiler python3 python3-pip python3-pandas
COPY --from=builder-riscv-toolchain /opt/riscv-cross /opt/riscv-cross
COPY --from=builder-spike-toolchain /opt/spike /opt/spike
COPY --from=builder-fail-if /usr/local/cargo/bin/fail_if /usr/bin/fail_if
WORKDIR /work
COPY helper /helper
COPY lcb_wrapper.sh .
COPY spike.sh .
COPY llvm.sh .
COPY filecheck.sh .
