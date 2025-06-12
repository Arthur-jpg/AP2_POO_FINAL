package dao;

import modelo.Giria;
import modelo.Usuario;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class GiriaDAO implements BaseDAO {

    private Connection connection;

    public GiriaDAO(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void salvar(Object objeto) {
        if (!(objeto instanceof Giria)) {
            throw new IllegalArgumentException("Objeto deve ser do tipo Giria.");
        }

        Giria giria = (Giria) objeto;

        try {
            // Verifica se já existe uma gíria com o mesmo termo
            String checkSql = "SELECT id FROM giria WHERE termo = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setString(1, giria.getTermo());
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    // Se já existe, atualiza os dados da gíria com o ID existente
                    int existingId = rs.getInt("id");
                    giria.setId(existingId);
                    atualizar(giria);
                    return;
                }
            }

            // Verifica se a gíria já existe para decidir entre INSERT e UPDATE
            if (giria.getId() == 0) { // Considera que ID 0 indica novo objeto
                String sql = "INSERT INTO giria (termo, data_cadastro, aprovada, usuario_propositor_id, data_criacao) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstm.setString(1, giria.getTermo());
                    pstm.setObject(2, giria.getDataCadastro());
                    pstm.setBoolean(3, giria.isAprovada());
                    pstm.setInt(4, giria.getUsuarioPropositor().getId());
                    pstm.setObject(5, giria.getDataCriacao());

                    pstm.execute();

                    try (ResultSet rst = pstm.getGeneratedKeys()) {
                        if (rst.next()) {
                            giria.setId(rst.getInt(1)); // Define o ID gerado pelo banco
                        }
                    }

                    // Salvando categorias da gíria
                    salvarCategorias(giria);

                    // Salvando regiões da gíria
                    salvarRegioes(giria);
                }
            } else { // Se já tem ID, tenta atualizar
                atualizar(giria); // Chama o método de atualização
            }

            // OBS: As explicações e votos são salvos pelos seus próprios DAOs, não aqui

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar gíria: " + e.getMessage(), e);
        }
    }

    private void salvarCategorias(Giria giria) throws SQLException {
        // Primeiro exclui as categorias existentes para essa gíria para evitar duplicidade
        if (giria.getId() > 0) {
            String sqlDeleteCategorias = "DELETE FROM giria_categoria WHERE giria_id = ?";
            try (PreparedStatement pstm = connection.prepareStatement(sqlDeleteCategorias)) {
                pstm.setInt(1, giria.getId());
                pstm.executeUpdate();
            }
        }

        // Insere as categorias atuais
        if (giria.getCategorias() != null && !giria.getCategorias().isEmpty()) {
            String sqlInsertCategoria = "INSERT INTO giria_categoria (giria_id, categoria_id) " +
                    "SELECT ?, id FROM categoria WHERE nome = ?";

            try (PreparedStatement pstm = connection.prepareStatement(sqlInsertCategoria)) {
                for (String categoria : giria.getCategorias()) {
                    // Primeiro verifica se a categoria existe, se não, cria
                    criarCategoriaSeNaoExiste(categoria);

                    // Insere na tabela de associação
                    pstm.setInt(1, giria.getId());
                    pstm.setString(2, categoria);
                    pstm.addBatch(); // Adiciona à batch para executar em lote
                }
                pstm.executeBatch(); // Executa todas as inserções em lote
            }
        }
    }

    private void criarCategoriaSeNaoExiste(String nomeCategoria) throws SQLException {
        String sqlCheck = "SELECT COUNT(*) FROM categoria WHERE nome = ?";
        try (PreparedStatement pstmCheck = connection.prepareStatement(sqlCheck)) {
            pstmCheck.setString(1, nomeCategoria);
            try (ResultSet rst = pstmCheck.executeQuery()) {
                if (rst.next() && rst.getInt(1) == 0) {
                    // Se não existe, cria a categoria
                    String sqlInsert = "INSERT INTO categoria (nome) VALUES (?)";
                    try (PreparedStatement pstmInsert = connection.prepareStatement(sqlInsert)) {
                        pstmInsert.setString(1, nomeCategoria);
                        pstmInsert.executeUpdate();
                    }
                }
            }
        }
    }

    private void salvarRegioes(Giria giria) throws SQLException {
        // Primeiro exclui as regiões existentes para essa gíria para evitar duplicidade
        if (giria.getId() > 0) {
            String sqlDeleteRegioes = "DELETE FROM giria_regiao WHERE giria_id = ?";
            try (PreparedStatement pstm = connection.prepareStatement(sqlDeleteRegioes)) {
                pstm.setInt(1, giria.getId());
                pstm.executeUpdate();
            }
        }

        // Insere as regiões atuais
        if (giria.getRegioes() != null && !giria.getRegioes().isEmpty()) {
            String sqlInsertRegiao = "INSERT INTO giria_regiao (giria_id, regiao_id) " +
                    "SELECT ?, id FROM regiao WHERE nome = ?";

            try (PreparedStatement pstm = connection.prepareStatement(sqlInsertRegiao)) {
                for (String regiao : giria.getRegioes()) {
                    // Primeiro verifica se a região existe, se não, cria
                    criarRegiaoSeNaoExiste(regiao);

                    // Insere na tabela de associação
                    pstm.setInt(1, giria.getId());
                    pstm.setString(2, regiao);
                    pstm.addBatch(); // Adiciona à batch para executar em lote
                }
                pstm.executeBatch(); // Executa todas as inserções em lote
            }
        }
    }

    private void criarRegiaoSeNaoExiste(String nomeRegiao) throws SQLException {
        String sqlCheck = "SELECT COUNT(*) FROM regiao WHERE nome = ?";
        try (PreparedStatement pstmCheck = connection.prepareStatement(sqlCheck)) {
            pstmCheck.setString(1, nomeRegiao);
            try (ResultSet rst = pstmCheck.executeQuery()) {
                if (rst.next() && rst.getInt(1) == 0) {
                    // Se não existe, cria a região
                    String sqlInsert = "INSERT INTO regiao (nome) VALUES (?)";
                    try (PreparedStatement pstmInsert = connection.prepareStatement(sqlInsert)) {
                        pstmInsert.setString(1, nomeRegiao);
                        pstmInsert.executeUpdate();
                    }
                }
            }
        }
    }

    @Override
    public Object buscarPorId(int id) {
        Giria giria = null;
        try {
            // Consulta principal para buscar uma gíria
            String sql = "SELECT g.id, g.termo, g.data_cadastro, g.aprovada, g.data_criacao, " +
                    "u.id as usuario_id, u.nome, u.email, u.senha, u.reputacao " +
                    "FROM giria g " +
                    "JOIN usuario u ON g.usuario_propositor_id = u.id " +
                    "WHERE g.id = ?";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, id);
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    if (rst.next()) {
                        // Cria o usuário propositor
                        Usuario propositor = new Usuario(
                                rst.getInt("usuario_id"),
                                rst.getString("nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        propositor.setReputacao(rst.getInt("reputacao"));

                        // Cria a gíria
                        giria = new Giria(
                                rst.getInt("id"),
                                rst.getString("termo"),
                                rst.getObject("data_cadastro", LocalDateTime.class),
                                rst.getBoolean("aprovada"),
                                propositor
                        );
                        giria.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));

                        // Carregar categorias e regiões
                        carregarCategorias(giria);
                        carregarRegioes(giria);
                    }
                }
            }

            return giria;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar gíria por ID: " + e.getMessage(), e);
        }
    }

    private void carregarCategorias(Giria giria) throws SQLException {
        String sql = "SELECT c.nome FROM categoria c " +
                "JOIN giria_categoria gc ON c.id = gc.categoria_id " +
                "WHERE gc.giria_id = ?";

        try (PreparedStatement pstm = connection.prepareStatement(sql)) {
            pstm.setInt(1, giria.getId());
            try (ResultSet rst = pstm.executeQuery()) {
                while (rst.next()) {
                    giria.adicionarCategoria(rst.getString("nome"));
                }
            }
        }
    }

    private void carregarRegioes(Giria giria) throws SQLException {
        String sql = "SELECT r.nome FROM regiao r " +
                "JOIN giria_regiao gr ON r.id = gr.regiao_id " +
                "WHERE gr.giria_id = ?";

        try (PreparedStatement pstm = connection.prepareStatement(sql)) {
            pstm.setInt(1, giria.getId());
            try (ResultSet rst = pstm.executeQuery()) {
                while (rst.next()) {
                    giria.adicionarRegiao(rst.getString("nome"));
                }
            }
        }
    }

    @Override
    public ArrayList<Object> listarTodosLazyLoading() {
        ArrayList<Object> girias = new ArrayList<>();
        try {
            String sql = "SELECT g.id, g.termo, g.data_cadastro, g.aprovada, g.data_criacao, " +
                    "u.id as usuario_id, u.nome, u.email, u.senha, u.reputacao " +
                    "FROM giria g " +
                    "JOIN usuario u ON g.usuario_propositor_id = u.id " +
                    "ORDER BY g.id";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        // Cria o usuário propositor
                        Usuario propositor = new Usuario(
                                rst.getInt("usuario_id"),
                                rst.getString("nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        propositor.setReputacao(rst.getInt("reputacao"));

                        // Cria a gíria
                        Giria giria = new Giria(
                                rst.getInt("id"),
                                rst.getString("termo"),
                                rst.getObject("data_cadastro", LocalDateTime.class),
                                rst.getBoolean("aprovada"),
                                propositor
                        );
                        giria.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));

                        // No lazy loading, não carregamos categorias, regiões, explicações ou votos
                        girias.add(giria);
                    }
                }
            }
            return girias;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar todas as gírias (Lazy Loading): " + e.getMessage(), e);
        }
    }

    @Override
    public ArrayList<Object> listarTodosEagerLoading() {
        ArrayList<Object> girias = new ArrayList<>();
        try {
            String sql = "SELECT g.id, g.termo, g.data_cadastro, g.aprovada, g.data_criacao, " +
                    "u.id as usuario_id, u.nome, u.email, u.senha, u.reputacao " +
                    "FROM giria g " +
                    "JOIN usuario u ON g.usuario_propositor_id = u.id " +
                    "ORDER BY g.id";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        // Cria o usuário propositor
                        Usuario propositor = new Usuario(
                                rst.getInt("usuario_id"),
                                rst.getString("nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        propositor.setReputacao(rst.getInt("reputacao"));

                        // Cria a gíria
                        Giria giria = new Giria(
                                rst.getInt("id"),
                                rst.getString("termo"),
                                rst.getObject("data_cadastro", LocalDateTime.class),
                                rst.getBoolean("aprovada"),
                                propositor
                        );
                        giria.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));

                        // No eager loading, carregamos categorias e regiões
                        carregarCategorias(giria);
                        carregarRegioes(giria);

                        // Carregar votos da gíria
                        VotoDAO votoDAO = new VotoDAO(connection);
                        ArrayList<modelo.Voto> votos = votoDAO.listarPorGiriaId(giria.getId());
                        for (modelo.Voto voto : votos) {
                            giria.adicionarVoto(voto);
                        }

                        // E também carregaríamos explicações e votos, mas isso é melhor
                        // ser feito por seus respectivos DAOs para evitar complexidade excessiva

                        girias.add(giria);
                    }
                }
            }
            return girias;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar todas as gírias (Eager Loading): " + e.getMessage(), e);
        }
    }

    @Override
    public void atualizar(Object objeto) {
        if (!(objeto instanceof Giria)) {
            throw new IllegalArgumentException("Objeto deve ser do tipo Giria.");
        }

        Giria giria = (Giria) objeto;

        String sql = "UPDATE giria SET termo = ?, data_cadastro = ?, aprovada = ?, " +
                "usuario_propositor_id = ? WHERE id = ?";

        try (PreparedStatement pstm = connection.prepareStatement(sql)) {
            pstm.setString(1, giria.getTermo());
            pstm.setObject(2, giria.getDataCadastro());
            pstm.setBoolean(3, giria.isAprovada());
            pstm.setInt(4, giria.getUsuarioPropositor().getId());
            pstm.setInt(5, giria.getId());

            int linhasAfetadas = pstm.executeUpdate();

            if (linhasAfetadas == 0) {
                throw new SQLException("Falha ao atualizar gíria: nenhuma linha foi afetada.");
            }

            // Atualiza categorias e regiões
            salvarCategorias(giria);
            salvarRegioes(giria);

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar gíria: " + e.getMessage(), e);
        }
    }

    @Override
    public void excluir(int id) {
        try {
            // Devido às configurações ON DELETE CASCADE, as entradas relacionadas
            // nas tabelas giria_categoria, giria_regiao serão excluídas automaticamente.
            // Também excluirá as explicações e votos se estiverem configurados como CASCADE.

            String sql = "DELETE FROM giria WHERE id = ?";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, id);

                int linhasAfetadas = pstm.executeUpdate();

                if (linhasAfetadas == 0) {
                    throw new SQLException("Falha ao deletar gíria: nenhuma linha foi afetada.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir gíria: " + e.getMessage(), e);
        }
    }

    // Métodos específicos para a classe GiriaDAO

    public ArrayList<Giria> buscarPorTermo(String termo) {
        ArrayList<Giria> girias = new ArrayList<>();
        try {
            String sql = "SELECT g.id, g.termo, g.data_cadastro, g.aprovada, g.data_criacao, " +
                    "u.id as usuario_id, u.nome, u.email, u.senha, u.reputacao " +
                    "FROM giria g " +
                    "JOIN usuario u ON g.usuario_propositor_id = u.id " +
                    "WHERE g.termo LIKE ? " +
                    "ORDER BY g.id";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setString(1, "%" + termo + "%");
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        Usuario propositor = new Usuario(
                                rst.getInt("usuario_id"),
                                rst.getString("nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        propositor.setReputacao(rst.getInt("reputacao"));

                        Giria giria = new Giria(
                                rst.getInt("id"),
                                rst.getString("termo"),
                                rst.getObject("data_cadastro", LocalDateTime.class),
                                rst.getBoolean("aprovada"),
                                propositor
                        );
                        giria.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));

                        girias.add(giria);
                    }
                }
            }
            return girias;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar gírias por termo: " + e.getMessage(), e);
        }
    }

    public ArrayList<Giria> buscarPorCategoria(String categoria) {
        ArrayList<Giria> girias = new ArrayList<>();
        try {
            String sql = "SELECT g.id, g.termo, g.data_cadastro, g.aprovada, g.data_criacao, " +
                    "u.id as usuario_id, u.nome, u.email, u.senha, u.reputacao " +
                    "FROM giria g " +
                    "JOIN usuario u ON g.usuario_propositor_id = u.id " +
                    "JOIN giria_categoria gc ON g.id = gc.giria_id " +
                    "JOIN categoria c ON gc.categoria_id = c.id " +
                    "WHERE c.nome LIKE ? " +
                    "ORDER BY g.id";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setString(1, "%" + categoria + "%");
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        Usuario propositor = new Usuario(
                                rst.getInt("usuario_id"),
                                rst.getString("nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        propositor.setReputacao(rst.getInt("reputacao"));

                        Giria giria = new Giria(
                                rst.getInt("id"),
                                rst.getString("termo"),
                                rst.getObject("data_cadastro", LocalDateTime.class),
                                rst.getBoolean("aprovada"),
                                propositor
                        );
                        giria.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));

                        girias.add(giria);
                    }
                }
            }
            return girias;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar gírias por categoria: " + e.getMessage(), e);
        }
    }

    public ArrayList<Giria> buscarPorRegiao(String regiao) {
        ArrayList<Giria> girias = new ArrayList<>();
        try {
            String sql = "SELECT g.id, g.termo, g.data_cadastro, g.aprovada, g.data_criacao, " +
                    "u.id as usuario_id, u.nome, u.email, u.senha, u.reputacao " +
                    "FROM giria g " +
                    "JOIN usuario u ON g.usuario_propositor_id = u.id " +
                    "JOIN giria_regiao gr ON g.id = gr.giria_id " +
                    "JOIN regiao r ON gr.regiao_id = r.id " +
                    "WHERE r.nome LIKE ? " +
                    "ORDER BY g.id";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setString(1, "%" + regiao + "%");
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        Usuario propositor = new Usuario(
                                rst.getInt("usuario_id"),
                                rst.getString("nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        propositor.setReputacao(rst.getInt("reputacao"));

                        Giria giria = new Giria(
                                rst.getInt("id"),
                                rst.getString("termo"),
                                rst.getObject("data_cadastro", LocalDateTime.class),
                                rst.getBoolean("aprovada"),
                                propositor
                        );
                        giria.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));

                        girias.add(giria);
                    }
                }
            }
            return girias;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar gírias por região: " + e.getMessage(), e);
        }
    }

    public ArrayList<Giria> listarGiriasAprovadas() {
        ArrayList<Giria> girias = new ArrayList<>();
        try {
            String sql = "SELECT g.id, g.termo, g.data_cadastro, g.aprovada, g.data_criacao, " +
                    "u.id as usuario_id, u.nome, u.email, u.senha, u.reputacao " +
                    "FROM giria g " +
                    "JOIN usuario u ON g.usuario_propositor_id = u.id " +
                    "WHERE g.aprovada = true " +
                    "ORDER BY g.id";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        Usuario propositor = new Usuario(
                                rst.getInt("usuario_id"),
                                rst.getString("nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        propositor.setReputacao(rst.getInt("reputacao"));

                        Giria giria = new Giria(
                                rst.getInt("id"),
                                rst.getString("termo"),
                                rst.getObject("data_cadastro", LocalDateTime.class),
                                rst.getBoolean("aprovada"),
                                propositor
                        );
                        giria.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));

                        girias.add(giria);
                    }
                }
            }
            return girias;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar gírias aprovadas: " + e.getMessage(), e);
        }
    }

    public ArrayList<Giria> listarGiriasAguardandoAprovacao() {
        ArrayList<Giria> girias = new ArrayList<>();
        try {
            String sql = "SELECT g.id, g.termo, g.data_cadastro, g.aprovada, g.data_criacao, " +
                    "u.id as usuario_id, u.nome, u.email, u.senha, u.reputacao " +
                    "FROM giria g " +
                    "JOIN usuario u ON g.usuario_propositor_id = u.id " +
                    "WHERE g.aprovada = false " +
                    "ORDER BY g.id";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        Usuario propositor = new Usuario(
                                rst.getInt("usuario_id"),
                                rst.getString("nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        propositor.setReputacao(rst.getInt("reputacao"));

                        Giria giria = new Giria(
                                rst.getInt("id"),
                                rst.getString("termo"),
                                rst.getObject("data_cadastro", LocalDateTime.class),
                                rst.getBoolean("aprovada"),
                                propositor
                        );
                        giria.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));

                        girias.add(giria);
                    }
                }
            }
            return girias;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar gírias aguardando aprovação: " + e.getMessage(), e);
        }
    }

    // Método adicional para obter categorias
    public Set<String> obterTodasCategorias() {
        Set<String> categorias = new HashSet<>();
        try {
            String sql = "SELECT nome FROM categoria ORDER BY nome";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                try (ResultSet rst = pstm.executeQuery()) {
                    while (rst.next()) {
                        categorias.add(rst.getString("nome"));
                    }
                }
            }
            return categorias;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter todas as categorias: " + e.getMessage(), e);
        }
    }

    // Método adicional para obter regiões
    public Set<String> obterTodasRegioes() {
        Set<String> regioes = new HashSet<>();
        try {
            String sql = "SELECT nome FROM regiao ORDER BY nome";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                try (ResultSet rst = pstm.executeQuery()) {
                    while (rst.next()) {
                        regioes.add(rst.getString("nome"));
                    }
                }
            }
            return regioes;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter todas as regiões: " + e.getMessage(), e);
        }
    }
}
