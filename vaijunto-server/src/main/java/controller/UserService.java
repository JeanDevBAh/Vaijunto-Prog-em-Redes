package controller;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import model.TipoUser;
import model.Usuario;
/**
 * Gerencia usuários cadastrados e sessões ativas.
 *
 * Mantém um mapa de usuários identificados pelo login e outro mapa que associa
 * tokens de autenticação aos usuários atualmente conectados.
 *
 * É responsável por cadastrar usuários, validar login e senha, criar tokens,
 * verificar sessões, realizar logout e alterar o tipo de usuário.
 *
 * Utiliza ConcurrentHashMap para permitir acesso seguro por múltiplas threads.
 */
public class UserService {
    
    private final Map<String, Usuario> usuarios;
    private final Map<String, Usuario> ativos;

    public UserService(){
        this.usuarios = new ConcurrentHashMap<>();
        this.ativos = new ConcurrentHashMap<>();
    }

    public boolean cadastrarUsuario(String login, String senha, TipoUser tipo){
        Usuario newUser = new Usuario(login, senha, tipo);

        Usuario existente = usuarios.putIfAbsent(login, newUser);

        return existente == null;

    }

    public String fazerLogin(String login, String senha){
        Usuario user = usuarios.get(login);
        if(user != null && user.getSenha().equals(senha)){
            String token = UUID.randomUUID().toString();
            this.ativos.put(token, user);
            return token;
        }
        return null;
    }

    public  Usuario validarSessao(String token){
        if(token==null){
            return null; 
        }
        Usuario user = ativos.get(token);
        if(user!=null) {
            return user;
        }
        return null;
    }


    public boolean logout(String token){
        if (token == null){return false;}
        Usuario removido = ativos.remove(token);
        return removido != null;
    }

    public synchronized boolean mudaTipoUser(String token, TipoUser tiponovo){
        Usuario user = ativos.get(token);
        if(user != null && tiponovo != null){
            user.setTipoUser(tiponovo);
            return true;
        }
        return false;
    }
}