FROM node:24-slim

USER root

# Dependências essenciais
RUN apt-get update && apt-get install -y \
    git \
    curl \
    zsh \
    bash

ENV CLAUDE_CONFIG_DIR=/home/dev/claude
RUN npm install -g @anthropic-ai/claude-code

ARG UID=501
RUN useradd -m -u ${UID} dev

RUN mkdir -p /home/dev/claude &&  \
    chown -R dev:dev /home/dev

USER dev

ENV HOME=/home/dev

WORKDIR /home/dev/project

ENTRYPOINT ["bash", "-c", "claude"]
