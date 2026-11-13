-- =====================================================================
-- FarmaciaDB — Esquema PostgreSQL y Funciones Atómicas para Supabase
-- Ejecuta este script completo una sola vez en:
-- Supabase Dashboard -> SQL Editor -> New Query -> Run
-- =====================================================================

-- 1. Tabla de Usuarios (Login clásico sin Google)
CREATE TABLE IF NOT EXISTS public.farmacia_users (
  uid TEXT PRIMARY KEY,
  email TEXT NOT NULL UNIQUE,
  nombre TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('admin', 'empleado')),
  password_hash TEXT NOT NULL,
  created_at BIGINT NOT NULL
);

-- 2. Tabla de Medicamentos / Inventario
CREATE TABLE IF NOT EXISTS public.medicamentos (
  id TEXT PRIMARY KEY,
  codigo TEXT NOT NULL,
  nombre TEXT NOT NULL,
  principio TEXT NOT NULL,
  categoria TEXT NOT NULL,
  caducidad TEXT NOT NULL,
  precio_compra DOUBLE PRECISION NOT NULL,
  precio_venta DOUBLE PRECISION NOT NULL,
  stock INTEGER NOT NULL DEFAULT 0
);

-- 3. Tabla de Proveedores
CREATE TABLE IF NOT EXISTS public.proveedores (
  id TEXT PRIMARY KEY,
  empresa TEXT NOT NULL,
  contacto TEXT NOT NULL,
  telefono TEXT NOT NULL,
  correo TEXT NOT NULL,
  direccion TEXT NOT NULL
);

-- 4. Tabla de Clientes (Puntos Salud)
CREATE TABLE IF NOT EXISTS public.clientes (
  id TEXT PRIMARY KEY,
  nombre TEXT NOT NULL,
  telefono TEXT NOT NULL,
  nit TEXT NOT NULL,
  puntos INTEGER NOT NULL DEFAULT 0
);

-- 5. Tabla de Compras
CREATE TABLE IF NOT EXISTS public.compras (
  id TEXT PRIMARY KEY,
  codigo TEXT NOT NULL,
  fecha BIGINT NOT NULL,
  proveedor_id TEXT NOT NULL,
  med_id TEXT NOT NULL,
  cantidad INTEGER NOT NULL,
  costo DOUBLE PRECISION NOT NULL,
  estado TEXT NOT NULL CHECK (estado IN ('Completado', 'Pendiente')),
  nota TEXT
);

-- 6. Tabla de Ventas
CREATE TABLE IF NOT EXISTS public.ventas (
  uuid TEXT PRIMARY KEY,
  codigo TEXT NOT NULL,
  fecha BIGINT NOT NULL,
  subtotal DOUBLE PRECISION NOT NULL,
  descuento DOUBLE PRECISION NOT NULL,
  desc_pct DOUBLE PRECISION NOT NULL,
  iva DOUBLE PRECISION NOT NULL,
  total DOUBLE PRECISION NOT NULL,
  vendedor_nombre TEXT NOT NULL,
  vendedor_id TEXT,
  cliente_id TEXT,
  puntos INTEGER NOT NULL DEFAULT 0
);

-- 7. Detalle de Ítems por Venta
CREATE TABLE IF NOT EXISTS public.venta_items (
  item_id BIGSERIAL PRIMARY KEY,
  venta_id TEXT NOT NULL REFERENCES public.ventas(uuid) ON DELETE CASCADE,
  med_id TEXT NOT NULL,
  nombre TEXT NOT NULL,
  cantidad INTEGER NOT NULL,
  precio DOUBLE PRECISION NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_venta_items_venta_id ON public.venta_items (venta_id);

-- =====================================================================
-- Funciones Atómicas (RPC) para Inventario, Compras y Ventas POS
-- =====================================================================

CREATE OR REPLACE FUNCTION public.registrar_compra_farmacia(
  p_id TEXT,
  p_codigo TEXT,
  p_fecha BIGINT,
  p_proveedor_id TEXT,
  p_med_id TEXT,
  p_cantidad INTEGER,
  p_costo DOUBLE PRECISION,
  p_estado TEXT,
  p_nota TEXT DEFAULT NULL
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM public.medicamentos WHERE id = p_med_id) THEN
    RAISE EXCEPTION 'Medicamento no encontrado: %', p_med_id;
  END IF;

  IF p_estado = 'Completado' THEN
    UPDATE public.medicamentos
    SET stock = stock + p_cantidad
    WHERE id = p_med_id;
  END IF;

  INSERT INTO public.compras (id, codigo, fecha, proveedor_id, med_id, cantidad, costo, estado, nota)
  VALUES (p_id, p_codigo, p_fecha, p_proveedor_id, p_med_id, p_cantidad, p_costo, p_estado, p_nota);
END;
$$;

CREATE OR REPLACE FUNCTION public.recibir_compra_farmacia(p_compra_id TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_compra RECORD;
BEGIN
  SELECT * INTO v_compra FROM public.compras WHERE id = p_compra_id FOR UPDATE;
  IF NOT FOUND OR v_compra.estado <> 'Pendiente' THEN
    RETURN FALSE;
  END IF;

  UPDATE public.medicamentos
  SET stock = stock + v_compra.cantidad
  WHERE id = v_compra.med_id;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Medicamento asociado no encontrado: %', v_compra.med_id;
  END IF;

  UPDATE public.compras SET estado = 'Completado' WHERE id = p_compra_id;
  RETURN TRUE;
END;
$$;

CREATE OR REPLACE FUNCTION public.registrar_venta_farmacia(
  p_uuid TEXT,
  p_codigo TEXT,
  p_fecha BIGINT,
  p_subtotal DOUBLE PRECISION,
  p_descuento DOUBLE PRECISION,
  p_desc_pct DOUBLE PRECISION,
  p_iva DOUBLE PRECISION,
  p_total DOUBLE PRECISION,
  p_vendedor_nombre TEXT,
  p_vendedor_id TEXT,
  p_cliente_id TEXT,
  p_puntos INTEGER,
  p_items JSONB
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_item JSONB;
  v_med_stock INTEGER;
  v_med_id TEXT;
  v_nombre TEXT;
  v_cantidad INTEGER;
  v_precio DOUBLE PRECISION;
BEGIN
  FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
  LOOP
    v_med_id := v_item->>'med_id';
    v_nombre := v_item->>'nombre';
    v_cantidad := (v_item->>'cantidad')::INTEGER;

    SELECT stock INTO v_med_stock
    FROM public.medicamentos
    WHERE id = v_med_id
    FOR UPDATE;

    IF NOT FOUND THEN
      RAISE EXCEPTION 'Producto no encontrado: %', v_nombre;
    END IF;

    IF v_med_stock < v_cantidad THEN
      RAISE EXCEPTION 'Stock insuficiente para % (disp: %)', v_nombre, v_med_stock;
    END IF;

    UPDATE public.medicamentos
    SET stock = stock - v_cantidad
    WHERE id = v_med_id;
  END LOOP;

  IF p_cliente_id IS NOT NULL AND p_cliente_id <> '' AND p_puntos > 0 THEN
    UPDATE public.clientes
    SET puntos = puntos + p_puntos
    WHERE id = p_cliente_id;
  END IF;

  INSERT INTO public.ventas (
    uuid, codigo, fecha, subtotal, descuento, desc_pct, iva, total,
    vendedor_nombre, vendedor_id, cliente_id, puntos
  ) VALUES (
    p_uuid, p_codigo, p_fecha, p_subtotal, p_descuento, p_desc_pct, p_iva, p_total,
    p_vendedor_nombre, p_vendedor_id, NULLIF(p_cliente_id, ''), p_puntos
  );

  FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
  LOOP
    v_med_id := v_item->>'med_id';
    v_nombre := v_item->>'nombre';
    v_cantidad := (v_item->>'cantidad')::INTEGER;
    v_precio := (v_item->>'precio')::DOUBLE PRECISION;

    INSERT INTO public.venta_items (venta_id, med_id, nombre, cantidad, precio)
    VALUES (p_uuid, v_med_id, v_nombre, v_cantidad, v_precio);
  END LOOP;
END;
$$;

-- =====================================================================
-- Permisos de acceso REST para la app móvil (anon / authenticated)
-- =====================================================================
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;
GRANT EXECUTE ON FUNCTION public.registrar_compra_farmacia(TEXT, TEXT, BIGINT, TEXT, TEXT, INTEGER, DOUBLE PRECISION, TEXT, TEXT) TO anon, authenticated;
GRANT EXECUTE ON FUNCTION public.recibir_compra_farmacia(TEXT) TO anon, authenticated;
GRANT EXECUTE ON FUNCTION public.registrar_venta_farmacia(TEXT, TEXT, BIGINT, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, TEXT, TEXT, TEXT, INTEGER, JSONB) TO anon, authenticated;

ALTER TABLE public.farmacia_users DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.medicamentos DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.proveedores DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.clientes DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.compras DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.ventas DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.venta_items DISABLE ROW LEVEL SECURITY;
