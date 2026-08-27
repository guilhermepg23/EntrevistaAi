# Build em duas etapas: a primeira tem o Maven + JDK completo (pesado, só
# usado pra compilar); a segunda copia apenas o .jar final pra uma imagem
# com JRE (bem menor), que é a que efetivamente roda em produção/local.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia só o pom.xml primeiro e baixa as dependências antes do código-fonte:
# assim, se só o código mudar (sem mexer no pom), o Docker reaproveita essa
# camada em cache e não rebaixa tudo de novo a cada build.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/entrevista-ia-0.1.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
