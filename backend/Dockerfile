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
# O nome do jar inclui a versão do pom (entrevista-ia-<versão>.jar). Renomear
# aqui evita que todo bump de versão quebre o COPY da etapa seguinte. O glob
# não pega o .jar.original que o spring-boot-maven-plugin deixa ao lado.
RUN mvn -B clean package -DskipTests && cp target/entrevista-ia-*.jar target/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
