# Desafio Hyperativa

API REST para cadastro e consulta de números de cartão, escrita em **Java 24 + Spring Boot 3.5**, autenticada via **JWT**, com **HTTPS/TLS** no transporte e armazenamento seguro via **SHA-256 + pepper** — o número original do cartão nunca é gravado.

---

## Stack

| Camada            | Escolha                                                          |
|-------------------|------------------------------------------------------------------|
| Linguagem         | Java 24                                                          |
| Framework         | Spring Boot 3.5 (Web, Data JPA, Security, Validation, Actuator)  |
| Banco             | PostgreSQL 16 (H2 nos testes)                                    |
| Migrações         | Flyway                                                           |
| Autenticação      | JWT (jjwt 0.12)                                                  |
| Hash de cartão    | SHA-256 + pepper                                                 |
| Criptografia TLS  | HTTPS/TLS com certificado RSA 2048-bit (autoassinado em dev)     |
| Documentação      | springdoc-openapi (Swagger UI)                                   |
| Build / Container | Maven + Docker multi-stage + Compose                             |
| Testes            | JUnit 5 + Mockito + AssertJ                                      |

---

## Como rodar (Docker — recomendado)

Requer Docker Desktop.

```bash
# na raiz do projeto
docker compose up --build
```

A primeira execução baixa as imagens, gera o certificado TLS e compila a API. As próximas execuções sobem em segundos.

- **API (HTTPS)**: `https://localhost:8443`
- **Swagger UI**: `https://localhost:8443/swagger-ui.html`
- **OpenAPI JSON**: `https://localhost:8443/v3/api-docs`
- **Healthcheck**: `https://localhost:8443/actuator/health`
- Requisições HTTP em `http://localhost:8080` são **redirecionadas automaticamente** para HTTPS.

> O certificado é autoassinado (gerado no build). Browsers e ferramentas exibirão aviso de segurança — use `-k` no curl ou aceite a exceção no browser. Em produção, substitua pelo certificado da sua CA (Let's Encrypt, etc.).

Para parar e limpar o volume do banco:

```bash
docker compose down -v
```

### Variáveis de ambiente

| Variável                  | Default (dev)                                      | Observação                                  |
|---------------------------|----------------------------------------------------|---------------------------------------------|
| `DB_URL`                  | `jdbc:postgresql://postgres:5432/hyperativa`       |                                             |
| `DB_USER` / `DB_PASSWORD` | `hyperativa` / `hyperativa`                        |                                             |
| `JWT_SECRET`              | secret de dev (≥ 32 bytes)                         | **Apenas para ambiente de desenvolvimento** |
| `JWT_EXPIRATION_MINUTES`  | `60`                                               |                                             |
| `CARD_HASH_PEPPER`        | `dev-only-pepper-replace-in-production`            | **Apenas para ambiente de desenvolvimentoo**                      |
| `SERVER_PORT`             | `8443`                                             |                                             |
| `SSL_ENABLED`             | `true`                                             |                                             |
| `SSL_KEYSTORE_PASSWORD`   | `hyperativa123`                                    | **Apenas para ambiente de desenvolvimentoo**                      |

---

## Como rodar localmente (sem Docker)

Pré-requisitos: JDK 24, Maven 3.9+, Postgres 16 acessível.

```bash
# subir só o Postgres via compose
docker compose up -d postgres

# gerar o keystore (necessário uma única vez)
keytool -genkeypair -alias hyperativa -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore src/main/resources/keystore.p12 \
  -validity 3650 -dname "CN=localhost,OU=API,O=Hyperativa,L=SP,ST=SP,C=BR" \
  -storepass hyperativa123 -noprompt

# build + run
mvn -B clean spring-boot:run
```

Para desabilitar SSL localmente (sem gerar o keystore):

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.ssl.enabled=false --server.port=8080"
```

Para testes (usam H2, não precisam de keystore):

```bash
mvn test
```

---

## Criptografia no tráfego (TLS/HTTPS)

Todo o tráfego é protegido por **TLS 1.2/1.3** com certificado RSA 2048-bit:

- O Dockerfile gera o keystore PKCS12 com `keytool` durante o build — o certificado fica embutido no JAR.
- Tomcat serve HTTPS na porta **8443** e adiciona um conector HTTP na porta **8080** que redireciona (302) para HTTPS automaticamente.
- HTTP/2 está habilitado sobre TLS.
- Em produção: monte o keystore via volume Docker e configure `SSL_KEYSTORE_PASSWORD` via secret manager. Nunca commite `keystore.p12` (já está no `.gitignore`).

---

## Autenticação

Há um usuário **seed** criado pela migration `V2`:

| usuário | senha      | role  |
|---------|------------|-------|
| `admin` | `admin123` | ADMIN |

> A senha é armazenada como hash BCrypt. **Substitua em qualquer ambiente real.**

### Login

```bash
curl -k -X POST https://localhost:8443/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Resposta:

```json
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

Use o token em todos os demais endpoints:

```bash
curl -k https://localhost:8443/api/v1/cards/4456897999999999 \
  -H "Authorization: Bearer <accessToken>"
```

---

## Endpoints

### `POST /api/v1/auth/login` — público
Autentica e devolve o JWT.

### `POST /api/v1/cards` — autenticado
Cadastra um cartão único. Operação **idempotente**: se o cartão já existe, retorna o mesmo `id`.

```bash
curl -k -X POST https://localhost:8443/api/v1/cards \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"cardNumber":"4456897999999999"}'
```

Resposta (201):
```json
{ "id": "4f3...uuid", "createdAt": "2026-05-08T12:34:56Z" }
```

### `POST /api/v1/cards/batch` — autenticado
Cadastra cartões a partir de arquivo TXT (multipart, campo `file`).

```bash
curl -k -X POST https://localhost:8443/api/v1/cards/batch \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@./exemplo.txt"
```

Resposta (201):
```json
{
  "batchId": "uuid",
  "batchNumber": "LOTE0001",
  "totalRecords": 10,
  "inserted": 10,
  "duplicates": 0
}
```

Validações:
- `header.batchNumber` precisa bater com `trailer.batchNumber`
- `header.recordCount` precisa bater com `trailer.recordCount` e com a quantidade real de linhas de cartão
- Cada linha de cartão começa com `C` na coluna 1
- Lote já registrado é rejeitado com **409 Conflict**
- Cartões duplicados (mesmo dentro do arquivo ou já existentes no banco) não são reinseridos

### `GET /api/v1/cards/{cardNumber}` — autenticado
Consulta um cartão. Retorna o `id` se existir, **404** caso contrário.

```bash
curl -k https://localhost:8443/api/v1/cards/4456897999999999 \
  -H "Authorization: Bearer $TOKEN"
```

---

## Formato do arquivo TXT

| Linha   | Layout |
|---------|--------|
| Header  | `[1-29] NOME` `[30-37] DATA(yyyyMMdd)` `[38-45] LOTE` `[46-51] QTD(6 dig)` |
| Cartão  | `[1] 'C'` `[2-7] sequência` `[8-26] cartão` |
| Trailer | `[1-8] LOTE` `[9-14] QTD(6 dig)` |

Exemplo (`exemplo.txt`):

```
DESAFIO-HYPERATIVA           20180524LOTE0001000010
C1     4456897922969999
C2     4456897999999999
C3     4456897999999999
C4     4456897998199999
C5     4456897999999999124
C6     4456897912999999
C7     445689799999998
C8     4456897919999999
C9     4456897999099999
C10    4456897919999999
LOTE0001000010
```

---

## Logging

Cada requisição gera duas linhas de log:

```
--> POST /api/v1/cards ip=172.18.0.1
<-- POST /api/v1/cards user=admin status=201 46ms
```

Campos: **método**, **URI + query string**, **IP do cliente** (com suporte a `X-Forwarded-For`), **usuário autenticado**, **HTTP status** e **latência**.

Todas as requisições recebem um `X-Correlation-Id` (gerado automaticamente se não enviado pelo cliente) propagado via MDC para rastreamento em logs distribuídos.

O número do cartão **nunca aparece nos logs** — apenas o UUID gerado pelo sistema.

---

## Decisões de arquitetura

### Segurança

| Camada | Mecanismo |
|--------|-----------|
| Transporte | TLS 1.2/1.3 (HTTPS), HTTP redireciona para HTTPS |
| Autenticação | JWT assinado com HMAC-SHA512, stateless |
| Armazenamento do cartão | SHA-256 + pepper (irreversível); número original nunca gravado |
| Senha do usuário | BCrypt (gerada pelo pgcrypto do PostgreSQL) |
| Logs | Número do cartão nunca registrado; apenas UUID |

### Camadas (SOLID)
- `controller/` — orquestração HTTP, sem regra de negócio
- `service/` — regra de negócio (`AuthService`, `CardService`, `CardHashService`, `BatchFileParser`)
- `domain/entity/` + `domain/repository/` — modelo persistente
- `security/` — JWT (`JwtService`, `JwtAuthenticationFilter`)
- `config/` — `SecurityConfig`, `HttpsConfig`, `OpenApiConfig`, `AppProperties`
- `exception/` + `GlobalExceptionHandler` — erros em `application/problem+json`
- `filter/RequestLoggingFilter` — log de entrada/saída com MDC e `X-Correlation-Id`

### Escalabilidade
- Busca por hash com índice único — O(log n) para qualquer volume de cartões
- `open-in-view: false` + `hibernate.jdbc.batch_size=500` + `order_inserts=true` para inserções em lote
- Parser linha-a-linha (baixo consumo de memória para arquivos grandes)
- Stateless (JWT) — escala horizontal sem sessão compartilhada

### Testes
- **21 testes unitários** cobrindo `CardHashService`, `BatchFileParser`, `CardService` (com mocks) e `JwtService`
- **18 testes de controller (MockMvc)** cobrindo `AuthController` e `CardController` — cenários de sucesso, erros de validação, exceções de serviço e acesso não autenticado
- Total: **39 testes**
- Executar: `mvn test`

#### Estratégia dos testes de controller

Os testes de controller usam `@WebMvcTest` (carrega apenas a camada web, sem contexto JPA/Flyway) combinado com `@MockBean` nos serviços e `@ActiveProfiles("test")` para aplicar `application-test.yml` (H2, SSL desabilitado).

```java
@WebMvcTest(CardController.class)
@ActiveProfiles("test")
class CardControllerTest {

    @MockBean private CardService cardService;
    @MockBean private JwtService jwtService;   // exigido pelo JwtAuthenticationFilter

    @Test
    @WithMockUser                              // simula usuário autenticado
    void createCardReturns201WithIdAndCreatedAt() throws Exception { ... }

    @Test
    void createCardUnauthenticatedReturns401() throws Exception { ... }
}
```

| Arquivo de teste | Controller | Testes |
|------------------|------------|--------|
| `AuthControllerTest` | `AuthController` | loginSuccess, loginInvalidCredentials, loginBlankUsername, loginBlankPassword, loginMissingBody |
| `CardControllerTest` | `CardController` | createCard (success, idempotente, número inválido, erro no serviço, não autenticado), findCard (success, not found, não autenticado), uploadBatch (success, arquivo vazio, arquivo inválido, lote duplicado, não autenticado) |

---
