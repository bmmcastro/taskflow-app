package com.algarit.taskflow.api

import com.algarit.taskflow.model.*
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Endpoints da API do TaskFlow — os mesmos que o website usa.
// Todas as chamadas são suspend: correm em coroutines fora da main thread.
interface ApiService {

    @POST("utilizadores/login")
    suspend fun login(@Body pedido: LoginPedido): LoginResposta

    @POST("utilizadores/registo")
    suspend fun registo(@Body pedido: RegistoPedido): Utilizador

    @PATCH("utilizadores/{id}")
    suspend fun atualizarPerfil(@Path("id") id: Int, @Body pedido: PerfilPedido): Utilizador

    @GET("utilizadores")
    suspend fun utilizadores(): List<Utilizador>

    @GET("projetos")
    suspend fun projetos(@Query("utilizador_id") utilizadorId: Int): List<Projeto>

    @POST("projetos")
    suspend fun criarProjeto(@Body pedido: ProjetoPedido): Projeto

    @DELETE("projetos/{id}")
    suspend fun apagarProjeto(
        @Path("id") id: Int,
        @Query("utilizador_id") utilizadorId: Int
    ): ResponseBody

    @GET("projetos/{id}/membros")
    suspend fun membros(@Path("id") projetoId: Int): List<Utilizador>

    @POST("projetos/{id}/membros")
    suspend fun juntarMembro(@Path("id") projetoId: Int, @Body pedido: MembroPedido): ResponseBody

    @DELETE("projetos/{id}/membros/{utilizadorId}")
    suspend fun removerMembro(@Path("id") projetoId: Int, @Path("utilizadorId") utilizadorId: Int): ResponseBody

    @GET("projetos/{id}/convites")
    suspend fun convites(@Path("id") projetoId: Int): List<ConviteInfo>

    @GET("convites/{token}")
    suspend fun convitePorToken(@Path("token") token: String): ConviteDetalhe

    @POST("convites/{token}/aceitar")
    suspend fun aceitarConvite(@Path("token") token: String, @Body pedido: MembroPedido): ResponseBody

    @POST("projetos/{id}/convidar")
    suspend fun convidar(@Path("id") projetoId: Int, @Body pedido: ConvitePedido): ConviteResposta

    @GET("tarefas")
    suspend fun tarefas(@Query("projeto_id") projetoId: Int): List<Tarefa>

    @POST("tarefas")
    suspend fun criarTarefa(@Body pedido: TarefaPedido): Tarefa

    @PATCH("tarefas/{id}")
    suspend fun editarTarefa(@Path("id") id: Int, @Body pedido: TarefaPedido): Tarefa

    @DELETE("tarefas/{id}")
    suspend fun apagarTarefa(@Path("id") id: Int): ResponseBody

    @GET("tarefas/relatorio")
    suspend fun relatorio(@Query("projeto_id") projetoId: Int): RelatorioProjeto

    @GET("subtarefas")
    suspend fun subtarefas(@Query("tarefa_id") tarefaId: Int): List<Subtarefa>

    @POST("subtarefas")
    suspend fun criarSubtarefa(@Body pedido: SubtarefaPedido): Subtarefa

    @PATCH("subtarefas/{id}")
    suspend fun alternarSubtarefa(@Path("id") id: Int, @Body estado: SubtarefaEstado): Subtarefa

    @DELETE("subtarefas/{id}")
    suspend fun apagarSubtarefa(@Path("id") id: Int): ResponseBody
}
