BikeFlowServer @VERSION@ — Windows x64

Requirements
  - Windows 10/11 x64
  - Docker Desktop with Linux containers enabled
  - About 3 GB of free disk space for the package and Docker images

First start
  1. Extract the complete ZIP archive to a folder.
  2. Run start.bat. It creates .env from .env.example and opens it in Notepad.
  3. Set unique values for POSTGRES_PASSWORD and BIKE_ADMIN_PASSWORD, then save .env.
     BIKE_ADMIN_PASSWORD must be at least 12 characters.
  4. Run start.bat again. It loads the bundled images and starts the server and database.
  5. Open https://localhost:8443/admin and sign in using BIKE_ADMIN_LOGIN and
     BIKE_ADMIN_PASSWORD from .env.

For access from another computer on your network, set its DNS name or IP address
in BIKE_TLS_DNS_NAMES or BIKE_TLS_IP_ADDRESSES before the first start. Names in
these settings are comma-separated. The certificate is created on first start.

Files
  start.bat  Creates .env if needed, loads images and starts the application.
  stop.bat   Stops the application without deleting its database or settings.
  logs.bat   Follows the server log. Press Ctrl+C to return to the console.
  .env       Local passwords and connection settings. Keep this file private.

The first start may take a minute while PostgreSQL initializes its database.
The browser may show a certificate warning for the self-signed local HTTPS
certificate. The server also exposes HTTP on port 8080.

To remove the installation and its saved database/settings, run this command
from this folder:

  docker compose --file compose.yaml down --volumes

This permanently deletes the database, server settings, certificate and logs.


BikeFlowServer @VERSION@ — Windows x64

Требования
  - Windows 10/11 x64
  - Docker Desktop с поддержкой Linux-контейнеров
  - Около 3 ГБ свободного места для пакета и Docker-образов

Первый запуск
  1. Распакуйте ZIP-архив в папку.
  2. Запустите start.bat. Скрипт создаст .env на основе .env.example и откроет файл в Блокноте.
  3. Задайте уникальные значения для POSTGRES_PASSWORD и BIKE_ADMIN_PASSWORD и сохраните .env.
     Длина BIKE_ADMIN_PASSWORD должна быть не менее 12 символов.
  4. Запустите start.bat ещё раз. Скрипт загрузит Docker-образы из архива и запустит сервер с базой данных.
  5. Откройте https://localhost:8443/admin и войдите, используя BIKE_ADMIN_LOGIN и
     BIKE_ADMIN_PASSWORD из файла .env.

Для доступа с другого компьютера в локальной сети укажите его DNS-имя или IP-адрес
в BIKE_TLS_DNS_NAMES или BIKE_TLS_IP_ADDRESSES до первого запуска. Если адресов
несколько, разделите их запятыми. Сертификат будет создан при первом запуске.

Файлы
  start.bat  Создаёт .env при необходимости, загружает образы и запускает приложение.
  stop.bat   Останавливает приложение, не удаляя базу данных и настройки.
  logs.bat   Показывает журнал сервера. Нажмите Ctrl+C, чтобы вернуться в консоль.
  .env       Локальные пароли и настройки подключения. Храните файл в безопасном месте.

При первом запуске PostgreSQL может понадобиться около минуты для инициализации базы.
Браузер может показать предупреждение о самоподписанном HTTPS-сертификате.
Сервер также использует HTTP-порт 8080.

Чтобы удалить установку вместе с сохранённой базой данных и настройками, выполните
в этой папке команду:

  docker compose --file compose.yaml down --volumes

Эта команда безвозвратно удалит базу данных, настройки сервера, сертификат и журналы.
