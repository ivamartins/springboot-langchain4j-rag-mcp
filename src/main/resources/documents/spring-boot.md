# Spring Boot

Spring Boot é um framework Java para construir aplicações standalone production-ready.
É uma camada sobre o Spring Framework que remove grande parte da configuração manual.

## Características

- **Auto-configuração** — detecta bibliotecas no classpath e configura beans razoáveis.
- **Starters** — dependências curadas (web, data, security, actuator, ...).
- **Servidor embarcado** — Tomcat, Jetty ou Undertow por padrão.
- **Configuração externa** — `application.yml` ou `application.properties` + profiles.

## Comandos úteis

```bash
mvn spring-boot:run        # Roda a app localmente
mvn clean package          # Gera o JAR executável
java -jar app.jar          # Roda o JAR gerado
```

## Estrutura típica

```
src/main/java/com/example/
  Application.java         # @SpringBootApplication
  config/                  # @Configuration classes
  controller/              # @RestController
  service/                 # @Service
  repository/              # @Repository
  model/                   # Entidades/DTOs

src/main/resources/
  application.yml          # Configuração
  static/                  # Arquivos estáticos (HTML, CSS, JS)
  templates/               # Templates (Thymeleaf, Freemarker)
```
