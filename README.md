# ImovelConecte

**Lead Automation SaaS** — Integração bidirecional com HubSpot CRM via OAuth 2.0, captura e enriquecimento de leads com processamento assíncrono de webhooks em RabbitMQ.

## Propósito

O ImovelConecte é uma aplicação de automação de leads para o setor imobiliário. Seu objetivo é:

1. **Integração bidirecional com HubSpot CRM**: Sincronizar leads entre a aplicação e o HubSpot através de OAuth 2.0, permitindo tanto a captura de novos contatos no CRM quanto o envio de leads enriquecidos para o HubSpot.
2. **Captura e enriquecimento de leads**: Processar webhooks do HubSpot de forma assíncrona, enriquecendo os dados dos leads com informações adicionais.
3. **Confiabilidade**: Garantir idempotência para evitar duplicações em reentregas e possuir fila de contingência para reprocessamento de falhas de integração.

## Tecnologias

- **Java 17** · **Spring Boot 3.2**
- **OAuth 2.0** — Autenticação com HubSpot
- **RabbitMQ** — Processamento assíncrono de webhooks
- **PostgreSQL / H2** — Persistência (H2 para desenvolvimento)
- **JPA/Hibernate** — ORM

## Pré-requisitos

- Java 17+
- Maven 3.8+
- Docker e Docker Compose (para RabbitMQ e PostgreSQL)
- Conta no [HubSpot](https://hubspot.com) e app configurado para OAuth

## Configuração

### 1. Variáveis de Ambiente

Copie o arquivo de exemplo e ajuste as credenciais:

```bash
cp .env.example .env
```

Edite o `.env` com suas credenciais do HubSpot e banco de dados. **Nunca commite o arquivo `.env`.**

### 2. HubSpot — Configurar OAuth

1. Acesse [HubSpot Developers](https://developers.hubspot.com/)
2. Crie uma app e configure OAuth
3. Adicione os scopes: `crm.objects.contacts.read`, `crm.objects.contacts.write`, `crm.objects.deals.read`, `crm.objects.deals.write`
4. Defina a URL de redirect: `http://localhost:8080/oauth/callback`
5. Copie o **Client ID** e **Client Secret** para o `.env`

## Como Rodar

### Desenvolvimento Local (com Docker para RabbitMQ)

```bash
# Subir RabbitMQ e PostgreSQL
docker-compose up -d

# Rodar a aplicação
mvn spring-boot:run
```

Ou usando variáveis do `.env`:

```bash
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run
```

### Desenvolvimento com H2 (sem Docker)

Para desenvolvimento rápido sem RabbitMQ/PostgreSQL:

```bash
# RabbitMQ precisa estar acessível - use docker apenas para Rabbit
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management-alpine

mvn spring-boot:run
```

O H2 será usado automaticamente se `DATABASE_URL` não estiver definida.

### Docker (imagem da aplicação)

```bash
# Build da imagem
docker build -t imovelconecte:1.0 .

# Com docker-compose completo (app + RabbitMQ + Postgres)
# Adicione o serviço 'app' ao docker-compose e rode
docker-compose up -d
```

### Testes

```bash
mvn test
```

Os testes usam **Testcontainers** para RabbitMQ. Certifique-se de que o Docker está rodando.

## Fluxo OAuth

1. `GET /oauth/authorize` — Retorna a URL para o usuário autorizar no HubSpot
2. Após autorização, HubSpot redireciona para `GET /oauth/callback?code=...`
3. A aplicação troca o `code` por tokens e os armazena

## API e Webhooks

| Endpoint | Método | Descrição |
|----------|--------|-----------|
| `/api/leads` | GET | Lista todos os leads |
| `/api/leads` | POST | Cria lead e sincroniza com HubSpot |
| `/api/leads/{id}` | GET | Busca lead por ID |
| `/webhooks/hubspot` | GET | Verificação do webhook (HubSpot envia `hub.challenge`) |
| `/webhooks/hubspot` | POST | Recebe eventos do HubSpot e enfileira para processamento |

## Arquitetura

```
[HubSpot] --> webhook POST --> [ImovelConecte]
                                    |
                                    v
                              [RabbitMQ - fila leads]
                                    |
                                    v
                              [Consumer] --> idempotência --> [LeadService]
                                    |                              |
                                    | falha                       v
                                    v                         [DB + HubSpot]
                              [Fila contingência]
```

- **Idempotência**: Chaves por `eventId` evitam processamento duplicado em reentregas
- **Fila de contingência**: Mensagens com falha são reenviadas para retry

## Estrutura do Projeto

```
imovel-conecte/
├── src/main/java/br/com/imovelconecte/
│   ├── config/          # RabbitMQ, HubSpot, WebClient
│   ├── consumer/        # Consumidores RabbitMQ (leads, contingência)
│   ├── domain/entity/   # Entidades JPA
│   ├── dto/             # DTOs
│   ├── repository/      # Repositórios JPA
│   ├── service/         # Regras de negócio
│   └── web/             # Controllers REST
├── src/test/            # Testes unitários e de integração
├── .env.example         # Template de variáveis
├── docker-compose.yml   # RabbitMQ + PostgreSQL
├── Dockerfile           # Build da aplicação
└── README.md
```
