# API товаров мастерской

Все маршруты требуют сессию ADMIN; POST, PUT и DELETE требуют CSRF-токен.
Товары мастерской имеют отдельный каталог и историю прайса. Детали клиента
хранятся отдельно в составе бланка и не считаются продажей.

## Создание товара

`POST /api/products`

```json
{
  "name": "Камера 26 дюймов",
  "article": "CAM-26",
  "category": "Камеры",
  "unit": "шт",
  "description": "Камера для колеса 26 дюймов",
  "price": 350.00,
  "validFrom": "2026-11-11"
}
```

Название и единица измерения обязательны. Артикул, категория, описание и дата
начала действия цены необязательны. Начальная цена обязательна; если дата не
задана, используется текущая дата сервера. В ответе `201 Created` возвращаются
поля товара и созданная цена.

## Чтение и изменение каталога

- `GET /api/products` — каталог, отсортированный по названию и ID;
- `GET /api/products/{id}` — товар с действующей сегодня ценой;
- `GET /api/products/search?query=...` — поиск по названию;
- `PUT /api/products/{id}` — изменение названия, артикула, категории, единицы,
  описания и признака `active`; изменение цены выполняется отдельно;
- `DELETE /api/products/{id}` — удаление товара, если он не использовался в заказах.

Отключённый товар остаётся в истории и не может быть добавлен в новый заказ.

## История цен

- `POST /api/products/{productId}/prices` — добавить цену;
- `GET /api/products/{productId}/prices` — история цен;
- `GET /api/products/{productId}/prices/{priceId}` — одна запись;
- `PUT /api/products/{productId}/prices/{priceId}` — изменить сумму записи;
- `DELETE /api/products/{productId}/prices/{priceId}` — удалить запись.

Формат создания такой же, как у начальной цены:

```json
{"price": 375.00, "validFrom": "2026-12-01"}
```

Новая дата должна быть позже даты начала последней цены товара. Предыдущий
открытый период закроется за день до новой цены. История следует тем же правилам,
что и прайс услуг.

## Расчёт цены товара со скидкой

`POST /api/work-orders/products/quote`

```json
{"products":[{"productId":5,"discountId":7,"quantity":2}]}
```

Для каждой строки сервер берёт действующую цену товара, применяет выбранную скидку
и возвращает снимок названия, артикула, единицы, цены до и после скидки, количества
и итог по строке. Суммы округляются до двух знаков по HALF_UP.

В `POST /api/work-orders` и `PUT /api/work-orders/{number}` товары передаются
массивом `products`. Поля строки: `productId`, `quantity`, необязательный
`discountId`, а также необязательные `priceWithoutDiscount` и `priceWithDiscount`
для сохранённых/ручных сумм. Сервер сохраняет цены и название как снимок заказа.

`customerParts` в запросе заказа — отдельный массив деталей клиента с названием,
маркой, моделью, серийным номером, количеством, состоянием при приёме,
неисправностью и комплектностью. Их можно передавать вместе с велосипедом либо
вместо него. Заказ без велосипеда должен содержать хотя бы одну деталь клиента.

## CRUD строк внутри заказа

Товары мастерской:

- `POST /api/work-orders/{blankNumber}/products` — добавить строку товара;
- `GET /api/work-orders/{blankNumber}/products` — список строк;
- `GET /api/work-orders/{blankNumber}/products/{itemId}` — одна строка;
- `PUT /api/work-orders/{blankNumber}/products/{itemId}` — заменить данные строки;
- `DELETE /api/work-orders/{blankNumber}/products/{itemId}` — удалить строку.

Тело POST и PUT соответствует `WorkOrderProductItemRequest`:

```json
{"productId":5,"quantity":2,"discountId":7}
```

Детали клиента:

- `POST /api/work-orders/{blankNumber}/customer-parts` — добавить принятую деталь;
- `GET /api/work-orders/{blankNumber}/customer-parts` — список деталей;
- `GET /api/work-orders/{blankNumber}/customer-parts/{partId}` — одна деталь;
- `PUT /api/work-orders/{blankNumber}/customer-parts/{partId}` — изменить деталь;
- `DELETE /api/work-orders/{blankNumber}/customer-parts/{partId}` — удалить деталь.

Тело POST и PUT соответствует `WorkOrderCustomerPartRequest`. Нельзя удалить
последнюю деталь из заказа без велосипеда. Вложенные ресурсы доступны только
через заказ, которому принадлежат; чужой ID вернёт 404.

## Создание товара при сохранении бланка

Строка `products` принимает либо `productId`, либо `newProduct` с полями
создания товара (`name`, `unit`, `price`, необязательные `article`, `category`,
`description`, `validFrom`). Новый товар, его начальная цена и строка бланка
сохраняются в одной транзакции. При ошибке сохранения бланка товар не создаётся.

`supplyStatus` принадлежит строке бланка: `IN_STOCK`, `TO_ORDER`, `ORDERED`,
`RECEIVED`. Если статус не передан, используется `IN_STOCK`. Статус возвращается
в `products` при чтении бланка и в CRUD строк товаров.
