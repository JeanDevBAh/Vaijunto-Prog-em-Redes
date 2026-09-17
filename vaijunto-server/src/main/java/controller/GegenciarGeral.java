package controller;

import java.util.List;
import java.util.Map;

import model.Carona;
import model.Cidades;
import model.Grafo;
import model.Itinerario;
import model.Reserva;
import model.TipoUser;
import model.Usuario;
/**
 * Fachada central dos serviços de negócio do servidor.
 *
 * Reúne e coordena UserService, CaronaService e ReservaService, oferecendo
 * uma interface única para o ClientHandler.
 *
 * Também realiza a validação inicial das sessões antes de permitir operações
 * protegidas, como oferecer caronas, cancelar caronas ou criar reservas.
 *
 * Essa classe reduz o acoplamento entre a camada de comunicação e os serviços
 * específicos da aplicação.
 */
public class GegenciarGeral {
    private final CaronaService caronaService;
    private final ReservaService reservaService;
    private final UserService userService;
    private final Grafo grafo;
    
    public GegenciarGeral(){
        this.grafo = new Grafo();
        this.caronaService = new CaronaService(this.grafo);
        this.reservaService = new ReservaService(this.grafo);
        this.userService = new UserService();
    }
    // ==========================================
    // FLUXOS DE USUÁRIO (Encaminha para o UserService)
    // ==========================================
    public boolean cadastrarUsuario(String login, String senha, TipoUser tipo) {
        return userService.cadastrarUsuario(login, senha, tipo);
    }

    public String fazerLogin(String login, String senha) {
        return userService.fazerLogin(login, senha);
    }

    public boolean fazerLogout(String token) {
        return userService.logout(token);
    }

    public boolean mudarTipoUsuario(String token, TipoUser tipoUser){
        return userService.mudaTipoUser(token, tipoUser);
    }

    public Usuario validarSessao(String token) {
        return userService.validarSessao(token);
    }

    // ==========================================
    // FLUXOS DE MOTORISTA (Encaminha para o CaronaService)
    // ==========================================
    public String oferecerCarona(String token, List<Cidades> rota, String data, String hora, 
                                int vagasTotais, boolean ativaOuNao, List<Double> precos) {
        // Opcional: Validar se o token pertence a um usuário válido antes de criar
        Usuario user = userService.validarSessao(token);
        if (user == null) {
            throw new SecurityException("Usuário não autenticado.");
        }
        
        return caronaService.criarCarona(rota, data, hora, vagasTotais, ativaOuNao, precos,  user.getLogin());
    }
    public List<Carona> listaCaronas(String token){
        Usuario motorista = userService.validarSessao(token);
        if (motorista == null) {
            throw new SecurityException("Usuário não autenticado.");
        }
        return caronaService.buscaCaronasPorMotorista(motorista.getLogin());
    }
    public boolean cancelarCarona(String token, String id){
        Usuario motorista = userService.validarSessao(token);
        if (motorista == null) {
            throw new SecurityException("Usuário não autenticado.");
        }
        return caronaService.cancelarCarona(id, motorista.getLogin());
    }

    public Map<String, List<String>> consultarPassageiros(String token, String idCarona){
        Usuario motorista = userService.validarSessao(token);
        if (motorista == null) {
            throw new SecurityException("Usuário não autenticado.");
        }
        return caronaService.consultarPassageirosDaCarona(idCarona, motorista.getLogin());
    }


    // ==========================================
    // FLUXOS DE PASSAGEIRO (Encaminha para o ReservaService)
    // ==========================================
    public List<Itinerario> buscarViagens(Cidades origem, Cidades destino, String data) {
        return reservaService.buscarViagens(origem, destino, data);
    }

    public String reservarViagem(String token, Itinerario itinerario) {
        Usuario passageiro = userService.validarSessao(token);
        if (passageiro == null) {
            throw new SecurityException("Usuário não autenticado.");
        }
        
        return reservaService.efetuarReserva(passageiro, itinerario);
    }

    public boolean cancelarReserva(String token, String idReserva) {
        Usuario passageiro = userService.validarSessao(token);
        if (passageiro == null) {
            return false;
        }
        return reservaService.cancelarReserva(idReserva, passageiro.getLogin());
    }

    public List<Reserva> listarReservas(String token) {
        Usuario passageiro = userService.validarSessao(token);
        if (passageiro == null) {
            throw new SecurityException("Usuário não autenticado.");
        }
        return reservaService.listarReservasPorPassageiro(passageiro.getLogin());
    }

   

}