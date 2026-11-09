# Yeamu — сайт, ModGate и ModSync

Репозиторий Minecraft-сервера **yeamu.my.id**:

| Что | Где | Описание |
|---|---|---|
| Сайт | `index.html`, `assets/` | GitHub Pages, стиль Minecraft, скачивание мода |
| ModGate | `plugin/ModGate` | Плагин Paper 1.21.11: требует клиентский мод |
| ModSync | `mod/ModSync` | Fabric-мод 1.21.11: ставит и обновляет моды сервера |
| Манифест | `manifest-source.json` → `manifest.json` + `.sig` | Список модов, подписанный Ed25519 |
| Утилита | `tools/manifest.py` | keygen / build / sign / verify |
| CI | `.github/workflows/build.yml` | Сборка jar, манифест, подпись, публикация |

## Как это работает

1. Вы правите `manifest-source.json` (добавляете моды) и пушите.
2. GitHub Actions собирает ModGate и ModSync, кладёт jar в `downloads/`, считает SHA-256 и размеры,
   повышает версию манифеста, подписывает его секретом `MANIFEST_PRIVATE_KEY` и коммитит обратно.
3. Pages публикует сайт и `https://yeamu.my.id/manifest.json`.
4. ModSync при запуске игры проверяет подпись, скачивает недостающее, проверяет SHA-256 и атомарно
   меняет файлы в `mods/`. Заблокированные файлы заменяет мини-процесс после выхода из игры.
5. При входе на сервер ModSync шлёт в канал `modgate:handshake` версию мода, версию манифеста,
   хеш списка модов и версию игры. ModGate сверяет это с манифестом.

## Первая настройка

1. **Settings → Secrets and variables → Actions → New repository secret**: `MANIFEST_PRIVATE_KEY` —
   приватный ключ (Base64 PKCS#8). Публичный ключ уже зашит в мод, плагин и `tools/manifest.py`.
   Новую пару можно создать: `python tools/manifest.py keygen` (тогда замените публичный ключ во всех трёх местах).
2. **Actions → «Сборка ModGate и ModSync» → Run workflow**.
3. **Settings → Pages**: источник — ветка `main`, папка `/ (root)`, домен `yeamu.my.id`.
4. Скачайте `downloads/ModGate-1.0.0.jar` (или артефакт сборки) и положите в `plugins/` сервера.
5. Настройте `plugins/ModGate/config.yml` и выполните `/modgate reload`.

## Добавить мод в сборку

```json
{
  "id": "sodium",
  "name": "Sodium",
  "version": "0.7.0",
  "url": "https://cdn.modrinth.com/data/.../sodium-fabric-0.7.0+mc1.21.11.jar",
  "description": "Оптимизация графики"
}
```

URL только HTTPS и только с доменов белого списка: `yeamu.my.id`, `github.com`, `objects.githubusercontent.com`,
`release-assets.githubusercontent.com`, `raw.githubusercontent.com`, `cdn.modrinth.com`, `maven.fabricmc.net`.
Удалить мод у игроков: добавьте `{"file": "old.jar"}` или `{"sha256": "..."}` в `remove`.
Разрешённые, но необязательные моды (не влияют на хеш) — SHA-256 в `optional`.

## ModGate

- Режимы `strict` (карантин и кик), `warn` (только предупреждение), `off`.
- Карантин: нельзя двигаться, ломать, писать, получать урон; игрок невидим для других.
- Исключения: право `modgate.bypass`, список ников/UUID, Bedrock-игроки по префиксу (Floodgate API, если есть).
- Команды (`modgate.admin`): `/modgate status <игрок>`, `/modgate list`, `/modgate reload`, `/modgate kick-outdated`.
- Лог входов: `plugins/ModGate/joins.log`, кэш манифеста: `plugins/ModGate/manifest-cache.json`.

## ModSync

- Логи: `logs/modsync.log`, служебная папка: `.modsync/` (резервные копии, отложенные замены, состояние).
- Кнопка «Проверить обновления» в главном меню.
- Если сайт недоступен, можно играть с тем, что установлено (будет предупреждение).

## Ограничения

- **Клиент можно подделать.** Модифицированный клиент может отправить «правильное» рукопожатие с любым хешем.
  Это защита от случайных ошибок и ленивых читеров, а не античит.
- Хеш и список модов клиент сообщает о себе сам — сервер не может проверить, что реально загружено.
- Исключение Bedrock по префиксу без Floodgate можно обойти, взяв ник с этим префиксом на offline-сервере.
- Проверка идёт после входа в мир (фаза play), а не в фазе configuration.
- Ресурспаки, шейдеры и конфиги модов не синхронизируются.

## Идеи

- Подпись рукопожатия: сервер шлёт случайный nonce, клиент отвечает HMAC(секрет, nonce + данные)
  (challenge-response; секрет всё равно извлекаем из jar, но повторы и простые подделки отсекаются).
- Проверка в фазе configuration, до появления игрока в мире.
- Статистика версий модов и манифеста, дашборд на сайте, вебхук в Discord.
- Интеграция с Modrinth API для автоматических обновлений модов в манифесте.
