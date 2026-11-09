# PixelAuth — авторизация (замена AuthMe)

Плагин Paper 1.21.11 (Java 21). Работает сам по себе (чат + титры + боссбар), а если на сервере стоит
плагин PixelUI, а у игрока мод PixelUI — показывает пиксельные окна входа, регистрации и смены пароля.

## Установка и переезд с AuthMe

1. Положите `PixelAuth-*.jar` (и желательно `PixelUI-Paper-*.jar`) в `plugins/`. PluginSync сделает это сам.
2. Пока стоит AuthMe, PixelAuth работает в **пассивном режиме** (ничего не блокирует).
3. В консоли: `/pixelauth import-authme` (читает `plugins/AuthMe/authme.db`, таблицу `authme`).
   Другой файл/таблица: `/pixelauth import-authme <файл> <таблица>`.
4. Удалите AuthMe и перезапустите сервер.

Пароли AuthMe (`$SHA$`) проверяются как есть и при первом входе перехешируются в PBKDF2-SHA256.
BCrypt не поддерживается — таким аккаунтам нужен `/pixelauth changepass`.

## Что умеет

- Регистрация / вход / выход / смена пароля; пиксельные окна с полями ввода и таймером.
- Карантин до входа: нельзя ходить, ломать, писать, брать/выбрасывать предметы, открывать инвентарь,
  получ��ть урон; слепота, невидимость для других, телепорт на спавн и возврат на место после входа.
- Таймаут и лимит попыток, временная блокировка IP после нескольких киков.
- Сессии по IP (перезаход без пароля N минут), лимит аккаунтов на IP (право `pixelauth.unlimited`).
- Проверка ника (регулярка, защита от «Steve»/«steve»), дубль-вход с тем же ником запрещён.
- Пароли: PBKDF2-SHA256 с солью, длина от/до, список запрещённых; хеширование не в главном потоке.
- Хранение: SQLite (по умолчанию) или MySQL/MariaDB.
- Bedrock-игроки (Geyser/Floodgate) входят автоматически (их уже проверил Xbox Live).
- Пароли не попадают в консоль, лог входов `plugins/PixelAuth/logins.log`.
- Если база не открылась — плагин включает whitelist, чтобы сервер не остался без защиты.

## Команды

| Команда | Что делает |
|---|---|
| `/register <пароль> <пароль>` (`/reg`) | Регистрация |
| `/login <пароль>` (`/l`, `/log`) | Вход |
| `/logout` | Выход из аккаунта |
| `/changepassword [старый новый]` (`/cp`) | Смена пароля (без аргументов — пиксельное окно) |
| `/pixelauth register\|unregister\|changepass\|forcelogin\|info\|import-authme\|stats\|reload` | Админка (`pixelauth.admin`) |

## API для других плагинов

```java
// plugin.yml: softdepend: [PixelAuth]
if (!PixelAuthAPI.isAuthenticated(player)) return;
PixelAuthAPI.forceLogin(player);
PixelAuthAPI.logout(player);

@EventHandler
public void onLogin(PixelAuthLoginEvent e) { /* e.getType(): PASSWORD, REGISTER, SESSION, BEDROCK, FORCED */ }
@EventHandler
public void onLogout(PixelAuthLogoutEvent e) { }
```

## Ограничения

- В offline-mode ник ничем не подтверждён — защита держится только на пароле. Сессия по IP удобна, но слабее.
- Трафик Minecraft без шифрования в offline-mode: пароль идёт по сети открыто (как и в AuthMe).
- Инвентарь до входа не прячется (только блокируется).
- BCrypt/Argon2 из AuthMe не переносятся; импорт только из SQLite.
