# Gaps e Bugs - ImovelConecte

Análise de gaps e bugs identificados no projeto.

---

## Bugs Críticos

### 1. **Webhook HubSpot envia ARRAY, não objeto único**

O HubSpot envia o payload como **array de eventos** (até 100 por request):

```json
[
  { "objectId": 1246965, "propertyName": "lifecyclestage", "propertyValue": "subscriber", ... },
  { "objectId": 1246978, "eventType": "contact.creation", ... }
]
```

O `WebhookController` tenta deserializar como `HubSpotWebhookPayload` (objeto único), o que **falha** com o payload real do HubSpot.

### 2. **Eventos de criação (contact.creation) não trazem propriedades**

Para `contact.creation` e `contact.propertyChange` (com uma propriedade), o webhook traz apenas:
- `objectId`, `portalId`, `occurredAt`, `subscriptionType`/`eventType`
- Para propertyChange: `propertyName`, `propertyValue`

Para **contact.creation** não há `email`, `firstname`, etc. no payload. É necessário chamar a API do HubSpot para buscar os dados do contato.

### 3. **WebhookFailure sem `maxRetries` ao criar novo registro**

Em `WebhookFailureService.recordFailure()`, ao criar novo `WebhookFailure`:

```java
WebhookFailure.builder()
    .eventId(eventId)
    .payload(payload)
    .retryCount(0)
    // maxRetries NÃO É SETADO - fica 0 por padrão!
```

`canRetry()` retorna `retryCount < maxRetries`, então com `maxRetries=0` nunca permite retry.

### 4. **Possível IndexOutOfBounds em HubSpotApiService**

```java
"lastname", lead.getFullName() != null && lead.getFullName().contains(" ")
    ? lead.getFullName().substring(lead.getFullName().indexOf(" ") + 1) : ""
```

Se `fullName` for `"Nome "` (espaço no final), `indexOf(" ")` é 4, `substring(5)` = `""`. OK.
Se for `"Nome"` sem espaço, `contains(" ")` é false, retorna `""`. OK.
Mas se `fullName` for `" "` (só espaço), `indexOf(" ")` = 0, `substring(1)` = `""`. OK.
Risco: se o nome tiver vários espaços ou caracteres especiais, a lógica pode ser incorreta (ex: sobrenome composto).

### 5. **Lead com email vazio pode ser criado**

`LeadService.createOrUpdateFromWebhook` usa:

```java
.email(props.getOrDefault("email", ""))
```

Webhook de `contact.creation` não traz email. Resultado: Lead com `email=""`, violando expectativa de negócio (email obrigatório para lead).

---

## Gaps de Segurança

### 6. **Validação de assinatura do webhook ausente**

HubSpot envia `X-HubSpot-Signature-v3` (ou `X-HubSpot-Signature`) para validar a origem. O endpoint não valida, permitindo que requisições forjadas disparem processamento.

### 7. **GET /webhooks/hubspot sem hub.challenge retorna 400**

O parâmetro `hub.challenge` é obrigatório. Se HubSpot enviar sem ele (ou com nome diferente), a verificação falha. Verificar documentação atual do HubSpot para o formato exato.

---

## Gaps de Resiliência

### 8. **HubSpotOAuthService sem tratamento de erro da API**

`exchangeCodeForTokens` e `refreshToken` usam `.block()` sem try-catch. Erros 4xx/5xx do HubSpot propagam exceção não tratada (ex: `WebClientResponseException`).

### 9. **HubSpotApiService.syncLeadToHubSpot sem tratamento de erro**

Se a API do HubSpot falhar (timeout, 429, 5xx), a exceção quebra `LeadService.createLead`. O lead já foi salvo no banco antes da chamada; fica lead órfão (não sincronizado) sem retry ou compensação.

### 10. **OAuth callback sem validação de `code`**

Se HubSpot redirecionar com `error=access_denied` (sem `code`), `@RequestParam("code")` gera 400. Melhor tratar explicitamente e retornar mensagem adequada.

---

## Gaps de Dados/Modelo

### 11. **DTO HubSpot: campo `eventType` vs `subscriptionType`**

Documentação do HubSpot usa `eventType` no payload; o DTO usa `subscriptionType`. Pode haver incompatibilidade dependendo da versão da API.

### 12. **PropertyChange com name/value null**

Em `LeadEnrichmentService.extractPropertiesFromPayload`, se `pv.getName()` ou `pv.getValue()` for null, entram null no `Map`. Pode gerar NPE ou dados inconsistentes em `LeadService`.

### 13. **fullName com espaços desnecessários**

```java
.fullName(props.getOrDefault("firstname", "") + " " + props.getOrDefault("lastname", ""))
```

Se ambos forem vazios, resulta em `" "`. Se só firstname existir, fica `"João "` com espaço à direita.

---

## Gaps de Configuração

### 14. **HubSpot OAuth com client_id/client_secret vazios**

Se `HUBSPOT_CLIENT_ID` ou `HUBSPOT_CLIENT_SECRET` estiverem vazios, `buildAuthorizationUrl()` gera URL inválida e `exchangeCodeForTokens` falha na chamada ao HubSpot.

### 15. **Fila de contingência sem DLQ**

`CONTINGENCY_QUEUE` não tem dead-letter configurado. Se o `ContingencyConsumer` falhar repetidamente, com `requeue-rejected: false` a mensagem pode ser descartada sem rastreio.

---

## Gaps de Testes

### 16. **Cenário de webhook sem objectId**

Payload pode ter `objectId` null em alguns eventos. `LeadService` usa `contactId = null` e `findByHubspotContactId(null)` pode se comportar de forma inesperada.

### 17. **Testes não cobrem falhas de integração**

Não há testes para:
- falha ao sincronizar com HubSpot;
- falha no OAuth (code inválido);
- timeout/erro na API do HubSpot.

---

## Resumo de Prioridade

| Prioridade | Item | Impacto | Status |
|------------|------|---------|--------|
| P0 | #1 Payload como array | Webhook não processa payload real | ✅ Corrigido |
| P0 | #2 Dados de contact.creation | Leads sem email/nome | ✅ Corrigido (busca na API + placeholder) |
| P0 | #3 maxRetries em WebhookFailure | Contingência nunca tenta retry | ✅ Corrigido |
| P1 | #6 Validação de assinatura | Risco de segurança | Pendente |
| P1 | #8/#9 Tratamento de erro HubSpot | Experiência e resiliência | Pendente |
| P2 | #4/#5/#12/#13 | Qualidade de dados | ✅ Parcialmente (null-safety, buildFullName) |
| P2 | #10/#14/#15 | Robustez e configuração | Pendente |
