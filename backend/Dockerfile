FROM maven:3.9.9-eclipse-temurin-17 AS build
ARG CREHN_MAVEN_OPTS="-Xmx1536m -XX:MaxMetaspaceSize=512m"
ENV MAVEN_OPTS=${CREHN_MAVEN_OPTS}
WORKDIR /workspace
COPY . .
RUN mvn -B -ntp -pl ruoyi-admin -am verify

FROM eclipse-temurin:17-jre-jammy
ARG CREHN_VERSION=local
ARG CREHN_SOURCE_REVISION=UNVERSIONED
LABEL org.opencontainers.image.title="CREHN Backend" \
      org.opencontainers.image.version="${CREHN_VERSION}" \
      org.opencontainers.image.revision="${CREHN_SOURCE_REVISION}"
ENV HOME=/app \
    SAL_USE_VCLPLUGIN=gen
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        curl \
        ffmpeg \
        fontconfig \
        fonts-noto-cjk \
        libreoffice-calc \
        libreoffice-core \
        libreoffice-impress \
        libreoffice-writer \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system crehn \
    && useradd --system --gid crehn --home /app crehn
COPY --from=build /workspace/ruoyi-admin/target/ruoyi-admin.jar /app/crehn.jar
RUN mkdir -p /app/logs /app/tmp /app/data/workbench-assets /app/data/portal-pdf \
    && chown -R crehn:crehn /app
USER crehn
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-Djava.io.tmpdir=/app/tmp","-jar","/app/crehn.jar"]
