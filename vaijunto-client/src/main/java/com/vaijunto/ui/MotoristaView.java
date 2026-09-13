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
 * Esqueleto do dashboard do motorista. As ações reais (oferecer carona,
 * listar minhas caronas, cancelar, consultar passageiros) entram aqui como
 * próximos passos -- por enquanto só demonstra a troca de tipo de usuário,
 * que foi o ponto que gerou a dúvida sobre SessaoUsuario.
 */
public class MotoristaView extends VBox {

    private final SessaoUsuario sessao;
    private final NavegacaoManager navegacao;
    private final Label labelStatus = new Label();

    public MotoristaView(SessaoUsuario sessao, NavegacaoManager navegacao) {
        this.sessao = sessao;
        this.navegacao = navegacao;
        montarLayout();
    }

    private void montarLayout() {
        setSpacing(12);
        setPadding(new Insets(24));

        Label titulo = new Label("Dashboard do Motorista — " + sessao.getLogin());
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // TODO: botões reais -- Oferecer carona, Minhas caronas,
        // Cancelar carona, Consultar passageiros por trecho.

        Button btnVirarPassageiro = new Button("Mudar para Passageiro");
        btnVirarPassageiro.setOnAction(e -> mudarTipo(TipoUser.PASSAGEIRO));

        getChildren().addAll(titulo, btnVirarPassageiro, labelStatus);
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
                    // Só atualiza o estado local DEPOIS da confirmação do
                    // servidor -- nunca antes, pra sessão local nunca ficar
                    // "à frente" do que o servidor realmente aplicou.
                    sessao.atualizarTipoUsuario(novoTipo);
                    RoteadorDashboard.mostrarDashboardCorreto(sessao, navegacao);
                },
                erro -> labelStatus.setText("Erro de conexão: " + erro.getMessage())
        );
    }
}