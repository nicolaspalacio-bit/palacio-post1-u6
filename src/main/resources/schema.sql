-- ---------------------------------------------------------------------------
-- Esquema inicial de pedidos-service (Parte 1).
-- Se recrea en cada arranque (spring.sql.init.mode=always) para que el
-- sistema y la suite de pruebas partan siempre del mismo estado conocido.
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS detalle_pedido;
DROP TABLE IF EXISTS pedidos;
DROP TABLE IF EXISTS facturas;
DROP TABLE IF EXISTS inventario;
DROP TABLE IF EXISTS productos;
DROP TABLE IF EXISTS clientes;

CREATE TABLE clientes (
    id            BIGINT PRIMARY KEY,
    nombre        VARCHAR(120) NOT NULL,
    tipo_cliente  VARCHAR(20)  NOT NULL,
    -- Columna agregada en la Parte 2 para la campana CORPORATIVO: un cliente
    -- con NIT registrado accede a un descuento adicional de campana.
    nit           VARCHAR(20)
);

CREATE TABLE productos (
    id      BIGINT PRIMARY KEY,
    nombre  VARCHAR(120) NOT NULL,
    precio  DOUBLE NOT NULL
);

CREATE TABLE inventario (
    producto_id  BIGINT PRIMARY KEY,
    stock        INT NOT NULL
);

CREATE TABLE facturas (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id  BIGINT NOT NULL,
    monto       DOUBLE NOT NULL,
    pagada      BOOLEAN NOT NULL
);

CREATE TABLE pedidos (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id  BIGINT NOT NULL,
    subtotal    DOUBLE NOT NULL,
    descuento   DOUBLE NOT NULL,
    impuesto    DOUBLE NOT NULL,
    total       DOUBLE NOT NULL,
    fecha       TIMESTAMP NOT NULL,
    estado      VARCHAR(20) NOT NULL
);

CREATE TABLE detalle_pedido (
    pedido_id    BIGINT NOT NULL,
    producto_id  BIGINT NOT NULL,
    cantidad     INT NOT NULL
);
