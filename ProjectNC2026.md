# LMS Lite: тестовый проект для поступления в учебный центр

## Описание задания

Вы создадите REST API для платформы онлайн-обучения:
- **Инструкторы** создают курсы и уроки
- **Студенты** записываются на курсы, проходят уроки, сдают тесты
- **Система** отслеживает прогресс и выдаёт сертификаты

**Стек:** Java 21, Spring Boot 3.x, Spring Data JPA (Hibernate 6), PostgreSQL, Maven, Lombok
**Опционально:** Docker / Docker Compose

---

## Как проходит отбор

Нам важно увидеть, как вы думаете и пишете код.

1. **Выполняете проект самостоятельно** (сроки см. ниже).
2. **Присылаете ссылку на репозиторий** (GitHub / GitLab) с README: как запустить, что сделано, что не успели.
3. **Собеседование по вашему коду** (30-60 минут): мы открываем ваш проект и просим объяснить решения, показать, как что-то работает, и внести небольшое изменение «вживую».

> Подсказка: Не страшно, если опциональная часть не готова. Лучше меньше, но так, чтобы вы понимали каждую строчку. Код, который вы не можете объяснить, работает против вас - в том числе код, сгенерированный AI. Пользоваться AI можно в качестве советника по реализации, но объяснять в коде придётся всё.

### Структура и Сроки проекта

| Часть | Описание | Время |
|-------|----------|-------|
| **MVP (обязательная)** | Базовый функционал | 3-4 недели |
| **Дополнительные задания** | Расширенный функционал для тех, кто успевает | +1 неделя |

Разбивка по «неделям» ниже - это логические этапы, а не жёсткий график. Распределяйте время сами; рекомендуем выполнить за 4-5 недель.

### На что мы смотрим

| Критерий | Что оцениваем |
|----------|---------------|
| Понимание ООП | Наследование, полиморфизм, инкапсуляция, абстрактные классы |
| Слоистая архитектура | Разделение controller / service / repository, DTO вместо Entity в API |
| Работа с БД и JPA | Связи, стратегия наследования, ленивая загрузка, N+1 |
| Обработка ошибок | Кастомные исключения, `@RestControllerAdvice`, корректные HTTP-статусы |
| Валидация и безопасность данных | Bean Validation, не отдавать правильные ответы теста |
| Отказоустойчивость | Понимание retry / circuit breaker / fallback |
| Чистота кода | Нейминг, отсутствие дублирования, читаемость |
| Умение объяснить | Можете ли вы пояснить любое своё решение |

---

# Полезные ресурсы

- [Spring Boot Reference](https://docs.spring.io/spring-boot/reference/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/)
- [Resilience4j](https://resilience4j.readme.io/docs/getting-started-3)
- [Project Lombok](https://projectlombok.org/)
- [springdoc-openapi](https://springdoc.org/)

---

## Структура приложения

```text
lms-lite/
├── controller/    ← REST-эндпоинты
├── service/       ← Бизнес-логика
├── repository/    ← Работа с БД
├── model/         ← Сущности (Entity)
├── dto/           ← Объекты запросов/ответов
├── enums/         ← Перечисления
├── exception/     ← Кастомные исключения
└── scheduler/     ← Фоновые задачи (доп.)
```

## Доменная модель

```text
┌─────────────┐       ┌─────────────┐
│    User     │       │   Course    │
│ (абстракт.) │       │             │
└──────┬──────┘       └──────┬──────┘
       │                     │
   ┌───┴───┐                 │ 1:N
   │       │                 ▼
┌──┴──┐ ┌──┴──────┐   ┌─────────────┐
│Stud-│ │Instruct-│   │   Lesson    │
│ent  │ │or       │   │ (абстракт.) │
└──┬──┘ └─────────┘   └──────┬──────┘
   │                         │
   │ N:M                 ┌───┼───┬───────┐
   │                     │   │   │       │
   ▼                     ▼   ▼   ▼       │
┌─────────────┐      Video Text Quiz     │
│ Enrollment  │      Lesson Lesson Lesson│
│ (прогресс)  │                   │      │
└──────┬──────┘                   │ 1:N  │
       │                          ▼      │
       │                    ┌──────────┐ │
       │                    │ Question │ │
       │                    └──────────┘ │
       │ 1:1 (при завершении)            │
       ▼                                 │
┌─────────────┐                          │
│ Certificate │◄─────────────────────────┘
└─────────────┘
```

---

# ЧАСТЬ 1: MVP (обязательная)

---

# Этап 1: Настройка и базовые модели

## Цель
Настроить проект на стеке Java 21 + Spring Boot 3, создать первые сущности.

## Задачи

### 1. Настройка проекта
Сгенерируйте проект через [Spring Initializr](https://start.spring.io) (Maven, Java 21, Spring Boot 3.x) с зависимостями: Spring Web, Validation, Spring Data JPA, PostgreSQL Driver, Lombok. Ваш `pom.xml` должен соответствовать примеру ниже (версию Boot берите актуальную 3.x).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>3.5.6</version> <!-- или актуальная 3.x -->
		<relativePath/>
	</parent>

	<groupId>com.example</groupId>
	<artifactId>lms-lite</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>lms-lite</name>
	<description>LMS Lite Project</description>

	<properties>
		<java.version>21</java.version>
	</properties>

	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.postgresql</groupId>
			<artifactId>postgresql</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<configuration>
					<excludes>
						<exclude>
							<groupId>org.projectlombok</groupId>
							<artifactId>lombok</artifactId>
						</exclude>
					</excludes>
				</configuration>
			</plugin>
		</plugins>
	</build>
</project>
```

> Важно: В Spring Boot 3 пакеты `javax.persistence.*` и `javax.validation.*` заменены на `jakarta.persistence.*` и `jakarta.validation.*`. Если IDE подсказывает `javax`, это устаревший вариант.

Настройте подключение к PostgreSQL в `application.yml` (`spring.datasource.*`, `spring.jpa.hibernate.ddl-auto=update` на время разработки). Параметры подключения не хардкодьте в коде, берите из переменных окружения или профиля.

### 2. Создать Enums

| Enum | Значения | Описание |
|------|----------|----------|
| `CourseStatus` | `DRAFT`, `PUBLISHED`, `ARCHIVED` | Статус курса |
| `EnrollmentStatus` | `ACTIVE`, `COMPLETED`, `DROPPED` | Статус записи студента |
| `LessonType` | `VIDEO`, `TEXT`, `QUIZ` | Тип урока |

> Подсказка: В JPA храните enum как строку: `@Enumerated(EnumType.STRING)`.

### 3. Создать модели пользователей

> Подсказка: Можно использовать Lombok (`@Getter/@Setter`, `@NoArgsConstructor`). Для Entity будьте осторожны с `@Data`: он генерирует `equals/hashCode/toString` по всем полям, что ломается на двусторонних связях. Будьте готовы объяснить, почему.

**User (абстрактный класс):**

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long | PK, автогенерация |
| firstName | String | Имя |
| lastName | String | Фамилия |
| email | String | Уникальный |
| createdAt | LocalDateTime | Дата регистрации |

Методы: `getFullName()` -> "Имя Фамилия"

**Student (наследует User):** `enrollments`: `List<Enrollment>`

**Instructor (наследует User):** `bio`: String, `courses`: `List<Course>`

### 4. Создать модель Course

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long | PK |
| title | String | Название курса |
| description | String | Описание (до 2000 символов) |
| author | Instructor | Автор (ManyToOne) |
| status | CourseStatus | Статус курса |
| lessons | List&lt;Lesson&gt; | Уроки (OneToMany) |
| createdAt | LocalDateTime | Дата создания |
| publishedAt | LocalDateTime | Дата публикации (nullable) |

Методы:
- `getTotalLessons()` -> количество уроков
- `isAvailableForEnrollment()` -> true если статус PUBLISHED
- `publish()` -> меняет статус на PUBLISHED, ставит publishedAt

## Результат этапа
- [ ] Проект собирается на JDK 21 (`mvn clean verify`)
- [ ] Таблицы `students`, `instructors`, `courses` создаются в БД
- [ ] Все модели имеют геттеры и сеттеры

---

# Этап 2: Иерархия уроков

## Цель
Применить наследование и полиморфизм для разных типов уроков.

## Задачи

### 1. Абстрактный класс Lesson

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long | PK |
| title | String | Название урока |
| orderIndex | int | Порядковый номер в курсе |
| course | Course | ManyToOne |

Абстрактные методы:
- `getType()` -> возвращает LessonType
- `getEstimatedMinutes()` -> примерное время на урок

> Подсказка: Используйте `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)`.

### 2. Типы уроков

**VideoLesson:** `videoUrl` (String), `durationMinutes` (int). `getType()` -> `VIDEO`, `getEstimatedMinutes()` -> durationMinutes.

**TextLesson:** `content` (String, Markdown), `readingTimeMinutes` (int). `getType()` -> `TEXT`, `getEstimatedMinutes()` -> readingTimeMinutes.

**QuizLesson:** `questions` (`List<Question>`), `passingScore` (int, %), `timeLimitMinutes` (Integer, nullable). `getType()` -> `QUIZ`, `getEstimatedMinutes()` -> timeLimitMinutes или количество вопросов.

### 3. Модель Question

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long | PK |
| text | String | Текст вопроса |
| options | List&lt;String&gt; | Варианты ответов (`@ElementCollection`) |
| correctOptionIndex | int | Индекс правильного ответа |
| quiz | QuizLesson | ManyToOne |

Метод: `isCorrectAnswer(int index)`

### 4. Метод в Course

```text
getTotalEstimatedMinutes():
    total = 0
    для каждого урока в lessons:
        total += урок.getEstimatedMinutes()  // полиморфизм!
    вернуть total
```

> Подсказка: Это же можно записать через Stream API: `lessons.stream().mapToInt(Lesson::getEstimatedMinutes).sum()`.

## Результат этапа
- [ ] Таблица `lessons` создаётся с колонкой-дискриминатором
- [ ] Таблица `questions` создаётся
- [ ] Можно создать разные типы уроков
- [ ] `getTotalEstimatedMinutes()` работает для курса со смешанными уроками

---

# Этап 3: Репозитории и сервисы

## Цель
Создать слой доступа к данным и бизнес-логику.

## Задачи

### 1. Репозитории
Для каждой сущности создайте интерфейс, наследующий `JpaRepository<Entity, Long>`.

**CourseRepository:**

| Метод | Описание |
|-------|----------|
| `findByStatus(CourseStatus)` | Курсы по статусу |
| `findByAuthorId(Long)` | Курсы автора |
| `findByTitleContainingIgnoreCase(String)` | Поиск по названию |

**StudentRepository:** `findByEmail(String)`, `existsByEmail(String)`

**LessonRepository:** `findByCourseIdOrderByOrderIndexAsc(Long)`

### 2. Модели Enrollment и LessonProgress

**Enrollment:**

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long | PK |
| student | Student | ManyToOne |
| course | Course | ManyToOne |
| status | EnrollmentStatus | Статус |
| completedLessons | Set&lt;LessonProgress&gt; | Пройденные уроки |
| enrolledAt | LocalDateTime | Дата записи |
| completedAt | LocalDateTime | Дата завершения (nullable) |

Метод: `getProgressPercent()` -> (пройденные / всего) * 100

**LessonProgress:** `id`, `enrollment` (ManyToOne), `lesson` (ManyToOne), `completedAt`, `quizScore` (Integer, nullable).

### 3. Сервисы

**CourseService:**

| Метод | Логика |
|-------|--------|
| `createCourse(title, description, instructorId)` | Создать курс в статусе DRAFT |
| `publishCourse(courseId)` | Проверить, что есть уроки, изменить статус |
| `getPublishedCourses()` | Только PUBLISHED |
| `getCourseById(id)` | Курс или исключение |

**EnrollmentService:**

| Метод | Логика |
|-------|--------|
| `enrollStudent(studentId, courseId)` | Курс опубликован, студент не записан |
| `getStudentEnrollments(studentId)` | Список записей студента |
| `completeLesson(enrollmentId, lessonId)` | Отметить урок пройденным |
| `getProgress(enrollmentId)` | Процент прогресса |

Используйте внедрение зависимостей через конструктор (`@RequiredArgsConstructor` + `final`-поля) и `@Transactional` там, где это нужно.

### 4. Кастомные исключения

| Исключение | Когда выбрасывать |
|------------|-------------------|
| `CourseNotFoundException` | Курс не найден |
| `StudentNotFoundException` | Студент не найден |
| `AlreadyEnrolledException` | Студент уже записан |
| `CourseNotPublishedException` | Запись на неопубликованный курс |

## Результат этапа
- [ ] Репозитории созданы
- [ ] Сервисы содержат бизнес-логику
- [ ] Исключения выбрасываются в нужных случаях
- [ ] Студента можно записать на курс через сервис

---

# Этап 4: REST API, пользователи и курсы

## Цель
Создать эндпоинты для курсов, уроков и пользователей.

## Задачи

### 1. DTO
**Зачем:** не отдавать Entity напрямую, контролировать, что видит клиент и избежать бесконечной рекурсии при сериализации.

Для DTO в Java 21 удобно использовать `record`. Создайте:
- `CreateCourseRequest` (title, description, instructorId)
- `CreateLessonRequest` (title, type, videoUrl/content/questions...)
- `CourseResponse` (id, title, description, authorName, status, totalLessons, duration)
- `LessonResponse` (id, title, type, orderIndex)

### 2. Инструкторы и студенты

```text
POST   /api/instructors              - регистрация инструктора
GET    /api/instructors/{id}         - профиль инструктора
GET    /api/instructors/{id}/courses - курсы инструктора

POST   /api/students                 - регистрация студента
GET    /api/students/{id}            - профиль студента
```

### 3. Курсы

```text
POST   /api/courses               - создать курс (DRAFT)
GET    /api/courses               - список опубликованных курсов
GET    /api/courses/{id}          - детали курса с уроками
PUT    /api/courses/{id}          - обновить курс
DELETE /api/courses/{id}          - удалить курс (только DRAFT)
PATCH  /api/courses/{id}/publish  - опубликовать курс
PATCH  /api/courses/{id}/archive  - архивировать курс
```

### 4. Уроки

```text
POST   /api/courses/{courseId}/lessons - добавить урок
GET    /api/courses/{courseId}/lessons - список уроков курса
GET    /api/lessons/{id}               - детали урока
PUT    /api/lessons/{id}               - обновить урок
DELETE /api/lessons/{id}               - удалить урок
```

### 5. Обработка ошибок
Создайте `@RestControllerAdvice` для глобальной обработки:

| Исключение | HTTP-статус | Сообщение |
|------------|-------------|-----------|
| `CourseNotFoundException` | 404 | "Курс не найден" |
| `AlreadyEnrolledException` | 409 | "Вы уже записаны на этот курс" |
| `IllegalStateException` | 400 | Текст исключения |

> Подсказка: Для единого формата ошибок можно использовать `ProblemDetail` (RFC 7807), встроенный в Spring 6 / Boot 3.

## Результат этапа
- [ ] Можно создать инструктора и студента через API
- [ ] Можно создать курс с уроками
- [ ] Можно получить список курсов и детали курса
- [ ] Ошибки возвращаются в понятном JSON-формате

---

# Этап 5: REST API, записи, прогресс и тесты

## Цель
Реализовать запись на курсы, отслеживание прогресса и прохождение тестов.

## Задачи

### 1. Записи (Enrollment)

```text
POST   /api/enrollments                - записаться на курс
GET    /api/students/{id}/enrollments  - мои курсы
GET    /api/enrollments/{id}           - детали записи с прогрессом
DELETE /api/enrollments/{id}           - отписаться (статус -> DROPPED)
```

**POST /api/enrollments, логика:**
1. Студент существует
2. Курс существует и опубликован
3. Студент ещё не записан
4. Создать Enrollment со статусом ACTIVE
5. Вернуть созданную запись

### 2. Прогресс

```text
POST   /api/enrollments/{id}/lessons/{lessonId}/complete - отметить урок пройденным
GET    /api/enrollments/{id}/progress                    - текущий прогресс
```

**Логика `complete`:**
1. Enrollment существует и активен
2. Урок принадлежит курсу
3. Если урок уже пройден, вернуть текущий статус
4. Создать LessonProgress
5. Если все уроки пройдены -> enrollment.status = COMPLETED
6. Вернуть обновлённый прогресс

### 3. Тесты

```text
GET    /api/lessons/{quizId}/questions - вопросы (без правильных ответов!)
POST   /api/lessons/{quizId}/submit    - отправить ответы
```

### 4. DTO для прогресса и тестов

**ProgressResponse:**
```json
{
  "enrollmentId": 1,
  "courseTitle": "Java для начинающих",
  "progressPercent": 45,
  "completedLessons": 5,
  "totalLessons": 11,
  "status": "ACTIVE",
  "lessons": [
    { "id": 1, "title": "Введение", "completed": true },
    { "id": 3, "title": "Циклы", "completed": false }
  ]
}
```

**QuestionResponse (для студента):**
```json
{
  "id": 1,
  "text": "Что выведет System.out.println(2+2)?",
  "options": ["3", "4", "22", "Ошибка"]
}
```
Поле `correctOptionIndex` в ответ **не включаем**.

**SubmitQuizRequest:**
```json
{
  "answers": [
    { "questionId": 1, "selectedOption": 1 },
    { "questionId": 2, "selectedOption": 0 }
  ]
}
```

**QuizResultResponse:**
```json
{
  "quizId": 5,
  "quizTitle": "Тест по циклам",
  "totalQuestions": 10,
  "correctAnswers": 8,
  "scorePercent": 80,
  "passed": true,
  "passingScore": 70
}
```

### 5. Логика проверки теста

```text
submitQuiz(enrollmentId, quizId, answers):
    1. Enrollment активен
    2. Quiz принадлежит курсу
    3. Получить все вопросы теста
    4. Для каждого ответа:
       - найти вопрос по questionId
       - сравнить selectedOption с correctOptionIndex
       - совпало -> correctCount++
    5. scorePercent = (correctCount * 100) / totalQuestions
    6. passed = scorePercent >= quiz.passingScore
    7. Сохранить результат в LessonProgress (quizScore)
    8. Если passed -> отметить урок пройденным
    9. Вернуть QuizResultResponse
```

### 6. Валидация
Добавьте в DTO аннотации `jakarta.validation`: `@NotBlank`, `@Email`, `@Size(min, max)`, `@NotNull`, `@Min/@Max`. В контроллерах используйте `@Valid`.

## Результат этапа
- [ ] Студент может записаться на опубликованный курс
- [ ] Нельзя записаться дважды
- [ ] Можно отмечать уроки пройденными, прогресс считается правильно
- [ ] Можно сдать тест и получить результат
- [ ] При прохождении всех уроков статус меняется на COMPLETED

---

# Этап 6: Сертификаты и работа с файлами

## Цель
Генерировать сертификаты при завершении курса.

## Задачи

### 1. Модель Certificate

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long | PK |
| student | Student | ManyToOne |
| course | Course | ManyToOne |
| issuedAt | LocalDate | Дата выдачи |
| uniqueCode | String | Уникальный код (UUID) |

### 2. Автоматическая генерация
Когда enrollment.status становится COMPLETED:
1. Проверить, что сертификат ещё не выдан
2. Создать Certificate с уникальным кодом (UUID)
3. Сохранить в БД

### 3. Эндпоинты

```text
GET    /api/students/{id}/certificates - мои сертификаты
GET    /api/certificates/{code}        - проверить сертификат по коду
GET    /api/certificates/{id}/download - скачать сертификат
```

### 4. Текстовый сертификат

```text
═══════════════════════════════════════════
           СЕРТИФИКАТ О ПРОХОЖДЕНИИ
═══════════════════════════════════════════

Настоящим подтверждается, что

            Иван Иванов

успешно завершил(а) курс

         "Java для начинающих"

Дата выдачи: 15 января 2026
Код сертификата: ABC-123-XYZ

═══════════════════════════════════════════
```

**Реализация:**
- Текст-блоки (`"""`) или `StringBuilder` / `String.formatted()`
- Возврат как `ResponseEntity<byte[]>` с `Content-Type: text/plain; charset=UTF-8`
- Или запись в файл через `Files.writeString` (NIO)

## Результат этапа
- [ ] Сертификат создаётся автоматически при завершении курса
- [ ] Можно проверить сертификат по коду
- [ ] Можно скачать сертификат как текстовый файл

---


# ЧАСТЬ 2: Дополнительные задания

> Выполняйте по желанию, в любом порядке. Каждое задание независимо. Для собеседования достаточно 1-2 заданий, сделанных качественно.

---

## Docker (рекомендуем)

**Сложность:** 2 из 3
**Время:** 2-3 часа

### Описание
Упакуйте приложение и базу данных так, чтобы проект запускался одной командой.

### Требования
- `Dockerfile` для приложения (желательно multi-stage: сборка Maven -> лёгкий образ с JRE 21, например `eclipse-temurin:21-jre`)
- `docker-compose.yml` с двумя сервисами: `app` и `db` (PostgreSQL)
- Параметры подключения к БД передаются через переменные окружения
- Данные БД сохраняются в volume
- Приложение стартует после готовности БД (`healthcheck` + `depends_on: condition: service_healthy`)
- Файл `.dockerignore`
- В README: команда запуска (`docker compose up --build`)

### Результат
- [ ] `docker compose up --build` поднимает приложение и БД с нуля
- [ ] Swagger / эндпоинты доступны на `localhost:8080`
- [ ] Контейнер не запускается от root (бонус)
- [ ] Данные не теряются после `docker compose restart`

---

## Задание 1: PDF-сертификаты

**Сложность:** 2 из 3 | **Время:** 3-4 часа

Замените текстовые сертификаты на PDF.

**Зависимость (Maven):**
```xml
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext7-core</artifactId>
    <version>7.2.5</version>
    <type>pom</type>
</dependency>
```
Можно использовать другую библиотеку (например, OpenPDF), объясните выбор и учтите лицензию (у iText она AGPL).

**Требования:**
- Ландшафтная ориентация
- Имя студента и название курса: крупным шрифтом по центру
- Код и дата: внизу
- `ResponseEntity<byte[]>` с `Content-Type: application/pdf`
- Опционально: рамка, шрифт с кириллицей, логотип

**Результат:**
- [ ] `/api/certificates/{id}/download` возвращает корректный PDF

---

## Задание 2: Экспорт данных в CSV/JSON

**Сложность:** 2 из 3 | **Время:** 2-3 часа

```text
GET /api/courses/{id}/export?format=csv
GET /api/courses/{id}/export?format=json
```

```csv
student_id,name,email,enrolled_at,progress,status
1,Иван Иванов,ivan@mail.ru,2026-01-10,100,COMPLETED
2,Мария Петрова,maria@mail.ru,2026-01-12,45,ACTIVE
```

- CSV: `StringBuilder`/`PrintWriter` (подумайте об экранировании запятых и кавычек)
- JSON: Jackson `ObjectMapper`
- Заголовки: `Content-Type`, `Content-Disposition: attachment; filename="course_1_students.csv"`

**Результат:** файлы скачиваются и корректно открываются в Excel и редакторе.

---

## Задание 3: Импорт вопросов из JSON

**Сложность:** 2 из 3 | **Время:** 2-3 часа

```text
POST /api/lessons/{quizId}/questions/import
Content-Type: multipart/form-data
```

```json
{
  "questions": [
    {
      "text": "Что выведет System.out.println(2+2)?",
      "options": ["3", "4", "22", "Ошибка компиляции"],
      "correctOptionIndex": 1
    }
  ]
}
```

**Реализация:** `@RequestParam("file") MultipartFile file` -> `ObjectMapper` -> создать `Question`, связать с `QuizLesson` -> сохранить -> вернуть количество.

**Ошибки:** невалидный JSON -> 400; пустой текст/неверный индекс -> 400 с описанием; quiz не найден -> 404. Всё или ничего: при ошибке в одном вопросе не сохранять ни один (транзакция).

---

## Задание 4: Фоновые задачи (@Scheduled)

**Сложность:** 2 из 3 | **Время:** 2-3 часа

**Напоминания:** раз в день в 9:00 найти enrollments со статусом ACTIVE, прогрессом > 0% и активностью более 7 дней назад; залогировать.

**Автоархивация:** раз в неделю (воскресенье, 3:00) переводить в ARCHIVED курсы PUBLISHED без новых записей более 6 месяцев.

```java
@EnableScheduling // в главном классе или конфигурации

@Component
public class ReminderScheduler {
    @Scheduled(cron = "0 0 9 * * *")
    public void sendReminders() { /* ... */ }
}
```

---

## Задание 5: Статистика курсов

**Сложность:** 3 из 3 | **Время:** 4-5 часов

```text
GET /api/courses/{id}/statistics
GET /api/statistics/top-courses
GET /api/instructors/{id}/statistics
```

**CourseStatisticsResponse:**
```json
{
  "courseId": 1,
  "courseTitle": "Java для начинающих",
  "totalEnrollments": 150,
  "activeStudents": 45,
  "completedStudents": 89,
  "droppedStudents": 16,
  "completionRate": 59.3,
  "averageProgress": 67.5,
  "averageQuizScore": 78.2
}
```

Расчёты: `completionRate` = completed / total * 100; `averageProgress` = средний прогресс; `averageQuizScore` = средний балл по тестам курса.

> Подсказка: Считайте агрегаты в БД (`@Query`, `GROUP BY`, `COUNT/AVG`), а не загружайте все записи в память. Будьте готовы объяснить разницу.

Остальные DTO (`TopCoursesResponse`, `InstructorStatisticsResponse`) спроектируйте самостоятельно по описанию эндпоинтов: топ курсов по числу записей за 30 дней; у инструктора: число курсов, опубликованных курсов, студентов, средний completion rate, выданных сертификатов.

---

## Задание 6: Рейтинг и отзывы

**Сложность:** 3 из 3 | **Время:** 4-5 часов

**CourseReview:** `id`, `enrollment` (OneToOne), `rating` (1-5), `comment` (nullable, до 1000), `createdAt`.

```text
POST   /api/enrollments/{id}/review
GET    /api/courses/{id}/reviews   - с пагинацией
GET    /api/courses/{id}/rating    - средний рейтинг и распределение
```

**Правила:** отзыв только для COMPLETED; один отзыв на enrollment; рейтинг 1-5 (валидация); комментарий опционален.

Пример ответа рейтинга:
```json
{
  "courseId": 1,
  "averageRating": 4.7,
  "totalReviews": 89,
  "distribution": { "5": 60, "4": 20, "3": 5, "2": 3, "1": 1 }
}
```

---

## Задание 7: Изменение порядка уроков

**Сложность:** 2 из 3 | **Время:** 2-3 часа

```text
PATCH /api/courses/{courseId}/lessons/reorder
```
```json
{ "lessonOrder": [3, 1, 4, 2, 5] }
```

**Логика:** все ID принадлежат курсу -> количество совпадает -> нет дубликатов -> обновить `orderIndex` -> сохранить. Любое нарушение -> 400.

---

## Задание 8: Результаты всех тестов

**Сложность:** 1 из 3 | **Время:** 1-2 часа

```text
GET /api/enrollments/{id}/quiz-results
```

Для каждого теста курса: `lessonId`, `lessonTitle`, `score` (null, если не сдавался), `passed`, `attempts`, `lastAttemptAt`. Итого: `averageScore`, `passedQuizzes`, `totalQuizzes`.

> Для хранения количества попыток понадобится изменить модель. Обоснуйте, как именно.

---

## Задание 9: Тесты

**Сложность:** 3 из 3 | **Время:** 4-6 часов

**Unit-тесты (JUnit 5 + Mockito)**

EnrollmentService: успешная запись; запись на неопубликованный курс; повторная запись; расчёт прогресса; автозавершение при 100%.
QuizService: подсчёт баллов; passed/failed; сохранение лучшего результата при пересдаче.

```java
@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CourseRepository courseRepository;
    @InjectMocks private EnrollmentService enrollmentService;

    @Test
    void enrollStudent_CourseNotPublished_ThrowsException() {
        // Given: курс в статусе DRAFT
        // When: enrollStudent()
        // Then: CourseNotPublishedException
    }
}
```

**Бонус:** интеграционный тест с Testcontainers (PostgreSQL) или `@WebMvcTest` для контроллера.

**Результат:**
- [ ] Минимум 5 тестов для EnrollmentService и 3 для QuizService
- [ ] `mvn test` проходит

---

## Задание 10: Swagger / OpenAPI

**Сложность:** 2 из 3 | **Время:** 2-3 часа

Для Spring Boot 3 используйте springdoc-openapi 2.x (старый `springdoc-openapi-ui` 1.x с Boot 3 **не работает**):

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.13</version> <!-- или актуальная 2.x -->
</dependency>
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

Добавьте аннотации к контроллерам:

```java
@Operation(summary = "Создать курс", description = "Создаёт новый курс в статусе DRAFT")
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "Курс создан"),
    @ApiResponse(responseCode = "400", description = "Невалидные данные"),
    @ApiResponse(responseCode = "404", description = "Инструктор не найден")
})
@PostMapping
public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request) { /* ... */ }
```

**Результат:** Swagger UI показывает все эндпоинты, можно выполнять запросы, основные эндпоинты описаны.

---

# Что сдавать

- Ссылка на Git-репозиторий (с историей коммитов, не один коммит «final»)
- `README.md`: как запустить (локально и/или Docker), какие этапы выполнены, принятые допущения, что не успели
- `.gitignore` (без `target/`, `.idea/` и паролей)

# Формат собеседования по проекту

1. **Обзор (5-10 мин):** расскажите об архитектуре и о том, что считаете самым удачным и самым слабым местом.
2. **Разбор кода (15-25 мин):** мы выберем несколько мест в вашем коде и попросим объяснить: «как это работает», «почему так», «что будет, если...».
3. **Мини-изменение вживую (10-15 мин):** например, добавить поле, новый эндпоинт, правило валидации или тип урока.
4. **Общие вопросы:** Java 21 (records, switch expressions, pattern matching, virtual threads), Spring Boot 3, SQL, Git.

Удачи!

