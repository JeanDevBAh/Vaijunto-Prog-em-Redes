package controller;


import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import model.Cidades;
import model.Grafo;
import model.Itinerario;
import model.Reserva;
import model.Trecho;
import model.Usuario;

public class ReservaService {
    private final Grafo grafo;
    private final Map<String, Reserva> reservas;

    public ReservaService(Grafo grafo){
        this.grafo = grafo;
        this.reservas = new ConcurrentHashMap<>();
    }
    

    public List<Itinerario> buscarViagens(Cidades origem, Cidades destino, String data){
        return grafo.buscarItinerarios(origem, destino, data);
    }

    public String efetuarReserva(Usuario passageiro, Itinerario viagem){
    List<Trecho> trechos = viagem.getTrechos();
    
    // Sincroniza o processo de reserva para evitar que outra thread roube a vaga
    // entre a checagem e a confirmação.
    synchronized (this) {
        // 1. Fase de Validação: Checa todos os trechos primeiro
        for(Trecho trecho : trechos){
            if (trecho.getLugaresDisponiveis() <= 0) {
                return null; // Falha atômica: rejeita antes de alterar qualquer vaga
            }
        }
        
        // 2. Fase de Confirmação: Efetiva a reserva com segurança
        for(Trecho trecho : trechos){
            trecho.reservarLugar(passageiro);
        }
        
        Reserva reserva = new Reserva(passageiro.getId(), passageiro.getLogin(), trechos);
        reservas.put(reserva.getId(), reserva);
        return reserva.getId();
    }
}

    public boolean cancelarReserva(String idReserva, String login){
        Reserva reserva = reservas.get(idReserva);
        if (reserva != null && reserva.isAtiva() && reserva.getLoginPassageiro().equals(login)) {
            reserva.cancelar();
            return true;
        }
        return false;
    }

    public List<Reserva> listarReservasPorPassageiro(String login) {
        return reservas.values().stream()
                .filter(reserva -> reserva.getLoginPassageiro().equals(login))
                .toList();
    }
}
