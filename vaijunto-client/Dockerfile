# Dockerfile do CLIENTE
FROM maven:3.9-eclipse-temurin-21

# libs gráficas que o JavaFX precisa em runtime (Alpine não teria isso disponível)
RUN apt-get update && apt-get install -y \
    libgtk-3-0 libx11-6 libxext6 libxrender1 libxtst6 libxi6 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /vaijunto-client
COPY pom.xml .
COPY src ./src

RUN mvn -B dependency:go-offline

CMD ["mvn", "-B", "javafx:run"]