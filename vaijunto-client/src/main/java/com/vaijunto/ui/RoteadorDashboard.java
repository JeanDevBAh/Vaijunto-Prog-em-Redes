package com.vaijunto.ui;

import model.TipoUser;
import sessao.SessaoUsuario;

/**
 * Decide qual dashboard mostrar conforme o TipoUser da sessão. Existe como
 * classe separada porque essa decisão é disparada em DOIS pontos diferentes
 * do fluxo -- logo após o login/cadastro, e logo após o servidor confirmar
 * uma troca de tipo (MUDAR_TIPO_USUARIO) -- e não queremos duas cópias da
 * mesma regra "MOTORISTA -> tal tela, PASSAGEIRO -> tal outra" podendo
 * divergir com o tempo.
 */
public final class RoteadorDashboard {

    private RoteadorDashboard() {}

    public static void mostrarDashboardCorreto(SessaoUsuario sessao, NavegacaoManager navegacao) {
        if (sessao.getTipoUser() == TipoUser.MOTORISTA) {
            navegacao.mostrar(new MotoristaView(sessao, navegacao));
        } else {
            navegacao.mostrar(new PassageiroView(sessao, navegacao));
        }
    }
}