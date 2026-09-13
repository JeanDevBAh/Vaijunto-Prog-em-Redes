package com.vaijunto;

import com.vaijunto.ui.LoginView;
import com.vaijunto.ui.NavegacaoManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("VaiJunto");

        NavegacaoManager navegacao = new NavegacaoManager(stage);
        navegacao.mostrar(new LoginView(navegacao));
    }

    public static void main(String[] args) {
        launch(args);
    }
}