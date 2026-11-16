# BBans

Менеджер наказаний (BAN / MUTE / WARN / KICK) для Paper 1.16.5: иерархия модераторов,
история, автоматические наказания за WARN, SQLite, публичный Java API.

## Сборка

Нужны JDK 8+ и Maven:

```bash
mvn clean package
```

Готовый плагин: `target/BBans-1.0.0.jar` → положить в `plugins/`.

## Команды и права

| Команда | Право |
|---|---|
| `/ban <игрок> [время] <причина>` | `bbans.ban` |
| `/unban <игрок>` | `bbans.unban` |
| `/mute <игрок> [время] <причина>` | `bbans.mute` |
| `/unmute <игрок>` | `bbans.unmute` |
| `/warn <игрок> <причина>` | `bbans.warn` |
| `/unwarn <игрок> [номер]` | `bbans.unwarn` |
| `/warnings <игрок>` | `bbans.warnings` |
| `/check <игрок> [страница]` | `bbans.check` |
| `/banlist [страница]` / `/mutelist [страница]` | `bbans.banlist` / `bbans.mutelist` |
| `/kick <игрок> <причина>` | `bbans.kick` |
| `/bbans reload` | `bbans.reload` |

Иерархия: `bbans.hierarchy.admin` / `moder` / `helper` (группы и правила — в `config.yml`).
Время: `30s`, `10m`, `2h`, `7d`, `2w`, `1mo`, `1y` (можно `1d12h`); без времени — навсегда.

## API

```java
RegisteredServiceProvider<BBansAPI> p = Bukkit.getServicesManager().getRegistration(BBansAPI.class);
BBansAPI api = p.getProvider();
api.getPunishmentManager().banAsync(uuid, "Reason", moderatorUuid, Duration.ofDays(7));
```

Стабильный пакет — `ru.example.bbans.api`. В `plugin.yml` своего плагина укажите `depend`/`softdepend: [BBans]`.

## Структура

`command` → `service` → `repository` → SQLite. API (`api` + `apiimpl`) ходит через тот же `PunishmentService`.
Работа с БД выполняется в отдельных потоках; кик, события и сообщения не блокируют основной поток.
