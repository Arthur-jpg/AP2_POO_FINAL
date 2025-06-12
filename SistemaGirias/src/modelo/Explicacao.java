package modelo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Explicacao extends EntidadeBase implements Avaliavel {
    private String definicao;
    private String exemploUso;
    private boolean aprovada;
    private LocalDate dataProposta;
    private Usuario usuarioPropositor;
    private Giria giriaAssociada;
    private List<Voto> votos;
    
    public Explicacao() {
        super();
        this.dataProposta = LocalDate.now();
        this.aprovada = false;
        this.votos = new ArrayList<>();
    }
    
    public Explicacao(int id, String definicao, String exemploUso, Usuario usuarioPropositor, Giria giriaAssociada) {
        super(id);
        this.definicao = definicao;
        this.exemploUso = exemploUso;
        this.dataProposta = LocalDate.now();
        this.aprovada = false;
        this.usuarioPropositor = usuarioPropositor;
        this.giriaAssociada = giriaAssociada;
        this.votos = new ArrayList<>();
    }
    
    @Override
    public boolean validar() {
        return definicao != null && !definicao.trim().isEmpty() && 
               usuarioPropositor != null && 
               giriaAssociada != null;
    }
    
    @Override
    public int getId() {
        return id;
    }
    
    @Override
    public void adicionarVoto(Voto voto) {
        if (voto.getTipoObjetoAvaliado().equals("EXPLICACAO") && voto.getObjetoAvaliadoId() == this.id) {
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

    public String getDefinicao() {
        return definicao;
    }

    public String getExemploUso() {
        return exemploUso;
    }

    public boolean isAprovada() {
        return aprovada;
    }

    public void setAprovada(boolean aprovada) {
        this.aprovada = aprovada;
    }

    public void aprovar() {
        this.aprovada = true;
    }
    public void desaprovar() {
        this.aprovada = false;
        usuarioPropositor.decrementarReputacao();
    }

    public LocalDate getDataProposta() {
        return dataProposta;
    }

    public void setDataProposta(LocalDate dataProposta) {
        this.dataProposta = dataProposta;
    }

    public Usuario getUsuarioPropositor() {
        return usuarioPropositor;
    }

    public Giria getGiriaAssociada() {
        return giriaAssociada;
    }

    public void setGiriaAssociada(Giria giriaAssociada) {
        this.giriaAssociada = giriaAssociada;
    }

}
