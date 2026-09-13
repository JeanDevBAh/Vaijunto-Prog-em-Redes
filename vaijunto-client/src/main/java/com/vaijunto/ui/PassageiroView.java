package com.vaijunto.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import model.TipoUser;
import network.DTOResponse;
import sessao.SessaoUsuario;
import util.ClienteTask;

/**
 * Esqueleto do dashboard do passageiro. As ações reais (buscar viagens,
 * reservar, minhas reservas, cancelar reserva) entram aqui como próximos
 * passos.
 */
public class PassageiroView extends VBox {

    private final SessaoUsuario sessao;
    private final NavegacaoManager navegacao;
    private final Label labelStatus = new Label();

    public PassageiroView(SessaoUsuario sessao, NavegacaoManager navegacao) {
        this.sessao = sessao;
        this.navegacao = navegacao;
        montarLayout();
    }

    private void montarLayout() {
        setSpacing(12);
        setPadding(new Insets(24));

        Label titulo = new Label("Dashboard do Passageiro — " + sessao.getLogin());
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // TODO: botões reais -- Buscar viagens, Reservar, Minhas reservas,
        // Cancelar reserva.

        Button btnVirarMotorista = new Button("Mudar para Motorista");
        btnVirarMotorista.setOnAction(e -> mudarTipo(TipoUser.MOTORISTA));

        getChildren().addAll(titulo, btnVirarMotorista, labelStatus);
    }

    private void mudarTipo(TipoUser novoTipo) {
        labelStatus.setText("Atualizando tipo de usuário...");

        ClienteTask.executar(
                () -> sessao.getClient().mudarTipoUsuario(sessao.getToken(), novoTipo),
                (DTOResponse<?> resposta) -> {
                    if (!resposta.isSucesso()) {
                        labelStatus.setText(resposta.getMensagem());
                        return;
                    }
                    sessao.atualizarTipoUsuario(novoTipo);
                    RoteadorDashboard.mostrarDashboardCorreto(sessao, navegacao);
                },
                erro -> labelStatus.setText("Erro de conexão: " + erro.getMessage())
        );
    }
}