# Task Manager REST API

Учебный проект на Spring Boot, Spring Web MVC и Spring Data JPA. Задачи хранятся в in-memory базе H2; схему при запуске создаёт Hibernate.

## Запуск

Требуются Java 21 и Maven 3.9+ (либо Maven Wrapper из проекта).

```bash
./mvnw clean test
./mvnw spring-boot:run
```

API доступен на `http://localhost:8080`. H2 Console: `http://localhost:8080/h2-console`, JDBC URL: `jdbc:h2:mem:taskdb`, пользователь: `sa`, пароль пустой. Для просмотра задач выполните `SELECT * FROM TASKS;`.

База настроена с `ddl-auto=create-drop`: таблица создаётся при старте и удаляется при остановке приложения, поэтому данные между запусками не сохраняются. SQL Hibernate выводится в консоль.

## API

| Метод | URI | Статус | Назначение |
|---|---|---:|---|
| POST | `/tasks` | 201 | Создать задачу |
| GET | `/tasks` | 200 | Страница задач, фильтрация и сортировка |
| GET | `/tasks/{id}` | 200 | Получить задачу |
| PUT | `/tasks/{id}` | 200 | Полностью обновить задачу |
| PATCH | `/tasks/{id}/status` | 200 | Изменить статус |
| DELETE | `/tasks/{id}` | 204 | Удалить задачу |
| GET | `/tasks/search?keyword=...` | 200 | Поиск по title и description |
| GET | `/tasks/stats` | 200 | Количество задач по статусам |

`GET /tasks` по умолчанию использует `page=0`, `size=10`, `sort=id` и возвращает стандартный объект `Page` с `content`, `totalElements`, `totalPages`, `number`, `size` и другими метаданными.

## Примеры

```bash
curl -i -X POST http://localhost:8080/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"Изучить JPA","description":"Derived queries","priority":"HIGH"}'

curl -i 'http://localhost:8080/tasks?page=0&size=3&sort=id'
curl -i 'http://localhost:8080/tasks?page=1&size=3&sort=id'
curl -i 'http://localhost:8080/tasks?status=NEW&page=0&size=10'
curl -i 'http://localhost:8080/tasks?priority=HIGH&page=0&size=10'
curl -i 'http://localhost:8080/tasks?status=NEW&priority=HIGH&page=0&size=10'
curl -i 'http://localhost:8080/tasks/search?keyword=jpa'
curl -i http://localhost:8080/tasks/stats

curl -i -X PUT http://localhost:8080/tasks/1 \
  -H 'Content-Type: application/json' \
  -d '{"title":"Изучить JPA","description":"Готово","priority":"HIGH","status":"DONE"}'

curl -i -X PATCH http://localhost:8080/tasks/1/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"IN_PROGRESS"}'

curl -i -X DELETE http://localhost:8080/tasks/1
```

Сборка исполняемого JAR:

```bash
./mvnw clean verify
java -jar target/task-manager-0.0.1-SNAPSHOT.jar
```
