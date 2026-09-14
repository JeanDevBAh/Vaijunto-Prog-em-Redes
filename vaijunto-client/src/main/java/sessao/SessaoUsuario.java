package sessao;

import client.VaiJuntoClient;
import model.TipoUser;
import java.io.IOException;

/**
 * Estado da sessão do usuário logado. Guarda também a instância do
 * VaijuntoClient em uso -- a MESMA conexão TCP precisa ser reaproveitada
 * durante toda a sessão, porque o servidor mantém estado por conexão
 * (ex.: o resultado da última BUSCAR_VIAGENS, usado depois em RESERVAR).
 * Abrir uma conexão nova a cada tela perderia esse estado.
 */
public class SessaoUsuario {
    private final String token;
    private final String login;
    private final VaiJuntoClient client;
    private TipoUser tipoUser; // não é final: pode mudar via MUDAR_TIPO_USUARIO

    public SessaoUsuario(String token, String login, TipoUser tipoUser, VaiJuntoClient client) {
        this.token = token;
        this.login = login;
        this.tipoUser = tipoUser;
        this.client = client;
    }

    public String getToken() {
        return token;
    }

    public String getLogin() {
        return login;
    }

    public TipoUser getTipoUser() {
        return tipoUser;
    }

    public VaiJuntoClient getClient() {
        return client;
    }

    /**
     * Só deve ser chamado DEPOIS que o servidor confirmar a troca (resposta
     * sucesso=true de MUDAR_TIPO_USUARIO). Este método não sincroniza nada
     * sozinho -- só evita que a UI fique com um tipoUser desatualizado em
     * relação ao que o servidor já aplicou.
     */
    public void atualizarTipoUsuario(TipoUser novoTipo) {
        this.tipoUser = novoTipo;
    }

    public void fechar() throws IOException {
        client.close();
    }
}