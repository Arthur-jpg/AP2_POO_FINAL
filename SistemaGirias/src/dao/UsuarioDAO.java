
package dao;

import modelo.Giria;
import modelo.Usuario;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class UsuarioDAO implements BaseDAO {

    private Connection connection;

    public UsuarioDAO(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void salvar(Object objeto) {
        if (!(objeto instanceof Usuario)) {
            throw new IllegalArgumentException("Objeto deve ser do tipo Usuario.");
        }

        Usuario usuario = (Usuario) objeto;

        try {
            // Verifica se já existe um usuário com o mesmo email
            String checkSql = "SELECT id FROM usuario WHERE email = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setString(1, usuario.getEmail());
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    // Se já existe, atualiza os dados do usuário com o ID existente
                    int existingId = rs.getInt("id");
                    usuario.setId(existingId);
                    atualizar(usuario);
                    return;
                }
            }

            // Verifica se o usuário já existe para decidir entre INSERT e UPDATE
            if (usuario.getId() == 0) { // Considera que ID 0 indica novo objeto
                String sql = "INSERT INTO usuario (nome, email, senha, reputacao, data_criacao) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstm.setString(1, usuario.getNome());
                    pstm.setString(2, usuario.getEmail());
                    pstm.setString(3, usuario.getSenha());
                    pstm.setInt(4, usuario.getReputacao());
                    pstm.setObject(5, LocalDateTime.now()); // Data de criação no momento do salvamento

                    pstm.execute();

                    try (ResultSet rst = pstm.getGeneratedKeys()) {
                        if (rst.next()) {
                            usuario.setId(rst.getInt(1)); // Define o ID gerado pelo banco
                        }
                    }
                }
            } else { // Se já tem ID, tenta atualizar
                atualizar(usuario); // Chama o método de atualização
            }

            // **IMPORTANTE:**
            // Para as listas (giriasPropostas, explicacoesPropostas, votosRealizados),
            // a persistência de "relacionamentos" (1:N) é geralmente feita salvando
            // os objetos relacionados separadamente (com a FK apontando para este Usuario).
            // A abordagem Lazy/Eager Loading no listarTodos também lida com isso.
            // Para o salvar inicial, não vamos salvar as listas aqui para evitar recursão ou complexidade excessiva
            // na primeira versão, seguindo o padrão da PessoaDAO que só salva os telefones após a pessoa.

            // No entanto, se você quiser persistir as gírias/explicações/votos AQUI no salvar do usuário,
            // teria que iterar sobre as listas e chamar os DAOs correspondentes, assim como feito na PessoaDAO com Telefone.
            // Exemplo (se for adicionar depois):
            // GiriaDAO giriaDAO = new GiriaDAO(connection);
            // for (Giria giriaProposta : usuario.getGiriasPropostas()) {
            //     giriaDAO.salvar(giriaProposta); // Certifique-se que o salvar da Giria lida com a FK do usuário
            // }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public Object buscarPorId(int id) {
        Usuario usuario = null;
        try {
            String sql = "SELECT id, nome, email, senha, reputacao, data_criacao FROM usuario WHERE id = ?";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, id);
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    if (rst.next()) {
                        usuario = new Usuario(
                                rst.getInt("id"),
                                rst.getString("nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        usuario.setReputacao(rst.getInt("reputacao"));
                        // data_criacao não tem setter direto no construtor Usuario, mas pode ser setado via super.setDataCriacao() ou em um construtor mais completo
                        // usuario.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));
                    }
                }
            }
            return usuario;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuário por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public ArrayList<Object> listarTodosLazyLoading() {
        ArrayList<Object> usuarios = new ArrayList<>();
        try {
            String sql = "SELECT id, nome, email, senha, reputacao, data_criacao FROM usuario";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        Usuario usuario = new Usuario(
                                rst.getInt("id"),
                                rst.getString("nome"),
                                rst.getString("email"),
                                rst.getString("senha")
                        );
                        usuario.setReputacao(rst.getInt("reputacao"));
                        // usuario.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));
                        usuarios.add(usuario);
                    }
                }
            }
            return usuarios;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar todos os usuários (Lazy Loading): " + e.getMessage(), e);
        }
    }

    @Override
    public ArrayList<Object> listarTodosEagerLoading() {
        // Implementação de Eager Loading para Usuario:
        // Isso significaria carregar as Girias, Explicacoes e Votos relacionados a cada usuário
        // em uma única consulta ou com consultas adicionais em loop.
        // Seguiremos o padrão da PessoaDAO, usando LEFT JOIN para carregar as gírias propostas.
        // Para Votos e Explicações seria similar, ou consultas separadas.

        ArrayList<Object> usuarios = new ArrayList<>();
        Usuario ultimoUsuario = null;

        try {
            // Consulta para carregar Usuario e suas Girias propostas
            String sql = "SELECT " +
                    "u.id AS u_id, u.nome, u.email, u.senha, u.reputacao, u.data_criacao, " +
                    "g.id AS g_id, g.termo, g.data_cadastro, g.aprovada " + // Faltam categorias e regioes aqui
                    "FROM usuario AS u " +
                    "LEFT JOIN giria AS g ON u.id = g.usuario_propositor_id " +
                    "ORDER BY u.id, g.id"; // Garante que as linhas do mesmo usuário venham juntas

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.execute();

                try (ResultSet rst = pstm.getResultSet()) {
                    while (rst.next()) {
                        int currentUserId = rst.getInt("u_id");

                        // Se for um novo usuário ou o primeiro
                        if (ultimoUsuario == null || ultimoUsuario.getId() != currentUserId) {
                            ultimoUsuario = new Usuario(
                                    currentUserId,
                                    rst.getString("nome"),
                                    rst.getString("email"),
                                    rst.getString("senha")
                            );
                            ultimoUsuario.setReputacao(rst.getInt("reputacao"));
                            // ultimoUsuario.setDataCriacao(rst.getObject("data_criacao", LocalDateTime.class));
                            usuarios.add(ultimoUsuario);
                        }

                        // Adiciona a gíria se ela existir (g_id não for 0)
                        if (rst.getInt("g_id") != 0) {
                            Giria giria = new Giria(
                                    rst.getInt("g_id"),
                                    rst.getString("termo"),
                                    // Definicao, ExemploUso não estão na tabela giria do SQL (precisariam ser adicionados ou carregados separadamente)
                                    rst.getObject("data_cadastro", LocalDateTime.class),
                                    rst.getBoolean("aprovada"),
                                    ultimoUsuario // Referência ao usuário já carregado
                            );
                            // Aqui você precisaria carregar as categorias, regiões, explicações e votos da gíria
                            // Isso tornaria o Eager Loading mais complexo e pode exigir DAOs aninhados ou consultas adicionais.
                            // Por simplicidade neste exemplo, apenas os atributos básicos da Giria estão sendo carregados.
                            ultimoUsuario.adicionarGiriaProposta(giria);
                        }
                    }
                }
            }
            return usuarios;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar todos os usuários (Eager Loading): " + e.getMessage(), e);
        }
    }

    @Override
    public void atualizar(Object objeto) {
        if (!(objeto instanceof Usuario)) {
            throw new IllegalArgumentException("Objeto deve ser do tipo Usuario.");
        }

        Usuario usuario = (Usuario) objeto;

        String sql = "UPDATE usuario SET nome = ?, email = ?, senha = ?, reputacao = ? WHERE id = ?";

        try (PreparedStatement pstm = connection.prepareStatement(sql)) {
            pstm.setString(1, usuario.getNome());
            pstm.setString(2, usuario.getEmail());
            pstm.setString(3, usuario.getSenha());
            pstm.setInt(4, usuario.getReputacao());
            pstm.setInt(5, usuario.getId());

            int linhasAfetadas = pstm.executeUpdate();

            if (linhasAfetadas == 0) {
                throw new SQLException("Falha ao atualizar usuário: nenhuma linha foi afetada.");
            }

            // **IMPORTANTE:**
            // A atualização das listas (giriasPropostas, explicacoesPropostas, votosRealizados)
            // é um ponto complexo no DAO e geralmente envolve:
            // 1. Deletar os antigos registros relacionados.
            // 2. Inserir os novos registros relacionados.
            // Ou, para cada item na lista: verificar se existe, atualizar se sim, inserir se não, deletar se não está mais na lista.
            // Para simplicidade, e seguindo o exemplo da PessoaDAO que só faz update nos Telefones existentes,
            // não vou implementar a atualização completa das listas aqui.
            // Isso seria feito pelos DAOs de Giria, Explicacao e Voto, individualmente.
            // Exemplo (se for adicionar depois, para as gírias propostas):
            // GiriaDAO giriaDAO = new GiriaDAO(connection);
            // for (Giria giriaProposta : usuario.getGiriasPropostas()) {
            //     giriaDAO.atualizar(giriaProposta);
            // }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao atualizar usuário: " + e.getMessage());
        }
    }

    @Override
    public void excluir(int id) {
        try {
            // Ao excluir um usuário, é crucial decidir o que fazer com as gírias, explicações e votos
            // que ele propôs ou realizou. Pode ser:
            // 1. DELETE CASCADE no banco de dados (configurado na FK).
            // 2. SET NULL (a FK do usuário nas tabelas de gíria/explicação/voto se torna NULL).
            // 3. Deletar manualmente os registros relacionados primeiro.
            // Para este exemplo, assumimos que o banco de dados tem CASCADE DELETE ou que os objetos
            // relacionados podem existir sem o usuário propositor (FK NULL).
            // Se for DELETE CASCADE, o TelefoneDAO da PessoaDAO mostra que é melhor ter um DAO separado
            // para gerenciar as exclusões de dependências, ou usar o banco de dados para isso.

            String sql = "DELETE FROM usuario WHERE id = ?";

            try (PreparedStatement pstm = connection.prepareStatement(sql)) {
                pstm.setInt(1, id);

                int linhasAfetadas = pstm.executeUpdate();

                if (linhasAfetadas == 0) {
                    throw new SQLException("Falha ao deletar usuário: nenhuma linha foi afetada.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir usuário: " + e.getMessage(), e);
        }
    }
}