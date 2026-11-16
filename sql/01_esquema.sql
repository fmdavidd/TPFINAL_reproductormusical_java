
-- Inicializacion manual para una base nueva. No elimina bases ni datos.
CREATE DATABASE IF NOT EXISTS ReproductorMusicalBD CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ReproductorMusicalBD;

CREATE TABLE usuario (
    id_usuario        INT AUTO_INCREMENT PRIMARY KEY,
    nombre            VARCHAR(60)  NOT NULL,
    email             VARCHAR(100) NOT NULL UNIQUE,
    hash_contrasenia  VARCHAR(128) NOT NULL,
    salt              VARCHAR(64)  NOT NULL,
    rol               ENUM('NORMAL','ADMIN') NOT NULL,
    premium           BOOLEAN NULL,          -- solo NORMAL
    nivel_admin       VARCHAR(20) NULL,      -- solo ADMIN
    fecha_registro    DATE NOT NULL,
    activo            BOOLEAN DEFAULT TRUE
);

CREATE TABLE artista (
    id_artista   INT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(80) NOT NULL,
    pais_origen  VARCHAR(50),
    activo       BOOLEAN DEFAULT TRUE
);

CREATE TABLE artista_genero (
    id_artista  INT NOT NULL,
    genero      VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_artista, genero),
    FOREIGN KEY (id_artista) REFERENCES artista(id_artista)
);

CREATE TABLE album (
    id_album          INT AUTO_INCREMENT PRIMARY KEY,
    titulo            VARCHAR(100) NOT NULL,
    anio_lanzamiento  INT,
    portada           VARCHAR(255),
    id_artista        INT NOT NULL,
    activo            BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (id_artista) REFERENCES artista(id_artista)
);

CREATE TABLE cancion (
    id_cancion         INT AUTO_INCREMENT PRIMARY KEY,
    titulo             VARCHAR(100) NOT NULL,
    duracion_segundos  INT NOT NULL,
    ruta_archivo       VARCHAR(255),
    genero             VARCHAR(20) NOT NULL,
    id_artista         INT NOT NULL,
    id_album           INT NULL,             -- NULL si es single
    activo             BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (id_artista) REFERENCES artista(id_artista),
    FOREIGN KEY (id_album)   REFERENCES album(id_album)
);

CREATE TABLE playlist (
    id_playlist  INT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(80) NOT NULL,
    descripcion  VARCHAR(200),
    publica      BOOLEAN DEFAULT FALSE,
    id_usuario   INT NOT NULL,
    activo       BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);

CREATE TABLE playlist_cancion (
    id_playlist  INT NOT NULL,
    id_cancion   INT NOT NULL,
    posicion     INT NOT NULL,
    PRIMARY KEY (id_playlist, id_cancion),
    FOREIGN KEY (id_playlist) REFERENCES playlist(id_playlist),
    FOREIGN KEY (id_cancion)  REFERENCES cancion(id_cancion)
);

CREATE TABLE favorito (
    id_usuario      INT NOT NULL,
    id_cancion      INT NOT NULL,
    fecha_agregado  DATE,
    PRIMARY KEY (id_usuario, id_cancion),
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
    FOREIGN KEY (id_cancion) REFERENCES cancion(id_cancion)
);

CREATE TABLE registro_accion (
    id_registro  INT AUTO_INCREMENT PRIMARY KEY,
    id_admin     INT NOT NULL,
    fecha_hora   DATETIME NOT NULL,
    accion       VARCHAR(200) NOT NULL,
    FOREIGN KEY (id_admin) REFERENCES usuario(id_usuario)
);
