package modelo;

import java.time.LocalDateTime;

public class Voto extends EntidadeBase {
    private EnumVoto tipo;
    private LocalDateTime dataVoto;
    private Usuario usuarioVotante;
    private int objetoAvaliadoId;
    private String tipoObjetoAvaliado;
    
    public Voto() {
        super();
        this.dataVoto = LocalDateTime.now();
    }
    
    public Voto(int id, EnumVoto tipo, Usuario usuarioVotante, int objetoAvaliadoId, String tipoObjetoAvaliado) {
        super(id);
        this.tipo = tipo;
        this.dataVoto = LocalDateTime.now();
        this.usuarioVotante = usuarioVotante;
        this.objetoAvaliadoId = objetoAvaliadoId;
        this.tipoObjetoAvaliado = tipoObjetoAvaliado;
    }
    
    @Override
    public boolean validar() {
        return tipo != null && 
               usuarioVotante != null && 
               objetoAvaliadoId > 0 && 
               tipoObjetoAvaliado != null && !tipoObjetoAvaliado.trim().isEmpty() && 
               ("GIRIA".equals(tipoObjetoAvaliado) || "EXPLICACAO".equals(tipoObjetoAvaliado));
    }
    
    @Override
    public int getId() {
        return id;
    }
    
    public boolean isPositivo() {
        return tipo == EnumVoto.POSITIVO;
    }

    public EnumVoto getTipo() {
        return tipo;
    }


    public LocalDateTime getDataVoto() {
        return dataVoto;
    }

    public void setDataVoto(LocalDateTime dataVoto) {
        this.dataVoto = dataVoto;
    }

    public Usuario getUsuarioVotante() {
        return usuarioVotante;
    }

    public int getObjetoAvaliadoId() {
        return objetoAvaliadoId;
    }

    public String getTipoObjetoAvaliado() {
        return tipoObjetoAvaliado;
    }
}
