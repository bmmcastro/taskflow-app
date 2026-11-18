# TaskFlow — Planeamento

**UC00609 — Desenvolver aplicações móveis (plataforma Android)** · Trabalho individual
**Autor:** Bruno Castro

## 1. A app

**Objetivo.** Uma app Android em Kotlin para gerir projetos e tarefas em equipa, estilo Trello. É o cliente móvel do **TaskFlow** (o projeto web da UC00605, em produção em `https://taskflow.algarit.pt`): a app consome a API REST do site, por isso uma tarefa movida no telemóvel aparece logo no site e vice-versa.

**Público-alvo.** Pequenas equipas com trabalho partilhado: trabalhos de escola, projetos de curso, pequenas empresas.

**Porque este tema.** As sugestões do enunciado usavam APIs públicas de demonstração; escolhi a API real do TaskFlow porque a conheço bem (fiz o recurso Tarefas no projeto web), tem casos reais (autenticação, validações, erros de negócio) e o resultado é útil de verdade.

**API.** REST, JSON, HTTPS, em `https://taskflow.algarit.pt`. A autenticação é a da própria API (id do utilizador nos pedidos, como o site faz no browser). Conta de demonstração: `demo@taskflow.pt / 1234`.

## 2. User stories e priorização (MoSCoW)

| ID | User story | MoSCoW |
|---|---|---|
| US01 | Como utilizador, quero entrar com a conta do site para aceder aos meus projetos. | Must |
| US02 | Como utilizador, quero criar conta nova na app. | Must |
| US03 | Como utilizador, quero ver os meus projetos e criar/apagar os meus. | Must |
| US04 | Como membro, quero ver o quadro com as três colunas e filtrar por pessoa. | Must |
| US05 | Como membro, quero criar, editar, mover e apagar tarefas com responsável e prazo. | Must |
| US06 | Como membro, quero gerir a checklist de subtarefas de uma tarefa com progresso. | Must |
| US07 | Como gestor, quero ver os membros, juntar utilizadores e convidar por email. | Should |
| US08 | Como convidado, quero aceitar um convite a partir do link. | Should |
| US09 | Como gestor, quero o relatório do projeto (contagens, percentagem, por pessoa). | Should |
| US10 | Como utilizador, quero editar o meu perfil e terminar sessão. | Should |
| US11 | Como utilizador, quero mensagens claras de erro (sem ligação, timeout, servidor) com repetição. | Should |
| US12 | Como membro, quero ver o prazo atrasado destacado no cartão. | Could |

**Won't have (v1):** base de dados local/modo offline (a API é a fonte da verdade), notificações push, envio de emails a partir da app (o convite gera o link; envia-se à mão), arrastar-e-largar cartões (v2).

## 3. Kanban

Fluxo: **Backlog → A fazer → Em curso (máx. 2) → Em revisão → Concluído**. O próprio quadro deste projeto está no TaskFlow (projeto "App TaskFlow Android (UC00609)") — as tarefas abaixo são as que lá ficaram registadas, com datas reais:

| Tarefa | Estado | Data |
|---|---|---|
| Fundação: API, sessão, login e registo | Concluída | 21/10 |
| Ecrã de projetos e quadro de tarefas | Concluída | 24/10 |
| Detalhe da tarefa com checklist | Concluída | 25/10 |
| Membros, convites, relatório e perfil | Concluída | 26/10 |
| Ícone da app e README | Concluída | 26/10 |
| Conversão para Navigation Jetpack (1 Activity + Fragments, Safe Args, Toolbar, ViewBinding, ListAdapter/DiffUtil) | Concluída | 28/10 |
| Retrofit com coroutines e MVVM (ViewModels, release assinado, testes) | Concluída | 28/10 |
| Sessão 11: paralelismo async/await, cancelamento, ProgressBars, Snackbar de retry | Concluída | 30/10 |
| Aplicar a revisão do formador (3 erros + melhorias) | Concluída | 03/11 |
| Atualizar relatório e manual | Em curso | 18/11 |
| Entregar no Moodle | Por fazer | 20/11 |

## 4. Wireframes e fluxo de navegação

O fluxo navegação: Login → Projetos (home) → Quadro → Detalhe; do Quadro abrem-se Membros e Relatório; o Perfil abre dos Projetos; o Convite é ponto de entrada autónomo (Activity). O gráfico final coincidiu com o planeado: uma Activity com Toolbar e NavHost, oito destinos no grafo, argumentos por Safe Args, botão voltar do sistema sempre funcional. (Wireframes originais em anexo à primeira entrega; o separador de colunas por tabs foi a adaptação a ecrã pequeno que se manteve.)

## 5. Requisitos técnicos (como cada exigência foi cumprida)

| Exigência | Implementação |
|---|---|
| Kotlin, ViewBinding, ConstraintLayout | sim; binding em todos os ecrãs e diálogos; itens em ConstraintLayout |
| RecyclerView + ListAdapter + DiffUtil + cliques | listas de projetos, tarefas, subtarefas e membros |
| Navigation (Jetpack) + Fragments + Safe Args + voltar | single-activity, grafo com 8 destinos, Toolbar ligada ao NavController |
| MVVM (ViewModel por ecrã) | 8 ViewModels + LiveData + UiState sealed (Idle/Loading/Sucesso/Erro) |
| Coroutines + ProgressBar | viewModelScope, async/await em paralelo no Repo, withContext(IO), cancelamento; indicadores em todos os ecrãs |
| Networking + vários endpoints + erros | Retrofit+Gson, 18 endpoints; mensagens distintas sem-ligação/timeout/servidor |
| data class / enum / sealed class | modelos Gson; EstadoTarefa; UiState |
| Testes JUnit | 9 testes (datas + modelos), todos a passar |
| Dispositivo físico + emulador | Xiaomi (Android 16) e emulador API 35 |
| Publicação/normas | keystore própria, senhas no local.properties (fora do git), RGPD em baixo |
| Documentação | este planeamento, relatório técnico e manual de utilizador |

**RGPD:** a app recolhe nome, email e palavra-passe (contas) e o conteúdo dos projetos — os mesmos dados do site, com o mesmo responsável (algarit.pt), política de privacidade do site aplicável. No dispositivo guarda apenas o id/nome da sessão (SharedPreferences). Uma única permissão: `INTERNET`, justificada pelo consumo da API. `allowBackup="false"` para os dados de sessão não irem para backup da cloud.

## 6. Entregáveis

1. Código-fonte: **github.com/bmmcastro/taskflow-app** (commits por fases, com README)
2. Este documento de planeamento
3. Relatório técnico (`RELATORIO-UC00609`)
4. Manual de utilizador (`docs/MANUAL-UTILIZADOR`)

Prazo: Moodle até 23h55 de 20 de novembro de 2026.
