FROM clojure:lein AS build
ENV BIN_NAME="bibcal"

# The current versions to build against:
ENV MUSL_VERSION="1.2.2"
ENV ZLIB_VERSION="1.2.11"
ENV GRAALVM_VERSION="21.2.0"

# Set the shell options:
SHELL ["/bin/bash", "-o", "pipefail", "-c"]

# The folders where to put gu, musl and zlib:
ENV RESOURCE_DIR="/usr/local/static-build-resources"
RUN mkdir -p ${RESOURCE_DIR}/bin
ENV GRAALVM_HOME="/opt/graalvm-ce-java11-${GRAALVM_VERSION}"
ENV PATH="${PATH}:${RESOURCE_DIR}/bin:${GRAALVM_HOME}/bin"

# Install build dependencies:
RUN apt-get update && apt-get install -yy make gcc libstdc++-10-dev

# Download and compile musl:
WORKDIR /
RUN curl -sL https://musl.libc.org/releases/musl-${MUSL_VERSION}.tar.gz \
| tar -C /opt -xzvf -

WORKDIR /opt/musl-${MUSL_VERSION}
RUN ./configure --disable-shared --prefix=${RESOURCE_DIR} && make && make install

# Now set musl-gcc to be the CC:
ENV CC=musl-gcc

# Download and compile zlib:
RUN curl -sL https://sourceforge.net/projects/libpng/files/zlib/${ZLIB_VERSION}/zlib-${ZLIB_VERSION}.tar.gz \
| tar -C /opt -xzvf -

WORKDIR /opt/zlib-${ZLIB_VERSION}
RUN ./configure --static --prefix=${RESOURCE_DIR} && make && make install

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
--initialize-at-build-time \
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
