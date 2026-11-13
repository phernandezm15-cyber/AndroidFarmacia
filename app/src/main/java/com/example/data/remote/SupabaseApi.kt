package com.example.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApi {
  @GET("rest/v1/farmacia_users")
  suspend fun findUserByEmail(
    @Query("email") emailFilter: String,
    @Query("select") select: String = "*",
    @Query("limit") limit: Int = 1,
  ): List<SupabaseUserRow>

  @GET("rest/v1/farmacia_users")
  suspend fun findUserById(
    @Query("uid") uidFilter: String,
    @Query("select") select: String = "*",
    @Query("limit") limit: Int = 1,
  ): List<SupabaseUserRow>

  @GET("rest/v1/farmacia_users")
  suspend fun getUsers(
    @Query("select") select: String = "*",
    @Query("order") order: String = "nombre.asc",
  ): List<SupabaseUserRow>

  @POST("rest/v1/farmacia_users")
  suspend fun insertUser(
    @Body user: SupabaseUserRow,
    @Header("Prefer") prefer: String = "return=representation",
  ): List<SupabaseUserRow>

  @PATCH("rest/v1/farmacia_users")
  suspend fun updateUserPasswordHash(
    @Query("uid") uidFilter: String,
    @Body patch: PasswordHashPatch,
    @Header("Prefer") prefer: String = "return=representation",
  ): List<SupabaseUserRow>

  @GET("rest/v1/medicamentos")
  suspend fun getMedicamentos(
    @Query("select") select: String = "*",
    @Query("order") order: String = "id.asc",
    @Query("limit") limit: Int? = null,
  ): List<SupabaseMedicamentoRow>

  @POST("rest/v1/medicamentos")
  suspend fun upsertMedicamentos(
    @Body meds: List<SupabaseMedicamentoRow>,
    @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
  ): List<SupabaseMedicamentoRow>

  @DELETE("rest/v1/medicamentos")
  suspend fun deleteMedicamento(
    @Query("id") idFilter: String,
  )

  @GET("rest/v1/proveedores")
  suspend fun getProveedores(
    @Query("select") select: String = "*",
    @Query("order") order: String = "id.asc",
  ): List<SupabaseProveedorRow>

  @POST("rest/v1/proveedores")
  suspend fun upsertProveedores(
    @Body provs: List<SupabaseProveedorRow>,
    @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
  ): List<SupabaseProveedorRow>

  @DELETE("rest/v1/proveedores")
  suspend fun deleteProveedor(
    @Query("id") idFilter: String,
  )

  @GET("rest/v1/clientes")
  suspend fun getClientes(
    @Query("select") select: String = "*",
    @Query("order") order: String = "id.asc",
  ): List<SupabaseClienteRow>

  @POST("rest/v1/clientes")
  suspend fun upsertClientes(
    @Body clientes: List<SupabaseClienteRow>,
    @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
  ): List<SupabaseClienteRow>

  @DELETE("rest/v1/clientes")
  suspend fun deleteCliente(
    @Query("id") idFilter: String,
  )

  @GET("rest/v1/compras")
  suspend fun getCompras(
    @Query("select") select: String = "*",
    @Query("order") order: String = "fecha.desc",
  ): List<SupabaseCompraRow>

  @POST("rest/v1/compras")
  suspend fun upsertCompras(
    @Body compras: List<SupabaseCompraRow>,
    @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
  ): List<SupabaseCompraRow>

  @GET("rest/v1/ventas")
  suspend fun getVentas(
    @Query("select") select: String = "*,venta_items(*)",
    @Query("order") order: String = "fecha.desc",
  ): List<SupabaseVentaRow>

  @POST("rest/v1/ventas")
  suspend fun upsertVentas(
    @Body ventas: List<SupabaseVentaRow>,
    @Header("Prefer") prefer: String = "resolution=ignore-duplicates,return=minimal",
  )

  @POST("rest/v1/venta_items")
  suspend fun insertVentaItems(
    @Body items: List<SupabaseVentaItemInsert>,
    @Header("Prefer") prefer: String = "return=minimal",
  )

  @POST("rest/v1/rpc/registrar_compra_farmacia")
  suspend fun registrarCompraRpc(
    @Body payload: RegistrarCompraRpcRequest,
  )

  @POST("rest/v1/rpc/recibir_compra_farmacia")
  suspend fun recibirCompraRpc(
    @Body payload: RecibirCompraRpcRequest,
  ): Boolean

  @POST("rest/v1/rpc/registrar_venta_farmacia")
  suspend fun registrarVentaRpc(
    @Body payload: RegistrarVentaRpcRequest,
  )
}
