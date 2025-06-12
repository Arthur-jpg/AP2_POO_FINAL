package dao;

import modelo.Explicacao;
import modelo.Giria;
import modelo.Usuario;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class ExplicacaoDAO implements BaseDAO {

    private Connection connection;

    public ExplicacaoDAO(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void salvar(Object objeto) {
        if (!(objeto instanceof Explicacao)) {
            throw new IllegalArgumentException("Objeto deve ser do tipo Explicacao.");
        }

        Explicacao explicacao = (Explicacao) objeto;

        try {
            // Verifica se já existe uma explicação com a mesma definição para a mesma gíria
            String checkSql = "SELECT id FROM explicacao WHERE definicao = ? AND giria_associada_id = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setString(1, explicacao.getDefinicao());
                checkStmt.setInt(2, explicacao.getGiriaAssociada().getId());
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    // Se já existe, atualiza os dados da explicação com o ID existente
                    int existingId = rs.getInt("id");
                    explicacao.setId(existingId);
                    atualizar(explicacao);
                    return;
                }
            }

            // Verifica se a explicação já existe para decidir entre INSERT e UPDATE
            if (explicacao.getId() == 0) { // Considera que ID 0 indica novo objeto
                String sql = "INSERT INTO explicacao (definicao, exemplo_uso, aprovada, data_proposta, " +
                        "usuario_propositor_id, giria_associada_id, data_criacao) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstm.setString(1, explicacao.getDefinicao());
                    pstm.setString(2, explicacao.getExemploUso());
                    pstm.setBoolean(3, explicacao.isAprovada());
                    pstm.setObject(4, explicacao.getDataProposta());
                    pstm.setInt(5, explicacao.getUsuarioPropositor().getId());
                    pstm.setInt(6, explicacao.getGiriaAssociada().getId());
                    pstm.setObject(7, explicacao.getDataCriacao());

                    pstm.execute();

                    try (ResultSet rst = pstm.getGeneratedKeys()) {
                        if (rst.next()) {
                            explicacao.setId(rst.getInt(1)); // Define o ID gerado pelo banco
                        }
                    }
                }
            } else { // Se já tem ID, tenta atualizar
                atualizar(explicacao); // Chama o método de atualização
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar explicação: " + e.getMessage(), e);
        }
    }

    @Override
    public Object buscarPorId(int id) {
        Explicacao explicacao = null;
        try {
            String sql = "SELECT e.id, e.definicao, e.exemplo_uso, e.aprovada, e.data_proposta, e.data_criacao, " +
                    "u.id as usuario_id, u.nome as usuario_nome, u.email, u.senha, u.reputacao, " +
                    "g.id as giria_id, g.termo, g.data_cadastro, g.aprovada as giria_aprovada " +
                    "FROM explicacao e " +
                    "JOIN usuario u ON e.usuario_propositor_id = u.id " +
                    "JOIN giria g ON e.giria_associada_id = g.id " +
                    "WHERE e.id = ?";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, id);
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    if (rst.next()) {
                        // Cria o usuário propositor
                        Usuario usuarioPropositor = new Usuario(
                                rst.getInt("usuario_id"),
                                rst.getString("usuario_nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        usuarioPropositor.setReputacao(rst.getInt("reputacao"));

                        // Para a gíria, precisaríamos de mais informações como o usuário propositor dela
                        // Mas isso aumentaria muito a complexidade da consulta. Uma opção é usar o DAO da Giria
                        GiriaDAO giriaDAO = new GiriaDAO(connection);
                        Giria giriaAssociada = (Giria) giriaDAO.buscarPorId(rst.getInt("giria_id"));

                        // Se por algum motivo a consulta acima falhar, cria uma gíria básica com os dados disponíveis
                        if (giriaAssociada == null) {
                            giriaAssociada = new Giria(
                                    rst.getInt("giria_id"),
                                    rst.getString("termo"),
                                    rst.getObject("data_cadastro", LocalDateTime.class),
                                    rst.getBoolean("giria_aprovada"),
                                    usuarioPropositor // Isso não é correto, mas é um fallback temporário
                            );
                        }

                        // Cria a explicação
                        explicacao = new Explicacao(
                                rst.getInt("id"),
                                rst.getString("definicao"),
                                rst.getString("exemplo_uso"),
                                usuarioPropositor,
                                giriaAssociada
                        );
                        explicacao.setDataProposta(rst.getObject("data_proposta", LocalDate.class));
                        explicacao.setAprovada(rst.getBoolean("aprovada"));
                        explicacao.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));
                    }
                }
            }

            return explicacao;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar explicação por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public ArrayList<Object> listarTodosLazyLoading() {
        ArrayList<Object> explicacoes = new ArrayList<>();
        try {
            String sql = "SELECT e.id, e.definicao, e.exemplo_uso, e.aprovada, e.data_proposta, e.data_criacao, " +
                    "u.id as usuario_id, u.nome as usuario_nome, u.email, u.senha, u.reputacao, " +
                    "g.id as giria_id, g.termo, g.aprovada as giria_aprovada " +
                    "FROM explicacao e " +
                    "JOIN usuario u ON e.usuario_propositor_id = u.id " +
                    "JOIN giria g ON e.giria_associada_id = g.id " +
                    "ORDER BY e.id";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        // Cria o usuário propositor
                        Usuario usuarioPropositor = new Usuario(
                                rst.getInt("usuario_id"),
                                rst.getString("usuario_nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        usuarioPropositor.setReputacao(rst.getInt("reputacao"));

                        // Cria uma gíria com informações básicas
                        Giria giriaAssociada = new Giria();
                        giriaAssociada.setId(rst.getInt("giria_id"));
                        giriaAssociada.setTermo(rst.getString("termo"));
                        giriaAssociada.setAprovada(rst.getBoolean("giria_aprovada"));

                        // Cria a explicação
                        Explicacao explicacao = new Explicacao(
                                rst.getInt("id"),
                                rst.getString("definicao"),
                                rst.getString("exemplo_uso"),
                                usuarioPropositor,
                                giriaAssociada
                        );
                        explicacao.setDataProposta(rst.getObject("data_proposta", LocalDate.class));
                        explicacao.setAprovada(rst.getBoolean("aprovada"));
                        explicacao.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));

                        explicacoes.add(explicacao);
                    }
                }
            }
            return explicacoes;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar todas as explicações (Lazy Loading): " + e.getMessage(), e);
        }
    }

    @Override
    public ArrayList<Object> listarTodosEagerLoading() {
        // Para Eager Loading, precisaríamos carregar também os votos relacionados a cada explicação
        ArrayList<Object> explicacoes = new ArrayList<>();

        try {
            String sql = "SELECT e.id, e.definicao, e.exemplo_uso, e.aprovada, e.data_proposta, e.data_criacao, " +
                    "u.id as usuario_id, u.nome as usuario_nome, u.email, u.senha, u.reputacao " +
                    "FROM explicacao e " +
                    "JOIN usuario u ON e.usuario_propositor_id = u.id " +
                    "ORDER BY e.id";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        // Para cada explicação, chamar o buscarPorId que já carrega tudo
                        Explicacao explicacao = (Explicacao) buscarPorId(rst.getInt("id"));

                        // Carregar votos da explicação
                        VotoDAO votoDAO = new VotoDAO(connection);
                        ArrayList<modelo.Voto> votos = votoDAO.listarPorExplicacaoId(explicacao.getId());
                        for (modelo.Voto voto : votos) {
                            explicacao.adicionarVoto(voto);
                        }

                        explicacoes.add(explicacao);
                    }
                }
            }
            return explicacoes;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar todas as explicações (Eager Loading): " + e.getMessage(), e);
        }
    }

    @Override
    public void atualizar(Object objeto) {
        if (!(objeto instanceof Explicacao)) {
            throw new IllegalArgumentException("Objeto deve ser do tipo Explicacao.");
        }

        Explicacao explicacao = (Explicacao) objeto;

        String sql = "UPDATE explicacao SET definicao = ?, exemplo_uso = ?, aprovada = ?, " +
                "data_proposta = ?, usuario_propositor_id = ?, giria_associada_id = ? " +
                "WHERE id = ?";

        try (PreparedStatement pstm = connection.prepareStatement(sql)) {
            pstm.setString(1, explicacao.getDefinicao());
            pstm.setString(2, explicacao.getExemploUso());
            pstm.setBoolean(3, explicacao.isAprovada());
            pstm.setObject(4, explicacao.getDataProposta());
            pstm.setInt(5, explicacao.getUsuarioPropositor().getId());
            pstm.setInt(6, explicacao.getGiriaAssociada().getId());
            pstm.setInt(7, explicacao.getId());

            int linhasAfetadas = pstm.executeUpdate();

            if (linhasAfetadas == 0) {
                throw new SQLException("Falha ao atualizar explicação: nenhuma linha foi afetada.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar explicação: " + e.getMessage(), e);
        }
    }

    @Override
    public void excluir(int id) {
        try {
            String sql = "DELETE FROM explicacao WHERE id = ?";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, id);

                int linhasAfetadas = pstm.executeUpdate();

                if (linhasAfetadas == 0) {
                    throw new SQLException("Falha ao deletar explicação: nenhuma linha foi afetada.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir explicação: " + e.getMessage(), e);
        }
    }

    // Métodos específicos para a classe ExplicacaoDAO

    public ArrayList<Explicacao> buscarPorGiria(int giriaId) {
        ArrayList<Explicacao> explicacoes = new ArrayList<>();
        try {
            String sql = "SELECT e.id, e.definicao, e.exemplo_uso, e.aprovada, e.data_proposta, e.data_criacao, " +
                    "u.id as usuario_id, u.nome as usuario_nome, u.email, u.senha, u.reputacao " +
                    "FROM explicacao e " +
                    "JOIN usuario u ON e.usuario_propositor_id = u.id " +
                    "WHERE e.giria_associada_id = ? " +
                    "ORDER BY e.id";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, giriaId);
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        // Recupera a gíria usando o GiriaDAO para ter todas as informações
                        GiriaDAO giriaDAO = new GiriaDAO(connection);
                        Giria giriaAssociada = (Giria) giriaDAO.buscarPorId(giriaId);

                        // Cria o usuário propositor
                        Usuario usuarioPropositor = new Usuario(
                                rst.getInt("usuario_id"),
                                rst.getString("usuario_nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        usuarioPropositor.setReputacao(rst.getInt("reputacao"));

                        // Cria a explicação
                        Explicacao explicacao = new Explicacao(
                                rst.getInt("id"),
                                rst.getString("definicao"),
                                rst.getString("exemplo_uso"),
                                usuarioPropositor,
                                giriaAssociada
                        );
                        explicacao.setDataProposta(rst.getObject("data_proposta", LocalDate.class));
                        explicacao.setAprovada(rst.getBoolean("aprovada"));
                        explicacao.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));

                        explicacoes.add(explicacao);
                    }
                }
            }
            return explicacoes;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar explicações por gíria: " + e.getMessage(), e);
        }
    }

    public ArrayList<Explicacao> listarExplicacoesAprovadas() {
        ArrayList<Explicacao> explicacoes = new ArrayList<>();
        try {
            String sql = "SELECT e.id, e.definicao, e.exemplo_uso, e.aprovada, e.data_proposta, e.data_criacao, " +
                    "u.id as usuario_id, u.nome as usuario_nome, u.email, u.senha, u.reputacao, " +
                    "g.id as giria_id, g.termo, g.aprovada as giria_aprovada " +
                    "FROM explicacao e " +
                    "JOIN usuario u ON e.usuario_propositor_id = u.id " +
                    "JOIN giria g ON e.giria_associada_id = g.id " +
                    "WHERE e.aprovada = true " +
                    "ORDER BY e.id";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        // Cria o usuário propositor
                        Usuario usuarioPropositor = new Usuario(
                                rst.getInt("usuario_id"),
                                rst.getString("usuario_nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        usuarioPropositor.setReputacao(rst.getInt("reputacao"));

                        // Cria uma gíria básica
                        Giria giriaAssociada = new Giria();
                        giriaAssociada.setId(rst.getInt("giria_id"));
                        giriaAssociada.setTermo(rst.getString("termo"));
                        giriaAssociada.setAprovada(rst.getBoolean("giria_aprovada"));

                        // Cria a explicação
                        Explicacao explicacao = new Explicacao(
                                rst.getInt("id"),
                                rst.getString("definicao"),
                                rst.getString("exemplo_uso"),
                                usuarioPropositor,
                                giriaAssociada
                        );
                        explicacao.setDataProposta(rst.getObject("data_proposta", LocalDate.class));
                        explicacao.setAprovada(rst.getBoolean("aprovada"));
                        explicacao.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));

                        explicacoes.add(explicacao);
                    }
                }
            }
            return explicacoes;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar explicações aprovadas: " + e.getMessage(), e);
        }
    }

    public ArrayList<Explicacao> listarExplicacoesAguardandoAprovacao() {
        ArrayList<Explicacao> explicacoes = new ArrayList<>();
        try {
            String sql = "SELECT e.id, e.definicao, e.exemplo_uso, e.aprovada, e.data_proposta, e.data_criacao, " +
                    "u.id as usuario_id, u.nome as usuario_nome, u.email, u.senha, u.reputacao, " +
                    "g.id as giria_id, g.termo, g.aprovada as giria_aprovada " +
                    "FROM explicacao e " +
                    "JOIN usuario u ON e.usuario_propositor_id = u.id " +
                    "JOIN giria g ON e.giria_associada_id = g.id " +
                    "WHERE e.aprovada = false " +
                    "ORDER BY e.id";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        // Cria o usuário propositor
                        Usuario usuarioPropositor = new Usuario(
                                rst.getInt("usuario_id"),
                                rst.getString("usuario_nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        usuarioPropositor.setReputacao(rst.getInt("reputacao"));

                        // Cria uma gíria básica
                        Giria giriaAssociada = new Giria();
                        giriaAssociada.setId(rst.getInt("giria_id"));
                        giriaAssociada.setTermo(rst.getString("termo"));
                        giriaAssociada.setAprovada(rst.getBoolean("giria_aprovada"));

                        // Cria a explicação
                        Explicacao explicacao = new Explicacao(
                                rst.getInt("id"),
                                rst.getString("definicao"),
                                rst.getString("exemplo_uso"),
                                usuarioPropositor,
                                giriaAssociada
                        );
                        explicacao.setDataProposta(rst.getObject("data_proposta", LocalDate.class));
                        explicacao.setAprovada(rst.getBoolean("aprovada"));
                        explicacao.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));

                        explicacoes.add(explicacao);
                    }
                }
            }
            return explicacoes;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar explicações aguardando aprovação: " + e.getMessage(), e);
        }
    }

    public ArrayList<Explicacao> buscarPorUsuario(int usuarioId) {
        ArrayList<Explicacao> explicacoes = new ArrayList<>();
        try {
            String sql = "SELECT e.id, e.definicao, e.exemplo_uso, e.aprovada, e.data_proposta, e.data_criacao, " +
                    "g.id as giria_id, g.termo, g.aprovada as giria_aprovada " +
                    "FROM explicacao e " +
                    "JOIN giria g ON e.giria_associada_id = g.id " +
                    "WHERE e.usuario_propositor_id = ? " +
                    "ORDER BY e.id";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, usuarioId);
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        // Recupera o usuário usando o UsuarioDAO
                        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
                        Usuario usuarioPropositor = (Usuario) usuarioDAO.buscarPorId(usuarioId);

                        // Cria uma gíria básica
                        Giria giriaAssociada = new Giria();
                        giriaAssociada.setId(rst.getInt("giria_id"));
                        giriaAssociada.setTermo(rst.getString("termo"));
                        giriaAssociada.setAprovada(rst.getBoolean("giria_aprovada"));

                        // Cria a explicação
                        Explicacao explicacao = new Explicacao(
                                rst.getInt("id"),
                                rst.getString("definicao"),
                                rst.getString("exemplo_uso"),
                                usuarioPropositor,
                                giriaAssociada
                        );
                        explicacao.setDataProposta(rst.getObject("data_proposta", LocalDate.class));
                        explicacao.setAprovada(rst.getBoolean("aprovada"));
                        explicacao.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));

                        explicacoes.add(explicacao);
                    }
                }
            }
            return explicacoes;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar explicações por usuário: " + e.getMessage(), e);
        }
    }
}
