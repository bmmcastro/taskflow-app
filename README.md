# TaskFlow — App Android (UC00609)

App Android nativa (Kotlin) que é cliente da API TaskFlow criada na UC
**UC00605 — Programar para a web (servidor)** e publicada em
**https://taskflow.algarit.pt**. Mesma conta, mesmos dados, outra plataforma.

## Como abrir

1. Android Studio → **File → Open** → esta pasta.
2. Esperar pelo Gradle Sync (a primeira vez descarrega o Volley).
3. **Run** num emulador ou telemóvel (Android 7.0+ / minSdk 24).

> O JDK para compilar já está fixado no `gradle.properties`
> (`org.gradle.java.home`), por isso compila igual na linha de comandos:
> `gradlew assembleDebug`.

## Conta de demonstração

- Email: `demo@taskflow.pt`
- Password: `1234`

(ou criar conta no ecrã de registo — é a mesma base de dados do site)

## O que a app faz

| Ecrã | Função |
|---|---|
| Login / Registo | entrar ou criar conta (`POST /utilizadores/login`, `/registo`) |
| Projetos | projetos onde sou membro; criar; apagar (só o criador) |
| Quadro | 3 colunas (Por fazer / Em curso / Concluída); criar, editar e mover tarefas; filtro por pessoa; ordenado por prazo |
| Detalhe da tarefa | informação, editar, apagar e checklist de subtarefas com barra de progresso |
| Membros | quem está no projeto, adicionar registados, convidar por email (link com token), convites pendentes |
| Convite | colar o link de convite e aceitar (fica membro do projeto) |
| Relatório | total, concluídas, em curso, atrasadas, percentagem e tarefas por pessoa |
| Perfil | mudar nome/email/password e terminar sessão |

## Como está organizada

```
app/src/main/java/com/algarit/taskflow/
├── api/      Api.kt (Volley, chamadas HTTP), Sessao.kt (SharedPreferences)
├── model/    Modelos.kt (Utilizador, Projeto, Tarefa, Subtarefa)
├── ui/       As 9 activities (Login, Registo, Projetos, Quadro,
│             DetalheTarefa, Membros, Convite, Relatorio, Perfil) + Ui.kt
└── adapter/  Adaptadores dos RecyclerViews (Projetos, Tarefas, Subtarefas)
```

## Notas técnicas (para a defesa)

- **HTTP com Volley** (`StringRequest` + `org.json`), base URL
  `https://taskflow.algarit.pt`. A API devolve JSON *ou* texto simples; o
  `Api.kt` interpreta os dois e entrega sempre o mesmo tipo `Resposta`.
- **Sem tokens**: a API da UC00605 não usa sessões/JWT; a app guarda o
  utilizador em `SharedPreferences` (papel do `localStorage` no site) e envia
  o `utilizador_id` / `projeto_id` onde a API espera.
- **UI**: Kotlin + XML + Material 3, tema claro/escuro, estilo minimalista
  (fundo cinzento `#F2F2F7`, cartões brancos arredondados, acento roxo
  `#6D28D9` — a cor do site).
- **Datas**: guardadas em ISO (`2026-11-02`), mostradas como `02/11/2026`;
  prazo a vermelho quando passa da data sem estar concluída.

*Projeto individual de Bruno Castro (algarIT) — UC00609, 2026.*
