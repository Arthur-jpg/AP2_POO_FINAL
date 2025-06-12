package bd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Classe responsável por criar conexões com o banco de dados.
 * Implementa o padrão Singleton para garantir uma única instância da conexão.
 */
public class ConnectionFactory {

    private static Connection connection;

    private ConnectionFactory() {
        // Construtor privado para evitar instanciação
    }

    /**
     * Recupera uma conexão com o banco de dados.
     * Se a conexão já existe e está válida, retorna a mesma.
     * Caso contrário, cria uma nova conexão.
     * @return Uma conexão com o banco de dados
     */
    public static Connection getConnection() {
        if (connection == null) {
            try {
                String sgbd = "mysql";
                String endereco = "localhost";
                String bd = "sistema_girias";
                String usuario = "root";
                String senha = "admin"; // Altere conforme necessário

                connection = DriverManager.getConnection(
                        "jdbc:" + sgbd + "://" + endereco + "/" + bd, usuario, senha);

                // Configura a conexão para não fazer autocommit
                connection.setAutoCommit(false);

                return connection;
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao conectar ao banco de dados: " + e.getMessage(), e);
            }
        }
        return connection;
    }

    /**
     * Fecha a conexão com o banco de dados, se estiver aberta.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao fechar conexão com o banco de dados: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Confirma as transações realizadas na conexão.
     */
    public static void commit() {
        if (connection != null) {
            try {
                connection.commit();
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao realizar commit: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Reverte as transações realizadas na conexão.
     */
    public static void rollback() {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao realizar rollback: " + e.getMessage(), e);
            }
        }
    }
}

