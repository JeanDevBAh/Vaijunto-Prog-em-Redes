package network;

public class DTOResponse<T> {
    
    private boolean sucesso;
    private String mensagem;
    private T dados; // Pode guardar uma lista de itinerários, uma reserva, etc.

    // Construtor para respostas simples (sucesso/erro + texto)
    public DTOResponse(boolean sucesso, String mensagem) {
        this.sucesso = sucesso;
        this.mensagem = mensagem;
        this.dados = null;
    }

    // Construtor para respostas com dados complexos
    public DTOResponse(boolean sucesso, String mensagem, T dados) {
        this.sucesso = sucesso;
        this.mensagem = mensagem;
        this.dados = dados;
    }

    // Getters e Setters
    public boolean isSucesso() {
        return sucesso;
    }

    public void setSucesso(boolean sucesso) {
        this.sucesso = sucesso;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public T getDados() {
        return dados;
    }

    public void setDados(T dados) {
        this.dados = dados;
    }
}