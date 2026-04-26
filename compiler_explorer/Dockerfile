#!/usr/bin/env -S docker build . --tag stiemannkj1/compiler_explorer:0.1 --file

# --- Stage 1: Build Stage (Full Debian for compilation) ---
FROM debian:12 AS builder

SHELL ["/bin/bash", "-c"]

RUN apt-get update && \
    apt-get install -y \
        autoconf \
        build-essential \
        curl \
        file \
        git \
        libasound2-dev \
        libcapstone-dev \
        libcups2-dev \
        libfontconfig1-dev \
        libx11-dev \
        libxrandr-dev \
        libxrender-dev \
        libxext-dev \
        libxt-dev \
        libxtst-dev \
        python3 \
        zip \
    && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /build

RUN \
    echo 'Downloading and extracting JDK 24...' && \
    curl --location --output /tmp/jdk24.tar.gz 'https://github.com/adoptium/temurin24-binaries/releases/download/jdk-24.0.2%2B12/OpenJDK24U-jdk_x64_linux_hotspot_24.0.2_12.tar.gz' && \
    diff <(sha256sum /tmp/jdk24.tar.gz) <(echo 'aea1cc55e51cf651c85f2f00ad021603fe269c4bb6493fa97a321ad770c9b096  /tmp/jdk24.tar.gz') && \
    mkdir -p /build/jdk24 && \
    tar -xzf /tmp/jdk24.tar.gz -C /build/jdk24 --strip-components=1 && \
    rm /tmp/jdk24.tar.gz && \
    echo 'Done extracting JDK 24.'

RUN \
    echo 'Downloading and extracting JDK 25...' && \
    curl --location --output /tmp/jdk25.tar.gz 'https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.3%2B9/OpenJDK25U-jdk_x64_linux_hotspot_25.0.3_9.tar.gz' && \
    diff <(sha256sum /tmp/jdk25.tar.gz) <(echo '69264a7a211bf5029830d07bc3370f879769d62ebc5b5488e90c9343a2da0e1f  /tmp/jdk25.tar.gz') && \
    mkdir -p /build/jdk25 && \
    tar -xzf /tmp/jdk25.tar.gz -C /build/jdk25 --strip-components=1 && \
    rm /tmp/jdk25.tar.gz && \
    echo 'Done extracting JDK 25.'

RUN \
    echo 'Building hsdis for JDK 25...' && \
    git clone --depth 1 -b jdk-25+0 https://github.com/openjdk/jdk.git hsdis-jdk25 && \
    cd /build/hsdis-jdk25 && \
    chmod +x configure && \
    ./configure --with-hsdis=capstone --with-boot-jdk=/build/jdk24 && \
    make build-hsdis && \
    cp /build/hsdis-jdk25/build/*/support/hsdis/libhsdis.so /build/jdk25/lib/hsdis-amd64.so && \
    rm -rf /build/hsdis-jdk25 && \
    echo 'Done building hsdis for JDK 25...'

# TODO loudly break the build if this image is built on anything except amd64.

# --- Stage 2: Minimal Runtime (Debian Slim) ---
FROM debian:12-slim

SHELL ["/bin/bash", "-c"]

# Copy only the disassembler plugin from the builder
COPY --from=builder /build/jdk25/ /opt/jdk25

ENV JAVA_HOME=/opt/jdk25
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /compiler_explorer
