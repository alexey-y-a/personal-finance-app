## Консольное приложение для управления личными финансами
Java CLI-приложение для отслеживания доходов/расходов, установки бюджетов и анализа финансов.
Поддерживает мультипользовательский режим, persistence в JSON, оповещения и экспорт.

## Функциональность
- Авторизация/регистрация пользователей.
- Добавление доходов/расходов по категориям.
- Установка/редактирование бюджетов per категория.
- Статистика: общая/по категориям (с фильтрами), остаток бюджета.
- Оповещения: превышение бюджета (вкл. 80% лимита), расходы > доходов, нулевой баланс.
- Переводы между пользователями.
- Экспорт статистики в JSON.
- Валидация ввода, уведомления об ошибках/неизвестных категориях.

## Требования
- Java 17+.
- Maven 3.6+.

## Установка и запуск
1. Клонируйте репозиторий: `git clone https://github.com/alexey-y-a/personal-finance-app`.
2. Соберите: `mvn clean package`.
3. Запустите: `mvn exec:java` или `java -jar target/personal-finance-app-1.0.0.jar`.

Данные сохраняются в `wallets/{username}.wallet.json`.

## Использование

1. Зарегистрируйтесь или войдите в существующий аккаунт (`register/login <user> <pass>`).
2. Добавляйте доходы и расходы по категориям (`add <income|expense> <cat> <amt>`).
3. Устанавливайте бюджеты для контроля расходов (`set/edit budget <cat> <amt>`).
4. Просматривайте статистику по своим финансам (`stats [categories <cats>]`).

## Команды (UX CLI)
Вводите команды в формате `command [args]` (help для справки). Примеры:
- `register user1 pass123` — регистрация.
- `login user1 pass123` — вход.
- `add income salary 50000` — добавить доход.
- `add expense food 1000` — добавить расход (оповещение если >80% бюджета).
- `set budget food 4000` — установить бюджет.
- `edit budget food 5000` — обновить бюджет.
- `stats` — общая статистика.
- `stats categories food transport` — статистика по категориям (уведомление если категория не найдена).
- `transfer user2 1000` — перевод (расход у вас, доход у получателя).
- `export stats.json` — экспорт в файл.
- `list categories` — список категорий.
- `logout` — выход (с сохранением).
- `help` — справка.
- `exit` — завершение.

## Пример вывода статистики:

```
Общий доход: 63000.0
Общие расходы: 8300.0

Category       Income      Expense     Budget     Remaining
Зарплата       60000.0     0.0         N/A        N/A
Еда            0.0         800.0       4000.0     3200.0
Развлечения    0.0         3000.0      3000.0     0.0

Внимание: Расходы превышают доходы!
```

## Тестирование
- Unit/интеграционные: `mvn test`.
- Покрытие: `mvn jacoco:report` (отчёт в target/site/jacoco/index.html).
- Примеры: Тесты на авторизацию, операции, бюджеты, persistence, переводы.

## Архитектура
Слои (SOLID, DI через конструкторы):
- **Model (core)**: User, Wallet, Transaction (enum Type: INCOME/EXPENSE).
- **Service (core)**: UserService, FinanceService (логика подсчётов, оповещений).
- **Infra**: Storage интерфейс, FileJsonStorage (Gson для JSON).
- **CLI**: ConsoleApp (парсинг команд, вывод таблиц).

## CI/CD

(см. .github/workflows/maven.yml)

GitHub Actions:
- Сборка: mvn -B clean compile — компиляция кода,
- Тесты: mvn -B test jacoco:report — запуск JUnit + отчёт покрытия (JaCoCo),
- Линт: mvn spotless:check — проверка стиля (если fail — CI падает).

## Структура проекта

```
src/
├── main/
│   └── java/
│      └── ru/
│          └── financeapp/
│              ├── Main.java  # Точка входа приложения
│              ├── cli/       # CLI-слой
│              │   ├── CommandParser.java  # Парсер команд пользователя
│              │   └── ConsoleApp.java     # Основной CLI-класс
│              ├── core/      # Core-слой (модели и сервисы)
│              │   ├── User.java           # Модель пользователя
│              │   ├── Wallet.java         # Модель кошелька
│              │   ├── Transaction.java    # Модель транзакции
│              │   ├── UserService.java    # Сервис пользователей
│              │   └── FinanceService.java # Сервис финансов
│              ├── exceptions/ # Пользовательские исключения
│              │   ├── InvalidCredentialsException.java
│              │   ├── InvalidInputException.java
│              │   └── UserNotFoundException.java
│              └── infra/     # Инфраструктура (хранение данных)
│                  ├── Storage.java        # Интерфейс хранения
│                  └── FileJsonStorage.java # JSON-реализация хранения
│
└── test/
└── java/
└── ru/
└── financeapp/
├── cli/      # Тесты CLI
│   ├── CommandParserTest.java
│   └── ConsoleAppTest.java
├── core/     # Тесты core (unit для сервисов/моделей)
│   ├── FinanceServiceTest.java
│   └── UserServiceTest.java
└── infra/    # Тесты инфраструктуры
└── FileJsonStorageTest.java
```
