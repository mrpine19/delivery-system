Integrantes:

| Nome | RM |
|---|---|
| Arthur Graciani | RM561728 |
| Gustavo Oliveira | RM566358 |
| Lucas Hideki | RM565355 |

# delivery-system

Sistema de pedidos de delivery com microsserviços (Projeto Diamante 02, FIAP Java Advanced). A API segue o contrato consumido pelo app React Native [diamante-delivery](https://github.com/joaocarloslima/diamante-delivery).

O cenário é uma promoção relâmpago numa sexta às 20h. Cada problema tem uma solução específica:

| Problema | Solução |
|---|---|
| Estoque limitado (race condition) | Lock pessimista (`PESSIMISTIC_WRITE` / `SELECT ... FOR UPDATE`) |
| Pagamento instável (~50% de falha) | Eureka + `@LoadBalanced` entre duas instâncias + `@Retryable` com backoff exponencial |
| Avaliações em massa | RabbitMQ + buffer em memória com gravação em lote a cada 5 s (backpressure) |
| Dúvidas sobre o cardápio | Assistente com Spring AI (Google Gemini) |

## Arquitetura

```
                          ┌──────────────────────┐
                          │ eureka-server :8761  │◄──── registro dos 3 serviços
                          └──────────────────────┘
                                     ▲
 App React Native                    │ descoberta (PAYMENT-SERVICE)
        │                            │
        ▼                    ┌───────┴────────┐  HTTP (load balance + retry)  ┌──────────────────────┐
  ┌───────────────────────►  │ order-service  │ ────────────────────────────► │ payment-service :8081│
  │                          │     :8080      │ ────────────────────────────► │ payment-service :8082│
  │                          └───┬────────┬───┘                               └──────────────────────┘
  │                              │        │
  │                   AMQP       │        │ HTTPS
  │      delivery.exchange       │        ▼
  │      → reviews.queue         │   Google Gemini (Spring AI)
  │                              ▼
  │                       ┌──────────────┐
  │                       │   RabbitMQ   │ :5672 / painel :15672
  │                       └──────┬───────┘
  │                              ▼
  └── GET /reviews/ranking ─ review-service :8083 (buffer + flush a cada 5 s → H2)
```

| Serviço | Porta | Papel |
|---|---|---|
| `eureka-server` | 8761 | Service discovery. Os outros três serviços se registram pelo `spring.application.name`. |
| `order-service` | 8080 | Cardápio, pedidos com lock pessimista, chamada ao pagamento com load balance e retry, publicação de avaliações e assistente de IA. Banco H2 em memória. |
| `payment-service` | 8081 e 8082 | Pagamento instável (~50% de respostas 500). Roda em duas instâncias; o `instance-id` `${spring.application.name}:${server.port}` diferencia as duas no Eureka. |
| `review-service` | 8083 | Consome avaliações da fila, acumula num buffer em memória e grava em lote a cada 5 s. Expõe o ranking. Banco H2 em memória. |

Outros componentes:

- **RabbitMQ**: sobe via `compose.yaml` na raiz. Exchange `delivery.exchange` (topic), fila durável `reviews.queue`, routing key `reviews.new`.
- **Modelo de IA**: Google Gemini via `spring-ai-starter-model-google-genai`.

Cada serviço é um projeto Gradle independente (não há build multi-projeto na raiz).

### Detalhes de implementação

**Pedidos e estoque.** `CustomerOrderService.createOrder` é transacional: valida a quantidade → busca o prato com `DishRepository.findByIdForUpdate` (lock pessimista) → checa o estoque → calcula o total → chama o pagamento → decrementa o estoque → salva o pedido `CONFIRMED`. Se o pagamento falhar em todas as tentativas, é lançada `PaymentUnavailableException`, a transação faz rollback (estoque intacto, pedido não salvo) e a API responde 502.

**Load balance e retry.** O `RestTemplate` é anotado com `@LoadBalanced` e o `PaymentClient` chama `http://PAYMENT-SERVICE/payments` (nome no Eureka, sem `localhost`). O método usa `@Retryable` (Spring Framework 7, habilitado por `@EnableResilientMethods`) para `HttpServerErrorException` e `ResourceAccessException`: até 4 novas tentativas, atraso inicial de 200 ms, multiplicador 2, jitter de 100 ms e atraso máximo de 2 s.

**Mensageria.** `POST /reviews` valida a nota (1 a 5) e a existência do prato, publica `{"dishId", "dishName", "rating", "comment"}` em JSON e responde 202 sem gravar nada no order-service.

**Backpressure.** O `@RabbitListener` do review-service acumula as notas num `ConcurrentHashMap<Long, ReviewAccumulator>` (nome do prato, soma, quantidade). Um `@Scheduled(fixedRate = 5000)` remove cada entrada do buffer e soma os totais na tabela `review_summary`. O ranking calcula `average = ratingSum / count` com 1 casa decimal (HALF_UP) e ordena por média e, no empate, por quantidade de avaliações.

**Assistente.** A cada pergunta, o `ChatService` lê todos os pratos do banco (sem cache) e injeta o cardápio, com preço e disponibilidade, no system prompt. O assistente responde em português, em até 3 frases curtas, só com base no cardápio, e recusa educadamente perguntas fora do tema.

## Tecnologias

- Java 25
- Spring Boot 4.1, Spring Cloud 2025.1 (Netflix Eureka, LoadBalancer)
- Spring AI 2.0 (Google GenAI / Gemini)
- Spring Data JPA + H2 em memória
- Spring AMQP + RabbitMQ
- Gradle (wrapper incluso em cada serviço)
- Lombok

## Pré-requisitos

- **JDK 25**
- **Docker** com Docker Compose (para o RabbitMQ)
- **Chave de API do Google Gemini** (gerada no Google AI Studio)
- `curl` para testar os endpoints. No Windows PowerShell 5.1, `curl` é um alias de `Invoke-WebRequest`: use `curl.exe` ou o Git Bash.

## Variáveis de ambiente

| Variável | Serviço | Obrigatória | Uso |
|---|---|---|---|
| `GEMINI_API_KEY` | order-service | Sim | Chave do Gemini, lida por `spring.ai.google.genai.api-key=${GEMINI_API_KEY}` |
| `EUREKA_URL` | order, payment e review | Não | Endereço do Eureka. Padrão: `http://localhost:8761/eureka/` |

Nenhuma chave fica no repositório. Defina a variável no terminal onde o order-service vai rodar:

```bash
# bash
export GEMINI_API_KEY="sua-chave-aqui"
```

```powershell
# PowerShell
$env:GEMINI_API_KEY="sua-chave-aqui"
```

## Como executar

Suba tudo nesta ordem, cada serviço num terminal separado.

**1. RabbitMQ** (na raiz do repositório)

```bash
docker compose up -d
```

Portas 5672 (AMQP) e 15672 (painel). As credenciais locais de desenvolvimento estão no `compose.yaml` (`myuser` / `secret`).

**2. eureka-server**

```bash
cd eureka-server
./gradlew bootRun
```

**3. payment-service, instância 1 (porta 8081)**

```bash
cd payment-service
./gradlew bootRun
```

**4. payment-service, instância 2 (porta 8082)**

```bash
cd payment-service
./gradlew bootRun --args='--server.port=8082'
```

A segunda instância não tem arquivo de configuração próprio: ela existe só por causa do argumento `--server.port=8082`.

**5. review-service**

```bash
cd review-service
./gradlew bootRun
```

**6. order-service** (com a chave definida no mesmo terminal)

```bash
export GEMINI_API_KEY="sua-chave-aqui"
cd order-service
./gradlew bootRun
```

No Windows, use `gradlew.bat bootRun` no CMD ou `.\gradlew bootRun` no PowerShell. No PowerShell, a segunda instância do pagamento sobe com `.\gradlew bootRun --args="--server.port=8082"`.

> **IntelliJ no Windows:** o order-service pode falhar com "Command line is too long" por causa das dependências do Spring AI. Para resolver: Run → Edit Configurations → Modify options → Shorten command line → `@argfiles (Java 9+)`. Isso não afeta o `./gradlew bootRun`.

## Endpoints

### order-service (8080)

| Método | Rota | Corpo | Respostas |
|---|---|---|---|
| GET | `/dishes` | — | 200 lista de Dish |
| GET | `/dishes/{id}` | — | 200 Dish · 404 |
| POST | `/orders` | `{"dishId": 1, "quantity": 2}` | 201 Order · 400 · 404 · 409 · 502 |
| GET | `/orders/{id}` | — | 200 Order · 404 |
| POST | `/reviews` | `{"dishId": 1, "rating": 5, "comment": "Great"}` | 202 sem corpo · 400 · 404 |
| POST | `/assistant` | `{"question": "Tem opção vegana?"}` | 200 `{"answer": "..."}` · 400 |

### review-service (8083)

| Método | Rota | Respostas |
|---|---|---|
| GET | `/reviews/ranking` | 200 `[{"dishId", "dishName", "average", "count"}]` ordenado pela média |

### payment-service (8081 / 8082, interno)

| Método | Rota | Corpo | Respostas |
|---|---|---|---|
| POST | `/payments` | `{"amount": 79.80}` | 200 `{"status": "APPROVED", "instance": 8081}` · 500 (falha simulada) |

O app não chama esse serviço. Só o order-service, via Eureka.

### Formatos

- **Dish**: `{"id", "name", "description", "price", "stock"}`
- **Order**: `{"id", "dishId", "quantity", "totalPrice", "status": "CONFIRMED", "createdAt": "2026-10-08T20:15:00"}`
- **Erro** (4xx e 502): `{"error": "Dish out of stock"}`

Mensagens de erro: `Dish not found`, `Order not found`, `Dish out of stock`, `Payment unavailable`, `Malformed JSON` e as mensagens de validação do Bean Validation.

### Cardápio inicial

Carregado pelo `DataLoader` na subida do order-service:

| id | Prato | Preço | Estoque |
|---|---|---|---|
| 1 | X-Burger | 39.90 | 12 |
| 2 | Coca-Cola | 8.50 | 25 |
| 3 | Salada Verde (vegetariana/vegana) | 24.00 | 18 |
| 4 | **Promo do Dia** | 29.90 | **10** |
| 5 | Frango à Parmegiana | 42.00 | 9 |

### Exemplos com curl

```bash
# Cardápio
curl http://localhost:8080/dishes
curl http://localhost:8080/dishes/4

# Criar pedido
curl -i -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"dishId": 1, "quantity": 2}'

# Consultar pedido
curl http://localhost:8080/orders/1

# Erros: quantidade inválida (400), prato inexistente (404)
curl -i -X POST http://localhost:8080/orders -H "Content-Type: application/json" -d '{"dishId": 1, "quantity": 0}'
curl -i -X POST http://localhost:8080/orders -H "Content-Type: application/json" -d '{"dishId": 99, "quantity": 1}'

# Enviar avaliação (202 sem corpo)
curl -i -X POST http://localhost:8080/reviews \
  -H "Content-Type: application/json" \
  -d '{"dishId": 1, "rating": 5, "comment": "Great"}'

# Ranking (depois do flush de 5 s)
curl http://localhost:8083/reviews/ranking

# Assistente
curl -X POST http://localhost:8080/assistant \
  -H "Content-Type: application/json" \
  -d '{"question": "Tem opção vegana?"}'

# Pagamento direto numa instância (só para teste)
curl -i -X POST http://localhost:8081/payments \
  -H "Content-Type: application/json" \
  -d '{"amount": 79.80}'
```

## Como validar cada requisito

### 1. Eureka

Abra http://localhost:8761. Em "Instances currently registered with Eureka" devem aparecer:

- `ORDER-SERVICE`: `order-service:8080`
- `PAYMENT-SERVICE`: duas instâncias, `payment-service:8081` e `payment-service:8082`
- `REVIEW-SERVICE`: `review-service:8083`

### 2. Load balance (alternância 8081/8082)

Crie alguns pedidos de pratos com estoque (por exemplo, `dishId` 2) e acompanhe os terminais das duas instâncias do pagamento. As chamadas se alternam entre elas:

```
[instance 8081] Payment APPROVED - amount: 8.50
[instance 8082] Payment FAILED (simulated) - amount: 8.50
```

No log do order-service, cada pedido aprovado mostra qual instância respondeu: `Payment APPROVED by instance 8082`.

### 3. Retry

Como ~50% das chamadas falham, o log do order-service mostra várias linhas `Calling payment-service - amount: ...` para o mesmo pedido, com intervalos crescentes (200 ms, 400 ms, 800 ms...). Pela alternância, as tentativas caem ora numa instância, ora na outra.

Para ver o 502, derrube as duas instâncias do pagamento e crie um pedido. Depois das tentativas, a resposta é `502 {"error": "Payment unavailable"}`, e `GET /dishes/{id}` mostra que o estoque não mudou (rollback).

### 4. Race condition (50 requisições simultâneas)

A "Promo do Dia" (`dishId` 4) começa com estoque 10. Disparando 50 pedidos simultâneos de 1 unidade, o resultado esperado é **exatamente 10 respostas 201**, o restante 409 (`Dish out of stock`) e estoque final 0.

**bash** (Linux, macOS ou Git Bash):

```bash
seq 1 50 | xargs -P 50 -I{} curl -s -o /dev/null -w "%{http_code}\n" \
  -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"dishId": 4, "quantity": 1}' | sort | uniq -c
```

**PowerShell** (5.1 ou 7):

```powershell
Add-Type -AssemblyName System.Net.Http
[System.Net.ServicePointManager]::DefaultConnectionLimit = 100   # no 5.1 o padrão é 2 conexões simultâneas
$client = [System.Net.Http.HttpClient]::new()
$body = '{"dishId": 4, "quantity": 1}'

$tasks = 1..50 | ForEach-Object {
    $content = [System.Net.Http.StringContent]::new($body, [System.Text.Encoding]::UTF8, "application/json")
    $client.PostAsync("http://localhost:8080/orders", $content)
}
[System.Threading.Tasks.Task]::WaitAll([System.Threading.Tasks.Task[]]$tasks)

$tasks | Group-Object { [int]$_.Result.StatusCode } | Select-Object @{n="Status";e={$_.Name}}, Count
```

Depois, confira o estoque:

```bash
curl http://localhost:8080/dishes/4      # "stock": 0
```

Se o pagamento falhar em todas as tentativas de algum pedido (raro), esse pedido recebe 502 e o estoque dele não é consumido. Por isso, a soma de 201 nunca passa de 10.

Para conferir no banco, abra o console H2 em http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:orderdb`, usuário `sa`, senha vazia) e rode:

```sql
SELECT COUNT(*) FROM CUSTOMER_ORDERS WHERE DISH_ID = 4;
```

Para repetir o teste, reinicie o order-service: o banco é em memória e o estoque volta a 10.

### 5. Mensageria (fila no painel do RabbitMQ)

Abra http://localhost:15672 com as credenciais do `compose.yaml`. Na aba **Exchanges** está `delivery.exchange`; na aba **Queues**, `reviews.queue`.

Com o review-service rodando, as mensagens são consumidas na hora (veja o gráfico "Message rates"). Para ver mensagens acumuladas na fila:

1. Pare o review-service.
2. Envie algumas avaliações:
   ```bash
   for i in $(seq 1 20); do
     curl -s -o /dev/null -w "%{http_code} " -X POST http://localhost:8080/reviews \
       -H "Content-Type: application/json" \
       -d "{\"dishId\": $(( (i % 5) + 1 )), \"rating\": $(( (i % 5) + 1 )), \"comment\": \"test $i\"}"
   done
   ```
   Todas respondem `202`.
3. Em **Queues → reviews.queue**, a coluna "Ready" mostra 20 mensagens.
4. Suba o review-service de novo: a fila esvazia e as avaliações entram no ranking.

### 6. Backpressure e ranking após o flush

Com o review-service rodando, envie avaliações e consulte o ranking logo em seguida:

```bash
curl -X POST http://localhost:8080/reviews -H "Content-Type: application/json" -d '{"dishId": 1, "rating": 5, "comment": "Great"}'
curl -X POST http://localhost:8080/reviews -H "Content-Type: application/json" -d '{"dishId": 1, "rating": 4, "comment": "Good"}'
curl -X POST http://localhost:8080/reviews -H "Content-Type: application/json" -d '{"dishId": 3, "rating": 5, "comment": "Fresh"}'

curl http://localhost:8083/reviews/ranking   # pode ainda estar vazio: as notas estão no buffer
sleep 5
curl http://localhost:8083/reviews/ranking
```

Resposta esperada depois do flush:

```json
[
  {"dishId": 3, "dishName": "Salada Verde", "average": 5.0, "count": 1},
  {"dishId": 1, "dishName": "X-Burger", "average": 4.5, "count": 2}
]
```

No log do review-service, cada flush grava uma vez por prato, não uma vez por mensagem: `Processed review summary for dishId: 1, total reviews: 2`.

### 7. Assistente (Spring AI)

```bash
curl -X POST http://localhost:8080/assistant -H "Content-Type: application/json" \
  -d '{"question": "Tem opção vegana?"}'
# Deve indicar a Salada Verde

curl -X POST http://localhost:8080/assistant -H "Content-Type: application/json" \
  -d '{"question": "Quanto custa o X-Burger?"}'
# R$ 39,90

curl -X POST http://localhost:8080/assistant -H "Content-Type: application/json" \
  -d '{"question": "A Promo do Dia ainda está disponível?"}'
# Depois do teste das 50 requisições: esgotada, com sugestão de outro prato

curl -X POST http://localhost:8080/assistant -H "Content-Type: application/json" \
  -d '{"question": "Quem ganhou a Copa de 2002?"}'
# Recusa educada, oferecendo ajuda com o cardápio

curl -i -X POST http://localhost:8080/assistant -H "Content-Type: application/json" \
  -d '{"question": ""}'
# 400
```

O cardápio é lido do banco a cada pergunta, então a disponibilidade reflete o estoque atual.
