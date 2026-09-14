# VaiJunto - Sistema de Caronas Compartilhadas

## Visão geral

O projeto VaiJunto implementa um sistema de caronas compartilhadas em arquitetura cliente-servidor, com comunicação em sockets TCP/IP e troca de mensagens em JSON. A aplicação foi desenvolvida em Java, com um servidor central responsável por manter o estado das caronas, reservas, autenticação e concorrência, e clientes com interface gráfica JavaFX para motorista e passageiro.

## Objetivo

O sistema simula a operação de uma plataforma de mobilidade em que:

- motoristas publicam caronas com rota, data, horário, vagas e preço por trecho;
- passageiros consultam itinerários entre origem e destino;
- reservas são confirmadas de forma atômica por trecho;
- o controle de concorrência evita que o mesmo assento seja vendido duas vezes;
- a aplicação mantém autenticação por token e gestão de sessão.

## Funcionalidades implementadas

### Cliente motorista

- cadastro e login;
- publicação de carona com rota, data, horário, vagas e precos por trecho;
- visualização das caronas próprias;
- consulta de passageiros por trecho;
- cancelamento de carona.

### Cliente passageiro

- cadastro e login;
- busca de itinerários por origem, destino e data;
- confirmação de reserva de viagem composta por um ou mais trechos;
- cancelamento de reserva.

### Servidor central

- autenticação de usuários;
- armazenamento de usuários e sessões;
- gestão de caronas e reservas;
- cálculo de itinerários com múltiplos trechos;
- controle de disponibilidade de assentos por trecho;
- processamento concorrente de múltiplos clientes na mesma porta TCP.

## Arquitetura

O repositório está organizado em dois módulos principais:

- `vaijunto-server/`: backend em Java com sockets TCP, lógica de negócio e testes de concorrência;
- `vaijunto-client/`: frontend em JavaFX para interação com o servidor.

A comunicação entre cliente e servidor é feita com objetos `DTORequest` e `DTOResponse` serializados para JSON, o que torna a troca de mensagens padronizada e facilmente extensível.

## Requisitos e tecnologias

- Java 21
- Maven
- JavaFX
- Sockets TCP/IP
- JSON via Gson
- Docker (arquivos Dockerfile disponíveis para os módulos)

## Como executar

### 1. Servidor

```bash
cd vaijunto-server
mvn clean package
java -jar target/vaijunto-server-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### 2. Cliente

```bash
cd vaijunto-client
mvn javafx:run
```

### 3. Com Docker

```bash
cd vaijunto-server
docker build -t vaijunto-server .
docker run --rm -p 8080:8080 vaijunto-server
```

```bash
cd vaijunto-client
docker build -t vaijunto-client .
docker run --rm -e DISPLAY=:0 --network host vaijunto-client
```

## Testes

O módulo do servidor inclui testes automatizados de concorrência para validar:

- múltiplos cadastros simultâneos;
- exclusão de login duplicado;
- limite de vagas em reservas simultâneas;
- prevenção de venda duplicada do mesmo assento.

Comando de validação:

```bash
cd vaijunto-server
mvn test
```

## Avaliação geral

O projeto está em um nível funcional satisfatório para o contexto do problema proposto. Ele implementa a base do sistema de caronas compartilhadas, cobre a lógica central de negócio, utiliza sockets nativos sobre TCP/IP e inclui testes de concorrência que demonstram preocupação com consistência de dados e atomicidade de reservas.

Em termos de escopo, o sistema atende aos principais requisitos do enunciado e está pronto para uso como protótipo funcional de plataforma de caronas compartilhadas.

## Observação

Este repositório concentra a implementação principal do produto e a documentação operacional necessária para execução, sendo o ponto de partida para uso, testes e evolução do sistema.
