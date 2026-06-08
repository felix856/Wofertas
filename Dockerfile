# Estágio 1: Build da aplicação (compilação)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copia os arquivos de configuração do Maven para baixar as dependências
COPY pom.xml .

# Baixa as dependências offline (cache de build)
RUN mvn dependency:go-offline -B

# Copia o código fonte e os arquivos de front-end (view)
COPY src src
COPY view view

# Compila e empacota o projeto em um arquivo .jar ignorando os testes (os testes já passaram localmente)
RUN mvn clean package -DskipTests

# Estágio 2: Cria a imagem final para rodar a aplicação
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copia o arquivo .jar gerado no Estágio 1
COPY --from=build /app/target/*.jar app.jar

# IMPORTANTÍSSIMO: Copia a pasta view para o contêiner final para que o WebConfig possa servi-la
COPY --from=build /app/view ./view

# Cria a pasta de uploads para evitar erros de diretório não encontrado ao salvar imagens
RUN mkdir uploads && chmod 777 uploads

# Expõe a porta que o Render vai usar
EXPOSE 8080

# Executa o projeto ativando o perfil 'prod'
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
