package com.vaijunto.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Único ponto do app que sabe manipular a Stage/Scene principal. Cada tela
 * chama navegacao.mostrar(novaTela) em vez de mexer em Stage diretamente --
 * assim nenhuma tela precisa saber como a janela principal está montada.
 */
public class NavegacaoManager {
    private final Stage stage;

    public NavegacaoManager(Stage stage) {
        this.stage = stage;
    }

    public void mostrar(Parent raiz) {
        Scene cenaAtual = stage.getScene();
        if (cenaAtual == null) {
            stage.setScene(new Scene(raiz, 480, 360));
        } else {
            cenaAtual.setRoot(raiz);
        }
        stage.show();
    }
}