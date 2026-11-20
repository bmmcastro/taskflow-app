# TaskFlow — App Android (UC00609)

Aplicação Android em Kotlin para gerir projetos e tarefas em equipa. Usa a API
TaskFlow da **UC00605 — Programar para a web (servidor)**, disponível em
<https://taskflow.algarit.pt>. As contas e os dados são os mesmos do site.

## Abrir e executar

1. No Android Studio, escolher **File → Open** e abrir esta pasta.
2. Aguardar a sincronização do Gradle e a instalação dos componentes pedidos.
3. Executar num emulador ou telemóvel com Android 7.0 ou superior (minSdk 24).

O projeto usa compileSdk e targetSdk 37. O Gradle Wrapper define a versão do
Gradle e o ficheiro `gradle/gradle-daemon-jvm.properties` pede o JDK 25.
Não há um caminho de JDK fixo em `gradle.properties`.
O SDK local é indicado em `local.properties`, que fica fora do Git.

Na linha de comandos do Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest lintDebug
```

A compilação release usa o ficheiro `taskflow.jks` e as propriedades
`TF_STORE_PASSWORD`, `TF_KEY_ALIAS` e `TF_KEY_PASSWORD` em `local.properties`.
A chave e as passwords não devem ser incluídas no Git. Não são necessárias
para executar a versão debug.

## Conta de demonstração

- Email: `demo@taskflow.pt`
- Password: `1234`

Também é possível criar uma conta no ecrã de registo.

## Funcionalidades

| Ecrã | O que permite fazer |
|---|---|
| Login e registo | Entrar ou criar uma conta |
| Projetos | Consultar e criar projetos; apagar os projetos de que se é criador |
| Quadro | Ver tarefas por estado, filtrar por responsável e criar tarefas |
| Detalhe da tarefa | Consultar, editar e apagar a tarefa; gerir a checklist de subtarefas |
| Membros | Consultar e gerir membros; criar um convite associado a um email |
| Convite | Colar o link ou token e aceitar o convite depois de iniciar sessão |
| Relatório | Ver totais, estados, atrasos, percentagem e tarefas por pessoa |
| Perfil | Alterar nome, email e password; terminar sessão |

No quadro, um toque abre a tarefa e um toque longo permite mudar o estado.
As tarefas aparecem ordenadas pelo prazo. Na checklist, tocar na caixa ou na
linha altera o estado; um toque longo na linha apaga a subtarefa.

O botão **Tenho um convite** existe no login e nos projetos. Se for necessário
iniciar sessão ou criar conta, o convite é retomado a seguir. Os convites são
partilhados por link: a app pede à API `enviar_email = false`, por isso não envia
automaticamente um email ao convidado.

## Organização do código

```text
app/src/main/java/com/algarit/taskflow/
├── MainActivity.kt   Activity principal, toolbar e NavHost
├── TaskFlowApp.kt    Contexto da aplicação para as mensagens de erro
├── api/             Retrofit, endpoints e sessão local
├── data/            Repo: carregamento dos dados do quadro
├── model/           Modelos dos dados, pedidos e estados das tarefas
├── ui/              Fragments, ViewModels, UiState e ConviteActivity
└── adapter/         Listas de projetos, tarefas e subtarefas
```

Os layouts estão em `res/layout` e o grafo de navegação em
`res/navigation/nav_graph.xml`. Alguns layouts mantêm o prefixo `activity_`,
mas são usados pelos Fragments. O convite tem uma Activity própria.

## Interface

A interface tem uma estética inspirada nas aplicações Apple: títulos grandes,
fundo neutro, cartões arredondados e azul nas ações principais. O quadro usa
um seletor de estados e os formulários permitem deslocar o conteúdo quando o
teclado está aberto. O tema adapta as cores ao modo claro e escuro.
São usados os tipos de letra e os componentes do Android, sem bibliotecas
visuais adicionais. Os estilos comuns ficam em `res/values/themes.xml`.

## Decisões técnicas

- **Interface:** XML, Material 3 e ViewBinding, com cores para tema claro e escuro.
- **Navegação:** Navigation Component e Safe Args para passar os identificadores.
- **Dados dos ecrãs:** os ViewModels usam coroutines e expõem LiveData. O
  `UiState` representa os estados parado, a carregar, sucesso e erro.
- **Rede:** Retrofit e Gson para pedidos e respostas JSON. Os endpoints que
  devolvem texto usam `ResponseBody`, fechado depois do pedido.
- **Quadro:** o `Repo` carrega membros e tarefas em paralelo com `async/await`.
  Os restantes ViewModels chamam diretamente o serviço Retrofit.
- **Listas:** os adaptadores de projetos, tarefas e subtarefas usam
  `ListAdapter` e `DiffUtil` para atualizar os itens que mudaram.
- **Datas:** seguem o formato ISO na API e aparecem como `dd/MM/aaaa` na app.
  Os prazos ultrapassados aparecem a vermelho quando a tarefa não está concluída.

## Sessão e limitação da API

A app guarda o id, nome e email em `SharedPreferences`; não guarda a password.
Estes dados permitem recordar quem iniciou sessão, mas não são uma prova de
autenticação para o servidor. A API usada neste trabalho não fornece um token
de sessão ao cliente Android.

As verificações da interface, como permitir apagar um projeto apenas ao
criador, não substituem a autorização no servidor. Para utilização fora do
contexto da formação, é necessário implementar autenticação e validação de
permissões no backend e adaptar os pedidos Android. Essa parte do servidor
não está neste repositório.

## Testes

Os testes unitários verificam a formatação de datas e o mapeamento JSON dos
modelos. Existe também um teste instrumentado da checklist, para confirmar
que tocar na checkbox chama a ação de guardar e que uma linha reutilizada
mostra o estado da subtarefa correta.

Para executar os testes instrumentados, ligar um dispositivo ou iniciar um
emulador e executar:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

O relatório do lint fica em `app/build/reports/lint-results-debug.html`.
Os testes locais não validam todos os endpoints da API. Na revisão manual,
convém confirmar estes casos com uma conta de teste:

1. Criar e editar uma tarefa: ao guardar com campos em falta, o formulário
   deve continuar aberto e mostrar o erro.
2. Marcar e desmarcar uma subtarefa pela caixa e pela linha; voltar a abrir
   a tarefa e confirmar que o estado e a percentagem foram guardados.
3. Abrir um convite sem sessão, iniciar sessão e confirmar que regressa ao
   convite. Repetir com uma conta criada no registo.
4. Abrir **Tenho um convite** nos projetos, consultar e aceitar um convite válido.
5. Consultar um convite inválido e testar a app sem ligação à Internet.

*Bruno Castro (algarIT) — UC00609, 2026.*
