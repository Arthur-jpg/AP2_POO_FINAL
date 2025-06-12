package modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Giria extends EntidadeBase implements Avaliavel {
    private String termo;
    private LocalDateTime dataCadastro;
    private boolean aprovada;
    private Usuario usuarioPropositor;
    private List<Voto> votos;
    private List<Explicacao> explicacoes;
    private Set<String> categorias;
    private Set<String> regioes;
    
    public Giria() {
        super();
        this.dataCadastro = LocalDateTime.now();
        this.aprovada = false;
        this.votos = new ArrayList<>();
        this.explicacoes = new ArrayList<>();
        this.categorias = new HashSet<>();
        this.regioes = new HashSet<>();
    }
    
    public Giria(int id, String termo, Usuario usuarioPropositor) {
        super(id);
        this.termo = termo;
        this.dataCadastro = LocalDateTime.now();
        this.aprovada = false;
        this.usuarioPropositor = usuarioPropositor;
        this.votos = new ArrayList<>();
        this.explicacoes = new ArrayList<>();
        this.categorias = new HashSet<>();
        this.regioes = new HashSet<>();
    }


    //testar depois
    // Usado pelo DAO para carregar as girias com todos os dados
    public Giria(int id, String termo, LocalDateTime data, boolean aprovada, Usuario usuarioPropositor) {
        super(id);
        this.termo = termo;
        this.dataCadastro = data;
        this.aprovada = aprovada;
        this.usuarioPropositor = usuarioPropositor;
        this.votos = new ArrayList<>();
        this.explicacoes = new ArrayList<>();
        // Estudar HashSet
        this.categorias = new HashSet<>();
        this.regioes = new HashSet<>();
    }




    @Override
    public boolean validar() {
        return termo != null && !termo.trim().isEmpty() && usuarioPropositor != null;
    }
    
    @Override
    public int getId() {
        return id;
    }
    
    @Override
    public void adicionarVoto(Voto voto) {
        if (voto.getTipoObjetoAvaliado().equals("GIRIA") && voto.getObjetoAvaliadoId() == this.id) {
            votos.add(voto);
            if (voto.isPositivo()) {
                usuarioPropositor.incrementarReputacao();
            } else {
                usuarioPropositor.decrementarReputacao();
            }
        }
    }
    
    @Override
    public int getPontuacaoVotos() {
        int pontuacao = 0;
        for (Voto voto : votos) {
            if (voto.isPositivo()) {
                pontuacao++;
            } else {
                pontuacao--;
            }
        }
        return pontuacao;
    }

    // estava sendo redundante, pois a explicacao ja tem a giria associada
    public void adicionarExplicacao(Explicacao explicacao) {
        if (explicacao != null && explicacao.getGiriaAssociada() == this) {
            explicacoes.add(explicacao);
            if (explicacao.validar()) {
                explicacao.setGiriaAssociada(this);
                usuarioPropositor.incrementarReputacao();
            } else {
                throw new IllegalArgumentException("Explicação inválida.");
            }
        }
    }
    
    public void adicionarCategoria(String categoria) {
        if (categoria != null && !categoria.trim().isEmpty()) {
            categorias.add(categoria);
        }
    }
    
    public void adicionarRegiao(String regiao) {
        if (regiao != null && !regiao.trim().isEmpty()) {
            regioes.add(regiao);
        }
    }
    
    public String getTermo() {
        return termo;
    }
    
    public void setTermo(String termo) {
        this.termo = termo;
    }
    
    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }
    
    public boolean isAprovada() {
        return aprovada;
    }
    
    public void setAprovada(boolean aprovada) {
        this.aprovada = aprovada;
    }

    public void desaprovar() {
        this.aprovada = false;
        usuarioPropositor.decrementarReputacao();
    }

    public void aprovar() {
        this.aprovada = true;
    }
    
    public Usuario getUsuarioPropositor() {
        return usuarioPropositor;
    }
    
    public void setUsuarioPropositor(Usuario usuarioPropositor) {
        this.usuarioPropositor = usuarioPropositor;
    }
    
    public List<Voto> getVotos() {
        return votos;
    }
    
    public List<Explicacao> getExplicacoes() {
        return explicacoes;
    }
    
    public Set<String> getCategorias() {
        return categorias;
    }
    
    public Set<String> getRegioes() {
        return regioes;
    }
}
