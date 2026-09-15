# VaiJunto - Sistema de Caronas Compartilhadas

## Visão geral

O VaiJunto é um sistema cliente-servidor de caronas compartilhadas. O servidor central mantém usuários, sessões, caronas, itinerários e reservas; os clientes JavaFX oferecem as interfaces de motorista e passageiro.

As aplicações se comunicam por sockets TCP/IP usando mensagens JSON. O protocolo de aplicação é definido pelos DTOs `DTORequest` (cliente -> servidor) e `DTOResponse` (servidor -> cliente).

## Arquitetura e execução

O repositório possui dois módulos:

- `vaijunto-server/`: servidor TCP, regras de negócio e testes de concorrência;
- `vaijunto-client/`: cliente JavaFX.

Requisitos: Java 21, Maven, JavaFX, Gson e, opcionalmente, Docker.

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
   |--- CADASTRAR ----------------->|  (opcional)
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

## Testes

O módulo do servidor inclui testes de concorrência para múltiplos cadastros, exclusão de login duplicado, limite de vagas e prevenção de venda duplicada do mesmo assento:

```bash
cd vaijunto-server
mvn test
```

## Docker

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
