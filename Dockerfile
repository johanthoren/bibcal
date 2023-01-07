FROM clojure:lein AS build
ENV BIN_NAME="bibcal"

# The current versions to build against:
ENV MUSL_VERSION="1.2.3"
ENV ZLIB_VERSION="1.2.13"
ENV GRAALVM_VERSION="22.3.0"

# Set the shell options:
SHELL ["/bin/bash", "-o", "pipefail", "-c"]

# The folders where to put gu, musl and zlib:
ENV RESOURCE_DIR="/usr/local/static-build-resources"
RUN mkdir -p ${RESOURCE_DIR}/bin
ENV GRAALVM_HOME="/opt/graalvm-ce-java11-${GRAALVM_VERSION}"
ENV PATH="${PATH}:${RESOURCE_DIR}/bin:${GRAALVM_HOME}/bin"

# Install build dependencies:
RUN apt-get update && apt-get install -yy curl make gcc libstdc++-10-dev

# Download and compile the musl toolchain:
WORKDIR /
RUN curl -sL http://more.musl.cc/10/x86_64-linux-musl/x86_64-linux-musl-native.tgz \
| tar -C /opt -xzvf -

# Export the TOOLCHAIN_DIR variable:
ENV TOOLCHAIN_DIR="/opt/x86_64-linux-musl-native"

# Make sure TOOLCHAIN_DIR/bin is on the PATH:
ENV PATH=$TOOLCHAIN_DIR/bin:$PATH

# Now set musl-gcc to be the CC:
ENV CC=$TOOLCHAIN_DIR/bin/gcc

# Download and compile zlib:
RUN curl -sL https://zlib.net/zlib-${ZLIB_VERSION}.tar.gz \
| tar -C /opt -xzvf -

WORKDIR /opt/zlib-${ZLIB_VERSION}
RUN ./configure --static --prefix=${TOOLCHAIN_DIR} && make && make install

# Download and install graal-vm with native-image:
WORKDIR /
RUN curl -sL https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-${GRAALVM_VERSION}/graalvm-ce-java11-linux-amd64-${GRAALVM_VERSION}.tar.gz \
| tar -C /opt -xzvf -
RUN gu install native-image

# Transfer files and build uberjars:
COPY project.clj .
COPY resources resources
COPY src src
COPY test test
RUN lein deps
RUN lein make-uberjars

# Create the native-image based on the uberjar:
RUN native-image \
--static \
--libc=musl \
--no-fallback \
-H:+AllowIncompleteClasspath \
--report-unsupported-elements-at-runtime \
-H:ReflectionConfigurationFiles=/resources/META-INF/native-image/reflect-config.json \
-H:+ReportExceptionStackTraces \
-jar /target/uberjar/${BIN_NAME}-*-standalone.jar \
-H:Name=/target/${BIN_NAME}

# Finish by copying over the compiled native-image:
FROM scratch AS bin
ENV BIN_NAME="bibcal"
ENV BIN_TARGET_NAME="bibcal"
COPY --from=build /target/${BIN_NAME} /${BIN_TARGET_NAME}
