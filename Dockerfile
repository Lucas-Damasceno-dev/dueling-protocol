# Estágio 1: Build - Usa uma imagem com Maven e JDK para compilar o projeto
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
# Copia os arquivos de configuração do Maven e baixa as dependências
COPY pom.xml .
RUN mvn dependency:go-offline
# Copia o código fonte e compila, gerando o .jar
COPY src ./src
RUN mvn package -DskipTests

# Estágio 2: Run - Usa uma imagem leve, apenas com o Java Runtime, para rodar a aplicação
FROM openjdk:17-jre-slim
WORKDIR /app
# Copia o .jar gerado no estágio de build para a imagem final
COPY --from=build /app/target/dueling-protocol-1.0-SNAPSHOT.jar app.jar

# Expõe as portas que o servidor vai usar (TCP para o jogo, UDP para o ping)
EXPOSE 7777/tcp
EXPOSE 7778/udp

# Comando para iniciar o servidor quando o contêiner for executado
CMD ["java", "-jar", "app.jar"]