package controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import model.Carona;
import model.Cidades;
import model.Grafo;
import model.Trecho;
import model.Usuario;
 


public class CaronaService {
    private final Map<String, Carona>  caronas;
    private final Grafo grafo;

    public CaronaService(Grafo mapa){
        this.caronas = new ConcurrentHashMap<>();
        this.grafo = mapa;
    }
    
    public synchronized String criarCarona(List<Cidades> rota, String data, String hora,
                  int vagasTotais, boolean ativaOuNao, List<Double> precos, String motorista){
        for (Carona existente : caronas.values()) {
            if (Objects.equals(motorista, existente.getNomeMotorista())
                    && Objects.equals(data, existente.getData())
                    && Objects.equals(hora, existente.getHora())
                    && Objects.equals(rota, existente.getRota())) {
                throw new IllegalArgumentException(
                        "Você já publicou uma carona com a mesma rota, data e horário.");
            }
        }
        Carona carona = new Carona(rota, data, hora, vagasTotais, ativaOuNao, precos, motorista);
        caronas.put(carona.getId(), carona);
        if (carona.isAtivaOuNao()) {
            grafo.addTrechos(carona.getTrechos());
        }
        return carona.getId();
    }

    public List<Carona> buscaCaronasPorMotorista(String motorista){
        List<Carona> resultado = new ArrayList<>();
        if (motorista == null || motorista.isBlank()) {
            return resultado;
        }
        for (Carona carona : caronas.values()) {
            if (motorista.equals(carona.getNomeMotorista())) {
                resultado.add(carona);
            }
        }
        return resultado;
    }

    public Map<String, List<String>> consultarPassageirosDaCarona(String idCarona, String loginMotorista) {
        Carona carona = caronas.get(idCarona);
        
        // Valida se a carona existe e pertence ao motorista que fez a requisição
        if (carona == null || !carona.getNomeMotorista().equals(loginMotorista)) {
            return null;
        }

        // Usa LinkedHashMap para manter a ordem cronológica dos trechos da rota
        Map<String, List<String>> mapaPassageiros = new LinkedHashMap<>();

        for (Trecho trecho : carona.getTrechos()) {
            String nomeTrecho = trecho.getCidadeOrigem().getNome() + " -> " + trecho.getCidadeDestino().getNome();
            
            List<String> listaLogins = new ArrayList<>();
            for (Usuario passageiro : trecho.getPassageiros()) {
                listaLogins.add(passageiro.getLogin());
            }
            
            mapaPassageiros.put(nomeTrecho, listaLogins);
        }

        return mapaPassageiros;
    }

    public boolean cancelarCarona(String id, String loginMotorista){
        if (id == null || loginMotorista == null || loginMotorista.isBlank()) {
            return false;
        }
        Carona carona = caronas.get(id);
        if (carona != null && carona.isAtivaOuNao()
                && loginMotorista.equals(carona.getNomeMotorista())) {
            carona.setAtivaOuNao(false);
            grafo.removeTrechosDaCarona(id);
            caronas.remove(id);
            return true;
        }
        return false;
    }


}