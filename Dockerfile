FROM node:24-slim

USER root

RUN apt-get update && apt-get install -y \
    git \
    curl \
    zsh \
    bash && \
    rm -rf /var/lib/apt/lists/*

ARG UID=501
RUN useradd -m -u ${UID} dev && \
    mkdir -p /home/dev/claude && \
    chown -R dev:dev /home/dev

USER dev

ENV HOME=/home/dev
ENV CLAUDE_CONFIG_DIR=/home/dev/claude
ENV NPM_CONFIG_PREFIX=/home/dev/.npm-global
ENV PATH=/home/dev/.npm-global/bin:$PATH

RUN npm install -g @anthropic-ai/claude-code

WORKDIR /home/dev/project

ENTRYPOINT ["bash", "-c", "claude"]