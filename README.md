# GymTracker - Aplikacja Mobilna (Android)

Witamy w repozytorium aplikacji mobilnej GymTracker! Jest to klient na platformę Android, który pozwala użytkownikom efektywnie śledzić swoje treningi, monitorować postępy w pomiarach ciała, wyznaczać cele fitness oraz przeglądać osobiste osiągnięcia.

Aplikacja została napisana w języku **Java** z wykorzystaniem **Android SDK** i komunikuje się z dedykowanym backendem GymTracker (stworzonym w Spring Boot) poprzez API REST.

## Kluczowe Funkcjonalności

*   Rejestracja i logowanie użytkowników.
*   Tworzenie i zarządzanie spersonalizowanymi planami treningowymi.
*   Zapisywanie szczegółowych dzienników treningowych (ćwiczenia, serie, powtórzenia, obciążenie).
*   Śledzenie pomiarów ciała (waga, obwody) i wizualizacja postępów na wykresach.
*   Wyznaczanie i monitorowanie celów treningowych (np. docelowa waga, liczba dni treningowych).
*   Przeglądanie osiągnięć i rekordów w kluczowych ćwiczeniach.
*   Intuicyjny interfejs użytkownika.

## Wymagania Systemowe

*   Android Studio (zalecana najnowsza stabilna wersja)
*   Emulator Android lub fizyczne urządzenie z systemem Android (API Level 24 lub wyższy)
*   Działający serwer backendowy GymTracker (zobacz repozytorium backendu)

## Uruchomienie Aplikacji

1.  **Sklonuj repozytorium:**
    ```bash
    git clone [URL_TWOJEGO_FRONTEND_REPO]
    cd [nazwa_folderu_frontend]
    ```
2.  **Otwórz projekt w Android Studio.**
3.  **Konfiguracja połączenia z Backendem:**
    *   Adres URL bazowy backendu jest zdefiniowany w pliku:
        `app/src/main/java/api/model/ApiClient.java`
        ```java
        private static final String BASE_URL = "http://10.0.2.2:8080/api/v1/";
        ```
    *   `10.0.2.2` to specjalny alias adresu IP hosta (Twojego komputera), gdy uruchamiasz aplikację na emulatorze Android.
    *   Jeśli backend działa na innym adresie lub porcie, lub jeśli testujesz na fizycznym urządzeniu (w tej samej sieci Wi-Fi), zmień ten adres na odpowiedni adres IP Twojego komputera w sieci lokalnej (np. `http://192.168.1.100:8080/api/v1/`).
4.  **Upewnij się, że serwer backendowy GymTracker jest uruchomiony i dostępny** pod skonfigurowanym adresem.
5.  **Zbuduj i uruchom aplikację** na wybranym emulatorze lub urządzeniu.

## Struktura Projektu

*   `/app/src/main/java/api/model/`: Modele DTO, klient API (Retrofit) oraz interfejs serwisu API.
*   `/app/src/main/java/com/example/gymtracker/`: Główne pliki źródłowe aplikacji (Aktywności, Adaptery, klasy pomocnicze).
*   `/app/src/main/res/`: Zasoby aplikacji (layouty, drawable, stringi, itp.).
*   `/app/build.gradle.kts`: Skrypt budowania Gradle dla modułu aplikacji.

## Technologie

*   Java
*   Android SDK
*   Retrofit 2 (do komunikacji sieciowej)
*   Gson (do parsowania JSON)
*   MPAndroidChart (do generowania wykresów)
*   Material Design Components

---

Pamiętaj, aby zastąpić `[URL_TWOJEGO_FRONTEND_REPO]` i `[nazwa_folderu_frontend]` rzeczywistymi wartościami.
