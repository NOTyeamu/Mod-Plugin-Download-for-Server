#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Утилита для manifest.json сервера yeamu.

  python tools/manifest.py keygen            — создать пару ключей Ed25519
  python tools/manifest.py build             — собрать manifest.json из manifest-source.json
  python tools/manifest.py sign              — подписать (ключ в переменной MANIFEST_PRIVATE_KEY)
  python tools/manifest.py verify            — проверить подпись публичным ключом

Ключи: приватный — Base64 PKCS#8 DER, публичный — Base64 X.509 DER
(такой же формат читает Java: PKCS8EncodedKeySpec / X509EncodedKeySpec).
Подпись — Base64 от Ed25519-подписи точных байтов manifest.json.
"""
import base64
import datetime
import hashlib
import json
import os
import re
import sys
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE = os.path.join(ROOT, "manifest-source.json")
MANIFEST = os.path.join(ROOT, "manifest.json")
SIG = MANIFEST + ".sig"
MOD_PROPS = os.path.join(ROOT, "mod", "ModSync", "gradle.properties")
PIXELUI_PROPS = os.path.join(ROOT, "mod", "PixelUI", "gradle.properties")

# Должен совпадать с ModSyncConfig.PUBLIC_KEY и manifest.public-key в config.yml плагина
PUBLIC_KEY = "MCowBQYDK2VwAyEASH15CNNP+UybzhC9J0VyjZ/QEUBFTy/Fbum3wYcYsIM="

# Белый список доменов — как в моде
WHITELIST = {
    "yeamu.my.id", "github.com", "objects.githubusercontent.com",
    "release-assets.githubusercontent.com", "raw.githubusercontent.com",
    "cdn.modrinth.com", "maven.fabricmc.net",
}


def modsync_version(props=None):
    """Версия мода (по умолчанию ModSync) из gradle.properties."""
    props = props or MOD_PROPS
    with open(props, encoding="utf-8") as f:
        for line in f:
            m = re.match(r"\s*mod_version\s*=\s*(\S+)", line)
            if m:
                return m.group(1)
    raise SystemExit("mod_version не найден в " + props)


def sha_and_size(data: bytes):
    return hashlib.sha256(data).hexdigest(), len(data)


def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "yeamu-manifest-builder"})
    with urllib.request.urlopen(req, timeout=120) as r:
        return r.read()


def mods_hash(shas):
    """Хеш списка модов — тот же алгоритм, что в ModSync и ModGate."""
    joined = "\n".join(sorted(s.lower() for s in shas))
    return hashlib.sha256(joined.encode("utf-8")).hexdigest()


def build():
    with open(SOURCE, encoding="utf-8") as f:
        src = json.load(f)
    msv = modsync_version()
    subst = {"modsync": msv, "pixelui": modsync_version(PIXELUI_PROPS)}

    mods = []
    for m in src["mods"]:
        version = m.get("version", "?").format(**subst)
        if "local" in m:
            # Файл лежит в репозитории (собран CI) — раздаём его с сайта
            rel = m["local"].format(**subst)
            path = os.path.join(ROOT, rel)
            with open(path, "rb") as f:
                data = f.read()
            url = src["siteUrl"].rstrip("/") + "/" + rel.replace(os.sep, "/")
        else:
            url = m["url"].format(**subst)
            print("Скачиваю", url)
            data = fetch(url)
        host = urllib.parse.urlparse(url).hostname
        if not url.startswith("https://") or host not in WHITELIST:
            raise SystemExit(f"URL {url} не HTTPS или домен не в белом списке")
        sha, size = sha_and_size(data)
        file = m.get("file", os.path.basename(urllib.parse.urlparse(url).path)).format(**subst)
        mods.append({
            "id": m["id"], "name": m["name"], "version": version, "file": file,
            "url": url, "sha256": sha, "size": size,
            **({"description": m["description"]} if "description" in m else {}),
        })

    body = {
        "format": 1,
        "minecraft": src["minecraft"],
        "modsync": {"version": msv, "minVersion": src.get("modsyncMinVersion", msv).format(**subst)},
        "mods": mods,
        "remove": src.get("remove", []),
        "optional": [s.lower() for s in src.get("optional", [])],
        "modsHash": mods_hash([m["sha256"] for m in mods]),
    }

    # Версию повышаем, только если содержимое изменилось
    version = 1
    if os.path.exists(MANIFEST):
        try:
            with open(MANIFEST, encoding="utf-8") as f:
                old = json.load(f)
            old_body = {k: v for k, v in old.items() if k not in ("version", "generated")}
            version = old.get("version", 0) + (0 if old_body == body else 1)
            if old_body == body:
                print("Манифест не изменился, версия остаётся", version)
                return
        except Exception as e:
            print("Старый манифест не прочитан:", e)

    manifest = {"version": version,
                "generated": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
                **body}
    with open(MANIFEST, "w", encoding="utf-8", newline="\n") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)
        f.write("\n")
    # Старая подпись больше не действительна
    if os.path.exists(SIG):
        os.remove(SIG)
    print("manifest.json v%d: %d модов, хеш %s" % (version, len(mods), body["modsHash"][:12]))


def keygen():
    from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
    from cryptography.hazmat.primitives import serialization
    key = Ed25519PrivateKey.generate()
    priv = key.private_bytes(serialization.Encoding.DER, serialization.PrivateFormat.PKCS8,
                             serialization.NoEncryption())
    pub = key.public_key().public_bytes(serialization.Encoding.DER,
                                        serialization.PublicFormat.SubjectPublicKeyInfo)
    print("PRIVATE (секрет MANIFEST_PRIVATE_KEY):", base64.b64encode(priv).decode())
    print("PUBLIC  (в мод, плагин и этот скрипт):", base64.b64encode(pub).decode())


def sign():
    from cryptography.hazmat.primitives import serialization
    raw = os.environ.get("MANIFEST_PRIVATE_KEY", "").strip()
    if not raw:
        raise SystemExit("Переменная MANIFEST_PRIVATE_KEY не задана")
    key = serialization.load_der_private_key(base64.b64decode(raw), password=None)
    with open(MANIFEST, "rb") as f:
        data = f.read()
    sig = key.sign(data)
    with open(SIG, "w", encoding="ascii", newline="\n") as f:
        f.write(base64.b64encode(sig).decode() + "\n")
    print("Подписано:", SIG)


def verify():
    from cryptography.hazmat.primitives import serialization
    pub = serialization.load_der_public_key(base64.b64decode(PUBLIC_KEY))
    with open(MANIFEST, "rb") as f:
        data = f.read()
    with open(SIG, encoding="ascii") as f:
        sig = base64.b64decode(f.read().strip())
    pub.verify(sig, data)  # бросит исключение, если не сошлось
    print("Подпись верна ✔")


if __name__ == "__main__":
    import urllib.parse  # noqa: E402
    cmds = {"build": build, "sign": sign, "verify": verify, "keygen": keygen}
    if len(sys.argv) < 2 or sys.argv[1] not in cmds:
        print(__doc__)
        sys.exit(1)
    cmds[sys.argv[1]]()
