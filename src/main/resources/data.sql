-- ---------------------------------------------------------------------------
-- Datos semilla para desarrollo y para la suite de pruebas de regresion.
-- Cada cliente representa deliberadamente una ruta distinta del diagnostico:
--   1 -> VIP con historial para el descuento por monto
--   2 -> FRECUENTE con mas de 10 pedidos previos (descuento 8%)
--   3 -> FRECUENTE con 4 a 10 pedidos previos (descuento 4%)
--   4 -> MOROSO con deuda pendiente (ruta de rechazo)
--   5 -> ESTANDAR sin ninguna regla de descuento aplicable
--   6 -> MOROSO con su deuda ya saldada (no debe rechazarse)
--   7 -> ESTANDAR con NIT registrado, para la campana CORPORATIVO (Parte 2)
-- El id 9999 se deja deliberadamente sin fila en `clientes` para el caso
-- "cliente inexistente".
-- ---------------------------------------------------------------------------

INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES
  (1, 'Laura Restrepo',      'VIP',       NULL),
  (2, 'Comercial Andina SAS','FRECUENTE', NULL),
  (3, 'Julian Ospina',       'FRECUENTE', NULL),
  (4, 'Marcela Duque',       'MOROSO',    NULL),
  (5, 'Pedro Salcedo',       'ESTANDAR',  NULL),
  (6, 'Camila Rojas',        'MOROSO',    NULL),
  (7, 'Distribuciones Andina SAS', 'ESTANDAR', '900123456-7');

INSERT INTO productos (id, nombre, precio) VALUES
  (101, 'Teclado mecanico',        150000),
  (102, 'Mouse inalambrico',        80000),
  (103, 'Monitor 24 pulgadas',     650000),
  (104, 'Silla ergonomica',        900000),
  (105, 'Cable USB-C (1m)',         25000);

INSERT INTO inventario (producto_id, stock) VALUES
  (101, 50),
  (102, 100),
  (103, 20),
  (104, 10),
  (105, 2);

-- Deuda pendiente real: dispara el rechazo por mora dentro del horario de corte.
INSERT INTO facturas (cliente_id, monto, pagada) VALUES (4, 320000, FALSE);
-- Deuda ya saldada: no debe afectar el procesamiento del cliente 6.
INSERT INTO facturas (cliente_id, monto, pagada) VALUES (6, 150000, TRUE);

-- Historial de mas de 10 pedidos para el cliente FRECUENTE con mayor descuento (id 2).
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES
  (2, 200000, 0, 38000, 238000, '2026-01-05 10:00:00', 'CONFIRMADO'),
  (2, 210000, 0, 39900, 249900, '2026-01-12 10:00:00', 'CONFIRMADO'),
  (2, 195000, 0, 37050, 232050, '2026-01-19 10:00:00', 'CONFIRMADO'),
  (2, 220000, 0, 41800, 261800, '2026-02-02 10:00:00', 'CONFIRMADO'),
  (2, 205000, 0, 38950, 243950, '2026-02-09 10:00:00', 'CONFIRMADO'),
  (2, 230000, 0, 43700, 273700, '2026-02-16 10:00:00', 'CONFIRMADO'),
  (2, 198000, 0, 37620, 235620, '2026-03-01 10:00:00', 'CONFIRMADO'),
  (2, 215000, 0, 40850, 255850, '2026-03-08 10:00:00', 'CONFIRMADO'),
  (2, 225000, 0, 42750, 267750, '2026-03-15 10:00:00', 'CONFIRMADO'),
  (2, 190000, 0, 36100, 226100, '2026-03-22 10:00:00', 'CONFIRMADO'),
  (2, 240000, 0, 45600, 285600, '2026-04-05 10:00:00', 'CONFIRMADO');

-- Historial de 5 pedidos (entre 4 y 10) para el cliente FRECUENTE intermedio (id 3).
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES
  (3, 130000, 0, 24700, 154700, '2026-02-03 10:00:00', 'CONFIRMADO'),
  (3, 140000, 0, 26600, 166600, '2026-02-17 10:00:00', 'CONFIRMADO'),
  (3, 125000, 0, 23750, 148750, '2026-03-03 10:00:00', 'CONFIRMADO'),
  (3, 135000, 0, 25650, 160650, '2026-03-17 10:00:00', 'CONFIRMADO'),
  (3, 128000, 0, 24320, 152320, '2026-04-01 10:00:00', 'CONFIRMADO');
