package dao;

import modelo.EnumVoto;
import modelo.Usuario;
import modelo.Voto;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class VotoDAO implements BaseDAO {

    private Connection connection;

    public VotoDAO(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void salvar(Object objeto) {
        if (!(objeto instanceof Voto)) {
            throw new IllegalArgumentException("Objeto deve ser do tipo Voto.");
        }

        Voto voto = (Voto) objeto;

        try {
            // Verifica se já existe um voto do mesmo usuário para o mesmo objeto
            String checkSql = "SELECT id, tipo FROM voto WHERE usuario_votante_id = ? AND objeto_avaliado_id = ? AND tipo_objeto_avaliado = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setInt(1, voto.getUsuarioVotante().getId());
                checkStmt.setInt(2, voto.getObjetoAvaliadoId());
                checkStmt.setString(3, voto.getTipoObjetoAvaliado());
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    // Se já existe, atualiza apenas se o tipo de voto for diferente
                    int existingId = rs.getInt("id");
                    String existingTipo = rs.getString("tipo");

                    if (!existingTipo.equals(voto.getTipo().name())) {
                        voto.setId(existingId);
                        atualizar(voto);

                        // Atualiza a reputação considerando a mudança de voto (dobro do impacto)
                        atualizarReputacaoUsuarioMudancaVoto(voto, EnumVoto.valueOf(existingTipo));
                    }
                    return;
                }
            }

            // Verifica se o voto já existe para decidir entre INSERT e UPDATE
            if (voto.getId() == 0) { // Considera que ID 0 indica novo objeto
                String sql = "INSERT INTO voto (tipo, data_voto, usuario_votante_id, objeto_avaliado_id, " +
                        "tipo_objeto_avaliado, data_criacao) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstm.setString(1, voto.getTipo().name());
                    pstm.setObject(2, voto.getDataVoto());
                    pstm.setInt(3, voto.getUsuarioVotante().getId());
                    pstm.setInt(4, voto.getObjetoAvaliadoId());
                    pstm.setString(5, voto.getTipoObjetoAvaliado());
                    pstm.setObject(6, voto.getDataCriacao());

                    pstm.execute();

                    try (ResultSet rst = pstm.getGeneratedKeys()) {
                        if (rst.next()) {
                            voto.setId(rst.getInt(1)); // Define o ID gerado pelo banco
                        }
                    }
                }

                // Atualiza a reputação do usuário que criou o objeto avaliado
                atualizarReputacaoUsuario(voto);

            } else { // Se já tem ID, tenta atualizar
                atualizar(voto); // Chama o método de atualização
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar voto: " + e.getMessage(), e);
        }
    }

    private void atualizarReputacaoUsuario(Voto voto) throws SQLException {
        // Busca o ID do usuário que criou o objeto avaliado (giria ou explicacao)
        int usuarioPropositorId = 0;
        String tipo = voto.getTipoObjetoAvaliado();

        if ("GIRIA".equals(tipo)) {
            String sql = "SELECT usuario_propositor_id FROM giria WHERE id = ?";
            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, voto.getObjetoAvaliadoId());
                try (ResultSet rst = pstm.executeQuery()) {
                    if (rst.next()) {
                        usuarioPropositorId = rst.getInt("usuario_propositor_id");
                    }
                }
            }
        } else if ("EXPLICACAO".equals(tipo)) {
            String sql = "SELECT usuario_propositor_id FROM explicacao WHERE id = ?";
            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, voto.getObjetoAvaliadoId());
                try (ResultSet rst = pstm.executeQuery()) {
                    if (rst.next()) {
                        usuarioPropositorId = rst.getInt("usuario_propositor_id");
                    }
                }
            }
        }

        // Se encontrou o usuário propositor, atualiza sua reputação
        if (usuarioPropositorId > 0) {
            // Recupera reputação atual
            int reputacaoAtual = 0;
            String sqlSelect = "SELECT reputacao FROM usuario WHERE id = ?";
            try (PreparedStatement pstm = connection.prepareStatement(sqlSelect)) {
                pstm.setInt(1, usuarioPropositorId);
                try (ResultSet rst = pstm.executeQuery()) {
                    if (rst.next()) {
                        reputacaoAtual = rst.getInt("reputacao");
                    }
                }
            }

            // Calcula nova reputação
            int novaReputacao = reputacaoAtual;
            if (voto.getTipo() == EnumVoto.POSITIVO) {
                novaReputacao++;
            } else {
                novaReputacao--;
            }

            // Atualiza reputação
            String sqlUpdate = "UPDATE usuario SET reputacao = ? WHERE id = ?";
            try (PreparedStatement pstm = connection.prepareStatement(sqlUpdate)) {
                pstm.setInt(1, novaReputacao);
                pstm.setInt(2, usuarioPropositorId);
                pstm.executeUpdate();
            }
        }
    }

    private void atualizarReputacaoUsuarioMudancaVoto(Voto votoNovo, EnumVoto tipoAnterior) throws SQLException {
        // Busca o ID do usuário que criou o objeto avaliado (giria ou explicacao)
        int usuarioPropositorId = 0;
        String tipo = votoNovo.getTipoObjetoAvaliado();

        if ("GIRIA".equals(tipo)) {
            String sql = "SELECT usuario_propositor_id FROM giria WHERE id = ?";
            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, votoNovo.getObjetoAvaliadoId());
                try (ResultSet rst = pstm.executeQuery()) {
                    if (rst.next()) {
                        usuarioPropositorId = rst.getInt("usuario_propositor_id");
                    }
                }
            }
        } else if ("EXPLICACAO".equals(tipo)) {
            String sql = "SELECT usuario_propositor_id FROM explicacao WHERE id = ?";
            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, votoNovo.getObjetoAvaliadoId());
                try (ResultSet rst = pstm.executeQuery()) {
                    if (rst.next()) {
                        usuarioPropositorId = rst.getInt("usuario_propositor_id");
                    }
                }
            }
        }

        // Se encontrou o usuário propositor, atualiza sua reputação
        if (usuarioPropositorId > 0) {
            // Recupera reputação atual
            int reputacaoAtual = 0;
            String sqlSelect = "SELECT reputacao FROM usuario WHERE id = ?";
            try (PreparedStatement pstm = connection.prepareStatement(sqlSelect)) {
                pstm.setInt(1, usuarioPropositorId);
                try (ResultSet rst = pstm.executeQuery()) {
                    if (rst.next()) {
                        reputacaoAtual = rst.getInt("reputacao");
                    }
                }
            }

            // Calcula nova reputação
            int novaReputacao = reputacaoAtual;

            // Se o voto anterior era NEGATIVO e o novo é POSITIVO: +2 (remover -1 e adicionar +1)
            // Se o voto anterior era POSITIVO e o novo é NEGATIVO: -2 (remover +1 e adicionar -1)
            if (tipoAnterior == EnumVoto.NEGATIVO && votoNovo.getTipo() == EnumVoto.POSITIVO) {
                novaReputacao += 2;
            } else if (tipoAnterior == EnumVoto.POSITIVO && votoNovo.getTipo() == EnumVoto.NEGATIVO) {
                novaReputacao -= 2;
            }

            // Atualiza reputação
            String sqlUpdate = "UPDATE usuario SET reputacao = ? WHERE id = ?";
            try (PreparedStatement pstm = connection.prepareStatement(sqlUpdate)) {
                pstm.setInt(1, novaReputacao);
                pstm.setInt(2, usuarioPropositorId);
                pstm.executeUpdate();
            }
        }
    }

    @Override
    public Object buscarPorId(int id) {
        Voto voto = null;
        try {
            String sql = "SELECT v.id, v.tipo, v.data_voto, v.objeto_avaliado_id, v.tipo_objeto_avaliado, v.data_criacao, " +
                    "u.id as usuario_id, u.nome, u.email, u.senha, u.reputacao " +
                    "FROM voto v " +
                    "JOIN usuario u ON v.usuario_votante_id = u.id " +
                    "WHERE v.id = ?";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, id);
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    if (rst.next()) {
                        // Cria o usuário votante
                        Usuario usuarioVotante = new Usuario(
                                rst.getInt("usuario_id"),
                                rst.getString("nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        usuarioVotante.setReputacao(rst.getInt("reputacao"));

                        // Cria o voto
                        voto = new Voto(
                                rst.getInt("id"),
                                EnumVoto.valueOf(rst.getString("tipo")),
                                usuarioVotante,
                                rst.getInt("objeto_avaliado_id"),
                                rst.getString("tipo_objeto_avaliado")
                        );
                        voto.setDataVoto(rst.getObject("data_voto", LocalDateTime.class));
                        voto.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));
                    }
                }
            }

            return voto;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar voto por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public ArrayList<Object> listarTodosLazyLoading() {
        ArrayList<Object> votos = new ArrayList<>();
        try {
            String sql = "SELECT v.id, v.tipo, v.data_voto, v.objeto_avaliado_id, v.tipo_objeto_avaliado, v.data_criacao, " +
                    "u.id as usuario_id, u.nome, u.email, u.senha, u.reputacao " +
                    "FROM voto v " +
                    "JOIN usuario u ON v.usuario_votante_id = u.id " +
                    "ORDER BY v.id";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        // Cria o usuário votante
                        Usuario usuarioVotante = new Usuario(
                                rst.getInt("usuario_id"),
                                rst.getString("nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        usuarioVotante.setReputacao(rst.getInt("reputacao"));

                        // Cria o voto
                        Voto voto = new Voto(
                                rst.getInt("id"),
                                EnumVoto.valueOf(rst.getString("tipo")),
                                usuarioVotante,
                                rst.getInt("objeto_avaliado_id"),
                                rst.getString("tipo_objeto_avaliado")
                        );
                        voto.setDataVoto(rst.getObject("data_voto", LocalDateTime.class));
                        voto.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));

                        votos.add(voto);
                    }
                }
            }
            return votos;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar todos os votos (Lazy Loading): " + e.getMessage(), e);
        }
    }

    @Override
    public ArrayList<Object> listarTodosEagerLoading() {
        // Para votos, o eager loading seria igual ao lazy loading,
        // já que não há outras entidades dependentes para serem carregadas.
        // O objeto avaliado já é referenciado pelo ID e tipo.
        return listarTodosLazyLoading();
    }

    @Override
    public void atualizar(Object objeto) {
        if (!(objeto instanceof Voto)) {
            throw new IllegalArgumentException("Objeto deve ser do tipo Voto.");
        }

        Voto voto = (Voto) objeto;

        // Busca o voto atual para saber se o tipo mudou e precisa reverter reputação
        Voto votoAtual = (Voto) buscarPorId(voto.getId());
        boolean tipoMudou = votoAtual != null && votoAtual.getTipo() != voto.getTipo();

        String sql = "UPDATE voto SET tipo = ?, data_voto = ?, usuario_votante_id = ?, " +
                "objeto_avaliado_id = ?, tipo_objeto_avaliado = ? " +
                "WHERE id = ?";

        try (PreparedStatement pstm = connection.prepareStatement(sql)) {
            pstm.setString(1, voto.getTipo().name());
            pstm.setObject(2, voto.getDataVoto());
            pstm.setInt(3, voto.getUsuarioVotante().getId());
            pstm.setInt(4, voto.getObjetoAvaliadoId());
            pstm.setString(5, voto.getTipoObjetoAvaliado());
            pstm.setInt(6, voto.getId());

            int linhasAfetadas = pstm.executeUpdate();

            if (linhasAfetadas == 0) {
                throw new SQLException("Falha ao atualizar voto: nenhuma linha foi afetada.");
            }

            // Se o tipo de voto mudou, atualiza reputação do usuário que propôs o conteúdo
            if (tipoMudou) {
                // Um "truque" para duplicar o efeito da troca de voto: remover o efeito antigo e aplicar o novo
                // Para isso, inverte o voto antigo e salva para "cancelar" o efeito e depois salva o novo
                Voto votoReversao = new Voto(
                        0, // ID 0 para não atualizar o voto existente
                        votoAtual.getTipo() == EnumVoto.POSITIVO ? EnumVoto.NEGATIVO : EnumVoto.POSITIVO,
                        votoAtual.getUsuarioVotante(),
                        votoAtual.getObjetoAvaliadoId(),
                        votoAtual.getTipoObjetoAvaliado()
                );

                // Aplica a reversão no banco sem salvar como novo voto (só para atualizar reputação)
                atualizarReputacaoUsuario(votoReversao);

                // Agora aplica o novo voto
                atualizarReputacaoUsuario(voto);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar voto: " + e.getMessage(), e);
        }
    }

    @Override
    public void excluir(int id) {
        try {
            // Antes de excluir, recupera o voto para poder desfazer seu efeito na reputação
            Voto voto = (Voto) buscarPorId(id);

            String sql = "DELETE FROM voto WHERE id = ?";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, id);

                int linhasAfetadas = pstm.executeUpdate();

                if (linhasAfetadas == 0) {
                    throw new SQLException("Falha ao deletar voto: nenhuma linha foi afetada.");
                }
            }

            // Desfaz o efeito do voto na reputação do usuário propositor
            if (voto != null) {
                Voto votoReversao = new Voto(
                        0, // ID 0 para não atualizar o voto existente
                        voto.getTipo() == EnumVoto.POSITIVO ? EnumVoto.NEGATIVO : EnumVoto.POSITIVO,
                        voto.getUsuarioVotante(),
                        voto.getObjetoAvaliadoId(),
                        voto.getTipoObjetoAvaliado()
                );

                // Aplica a reversão no banco sem salvar (só para atualizar reputação)
                atualizarReputacaoUsuario(votoReversao);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir voto: " + e.getMessage(), e);
        }
    }

    // Métodos específicos para a classe VotoDAO

    public ArrayList<Voto> buscarVotosPorUsuario(int usuarioId) {
        ArrayList<Voto> votos = new ArrayList<>();
        try {
            String sql = "SELECT v.id, v.tipo, v.data_voto, v.objeto_avaliado_id, v.tipo_objeto_avaliado, v.data_criacao " +
                    "FROM voto v " +
                    "WHERE v.usuario_votante_id = ? " +
                    "ORDER BY v.data_voto DESC";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, usuarioId);
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        // Recupera o usuário para evitar consultas repetidas
                        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
                        Usuario usuarioVotante = (Usuario) usuarioDAO.buscarPorId(usuarioId);

                        // Cria o voto
                        Voto voto = new Voto(
                                rst.getInt("id"),
                                EnumVoto.valueOf(rst.getString("tipo")),
                                usuarioVotante,
                                rst.getInt("objeto_avaliado_id"),
                                rst.getString("tipo_objeto_avaliado")
                        );
                        voto.setDataVoto(rst.getObject("data_voto", LocalDateTime.class));
                        voto.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));

                        votos.add(voto);
                    }
                }
            }
            return votos;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar votos por usuário: " + e.getMessage(), e);
        }
    }

    public ArrayList<Voto> buscarVotosPorObjeto(int objetoId, String tipoObjeto) {
        ArrayList<Voto> votos = new ArrayList<>();
        try {
            String sql = "SELECT v.id, v.tipo, v.data_voto, v.data_criacao, " +
                    "u.id as usuario_id, u.nome, u.email, u.senha, u.reputacao " +
                    "FROM voto v " +
                    "JOIN usuario u ON v.usuario_votante_id = u.id " +
                    "WHERE v.objeto_avaliado_id = ? AND v.tipo_objeto_avaliado = ? " +
                    "ORDER BY v.data_voto DESC";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, objetoId);
                pstm.setString(2, tipoObjeto);
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        // Cria o usuário votante
                        Usuario usuarioVotante = new Usuario(
                                rst.getInt("usuario_id"),
                                rst.getString("nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        usuarioVotante.setReputacao(rst.getInt("reputacao"));

                        // Cria o voto
                        Voto voto = new Voto(
                                rst.getInt("id"),
                                EnumVoto.valueOf(rst.getString("tipo")),
                                usuarioVotante,
                                objetoId,
                                tipoObjeto
                        );
                        voto.setDataVoto(rst.getObject("data_voto", LocalDateTime.class));
                        voto.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));

                        votos.add(voto);
                    }
                }
            }
            return votos;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar votos por objeto: " + e.getMessage(), e);
        }
    }

    public int calcularPontuacaoVotos(int objetoId, String tipoObjeto) {
        int pontuacao = 0;
        try {
            String sql = "SELECT " +
                    "SUM(CASE WHEN tipo = 'POSITIVO' THEN 1 ELSE -1 END) as pontuacao " +
                    "FROM voto " +
                    "WHERE objeto_avaliado_id = ? AND tipo_objeto_avaliado = ?";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, objetoId);
                pstm.setString(2, tipoObjeto);

                try (ResultSet rst = pstm.executeQuery()) {
                    if (rst.next() && rst.getObject("pontuacao") != null) {
                        pontuacao = rst.getInt("pontuacao");
                    }
                }
            }
            return pontuacao;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao calcular pontuação de votos: " + e.getMessage(), e);
        }
    }

    public Voto buscarVotoUsuarioObjeto(int usuarioId, int objetoId, String tipoObjeto) {
        try {
            String sql = "SELECT id, tipo, data_voto, data_criacao " +
                    "FROM voto " +
                    "WHERE usuario_votante_id = ? AND objeto_avaliado_id = ? AND tipo_objeto_avaliado = ?";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, usuarioId);
                pstm.setInt(2, objetoId);
                pstm.setString(3, tipoObjeto);

                try (ResultSet rst = pstm.executeQuery()) {
                    if (rst.next()) {
                        // Recupera o usuário
                        UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
                        Usuario usuarioVotante = (Usuario) usuarioDAO.buscarPorId(usuarioId);

                        // Cria o voto
                        Voto voto = new Voto(
                                rst.getInt("id"),
                                EnumVoto.valueOf(rst.getString("tipo")),
                                usuarioVotante,
                                objetoId,
                                tipoObjeto
                        );
                        voto.setDataVoto(rst.getObject("data_voto", LocalDateTime.class));
                        voto.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));

                        return voto;
                    }
                }
            }
            return null; // Não encontrou voto desse usuário para esse objeto
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar voto de usuário em objeto: " + e.getMessage(), e);
        }
    }


    public ArrayList<Voto> listarPorGiriaId(int giriaId) {
        ArrayList<Voto> votos = new ArrayList<>();
        try {
            String sql = "SELECT v.id, v.tipo, v.data_voto, v.objeto_avaliado_id, v.tipo_objeto_avaliado, v.data_criacao, " +
                    "u.id as usuario_id, u.nome, u.email, u.senha, u.reputacao " +
                    "FROM voto v " +
                    "JOIN usuario u ON v.usuario_votante_id = u.id " +
                    "WHERE v.objeto_avaliado_id = ? AND v.tipo_objeto_avaliado = 'GIRIA' ";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, giriaId);
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        Usuario usuarioVotante = new Usuario(
                                rst.getInt("usuario_id"),
                                rst.getString("nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        usuarioVotante.setReputacao(rst.getInt("reputacao"));

                        Voto voto = new Voto(
                                rst.getInt("id"),
                                EnumVoto.valueOf(rst.getString("tipo")),
                                usuarioVotante,
                                rst.getInt("objeto_avaliado_id"),
                                rst.getString("tipo_objeto_avaliado")
                        );
                        voto.setDataVoto(rst.getObject("data_voto", LocalDateTime.class));
                        voto.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));
                        votos.add(voto);
                    }
                }
            }
            return votos;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar votos por gíria: " + e.getMessage(), e);
        }
    }


    public ArrayList<Voto> listarPorExplicacaoId(int explicacaoId) {
        ArrayList<Voto> votos = new ArrayList<>();
        try {
            String sql = "SELECT v.id, v.tipo, v.data_voto, v.objeto_avaliado_id, v.tipo_objeto_avaliado, v.data_criacao, " +
                    "u.id as usuario_id, u.nome, u.email, u.senha, u.reputacao " +
                    "FROM voto v " +
                    "JOIN usuario u ON v.usuario_votante_id = u.id " +
                    "WHERE v.objeto_avaliado_id = ? AND v.tipo_objeto_avaliado = 'EXPLICACAO' ";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, explicacaoId);
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        Usuario usuarioVotante = new Usuario(
                                rst.getInt("usuario_id"),
                                rst.getString("nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        usuarioVotante.setReputacao(rst.getInt("reputacao"));

                        Voto voto = new Voto(
                                rst.getInt("id"),
                                EnumVoto.valueOf(rst.getString("tipo")),
                                usuarioVotante,
                                rst.getInt("objeto_avaliado_id"),
                                rst.getString("tipo_objeto_avaliado")
                        );
                        voto.setDataVoto(rst.getObject("data_voto", LocalDateTime.class));
                        voto.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));
                        votos.add(voto);
                    }
                }
            }
            return votos;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar votos por explicação: " + e.getMessage(), e);
        }
    }
}
