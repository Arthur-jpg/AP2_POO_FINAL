package modelo;

import java.time.LocalDateTime;

public abstract class EntidadeBase {
    protected int id;
    protected LocalDateTime dataCriacao;
    
    public EntidadeBase(int id) {
        this.id = id;
        this.dataCriacao = LocalDateTime.now();
    }

    public EntidadeBase() {
        this.dataCriacao = LocalDateTime.now();
    }
    
    public abstract boolean validar();
    
    public abstract int getId();
    
    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
