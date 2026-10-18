"""Prueba HTTP de JSP/JSF y persistencia compartida. Python 3, sin dependencias.

Uso: python scripts/smoke_web.py http://localhost:8080
Ejecutar únicamente contra una instancia local de demostración.
"""
import http.cookiejar
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from html.parser import HTMLParser

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080").rstrip("/")
if urllib.parse.urlparse(BASE).hostname not in {"localhost", "127.0.0.1", "::1"}:
    raise SystemExit("Esta prueba crea datos de demostración: utilice una instancia local.")
PASSWORD = os.getenv("ADMIN_PASSWORD", "Admin123!")
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
checks = 0
created = {"pedidos": [], "clientes": [], "productos": []}
token = None


def check(condition, description):
    global checks
    assert condition, description
    checks += 1
    print("OK", description)


def request(path, method="GET", data=None, api=False):
    headers = {}
    if api:
        headers["Content-Type"] = "application/json"
        if token:
            headers["Authorization"] = "Bearer " + token
        body = json.dumps(data).encode() if data is not None else None
    else:
        body = urllib.parse.urlencode(data, doseq=True).encode() if data is not None else None
    req = urllib.request.Request(BASE + path, body, headers, method=method)
    try:
        with opener.open(req, timeout=20) as response:
            text = response.read().decode("utf-8")
            check(response.status in {200, 201, 204}, method + " " + path)
            if not api:
                check("Whitelabel Error" not in text and "Ã" not in text and "Â·" not in text,
                      "Página sin error ni texto mal codificado")
            return (json.loads(text) if api and text else text), response.url
    except urllib.error.HTTPError as error:
        raise AssertionError(f"{method} {path}: HTTP {error.code}; {error.read().decode('utf-8')[:250]}") from error


class HiddenInputs(HTMLParser):
    def __init__(self):
        super().__init__()
        self.fields = {}

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "input" and attrs.get("type") == "hidden" and attrs.get("name"):
            self.fields[attrs["name"]] = attrs.get("value", "")


def hidden(html):
    parser = HiddenInputs()
    parser.feed(html)
    return parser.fields


def main():
    global token
    auth, _ = request("/api/auth/login", "POST", {"username": "admin", "password": PASSWORD}, True)
    token = auth["token"]
    html, _ = request("/login")
    html, _ = request("/login", "POST", {**hidden(html), "username": "admin", "password": PASSWORD})
    check("Cada pedido, bajo control." in html, "Login de sesión y resumen JSP")
    for path in ["/productos", "/productos/nuevo", "/clientes", "/clientes/nuevo", "/pedidos", "/pedidos/nuevo"]:
        request(path)

    nonce = str(int(time.time() * 1000))[-10:]
    html, _ = request("/clientes/nuevo")
    client = {"ruc": "2" + nonce, "razonSocial": "Prueba JSP", "email": "jsp@example.com", "telefono": "999222333", "direccion": "Dirección de prueba"}
    html, _ = request("/clientes/guardar", "POST", {**hidden(html), **client})
    check("Cliente guardado" in html, "Alta por JSP y DAO JDBC")
    clients, _ = request("/api/clientes", api=True)
    cid = next(c["id"] for c in clients if c["ruc"] == client["ruc"])
    created["clientes"].append(cid)
    html, _ = request(f"/clientes/{cid}/editar")
    request("/clientes/guardar", "POST", {**hidden(html), **client, "id": cid, "razonSocial": "Prueba JSP editada"})
    client_db, _ = request(f"/api/clientes/{cid}", api=True)
    check(client_db["razonSocial"] == "Prueba JSP editada", "Edición JDBC visible mediante JPA")

    html, _ = request("/jsf/cliente.xhtml")
    fields = hidden(html)
    check("jakarta.faces.ViewState" in fields, "JSF genera estado de vista real")
    jsf = {"clienteForm:" + key: value for key, value in client.items()}
    jsf["clienteForm:ruc"] = "3" + nonce
    jsf["clienteForm:razonSocial"] = "Prueba JSF"
    html, _ = request("/jsf/cliente.xhtml", "POST", {**fields, **jsf, "clienteForm:guardar": "Guardar cliente"})
    check("Cliente registrado correctamente" in html, "Managed Bean JSF persiste el cliente")
    clients, _ = request("/api/clientes", api=True)
    jsfid = next(c["id"] for c in clients if c["ruc"] == "3" + nonce)
    created["clientes"].append(jsfid)

    product, _ = request("/api/productos", "POST", {"sku": "WEB-" + nonce, "nombre": "Producto prueba web", "categoria": "Pruebas", "precio": 10, "stock": 5}, True)
    pid = product["id"]
    created["productos"].append(pid)
    request(f"/productos/{pid}/editar")
    html, _ = request("/pedidos/nuevo")
    html, url = request("/pedidos/guardar", "POST", {**hidden(html), "clienteId": cid, "productoId": [pid, "", "", ""], "cantidad": [2, 1, 1, 1]})
    oid = int(url.rsplit("/", 1)[1])
    created["pedidos"].append(oid)
    check("Pedido guardado como borrador" in html, "Alta de pedido desde JSP")
    request(f"/pedidos/{oid}/editar")
    html, _ = request(f"/pedidos/{oid}/confirmar", "POST", hidden(html))
    check("CONFIRMADO" in html, "Confirmación desde JSP")
    stock, _ = request(f"/api/productos/{pid}", api=True)
    check(stock["stock"] == 3, "Confirmación web descuenta stock")
    html, _ = request(f"/pedidos/{oid}/cancelar", "POST", hidden(html))
    check("CANCELADO" in html, "Cancelación desde JSP")
    stock, _ = request(f"/api/productos/{pid}", api=True)
    check(stock["stock"] == 5, "Cancelación web devuelve stock")


try:
    main()
finally:
    if token:
        for resource, ids in created.items():
            for identifier in ids:
                request(f"/api/{resource}/{identifier}", "DELETE", api=True)
print(f"TOTAL: {checks} comprobaciones HTTP web correctas")
