FROM eclipse-temurin:25

USER root

RUN apt-get update && apt-get install -y \
    git \
    curl \
    unzip \
    zsh \
    vim \
    openssh-client \
    wget \
    groovy

RUN curl -fsSL https://deb.nodesource.com/setup_24.x | bash -

RUN apt-get install -y nodejs

# Gradle
ARG GRADLE_VERSION=9.1.0

RUN wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -P /tmp \
    && unzip -d /opt/gradle /tmp/gradle-${GRADLE_VERSION}-bin.zip \
    && ln -s /opt/gradle/gradle-${GRADLE_VERSION}/bin/gradle /usr/bin/gradle \
    && rm /tmp/gradle-${GRADLE_VERSION}-bin.zip

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