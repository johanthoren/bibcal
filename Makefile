BIN_NAME=bibcal
export DOCKER_BUILDKIT=1

all: target/${BIN_NAME}
.PHONY: target/${BIN_NAME}

target/${BIN_NAME}:
	@docker build --target bin --output target/ .
