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
| Login / Registo | entrar com a conta do site ou criar conta nova |
| Projetos | ver os projetos onde sou membro, criar, apagar (só o criador pode) |
| Quadro | as três colunas (Por fazer, Em curso, Concluída); criar, editar e mover tarefas; filtrar por pessoa |
| Detalhe da tarefa | ver e editar a tarefa, e gerir a checklist de subtarefas com barra de progresso |
| Membros | quem está no projeto, adicionar utilizadores registados, convidar por email, ver convites pendentes |
| Convite | quem recebe convite cola o link e aceita, ficando membro |
| Relatório | total, concluídas, em curso, atrasadas, percentagem e tarefas por pessoa |
| Perfil | mudar nome, email e password; terminar sessão |

## 3. Como está organizada

O projeto segue a organização habitual de uma app Android em Kotlin:

```
app/src/main/java/com/algarit/taskflow/
├── api/      Api.kt (todas as chamadas HTTP) e Sessao.kt (quem está com sessão iniciada)
├── model/    Modelos.kt (Utilizador, Projeto, Tarefa, Subtarefa — um data class por tabela)
├── ui/       Uma activity por ecrã + Ui.kt (funções de ajuda usadas em todos os ecrãs)
└── adapter/  Os adaptadores das listas (RecyclerView)
```

Escolhi separar assim para ser fácil de explicar e de mexer: quem quer perceber como a app fala com o servidor só tem de ler o `Api.kt`, e quem quer perceber um ecrã abre a activity correspondente.

## 4. A ligação à API

Toda a comunicação passa por um único objeto, o `Api.kt`. Ele usa a biblioteca **Volley** (da Google) e expõe quatro métodos — `get`, `post`, `patch` e `delete` — que recebem o caminho, os dados, e uma função para entregar a resposta. Todas as activities usam estes quatro métodos, nunca falam com a rede diretamente.

Um detalhe que me deu algum trabalho: a API da UC00605 não devolve sempre JSON. Os métodos que criam coisas devolvem objetos ou listas, mas os que apagam devolvem uma frase de texto ("Membro removido do projeto"). Por isso o `Api.kt` tenta interpretar a resposta como objeto JSON, depois como lista, e se não der entrega o texto como veio. Assim o resto da app recebe sempre o mesmo tipo `Resposta` e não tem de se preocupar com isso.

Sobre a autenticação: a API não usa tokens nem sessões — o site guarda o utilizador no `localStorage` e envia o id onde é preciso. Na app faço exatamente o mesmo, só que em `SharedPreferences`: depois do login guardo o id, nome e email, e é esse id que envio nos pedidos (por exemplo, `GET /projetos?utilizador_id=3`).

Os endpoints que a app usa são estes:

| Ecrã | Endpoints |
|---|---|
| Login / Registo | POST /utilizadores/login, POST /utilizadores/registo |
| Projetos | GET, POST, DELETE /projetos |
| Quadro | GET, POST, PATCH, DELETE /tarefas (com ?projeto_id=) |
| Detalhe | GET, POST, PATCH, DELETE /subtarefas (com ?tarefa_id=) |
| Membros | GET, POST /projetos/:id/membros, POST /projetos/:id/convidar, GET /projetos/:id/convites |
| Convite | GET /convites/:token, POST /convites/:token/aceitar |
| Relatório | GET /tarefas/relatorio |
| Perfil | PATCH /utilizadores/:id |

## 5. Decisões que tomei

**Views em XML em vez de Jetpack Compose.** O projeto começou no template de Views e a matéria da UC usa essa abordagem, por isso mantive. É mais verboso mas dá para seguir bem o que está a acontecer.

**Volley em vez de Retrofit.** Para uma app deste tamanho, o Volley chega perfeitamente e tem menos configuração. Quatro métodos no `Api.kt` e está feito.

**Sem base de dados local (Room).** A API já é a fonte da verdade e está sempre acessível (é HTTPS público). Guardar cópias locais só iria complicar. A app carrega sempre fresco, com pull-to-refresh nas listas.

**Datas em ISO na rede, dd/mm/aaaa no ecrã.** A API espera `2026-11-02`; eu mostro `02/11/2026`. Para escolher a data uso o DatePickerDialog do sistema e converto para ISO antes de enviar.

**Prazo atrasado a vermelho.** Nas listas, uma tarefa cujo prazo já passou e que não está concluída mostra a data a vermelho — é um aviso rápido sem precisar de abrir a tarefa.

**Convidar sem enviar email.** No site o convite é enviado por email. Na app, por agora, chamo o endpoint com `enviar_email: false` e mostro o link do convite para partilhar à mão (mensagem, etc.). Assim a app não depende do servidor de email durante a demonstração. É a única função que funciona de forma diferente do site.

**Design simples.** Fundo cinzento claro, cartões brancos arredondados, títulos grandes e um único tom de roxo (a cor do site, #6D28D9) para os botões e destaques. Tema escuro incluído.

## 6. Dificuldades que encontrei

A maior não foi da app em si, foi da ferramenta. Quando comecei a compilar fora do Android Studio (para ter a certeza que o projeto estava bem configurado), o build falhou com um erro estranho sobre um `jlink` que não existia. Demorei a perceber o que se passava, mas no fim eram três coisas em conjunto:

1. O meu JAVA_HOME apontava para um JDK com a barra final mal formatada, e o Gradle acabava por usar outro "Java" que encontrou no caminho — um JRE de uma extensão do VSCode, que não tem as ferramentas de JDK (por isso o jlink não existia).
2. O Gradle deteta sozinho JVMs espalhadas pelo computador e escolheu essa.
3. Para complicar, o Gradle guardou em cache a transformação que falhou, por isso mesmo depois de corrigir o Java o erro continuava igual.

Resolvi fixando o JDK correto no `gradle.properties` (`org.gradle.java.home`), desligando a deteção automática de JVMs (`org.gradle.java.installations.auto-detect=false`) e limpando as caches de transformação do Gradle. Foi uma boa lição sobre toolchains: o problema nem sempre está onde parece estar.

Na própria app, o outro desafio foi o `PATCH` das tarefas: a API exige que venham sempre o título, o responsável e o prazo, mesmo quando só quero mudar o estado. Por isso, mover uma tarefa de coluna reenvia a tarefa completa com o estado novo.

## 7. Como testar

1. Abrir o projeto no Android Studio e fazer Run (emulador ou telemóvel, Android 7.0 ou superior).
2. No login, usar a conta de demonstração: **demo@taskflow.pt / 1234** (ou criar conta — é a mesma base de dados do site).
3. Verificar contra o site: criar uma tarefa na app e vê-la aparecer no quadro do site, e vice-versa.

## 8. Conclusão

Ficou feita a app Android do TaskFlow, ligada à mesma API do projeto da UC00605, com as funções principais todas a funcionar: contas, projetos, quadro com as três colunas, tarefas com prazo e responsável, subtarefas com progresso, membros e convites, e relatório. Para além do resultado, o projeto deu-me prática real em consumir uma API REST em Android, em organizar um projeto com várias activities, e a resolver problemas de ferramentas que não são da app mas que fazem parte do trabalho de todos os dias.

Como melhorias futuras ficam: arrastar cartões entre colunas em vez do menu, notificações para prazos a aproximarem-se, e enviar os convites por email diretamente da app.

---

*Nota antes de entregar: reler este relatório, ajustar o que soar mal e acrescentar pormenores meus (por exemplo, algo que me tenha surpreendido no desenvolvimento) — na defesa quem responde sou eu.*
