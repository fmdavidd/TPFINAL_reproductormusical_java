"""Chequeo local de datos con SQLite; NO verifica el dialecto ni el servidor MySQL."""
from pathlib import Path
import hashlib
import re
import sqlite3

raiz = Path(__file__).resolve().parents[1]
esquema = (raiz / "sql/01_esquema.sql").read_text(encoding="utf-8")
datos = (raiz / "sql/02_datos.sql").read_text(encoding="utf-8")
assert "DROP DATABASE" not in esquema.upper()
# Solo se adaptan las declaraciones para verificar los registros y sus claves.
esquema = re.sub(r"CREATE DATABASE.*?;|USE .*?;", "", esquema)
esquema = esquema.replace("INT AUTO_INCREMENT PRIMARY KEY", "INTEGER PRIMARY KEY AUTOINCREMENT")
esquema = esquema.replace("ENUM('NORMAL','ADMIN')", "TEXT")
datos = re.sub(r"USE .*?;", "", datos)
base = sqlite3.connect(":memory:")
base.execute("PRAGMA foreign_keys = ON")
base.executescript(esquema)
base.executescript(datos)
for tabla in ["usuario", "artista", "album", "cancion", "playlist"]:
    cantidad = base.execute("SELECT COUNT(*) FROM " + tabla).fetchone()[0]
    assert cantidad >= 15, (tabla, cantidad)
    print(tabla + ": " + str(cantidad))
assert not base.execute("PRAGMA foreign_key_check").fetchall()
assert not base.execute("SELECT c.id_cancion FROM cancion c JOIN album a ON a.id_album = c.id_album WHERE c.id_artista <> a.id_artista").fetchall()
assert not base.execute("SELECT id_playlist FROM playlist_cancion GROUP BY id_playlist HAVING MIN(posicion) <> 0 OR MAX(posicion) <> COUNT(*) - 1 OR COUNT(DISTINCT posicion) <> COUNT(*)").fetchall()
assert not base.execute("SELECT p.id_playlist FROM playlist p JOIN usuario u ON p.id_usuario = u.id_usuario WHERE u.rol <> 'NORMAL'").fetchall()
for email, hash_guardado, salt, rol in base.execute("SELECT email, hash_contrasenia, salt, rol FROM usuario"):
    clave = "admin123" if rol == "ADMIN" else "1234"
    assert hashlib.sha256((salt + clave).encode()).hexdigest() == hash_guardado, email
assert (raiz / "lib/sql.txt").read_text(encoding="utf-8").endswith((raiz / "sql/02_datos.sql").read_text(encoding="utf-8").split("\n\n", 1)[1])
print("OK: cantidades, claves foraneas, propietarios, orden y 15 hashes. Falta ejecutar en MySQL.")
