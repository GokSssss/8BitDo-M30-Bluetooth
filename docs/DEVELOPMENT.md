# Документация для разработчика

README и CHANGELOG описывают возможности и изменения для пользователей. Правила заполнения: [PUBLICATION_STYLE.md](PUBLICATION_STYLE.md).

- Сборка и текущие проверки: [VALIDATION.txt](../VALIDATION.txt), [паспорт 1.62](releases/1.62-Паспорт.txt).
- Подробная история сборки и реализации: [прежний README](history/README-before-2026-11-15.md).
- Прежнее описание GitHub Release: [редакторский снимок](history/release-descriptions-before-2026-11-15.json).
- Прошивка и источники: [FIRMWARE-RESEARCH.md](../FIRMWARE-RESEARCH.md), [THIRD-PARTY-NOTICES.txt](../THIRD-PARTY-NOTICES.txt).
- Готовые файлы: [выпуск 1.62](https://github.com/GokSssss/8BitDo-M30-Bluetooth/releases/tag/v1.62).

Исторические разделы описывают состояние своих версий; для установки используйте актуальный README. Проверки логики, Android Views и реального Bluetooth фиксируются отдельно. Ключи подписи не входят в GitHub и SOURCE.

Редактура публичного описания не требует нового APK. Теги, бинарники и опубликованные исходные архивы сохраняются.

Android 16+ unpairing uses the public CompanionDeviceManager.removeBond(associationId), not disassociate alone. Requires an association owned by this app/user and BLUETOOTH_CONNECT. getMyAssociations() is scoped by Android; exact MAC is mandatory. True means request accepted: only BOND_NONE or a successful adapter snapshot with no pair clears metadata and removes app associations. Refusal/cancel/unknown state preserve data. Timeout offers Settings. Public API36 helper is compiled with the official SDK37 jar; no SDK stubs are packaged. API36 headless tests require Java21: set M30_TEST_JAVA_HOME to its installation; the app still builds with JDK17, min26/target35 unchanged.
