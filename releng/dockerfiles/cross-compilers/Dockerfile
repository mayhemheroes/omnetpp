FROM ubuntu:22.04 as builder

# Contains the (cross-)compilers and dependencies needed to build
# OMNeT++ core (without IDE or docs) for all 3 supported platforms.

# based on osxcross docker image from https://github.com/andrew-d/docker-osxcross

# This image is used (at the moment) as a foundation for: The OMNeT++/OMNEST release
# tarball building images, and for the image used on GitHub Actions to test INET.


# Install build tools for osxcross
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get upgrade -yy && \
    DEBIAN_FRONTEND=noninteractive apt-get install -yy \
        automake bison cmake curl file flex git libtool pkg-config python3 texinfo vim wget && \
    DEBIAN_FRONTEND=noninteractive apt-get clean

# Install osxcross
ENV OSXCROSS_SDK_VERSION 13.3
RUN mkdir /opt/osxcross && \
    cd /opt && \
    git clone https://github.com/tpoechtrager/osxcross.git && \
    cd osxcross && \
    git checkout e6ab3fa74 && \
    ./tools/get_dependencies.sh && \
    curl -L -o ./tarballs/MacOSX${OSXCROSS_SDK_VERSION}.sdk.tar.xz \
        https://github.com/alexey-lysiuk/macos-sdk/releases/download/${OSXCROSS_SDK_VERSION}/MacOSX${OSXCROSS_SDK_VERSION}.tar.xz && \
    yes | UNATTENDED=1 TARGET_DIR=/opt/clang-macos ./build.sh

# second stage build (copy only the final resuts from the previous build process)
FROM ubuntu:22.04
RUN mkdir -p /opt/clang-macos
WORKDIR /opt/clang-macos
COPY --from=builder /opt/clang-macos/ .
ENV PATH $PATH:/opt/clang-macos/bin

# add OMNeT++ specific dependencies and linux/aarch64 cross compiler
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get upgrade -yy && \
    DEBIAN_FRONTEND=noninteractive apt-get install --no-install-recommends -yy \
        build-essential cmake crossbuild-essential-arm64 perl tofrodos libxml2-dev zlib1g-dev libxml2-utils \
        ca-certificates curl tar xz-utils bzip2 gzip clang llvm && \
    apt-get clean && \
    apt-get autoclean

# add clang-mingw windows cross compiler
RUN mkdir /opt/clang-mingw && \
   curl -LsS "https://github.com/mstorsjo/llvm-mingw/releases/download/20260421/llvm-mingw-20260421-ucrt-ubuntu-22.04-x86_64.tar.xz" | tar -xJ --strip-components=1 -C /opt/clang-mingw
ENV PATH $PATH:/opt/clang-mingw/bin
