package util;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javafx.concurrent.Task;

/**
 * Roda uma chamada bloqueante (qualquer método de VaijuntoClient, que usa
 * Socket/BufferedReader.readLine() por baixo) numa thread separada da
 * JavaFX Application Thread. Sem isso, cada chamada de rede congelaria a
 * interface inteira até a resposta do servidor chegar.
 *
 * setOnSucceeded/setOnFailed do Task já são executados de volta na
 * Application Thread automaticamente -- por isso é seguro mexer em
 * componentes de UI (Label, Alert, trocar de tela) dentro de aoTerminar/
 * aoFalhar, sem precisar de Platform.runLater manual.
 */
public final class ClienteTask {

    private ClienteTask() {}

    public static <T> void executar(Callable<T> chamada,
                                     Consumer<T> aoTerminar,
                                     Consumer<Throwable> aoFalhar) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return chamada.call();
            }
        };
        task.setOnSucceeded(e -> aoTerminar.accept(task.getValue()));
        task.setOnFailed(e -> aoFalhar.accept(task.getException()));

        Thread thread = new Thread(task, "vaijunto-client-call");
        thread.setDaemon(true);
        thread.start();
    }
}