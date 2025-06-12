package dao;
import java.util.ArrayList;

public interface BaseDAO {
    public void salvar(Object objeto);

    public Object buscarPorId(int id);

    public ArrayList<Object> listarTodosLazyLoading();

    public ArrayList<Object> listarTodosEagerLoading();

    public void atualizar(Object objeto);

    public void excluir(int id);

}