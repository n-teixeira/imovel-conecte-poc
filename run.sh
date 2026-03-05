#!/bin/bash
# ImovelConecte - Script de execução

set -e

echo "=== ImovelConecte ==="

# Carrega .env se existir
if [ -f .env ]; then
  echo "Carregando variáveis de .env"
  export $(grep -v '^#' .env | xargs)
fi

# Verifica se Docker está rodando (para RabbitMQ)
if ! docker info &>/dev/null; then
  echo "Aviso: Docker não está rodando. RabbitMQ precisa estar acessível em localhost:5672"
fi

echo "Iniciando aplicação..."
mvn spring-boot:run "$@"
