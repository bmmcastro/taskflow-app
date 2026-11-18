# TaskFlow — App Android

**UC00609 — Desenvolver aplicações móveis (plataforma Android)**
**Autor:** Bruno Castro

## 1. Introdução

Na UC00605 (Programar para a web, vertente servidor) a nossa equipa desenvolveu o TaskFlow: uma aplicação web de gestão de tarefas em equipa, em Node.js + Express + PostgreSQL, publicada em https://taskflow.algarit.pt. Tem projetos, quadros estilo Trello com três colunas, tarefas com responsável e prazo, subtarefas, convites por email e relatórios.

Para esta UC decidi aproveitar esse trabalho em vez de começar um projeto novo de raiz: a minha app Android é um cliente da mesma API, com a mesma base de dados do site. Quem entra com a conta do site vê os mesmos projetos e as mesmas tarefas na app, e o que muda num lado aparece no outro. Foi esta a minha contribuição individual: levar o TaskFlow para Android nativo.

## 2. O que a app faz

A app cobre as funções principais do site:

| Ecrã | Função |
|---|---|
| Login / Registo | entrar com a conta do site ou criar conta nova (com botão de conta de demonstração) |
| Projetos | ver os projetos onde sou membro, criar, apagar (só o criador pode) |
| Quadro | as três colunas em tabs (Por fazer, Em curso, Concluída); criar, editar e mover tarefas; filtrar por pessoa com chips |
| Detalhe da tarefa | ver e editar a tarefa, e gerir a checklist de subtarefas com barra de progresso |
| Membros | quem está no projeto, adicionar utilizadores registados, convidar por email, ver convites pendentes |
| Convite | quem recebe convite cola o link e aceita, ficando membro |
| Relatório | total, concluídas, em curso, atrasadas, percentagem e tarefas por pessoa |
| Perfil | mudar nome, email e password; terminar sessão |

## 3. Como está organizada

A app segue a arquitetura **single-activity** com **Navigation (Jetpack)** e **MVVM**:

```
app/src/main/java/com/algarit/taskflow/
├── MainActivity.kt   a unica activity: Toolbar + NavHostFragment
├── ConviteActivity   ponto de entrada de quem recebe um convite (fora do grafo)
├── api/      ApiService.kt (interface Retrofit) e Api.kt (cliente + erros); Sessao.kt
├── model/    data classes (Utilizador, Projeto, Tarefa, Subtarefa, Relatorio, pedidos) + EstadoTarefa (enum)
├── data/     Repo.kt — camada de dados com coroutines (paralelismo do quadro)
├── ui/       um Fragment + um ViewModel por ecra; UiState (sealed); Ui.kt (ajudas)
└── adapter/  ListAdapter + DiffUtil das listas
```

A navegacao vive num **grafo** (`res/navigation/nav_graph.xml`) com 8 destinos e acoes; os ids de projeto e tarefa viajam por **Safe Args**, que gera classes com o tipo certo (erro em compilacao em vez de erro em runtime). A **Toolbar esta ligada ao NavController**: o titulo vem do grafo e a seta de voltar aparece sozinha nos ecra secundarios.

Cada ecra observa o seu ViewModel por **LiveData**, com um estado `UiState` (sealed class) em quatro casos: `Idle`, `Loading`, `Sucesso` e `Erro`. O `Idle` existe para os formularios: comecar em `Sucesso` fazia navegar assim que o ecra observasse o valor inicial (bug que o formador detetou e que corrigi).

## 4. A ligação à API

A comunicacao usa **Retrofit + Gson**: uma interface (`ApiService`) com todos os endpoints como funcoes **suspend**, e um cliente unico com timeouts (15 s de ligacao, 30 s de leitura). Os ViewModels chamam estas funcoes dentro de `viewModelScope.launch` — sem callbacks, e quando o ecra morre a coroutine e cancelada com ele.

Comecei o projeto com Volley e callback para cada pedido; migrei para Retrofit + coroutines quando chegamos a materia, e a diferenca nota-se: o codigo dos ecra ficou sequencial (parece sincrono mas nao bloqueia) e desapareceram os guardas de ciclo de vida que os callbacks exigiam.

Sobre a autenticao: a API nao usa tokens — o site guarda o utilizador no `localStorage` e envia o id onde e preciso. Na app faco o mesmo, em `SharedPreferences`.

Os endpoints que a app usa:

| Ecrã | Endpoints |
|---|---|
| Login / Registo | POST /utilizadores/login, POST /utilizadores/registo |
| Projetos | GET, POST, DELETE /projetos |
| Quadro | GET, POST, PATCH, DELETE /tarefas (com ?projeto_id=) |
| Detalhe | GET, POST, PATCH, DELETE /subtarefas (com ?tarefa_id=) |
| Membros | GET, POST /projetos/:id/membros, POST /projetos/:id/convidar, GET /projetos/:id/convites |
| Convite | GET /convites/:token, POST /convites/:token/aceitar |
| Relatorio | GET /tarefas/relatorio |
| Perfil | PATCH /utilizadores/:id |

## 5. Threads e coroutines (Sessão 11)

- Tudo o que demora corre fora da main thread — nunca `Thread.sleep`, nunca `runBlocking`, nunca `GlobalScope`.
- O quadro carrega membros e tarefas **em paralelo** com `async/await` dentro do `Repo.kt`, envolvido em `withContext(Dispatchers.IO)`: duas chamadas que custavam duas esperas passam a custar uma.
- O `QuadroViewModel` guarda o `Job` do carregamento e cancela o anterior ao recarregar (cancelamento cooperativo; sem Jobs sobrepostos a escrever no mesmo LiveData).
- Erros distintos para o utilizador: sem ligacao (`IOException`), timeout (`SocketTimeoutException`) e erro do servidor (`HttpException`) tem mensagens diferentes em `erroAmigavel`.
- Ecras que carregam dados mostram ProgressBar (Relatorio, Detalhe, Membros) ou SwipeRefreshLayout (Projetos, Quadro), ligados ao estado `Loading`.

## 6. Decisões que tomei

**Single-activity com Fragments em vez de Activities por ecra.** Foi a conversao que fiz quando chegamos a materia do Navigation: menos codigo repetido, transacoes e back stack geridos pelo NavController, Toolbar integrada. A `ConviteActivity` continua Activity de proposito — e o ponto de entrada de quem recebe um convite, fora do grafo.

**Retrofit + coroutines em vez de Volley** (razoes em cima). **MVVM com um ViewModel por ecra**: a logica saiu dos ecra; o ViewModel sobrevive a rotação.

**ListAdapter + DiffUtil nas listas**: submeter lista nova e so o que mudou e redesenhado, com animacoes. Cada modelo tem id estavel para o `areItemsTheSame`.

**Enum `EstadoTarefa`** em vez de constantes soltas: as tres colunas com rotulo, ordem e conversao a partir do texto da API.

**Sem base de dados local (Room).** A API e a fonte da verdade e esta sempre acessivel (HTTPS publico); a app carrega sempre fresco, com pull-to-refresh.

**Datas em ISO na rede, dd/mm/aaaa no ecra.** DatePickerDialog do sistema e conversao antes de enviar. **Prazo atrasado a vermelho** nas listas.

**Convidar sem enviar email.** Chamo o endpoint com `enviar_email: false` e mostro o link do convite para partilhar a mao — a app nao depende do servidor de email na demonstracao. E a unica funcao que funciona de forma diferente do site.

**Design simples.** Fundo cinzento claro, cartoes brancos arredondados, um tom de roxo (a cor do site) para botoes e destaques, tema escuro incluido.

## 7. Qualidade, testes e publicação

- **Testes unitarios (JUnit), 9/9 a passar**: formatacao de datas e o mapeamento JSON→modelos pelo Gson (o mesmo caminho que o Retrofit usa em producao).
- **Assinatura da build release**: keystore RSA 2048 criada para o projeto; as senhas vivem no `local.properties` (fora do git) e o `build.gradle.kts` le-as de la — nunca senhas no script versionado.
- **Testada no emulador e num telemovel fisico** (Xiaomi, Android 16): instalacao por USB e fluxo completo verificado.
- **Revisao do formador aplicada**: os tres erros que apontou (ciclo login-projetos pelo estado inicial `Sucesso`; convite aceite sem sessao; setas de voltar duplicadas) foram corrigidos, bem como as melhorias (enum do estado, mensagens de erro distintas, `setError` nas validacoes, `dimens.xml`, ConstraintLayout nos itens, ViewBinding tambem nos dialogos, senhas da keystore fora do build).

## 8. Dificuldades que encontrei

**A toolchain do Java (a maior).** Ao compilar fora do Android Studio, o build falhou com um erro de `jlink` inexistente. Causa: o Gradle detetou sozinho um JRE de uma extensao do VSCode (sem ferramentas de JDK) e usou-o, e ainda guardou a transformacao falhada em cache. Resolvi fixando o JDK no `gradle.properties`... e semanas depois o proprio fix me morreu: o Adoptium atualizou de versao, o caminho fixo deixou de existir e o projeto nao abria. Licao em dois tempos: (1) o problema nem sempre esta onde parece; (2) caminhos absolutos de JDK em projetos partilhados sao armadilha — a solucao final foi deixar o Gradle JDK do IDE (Embedded) mandar.

**O PATCH das tarefas.** A API exige titulo, responsavel e prazo em todos os PATCH, mesmo so para mudar o estado — mover uma tarefa reenvia-a completa com o estado novo.

**O Gson e os valores por omissao.** Um teste falhou porque uma tarefa sem `status` no JSON ficava com null em vez de "Por fazer": o Gson, sem construtor sem argumentos, preenche o objeto a marcar todos os campos. Correcao: valores por omissao em todos os parametros da data class — assim o Gson usa o construtor e o default aplica-se.

**UTF-8 na linha de comandos do Windows.** Ao criar o projeto desta app na API do TaskFlow (com curl), os textos acentuados ("Concluída") chegavam corrompidos ao servidor — o curl do Windows converte os argumentos pela code page ANSI. Correcao: enviar o JSON a partir de um ficheiro UTF-8 com `--data-binary @ficheiro`.

**Safe Args e AGP 9.** A versao 2.9.x do plugin recusa-se a aplicar com o AGP 9 ("must be used with android plugin"); tive de subir para a 2.10.2.

## 9. Como testar

1. Abrir o projeto no Android Studio e fazer Run (emulador ou telemovel, Android 7.0 ou superior).
2. No login, usar a conta de demonstracao: **demo@taskflow.pt / 1234** (ou criar conta — e a mesma base de dados do site).
3. Verificar contra o site: criar uma tarefa na app e ve-la aparecer no quadro do site, e vice-versa.

## 10. Conclusão

Ficou feita a app Android do TaskFlow, ligada a mesma API do projeto da UC00605, com as funcoes principais todas a funcionar: contas, projetos, quadro com as tres colunas, tarefas com prazo e responsavel, subtarefas com progresso, membros e convites, e relatorio. Pelo caminho a app foi reescrita duas vezes por razoes certas: primeiro para a arquitetura single-activity com Navigation e Safe Args, depois de Volley para Retrofit com coroutines e MVVM — e ainda absorveu a materia da sessao 11 (paralelismo, dispatchers, cancelamento) e uma revisao completa do formador. Para alem do resultado, o projeto deu-me pratica real em consumir uma API REST em Android, em evoluir uma app sem a partir, e a resolver problemas de ferramentas que nao sao da app mas fazem parte do trabalho de todos os dias.

Como melhorias futuras ficam: arrastar cartoes entre colunas em vez do menu, notificacoes para prazos a aproximarem-se, e enviar os convites por email diretamente da app.

---

*Nota antes de entregar: reler este relatorio, ajustar o que soar mal e acrescentar pormenores meus (por exemplo, algo que me tenha surpreendido no desenvolvimento) — na defesa quem responde sou eu.*
