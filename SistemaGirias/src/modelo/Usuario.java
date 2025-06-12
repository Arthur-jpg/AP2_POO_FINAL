package modelo;

import java.util.ArrayList;
import java.util.List;

public class Usuario extends EntidadeBase {
    private String nome;
    private String email;
    private String senha;
    private int reputacao;
    private List<Giria> giriasPropostas;
    private List<Explicacao> explicacoesPropostas;
    private List<Voto> votosRealizados;
    
    public Usuario() {
        super();
        this.reputacao = 0;
        this.giriasPropostas = new ArrayList<>();
        this.explicacoesPropostas = new ArrayList<>();
        this.votosRealizados = new ArrayList<>();
    }
    
    public Usuario(int id, String nome, String email, String senha) {
        super(id);
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.reputacao = 0;
        this.giriasPropostas = new ArrayList<>();
        this.explicacoesPropostas = new ArrayList<>();
        this.votosRealizados = new ArrayList<>();
    }

    public Usuario(String nome, String email, String senha) {
        super();
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.reputacao = 0;
        this.giriasPropostas = new ArrayList<>();
        this.explicacoesPropostas = new ArrayList<>();
        this.votosRealizados = new ArrayList<>();
    }

    // usado somente para adm
    public Usuario(int id, String nome, String email, String senha, int reputacao) {
        super(id);
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.reputacao = reputacao;
        this.giriasPropostas = new ArrayList<>();
        this.explicacoesPropostas = new ArrayList<>();
        this.votosRealizados = new ArrayList<>();
    }



    @Override
    public boolean validar() {
        return nome != null && !nome.trim().isEmpty() && 
               email != null && !email.trim().isEmpty() && 
               senha != null && !senha.trim().isEmpty();
    }
    
    @Override
    public int getId() {
        return id;
    }
    
    public int getReputacao() {
        return reputacao;
    }
    
    public void incrementarReputacao() {
        this.reputacao++;
    }
    
    public void decrementarReputacao() {
        this.reputacao--;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenha() {
        return senha;
    }

    public void setReputacao(int reputacao) {
        this.reputacao = reputacao;
    }

    public void adicionarGiriaProposta(Giria giria) {
        this.giriasPropostas.add(giria);
    }

}
