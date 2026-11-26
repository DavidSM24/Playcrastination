# Guía de Android en Java

Esta guía complementa el [AGENTS.md](../AGENTS.md) de Playcrastination. Los ejemplos en Java y vistas XML se aplican a cambios que usen esas tecnologías. Kotlin, Jetpack Compose, Hilt, Flow y coroutines siguen siendo la base del módulo `app`. Las pautas generales de Android Studio, persistencia, seguridad y verificación se aplican según el alcance del cambio.

## Configuración en Android Studio

- Abre la raíz del repositorio, sincroniza con el Gradle Wrapper y selecciona el módulo `app` en la variante `debug` para ejecutar localmente. Sigue los requisitos de JDK y SDK del [README](../README.md).
- Distingue el JDK con el que se ejecuta Gradle del nivel de compilación del código. El módulo configura Java 17 y el mismo destino JVM para Kotlin en [app/build.gradle.kts](../app/build.gradle.kts).
- Consulta el [catálogo de versiones](../gradle/libs.versions.toml) antes de incorporar una dependencia.

## Arquitectura y ciclo de vida

- Agrupa las clases de UI por funcionalidad. Coloca el acceso a APIs, DAOs y archivos en la capa de datos. Extrae un caso de uso o un módulo cuando exista lógica que reutilizar o una frontera que el proyecto necesite mantener.
- Crea los `ViewModel` de pantalla con una `ViewModelProvider.Factory` cuando tengan dependencias. Si el proyecto usa Hilt, utiliza su factory generada con `@HiltViewModel` e inyección por constructor. Usa `SavedStateHandle` para el estado pequeño que deba restaurarse tras una recreación del proceso.
- En una actividad, vincula las vistas a la instancia de la actividad. En un fragmento con View Binding, infla el binding en `onCreateView()`, observa `viewModel.state()` desde `onViewCreated()` con `getViewLifecycleOwner()` y establece `binding = null` en `onDestroyView()`. Usa el grafo de navegación del proyecto si existe.
- Para operaciones inmediatas de red o disco, reutiliza un executor gestionado por la aplicación o la capa de datos. Si una dependencia de larga duración necesita `Context`, proporciónale el contexto de aplicación. Mantén los textos de UI y las descripciones de accesibilidad en los recursos correspondientes.

Referencias de Android. [Arquitectura para Views](https://developer.android.com/topic/architecture/views/recommendations-views) · [Factories de ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories) · [View Binding](https://developer.android.com/topic/libraries/view-binding) · [Trabajo asíncrono en Java](https://developer.android.com/develop/background-work/background-tasks/asynchronous/java-threads)

### Ejemplo de estado de pantalla y carga asíncrona

La pantalla invoca `load()` en el hilo principal al iniciar la carga o al reintentar. Si el `ViewModel` ya tiene un estado al recrear la vista, la pantalla lo vuelve a observar. Inyecta un executor compartido para E/S y otro que ejecute en el hilo principal, por ejemplo `ContextCompat.getMainExecutor(application)`. La capa que crea el executor de E/S controla su ciclo de vida. El repositorio debe configurar tiempos de espera apropiados para la red y devolver un nombre válido al completar la carga.

```java
import androidx.annotation.MainThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;

public final class ProfileViewModel extends ViewModel {
    public interface ProfileRepository {
        String fetchName() throws IOException;
    }

    public static final class UiState {
        public enum Status { LOADING, CONTENT, ERROR }

        public final Status status;
        public final String name;

        private UiState(Status status, String name) {
            this.status = status;
            this.name = name;
        }
    }

    private final ProfileRepository repository;
    private final Executor ioExecutor;
    private final Executor mainExecutor;
    private final MutableLiveData<UiState> state = new MutableLiveData<>();
    private FutureTask<Void> pendingLoad;
    private int requestId;

    public ProfileViewModel(ProfileRepository repository, Executor ioExecutor,
                            Executor mainExecutor) {
        this.repository = repository;
        this.ioExecutor = ioExecutor;
        this.mainExecutor = mainExecutor;
    }

    public LiveData<UiState> state() {
        return state;
    }

    @MainThread
    public void load() {
        cancelPendingLoad();
        int currentRequest = ++requestId;
        FutureTask<Void> task = new FutureTask<>(() -> {
            UiState result = fetchState();
            mainExecutor.execute(() -> {
                if (currentRequest == requestId) {
                    pendingLoad = null;
                    state.setValue(result);
                }
            });
            return null;
        });

        pendingLoad = task;
        state.setValue(new UiState(UiState.Status.LOADING, null));
        if (currentRequest != requestId) {
            return;
        }
        try {
            ioExecutor.execute(task);
        } catch (RejectedExecutionException exception) {
            pendingLoad = null;
            state.setValue(new UiState(UiState.Status.ERROR, null));
        }
    }

    private UiState fetchState() {
        try {
            return new UiState(UiState.Status.CONTENT, repository.fetchName());
        } catch (IOException exception) {
            return new UiState(UiState.Status.ERROR, null);
        }
    }

    @Override
    protected void onCleared() {
        requestId++;
        cancelPendingLoad();
    }

    private void cancelPendingLoad() {
        if (pendingLoad != null) {
            pendingLoad.cancel(true);
            pendingLoad = null;
        }
    }
}
```

La vista traduce `ERROR` a un mensaje de `strings.xml` y puede ofrecer un botón de reintento. La interrupción solo detiene operaciones que la admiten. Si la biblioteca de red ofrece un mecanismo de cancelación, propágala a la solicitud desde el repositorio.

## Persistencia y trabajo en segundo plano

- Usa Room cuando el proyecto necesite una base de datos relacional local o ya la utilice. Ejecuta el acceso bloqueante al DAO fuera del hilo principal. Al cambiar el esquema, conserva las rutas de migración de las versiones admitidas y prueba la actualización con datos existentes. Para probar consultas, usa una base en memoria en un test instrumentado.
- Usa WorkManager para tareas persistentes como sincronizaciones aplazables. Declara restricciones como conectividad cuando correspondan. Devuelve `Result.retry()` para fallos transitorios y `Result.failure()` para fallos permanentes. Diseña las escrituras para que un reintento no duplique registros o envíos y usa trabajo único cuando varias acciones puedan encolar la misma tarea lógica.
- Reutiliza el cargador de imágenes del proyecto para imágenes remotas y utiliza un tamaño acorde a la vista. Mantén las jerarquías XML simples. Considera `ConstraintLayout` cuando simplifique un layout y `ViewStub` para contenido pesado que rara vez se muestra. Antes de añadir complejidad por rendimiento, mide el problema con las herramientas del IDE.

Referencias de Android. [Pruebas de Room](https://developer.android.com/training/data-storage/room/testing-db) · [Migraciones de Room](https://developer.android.com/training/data-storage/room/migrating-db-versions) · [WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent)

## Seguridad e integración con otras apps

- Para acciones internas o destinatarios sensibles conocidos, usa un intent explícito hacia el componente previsto. Usa el selector del sistema cuando la persona deba elegir con qué app compartir y limita el contenido a lo que decidió compartir. Evita incluir tokens o datos privados en intents implícitos.
- Declara `android:exported="false"` para componentes internos que admitan el atributo. Exporta los puntos de entrada que realmente lo necesiten, como la actividad de inicio, y valida los datos y permisos en los componentes que acepten llamadas externas. Para compartir archivos puntualmente, usa `FileProvider` con rutas acotadas y permisos temporales sobre la URI.
- Usa HTTPS y el almacén de confianza del sistema. Si aplicas una política de red, referencia el recurso XML mediante `android:networkSecurityConfig="@xml/network_security_config"` en `<application>`. Comprueba que el cliente HTTP respete esa política y aísla en la variante de depuración cualquier excepción que necesites para desarrollo.

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
</network-security-config>
```

Referencias de Android. [Intents implícitos](https://developer.android.com/privacy-and-security/risks/implicit-intent-hijacking) · [Componentes exportados](https://developer.android.com/privacy-and-security/risks/android-exported) · [Configuración de red](https://developer.android.com/privacy-and-security/security-config) · [Compartir archivos](https://developer.android.com/training/secure-file-sharing)

## Pruebas y lint

Prueba el `ViewModel` en `src/test` con JUnit y `InstantTaskExecutorRule` de `androidx.arch.core:core-testing`. En este ejemplo, los executors directos ejecutan la carga y la publicación en el mismo hilo de prueba. Para comprobar el orden de operaciones, el estado de carga o la cancelación, usa executors controlados que permitan avanzar la cola manualmente.

```java
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public final class ProfileViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTasks = new InstantTaskExecutorRule();

    @Test
    public void loadPublishesProfileName() {
        ProfileViewModel model = new ProfileViewModel(
                () -> "Ana", Runnable::run, Runnable::run);

        model.load();

        ProfileViewModel.UiState state = model.state().getValue();
        assertEquals(ProfileViewModel.UiState.Status.CONTENT, state.status);
        assertEquals("Ana", state.name);
    }

    @Test
    public void loadPublishesErrorWhenRepositoryFails() {
        ProfileViewModel model = new ProfileViewModel(
                () -> {
                    throw new IOException("Red no disponible");
                },
                Runnable::run, Runnable::run);

        model.load();

        assertEquals(ProfileViewModel.UiState.Status.ERROR, model.state().getValue().status);
    }
}
```

En `src/androidTest`, usa Espresso para los flujos de Views. Alimenta las pantallas con datos de prueba controlados y registra un `IdlingResource` si Espresso necesita esperar trabajo que no detecta por sí mismo. Ejecuta las pruebas conectadas en un dispositivo o emulador.

Configura lint en `android { lint { ... } }` en `app/build.gradle.kts` cuando el cambio lo requiera. Activa `warningsAsErrors` solo si forma parte de la política del proyecto. Para verificar la variante `debug`, ejecuta las tareas del módulo `app` desde la raíz del repositorio.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

En macOS o Linux usa `bash ./gradlew` con las mismas tareas. Ajusta los nombres si hay sabores de producto. En CI, pon compilación, pruebas locales y lint en el flujo habitual. Ejecuta las verificaciones instrumentadas en un trabajo con un dispositivo disponible cuando el proyecto las tenga configuradas.

Referencias de Android. [Pruebas por línea de comandos](https://developer.android.com/studio/test/command-line) · [Sincronización de Espresso](https://developer.android.com/training/testing/espresso/idling-resource) · [Lint por línea de comandos](https://developer.android.com/studio/write/lint#commandline) · [DSL de lint](https://developer.android.com/reference/tools/gradle-api/8.9/com/android/build/api/dsl/CommonExtension#lint)