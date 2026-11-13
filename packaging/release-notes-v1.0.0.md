# BikeFlowServer 1.0.0

Первый релиз Docker-образа BikeFlowServer для `linux/amd64`.

## Установка

Скачайте `BikeFlowServer-1.0.0-linux-amd64.zip` и распакуйте архив. В Windows запустите `start-windows.bat`; в Linux/macOS выполните `sh ./start-linux.sh`. При первом запуске скрипт создаст `.env`; задайте уникальные пароли базы данных и администратора и повторно запустите скрипт.

Архив содержит Docker-образы сервера и PostgreSQL, Docker Compose-конфигурацию и скрипты запуска для Windows и Linux/macOS. Требуется Docker с поддержкой Linux-контейнеров. Образ собран для `linux/amd64`; Java 21 уже включена в образ, устанавливать JVM или Maven на хост не нужно.

## Доступ

- Панель администратора: `https://localhost:8443/admin`
- Страница входа: `https://localhost:8443/login`
- HTTP: порт `8080`

Для доступа из локальной сети укажите адрес компьютера-сервера в `BIKE_TLS_DNS_NAMES` или `BIKE_TLS_IP_ADDRESSES` в `.env` **до первого запуска**.

Самоподписанный HTTPS-сертификат создаётся при первом старте. Для остановки без удаления базы и настроек используйте `stop-windows.bat` в Windows или `stop-linux.sh` в Linux/macOS. Журнал сервера показывают `logs.bat` или `logs.sh`.
