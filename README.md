# VaiJunto - Sistema de Caronas Compartilhadas

## Visão geral

O VaiJunto é um sistema cliente-servidor de caronas compartilhadas. O servidor central mantém usuários, sessões, caronas, itinerários e reservas; os clientes JavaFX oferecem as interfaces de motorista e passageiro.

As aplicações se comunicam por sockets TCP/IP usando mensagens JSON. O protocolo de aplicação é definido pelos DTOs `DTORequest` (cliente -> servidor) e `DTOResponse` (servidor -> cliente).

## Arquitetura e execução

O repositório possui dois módulos:

- `vaijunto-server/`: servidor TCP, regras de negócio e testes de concorrência;
- `vaijunto-client/`: cliente JavaFX.

Requisitos: Java 21, Maven, JavaFX, Gson e, opcionalmente, Docker.

## Estrutura do projeto e pacotes

```text
.
├── vaijunto-server/
│   ├── pom.xml
│   ├── Dockerfile
│   ├── src/main/java/
│   │   ├── controller/  # serviços de usuários, caronas e reservas
│   │   ├── model/       # entidades, cidades, grafo e trechos
│   │   ├── network/     # DTOs e conversão JSON
│   │   └── server/      # ServidorTCP e ClientHandler
│   └── src/test/java/   # testes de concorrência
├── vaijunto-client/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/
│       ├── client/      # cliente TCP e chamadas do protocolo
│       ├── com/vaijunto/ # classe principal e telas JavaFX
│       ├── model/       # enums compartilhados
│       ├── network/     # DTOs e conversão JSON
│       ├── sessao/      # sessão do usuário
│       └── util/        # tarefas assíncronas da interface
└── README.md
```

Os módulos são Maven independentes e devem ser compilados a partir de suas
respectivas pastas. As principais dependências são:

| Módulo | Dependências |
|---|---|
| Servidor | Gson 2.10.1, JUnit 4.13.2 (testes) |
| Cliente | Gson 2.10.1, JavaFX Controls 21.0.2, JavaFX FXML 21.0.2, JUnit 4.13.2 (testes) |

O servidor usa o plugin Assembly para gerar o JAR executável
`target/vaijunto-server-1.0-SNAPSHOT-jar-with-dependencies.jar`. O cliente usa
o plugin JavaFX Maven, com `com.vaijunto.App` como classe principal.

## Preparação, compilação e testes

Com Java 21 e Maven instalados, execute:

```bash
# Compilar e testar o servidor
cd vaijunto-server
mvn clean test

# Gerar o JAR executável do servidor
mvn package

# Compilar e testar o cliente
cd ../vaijunto-client
mvn clean test
```

Os artefatos compilados ficam em `target/` dentro de cada módulo. O cliente
JavaFX precisa de um ambiente gráfico compatível; em Linux, as bibliotecas
nativas são obtidas pelo Maven usando o classificador `linux`.

### Servidor

```bash
cd vaijunto-server
mvn clean package
java -jar target/vaijunto-server-1.0-SNAPSHOT-jar-with-dependencies.jar
```

O servidor escuta a porta `8080`, aceita conexões continuamente e cria uma thread `ClientHandler` para cada cliente.

### Cliente

```bash
cd vaijunto-client
mvn javafx:run
```

O host e a porta padrão podem ser substituídos pelas variáveis `VAIJUNTO_SERVER_HOST` e `VAIJUNTO_SERVER_PORT`.

### Configuração e execução com Docker

Os Dockerfiles são independentes e devem ser construídos dentro do diretório
do módulo correspondente. O build instala/obtém as dependências declaradas no
`pom.xml`, compila o código com Java 21 e falha se houver erro de compilação.

#### Servidor

```bash
docker build -t vaijunto-server ./vaijunto-server
docker run --rm --name vaijunto-server -p 8080:8080 vaijunto-server
```

O container do servidor não usa banco de dados nem volume: usuários, sessões,
caronas e reservas ficam em memória e são perdidos quando o container termina.

#### Cliente JavaFX

O Dockerfile do cliente instala as bibliotecas nativas GTK/X11 exigidas pelo
JavaFX, define `DISPLAY=:0`, baixa as dependências Maven e executa
`javafx:run`. Em um Linux com X11, o cliente pode ser iniciado assim:

```bash
xhost +local:docker
docker build -t vaijunto-client ./vaijunto-client
docker run --rm \
  --name vaijunto-client \
  --network host \
  -e DISPLAY=$DISPLAY \
  -e VAIJUNTO_SERVER_HOST=127.0.0.1 \
  -e VAIJUNTO_SERVER_PORT=8080 \
  -v /tmp/.X11-unix:/tmp/.X11-unix \
  vaijunto-client
```

Em uma rede Docker criada pelo usuário, substitua `--network host` por
`--network <rede>` e use `VAIJUNTO_SERVER_HOST` com o nome ou endereço
alcançável do container do servidor. Em ambientes sem X11, como servidores
sem interface gráfica ou Docker Desktop, o servidor pode ser executado
normalmente, mas o cliente JavaFX precisa de um display compatível (por
exemplo, X11 ou XWayland).

## Especificação do protocolo de aplicação

### Transporte e enquadramento

1. O cliente abre uma conexão TCP com o servidor (por padrão, `127.0.0.1:8080`).
2. Cada mensagem é um objeto JSON em uma única linha, terminado por `LF` (`\n`).
3. Para cada linha recebida, o servidor envia exatamente uma resposta JSON em uma linha.
4. A conexão é persistente: várias operações podem ser trocadas no mesmo socket.
5. O cliente encerra a conexão fechando o socket. O servidor detecta `EOF`, fecha os streams e libera o `ClientHandler`.
6. Não existe uma operação `DISCONNECT` no protocolo. `LOGOUT` encerra a sessão lógica, enquanto o fechamento do socket encerra a conexão TCP.

O JSON é desserializado sem distinção entre maiúsculas e minúsculas apenas para valores de cidade e `tipoUser`; o nome das propriedades deve seguir os nomes abaixo. Campos opcionais não utilizados podem ser omitidos (o Gson não os inclui quando possuem valor `null`).

### Formato das requisições

Uma requisição tem a forma:

```json
{
  "acao": "NOME_DA_OPERACAO",
  "...": "campos específicos da operação"
}
```

Campos disponíveis em `DTORequest`:

| Campo | Tipo JSON | Descrição |
|---|---|---|
| `acao` | string | Operação a executar. É obrigatória e comparada após `trim` e conversão para maiúsculas. |
| `data` | string | Data no formato ISO-8601 aceito por `LocalDate`, normalmente `YYYY-MM-DD`. |
| `hora` | string | Horário aceito por `LocalTime`, normalmente `HH:MM` ou `HH:MM:SS`. |
| `rota` | array de strings | Lista ordenada de cidades; deve possuir pelo menos duas cidades. |
| `precos` | array de números | Preço de cada trecho da rota, na mesma ordem; a quantidade deve ser `rota.length - 1`. |
| `idCarona` | string | Identificador da carona. |
| `token` | string | Token retornado por `LOGIN`, usado nas operações autenticadas. |
| `ativaOuNao` | boolean | Indica se a carona está ativa. |
| `login` | string | Login do usuário. |
| `senha` | string | Senha do usuário. |
| `origem` | string | Cidade de origem da busca. |
| `destino` | string | Cidade de destino da busca. |
| `vagas` | integer | Quantidade total de vagas, maior que zero. |
| `idReserva` | string | Identificador da reserva. |
| `tipoUser` | string | `MOTORISTA` ou `PASSAGEIRO`. |
| `indiceItinerario` | integer | Índice (base zero) de um itinerário retornado pela busca na mesma conexão TCP. |

As cidades válidas são `Valentine`, `Rhodes`, `Saint Denis`, `Strawberry`, `Blackwater`, `Anesburg`, `Vanhorn`, `Emerald Ranch`, `Cornwall Kerosene`, `Butcher Creek`, `Lagras`, `Braithwaite`, `Caliga Hall`, `Manzanita Post`, `Colter`, `Wapiti`, `Armadillo`, `Tumbleweed`, `Macfarlanes Ranch`, `Thieves Landing` e `Plainview`. Também são aceitos os nomes das constantes, como `SAINT_DENIS`. `Tahiti` existe no enum, mas é indisponível e não pode ser usada. Nas respostas, o Gson serializa os valores do enum pelo nome da constante, por exemplo, `SAINT_DENIS`.

### Formato das respostas

Toda resposta possui o envelope:

```json
{
  "sucesso": true,
  "mensagem": "Descrição da operação",
  "dados": {}
}
```

- `sucesso` (boolean): `true` quando a operação foi concluída; `false` em erro de validação, autenticação ou regra de negócio.
- `mensagem` (string): descrição legível do resultado.
- `dados` (qualquer tipo, opcional): resultado da operação. Pode ser omitido quando não há dados, uma string, um objeto, uma lista de caronas ou uma lista de itinerários.

JSON inválido recebe `{"sucesso":false,"mensagem":"JSON inválido: ..."}`. Requisições sem `acao` e ações desconhecidas também recebem uma resposta de erro.

### Operações

| `acao` | Campos obrigatórios | Resultado de sucesso em `dados` |
|---|---|---|
| `PING` | nenhum | ausente; mensagem `Pong! Servidor VaiJunto online.` |
| `CADASTRAR` | `login`, `senha`, `tipoUser` | ausente |
| `LOGIN` | `login`, `senha` | objeto com `token` e `tipoUser` |
| `LOGOUT` | `token` | ausente |
| `MUDAR_TIPO_USUARIO` | `token`, `tipoUser` | ausente |
| `OFERECER_CARONA` | `token`, `data`, `hora`, `rota`, `precos`, `vagas`, `ativaOuNao` | string `idCarona` |
| `MINHAS_CARONAS` | `token` | lista de objetos `Carona` |
| `CONSULTA_PASSAGEIROS` | `token`, `idCarona` | mapa `id/trecho -> lista de logins` |
| `CANCELAR_CARONA` | `token`, `idCarona` | ausente |
| `BUSCAR_VIAGENS` | `origem`, `destino`, `data` | lista de objetos `Itinerario` |
| `RESERVAR` | `token`, `indiceItinerario` | string `idReserva` |
| `CANCELAR_RESERVA` | `token`, `idReserva` | ausente |
| `MINHAS_RESERVAS` | `token` | lista de objetos `Reserva` do passageiro autenticado |

Regras específicas:

- `CADASTRAR` rejeita login ou senha vazios, tipo inválido e login já existente.
- `LOGIN` retorna um token de sessão; senha incorreta ou login inexistente falha.
- Operações com `token` falham com `Usuário não autenticado.` quando a sessão é inválida.
- `OFERECER_CARONA` exige rota com pelo menos duas cidades, uma data e hora válidas, preços não negativos e um preço por trecho. O motorista precisa estar autenticado.
- `BUSCAR_VIAGENS` grava o resultado no `ClientHandler`; por isso `RESERVAR` deve usar um índice retornado pela busca anterior na mesma conexão. Uma nova busca substitui a anterior.
- `RESERVAR` reserva atomicamente um lugar em todos os trechos do itinerário. Se qualquer trecho não tiver disponibilidade, a reserva inteira falha.
- Cancelamentos só podem ser realizados pelo usuário autorizado e uma operação de cancelamento repetida falha.

Os objetos retornados em listas seguem os getters dos modelos Java. Um `Itinerario` contém `trechos` e `precoTotal`; cada trecho contém `caronaId`, `data`, `cidadeOrigem`, `cidadeDestino`, `motorista`, `totalLugares`, `lugaresDisponiveis`, `preco` e `passageiros`. Uma `Carona` contém, entre outros, `id`, `motorista`, `rota`, `trechos`, `data`, `hora`, `vagasTotais`, `ativaOuNao` e `precoPorTrecho`. Senhas não são serializadas nas respostas.

## Fluxo de conexão, autenticação e desconexão

### Fluxo normal

```text
Cliente                         Servidor
   |                                |
   |--- abre TCP ------------------>|
   |--- PING ---------------------->|
   |<-- resposta DTOResponse -------|
   |--- CADASTRAR ----------------->| 
   |<-- resposta -------------------|
   |--- LOGIN --------------------->|
   |<-- token em dados -------------|
   |--- operações com token ------->|
   |<-- uma resposta por operação --|
   |--- LOGOUT -------------------->|
   |<-- confirmação ----------------|
   |--- fecha socket -------------->|
```

O servidor também encerra a conexão quando o cliente fecha o socket ou ocorre erro de I/O. Fechar o socket sem executar `LOGOUT` encerra o canal, mas não substitui a invalidação explícita da sessão.

## Exemplos de mensagens trocadas

Cada bloco abaixo representa uma linha enviada ou recebida.

### Teste de disponibilidade

Cliente -> servidor:

```json
{"acao":"PING"}
```

Servidor -> cliente:

```json
{"sucesso":true,"mensagem":"Pong! Servidor VaiJunto online."}
```

### Cadastro e login

Cliente -> servidor:

```json
{"acao":"CADASTRAR","login":"ana","senha":"segredo","tipoUser":"PASSAGEIRO"}
```

Servidor -> cliente:

```json
{"sucesso":true,"mensagem":"Usuário cadastrado com sucesso."}
```

Cliente -> servidor:

```json
{"acao":"LOGIN","login":"ana","senha":"segredo"}
```

Servidor -> cliente:

```json
{"sucesso":true,"mensagem":"Login efetuado com sucesso.","dados":{"token":"550e8400-e29b-41d4-a716-446655440000","tipoUser":"PASSAGEIRO"}}
```

### Oferta de carona

Cliente -> servidor:

```json
{"acao":"OFERECER_CARONA","token":"550e8400-e29b-41d4-a716-446655440000","data":"2026-10-05","hora":"08:30","rota":["Valentine","Rhodes","Saint Denis"],"precos":[25.0,30.0],"vagas":3,"ativaOuNao":true}
```

Servidor -> cliente:

```json
{"sucesso":true,"mensagem":"Carona publicada com sucesso.","dados":"7c3a1f1e-2c4a-4f3a-9d8b-123456789abc"}
```

### Busca e reserva

Cliente -> servidor:

```json
{"acao":"BUSCAR_VIAGENS","origem":"Valentine","destino":"Saint Denis","data":"2026-10-05"}
```

Servidor -> cliente (o índice `0` será usado na reserva):

```json
{"sucesso":true,"mensagem":"Itinerários encontrados","dados":[{"trechos":[{"caronaId":"7c3a1f1e-2c4a-4f3a-9d8b-123456789abc","data":"2026-10-05","cidadeOrigem":"VALENTINE","cidadeDestino":"RHODES","motorista":"joao","totalLugares":3,"lugaresDisponiveis":3,"preco":25.0},{"caronaId":"7c3a1f1e-2c4a-4f3a-9d8b-123456789abc","data":"2026-10-05","cidadeOrigem":"RHODES","cidadeDestino":"SAINT_DENIS","motorista":"joao","totalLugares":3,"lugaresDisponiveis":3,"preco":30.0}],"precoTotal":55.0}]}
```

Cliente -> servidor:

```json
{"acao":"RESERVAR","token":"550e8400-e29b-41d4-a716-446655440000","indiceItinerario":0}
```

Servidor -> cliente:

```json
{"sucesso":true,"mensagem":"Reserva confirmada com sucesso.","dados":"b9c1a2d3-e4f5-4678-9012-abcdefabcdef"}
```

### Cancelamento e erro

Cliente -> servidor:

```json
{"acao":"CANCELAR_RESERVA","token":"550e8400-e29b-41d4-a716-446655440000","idReserva":"b9c1a2d3-e4f5-4678-9012-abcdefabcdef"}
```

Servidor -> cliente:

```json
{"sucesso":true,"mensagem":"Reserva cancelada com sucesso."}
```

Cliente -> servidor:

```json
{"acao":"MINHAS_RESERVAS","token":"550e8400-e29b-41d4-a716-446655440000"}
```

Servidor -> cliente:

```json
{"sucesso":true,"mensagem":"Reservas encontradas","dados":[{"id":"b9c1a2d3-e4f5-4678-9012-abcdefabcdef","passageiroId":"...","loginPassageiro":"ana","trechos":[],"precoTotal":55.0,"ativa":true}]}
```

Cliente -> servidor:

```json
{"acao":"OPERACAO_INEXISTENTE"}
```

Servidor -> cliente:

```json
{"sucesso":false,"mensagem":"Ação desconhecida: OPERACAO_INEXISTENTE"}
```

# Topicos do barema:

## 1. Arquitetura detalhada

O VaiJunto utiliza uma arquitetura cliente-servidor centralizada.

O sistema é dividido em dois módulos principais:

- `vaijunto-server`: responsável por manter os usuários, sessões, caronas, trechos, itinerários e reservas.
- `vaijunto-client`: aplicação JavaFX utilizada pelos motoristas e passageiros.

O servidor é iniciado em `ServidorTCP.java`. Ele abre a porta TCP `8080`, aguarda conexões e cria um `ClientHandler` para cada cliente conectado.

O estado do sistema é mantido em memória pelo objeto `GegenciarGeral.java`, que centraliza os serviços:

- `UserService.java`: usuários e sessões;
- `CaronaService.java`: caronas oferecidas;
- `ReservaService.java`: itinerários e reservas;
- `Grafo.java`: mapa de cidades e trechos.

### Modelo de dados

A carona é representada por `Carona.java`. Ela contém:

- identificador;
- motorista;
- rota;
- data e hora;
- quantidade total de vagas;
- status de atividade;
- preço de cada trecho;
- lista de trechos individuais.

Uma rota é uma sequência de cidades, por exemplo:

```text
VALENTINE -> RHODES -> SAINT_DENIS
```

Essa rota gera dois trechos:

```text
VALENTINE -> RHODES
RHODES -> SAINT_DENIS
```

Cada trecho é representado por `Trecho.java`, que armazena:

- cidade de origem;
- cidade de destino;
- motorista;
- data;
- preço;
- quantidade total de lugares;
- lugares disponíveis;
- passageiros daquele trecho.

A reserva é representada por `Reserva.java`, contendo o passageiro, os trechos reservados, o preço total e o status da reserva.

O servidor mantém esses objetos em estruturas de dados em memória. Portanto, o estado é centralizado no processo do servidor, mas não há persistência em banco de dados.

---

## 2. Comunicação

A comunicação entre cliente e servidor utiliza sockets TCP/IP da biblioteca padrão Java:

```java
ServerSocket serverSocket = new ServerSocket(PORTA);
Socket socketCliente = serverSocket.accept();
```

Essa implementação está em `ServidorTCP.java`.

O servidor executa continuamente:

1. abre a porta `8080`;
2. aguarda um cliente com `accept()`;
3. recebe um `Socket`;
4. cria um `ClientHandler`;
5. executa o handler em uma nova thread.

Cada `ClientHandler` utiliza:

- `BufferedReader` para ler mensagens;
- `PrintWriter` para enviar respostas;
- `Socket` para manter a conexão TCP.

O código principal está em `ClientHandler.java`.

A conexão é persistente. Isso significa que o cliente pode enviar várias requisições no mesmo socket:

```text
LOGIN
BUSCAR_VIAGENS
RESERVAR
MINHAS_RESERVAS
LOGOUT
```

Cada mensagem é enviada em uma única linha JSON. O servidor usa `readLine()` para identificar o fim da mensagem.

Quando o cliente encerra a conexão, o `readLine()` retorna `null`. Nesse caso, o loop termina, os streams são fechados e o socket é encerrado no bloco `finally`.

O cliente utiliza a classe `VaiJuntoClient.java`, que encapsula a criação do socket, o envio das mensagens e o recebimento das respostas.

### TCP

O TCP foi escolhido porque o sistema precisa de:

- entrega confiável das mensagens;
- preservação da ordem das requisições;
- conexão persistente;
- retransmissão automática em caso de perda de pacotes;
- controle de fluxo entre cliente e servidor.

---

## 3. Protocolo de API remota

O protocolo remoto é baseado em JSON. As classes que definem o formato são `DTORequest.java` e `DTOResponse.java`.

### Formato da requisição

Uma requisição possui o campo obrigatório `acao` e outros campos dependentes da operação:

```json
{
  "acao": "LOGIN",
  "login": "joao",
  "senha": "1234"
}
```

### Fluxo de conexão e autenticação

O fluxo normal é:

```text
Cliente                         Servidor
   |                                |
   |------ conexão TCP ------------>|
   |------ PING ------------------->|
   |<----- resposta ---------------|
   |------ CADASTRAR -------------->|
   |<----- resposta ---------------|
   |------ LOGIN ------------------>|
   |<----- token -------------------|
   |------ operação com token ----->|
   |<----- resposta ---------------|
   |------ LOGOUT ----------------->|
   |<----- confirmação -------------|
   |------ fecha socket ------------>|
```

O servidor não aceita operações protegidas sem um token válido. O token é criado no login e armazenado em `UserService.ativos`.

A busca e a reserva possuem uma regra de sincronização específica: o cliente deve realizar `BUSCAR_VIAGENS` antes de `RESERVAR`. O servidor guarda a última busca em `ultimabusca`, dentro do `ClientHandler`, e a reserva usa o índice enviado pelo cliente.

---

## 4. Encapsulamento

O encapsulamento permite que o cliente e o servidor troquem objetos Java por meio de texto JSON.

A conversão é feita em `JsonUtil.java`, utilizando a biblioteca Gson.

O processo é:

1. o cliente cria um `DTORequest`;
2. o objeto é convertido em JSON;
3. o JSON é enviado como texto pelo socket;
4. o servidor recebe a linha;
5. o servidor converte o JSON novamente para `DTORequest`;
6. processa a operação;
7. cria um `DTOResponse`;
8. transforma a resposta em JSON;
9. envia a resposta ao cliente.

Por exemplo, no cliente:

```java
writer.println(JsonUtil.paraJson(request));
```

No servidor:

```java
request = JsonUtil.paraObject(mensagemRecebida, DTORequest.class);
```

As enumerações de cidades e tipos de usuário também são convertidas durante o parsing. A classe `Cidades.java` aceita tanto o nome da constante quanto o nome amigável:

```text
SAINT_DENIS
Saint Denis
```

Se o JSON estiver malformado, o servidor captura o erro e responde:

```json
{
  "sucesso": false,
  "mensagem": "JSON inválido: ..."
}
```

Também existem validações para:

- campos obrigatórios;
- datas inválidas;
- horários inválidos;
- cidades inexistentes;
- rotas com menos de duas cidades;
- preços negativos;
- quantidade incorreta de preços;
- tokens ausentes ou inválidos;
- índices de itinerário inexistentes.

As senhas dos usuários possuem o modificador `transient` em `Usuario.java`, evitando que sejam serializadas nas respostas.

---

## 5. Busca de itinerários

A busca é baseada em um grafo direcionado implementado em `Grafo.java`.

Nesse grafo:

- cada cidade é um vértice;
- cada trecho de carona é uma aresta direcionada;
- a origem do trecho é o vértice inicial;
- o destino do trecho é o vértice final.

Quando uma carona ativa é criada, seus trechos são adicionados ao grafo:

```java
grafo.addTrechos(carona.getTrechos());
```

Quando a carona é cancelada, os trechos são removidos:

```java
grafo.removeTrechosDaCarona(id);
```

A busca utiliza DFS, ou busca em profundidade. O método `buscaCaminhoDFS()` percorre os trechos disponíveis a partir da origem até chegar ao destino.

Durante a busca, são verificados:

- data do trecho;
- existência de vagas;
- cidade de destino disponível;
- cidade ainda não visitada;
- origem diferente do destino;
- cidade não bloqueada.

### Combinação de motoristas diferentes

Como o grafo é composto por trechos de todas as caronas ativas, um itinerário pode combinar trechos de motoristas diferentes.

Por exemplo:

```text
Trecho 1: Valentine -> Rhodes, motorista João
Trecho 2: Rhodes -> Saint Denis, motorista Maria
```

O resultado será um único `Itinerario` contendo os dois trechos.

A classe `Itinerario.java` calcula o preço total somando os preços de todos os trechos.

### Ordenação das opções

No código atual, os itinerários são encontrados na ordem da travessia DFS e na ordem em que os trechos aparecem nas listas de adjacência do grafo. Não existe uma ordenação explícita por menor preço, menor número de conexões ou horário.

Portanto, a implementação atual garante a busca dos caminhos válidos, mas não possui um critério adicional de ordenação das opções apresentadas.

---

## 6. Concorrência

O servidor atende vários clientes ao mesmo tempo utilizando uma thread por conexão.

Em `ServidorTCP.java`, cada conexão gera:

```java
ClientHandler handler = new ClientHandler(socketCliente, gerenciador);
new Thread(handler).start();
```

Dessa forma, um cliente realizando uma busca não bloqueia completamente os demais clientes.

As estruturas compartilhadas usam mecanismos de concorrência:

- `ConcurrentHashMap` para usuários;
- `ConcurrentHashMap` para sessões;
- `ConcurrentHashMap` para caronas;
- `ConcurrentHashMap` para reservas.

Também existem métodos e blocos `synchronized` nos pontos que modificam o estado crítico:

- inclusão e remoção de trechos no grafo;
- criação de caronas;
- mudança do tipo de usuário;
- reserva de lugares;
- cancelamento de reserva;
- atualização da quantidade de vagas.

O cliente JavaFX também evita bloquear a interface. As chamadas de rede são executadas em uma thread separada por `ClienteTask.java`.

### Mecanismo de desempenho

O servidor usa o modelo thread-per-connection. Não foi implementado um `ExecutorService` ou thread pool no servidor principal. Portanto, cada conexão cria uma nova thread, o que é simples e adequado para o projeto, mas pode ter custo elevado com um número muito grande de clientes.

Os testes utilizam `ExecutorService` para simular múltiplos clientes, mas isso é utilizado no ambiente de teste, não como mecanismo de execução do servidor.

---

## 7. Atomicidade da reserva

A reserva de um itinerário com vários trechos é implementada em `ReservaService.java`.

O método `efetuarReserva()` possui duas fases:

### Fase de validação

Primeiro, todos os trechos são examinados:

```java
for (Trecho trecho : trechos) {
    if (trecho.getLugaresDisponiveis() <= 0) {
        return null;
    }
}
```

Nesse momento, nenhuma vaga é alterada.

### Fase de confirmação

Somente se todos os trechos tiverem disponibilidade, o sistema efetiva a reserva:

```java
for (Trecho trecho : trechos) {
    trecho.reservarLugar(passageiro);
}
```

Todo o processo está protegido por:

```java
synchronized (this)
```

Isso significa que duas reservas não conseguem executar simultaneamente dentro do mesmo `ReservaService`.

Se o último trecho ficar sem vaga, o método retorna `null` antes de alterar qualquer trecho. Consequentemente, não ocorre reserva parcial.

### Bloqueios mútuos

O código não adquire locks dos trechos em uma ordem variável. Em vez disso, utiliza um lock global no objeto `ReservaService`. Assim, dois passageiros que tentam reservar os mesmos trechos não conseguem adquirir os trechos em ordens diferentes.

Essa solução reduz o risco de deadlock, porque a reserva é serializada pelo mesmo monitor. O custo é que reservas diferentes também podem esperar umas pelas outras, mesmo quando usam trechos independentes.

Os métodos de `Trecho.java`, como `reservarLugar()` e `liberarLugar()`, também são sincronizados para proteger a quantidade de vagas e a lista de passageiros.

---

## 8. Interação

O cliente possui uma interface JavaFX e classes responsáveis pela comunicação com o servidor.

A comunicação de baixo nível está em `VaiJuntoClient.java`. Essa classe possui métodos específicos para cada operação:

- `cadastro()`;
- `login()`;
- `logout()`;
- `oferecerCarona()`;
- `minhasCaronas()`;
- `consultaPassageiros()`;
- `cancelaCarona()`;
- `buscarViagens()`;
- `reservar()`;
- `cancelaReserva()`;
- `minhasReservas()`.

### Cliente motorista

A tela do motorista está em `MotoristaView.java`.

O motorista:

1. informa data e horário;
2. seleciona a rota;
3. define o preço de cada trecho;
4. informa a quantidade de vagas;
5. envia uma requisição `OFERECER_CARONA`;
6. recebe o identificador da carona;
7. consulta suas caronas;
8. consulta os passageiros confirmados por trecho;
9. pode cancelar a carona.

A consulta de passageiros retorna uma estrutura como:

```json
{
  "VALENTINE -> RHODES": ["ana", "carlos"],
  "RHODES -> SAINT_DENIS": ["ana"]
}
```

Isso permite que o motorista saiba quais passageiros estão ocupando cada trecho.

### Cliente passageiro

A tela do passageiro está em `PassageiroView.java`.

O passageiro:

1. informa origem, destino e data;
2. executa uma busca;
3. recebe a lista de itinerários;
4. escolhe um índice;
5. confirma a reserva;
6. consulta suas reservas;
7. cancela uma reserva quando necessário.

As chamadas são executadas fora da thread da interface por meio de `ClienteTask.java`, evitando que a interface congele enquanto aguarda o servidor.

---

## 9. Confiabilidade

O cliente possui timeout de leitura configurado no socket:

```java
socket.setSoTimeout(timeoutMillis);
```

O valor padrão utilizado pelo cliente é de 10 segundos. Isso impede que uma leitura fique bloqueada indefinidamente esperando uma resposta.

O cliente também verifica:

- host vazio;
- porta fora do intervalo válido;
- timeout inválido;
- falha ao enviar;
- resposta nula;
- JSON de resposta inválido.

Quando o servidor encerra a conexão sem responder, o cliente lança uma `IOException`.

No servidor, erros de entrada e saída são tratados em `ClientHandler.java`. O servidor registra o erro e fecha o socket no bloco `finally`.

### Encerramento durante uma reserva

A reserva é processada inteiramente no servidor. Portanto, se o cliente for encerrado antes de a requisição chegar, nenhuma vaga é alterada. Se o servidor já tiver confirmado a reserva antes de o cliente cair, a reserva permanece registrada no servidor e pode ser cancelada posteriormente.

A implementação não possui uma transação distribuída entre cliente e servidor nem um sistema de expiração automática de reservas. Também não há timeout de reserva que libere automaticamente o assento caso o cliente desapareça depois da confirmação.

Assim, a segurança contra reserva parcial é garantida pelo processamento atômico no servidor, mas a liberação automática de assentos abandonados após uma queda ainda não está implementada.

---

## 10. Testes

O principal teste automático está em `ConcorrenciaTest.java`.

Esse teste cria um servidor de teste e clientes TCP reais, permitindo verificar o comportamento da aplicação sob concorrência.

### Cadastro simultâneo de usuários diferentes

O teste cria 100 clientes simultâneos e verifica se todos conseguem cadastrar logins diferentes.

Isso valida:

- aceitação de múltiplas conexões;
- criação de múltiplos handlers;
- funcionamento do cadastro concorrente;
- ausência de perda de requisições.

### Cadastro simultâneo do mesmo login

O teste cria 20 requisições usando o mesmo login e verifica se apenas uma consegue realizar o cadastro.

Isso confirma o funcionamento de:

```java
usuarios.putIfAbsent(login, newUser);
```

Como o mapa é um `ConcurrentHashMap`, somente uma thread consegue inserir o usuário, enquanto as demais recebem falha de login duplicado.

### Reservas simultâneas

O teste cria:

- uma carona com três vagas;
- 12 passageiros;
- 12 buscas simultâneas;
- 12 tentativas de reserva simultâneas.

Ao final, verifica que exatamente três reservas foram confirmadas:

```java
assertEquals(vagas, reservasConfirmadas);
```

Esse teste valida a atomicidade da reserva e demonstra que o número de passageiros confirmados não ultrapassa o número de vagas disponíveis.

O teste também utiliza:

- `ExecutorService`;
- `CountDownLatch`;
- `Future`;
- `TimeUnit`.

Esses componentes permitem sincronizar o início das requisições e criar uma situação real de disputa por vagas.

### Desempenho

O teste mede indiretamente a capacidade de resposta por meio de timeouts de 10 e 15 segundos, mas não registra métricas detalhadas de latência, vazão ou uso de CPU.

Portanto, há um teste de corretude sob concorrência, mas não existe atualmente um benchmark completo de desempenho com relatório de throughput ou tempo médio por requisição.

---

## 11. Emulação com Docker

O projeto possui um Dockerfile para o servidor em `vaijunto-server/Dockerfile`.

Esse arquivo:

1. utiliza uma imagem Maven com Java 21;
2. copia o `pom.xml`;
3. copia o código-fonte;
4. executa o build Maven;
5. inicia o servidor pelo JAR executável.

O servidor pode ser executado em um contêiner com a porta TCP `8080` publicada para o host.

Também existe um Dockerfile para o cliente em `vaijunto-client/Dockerfile`. Ele instala as bibliotecas gráficas necessárias ao JavaFX, compila o cliente e configura a variável `DISPLAY`.

O host e a porta do servidor podem ser configurados por:

```text
VAIJUNTO_SERVER_HOST
VAIJUNTO_SERVER_PORT
```

Isso é importante quando o cliente está em um contêiner separado do servidor.

### Conectividade entre contêineres

O código atual permite configurar o endereço do servidor por variável de ambiente. Assim, quando cliente e servidor estão na mesma máquina, pode-se usar:

- uma rede Docker compartilhada;
- o nome do serviço ou contêiner como host;
- a porta `8080` para comunicação.

Quando os contêineres estão em máquinas distintas, o cliente deve utilizar o endereço IP ou DNS da máquina onde o servidor está executando, e a porta `8080` precisa estar publicada e liberada no firewall.

O projeto não contém atualmente um `docker-compose.yml`, uma configuração automática de rede entre máquinas ou um mecanismo próprio de descoberta de servidores. Portanto, a conectividade entre máquinas distintas depende da configuração externa da rede, da publicação da porta e das variáveis `VAIJUNTO_SERVER_HOST` e `VAIJUNTO_SERVER_PORT`.

### Vantagens da abordagem

O uso de Docker oferece:

- ambiente Java padronizado;
- reprodução do build em diferentes máquinas;
- isolamento entre cliente e servidor;
- facilidade para iniciar várias instâncias;
- redução de problemas de dependências;
- possibilidade de testar a comunicação em uma rede semelhante à do laboratório.

## Conclusão

O código atende diretamente aos principais requisitos de arquitetura, comunicação TCP, protocolo JSON, encapsulamento, autenticação, busca em grafo, concorrência, atomicidade de reservas e interação com os clientes.

Os pontos já implementados com maior destaque são:

- servidor central com múltiplos clientes;
- comunicação persistente por sockets TCP;
- protocolo JSON com DTOs;
- autenticação por token;
- modelagem de caronas em trechos;
- busca de itinerários usando DFS;
- combinação de trechos de motoristas diferentes;
- reserva atômica de itinerários;
- proteção contra excesso de reservas;
- testes automáticos de concorrência;
- configuração para execução em Docker.

Como limitações atuais, o código não possui:

- persistência em banco de dados;
- thread pool no servidor principal;
- ordenação explícita dos itinerários;
- expiração automática de reservas;
- recuperação automática após queda do cliente;
- `docker-compose`;
- configuração automática de rede entre máquinas;
- benchmark detalhado de desempenho.
