import modelo.*;
import dao.*;
import bd.ConnectionFactory;

import java.sql.Connection;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        System.out.println("Sistema de Gírias - Persistência de dados");

        // Obtém uma conexão com o banco de dados
        Connection connection = null;

        try {
            // Usa o método static do ConnectionFactory para obter uma conexão
            connection = ConnectionFactory.getConnection();
            System.out.println("Conexão com o banco de dados estabelecida com sucesso!");

            // Criando os DAOs
            UsuarioDAO usuarioDAO = new UsuarioDAO(connection);
            GiriaDAO giriaDAO = new GiriaDAO(connection);
            ExplicacaoDAO explicacaoDAO = new ExplicacaoDAO(connection);
            VotoDAO votoDAO = new VotoDAO(connection);

            System.out.println("\n===== CADASTRANDO USUÁRIOS =====");
            // Criando usuários
            Usuario usuario1 = new Usuario(0, "João Silva", "joao@email.com", "senha123");
            Usuario usuario2 = new Usuario(0, "Maria Santos", "maria@email.com", "senha456");
            Usuario usuario3 = new Usuario(0, "Arthur", "arthur@email.com", "senha456");
            Administrador admin = new Administrador(0, "Admin Sistema", "admin@sistema.com", "admin123");

            Usuario usuarioSEMID = new Usuario("YYYYYYY", "YYYYYY@gmail.com", "senhaYYYYY");

            Usuario usuario4 = new Usuario(0, "Carlos", "carlos@", "senha789");
            Usuario userSemIDDDDD = new Usuario("Usuário Sem ID", "userrrr@", "senhaSemID");

            Usuario talita = new Usuario("Talita", "talita@gmail.com", "senhaTalita");

            Administrador admin2 = new Administrador(0, "Admin23", "admin23@gmail.com", "senhaAdmin23");

            Usuario arthur = new Usuario(0, "Arthur", "arthgur@ason", "senhaArthur");

            // Persistindo os usuários no banco
            usuarioDAO.salvar(usuario1);
            System.out.println("Usuário 1 salvo com sucesso! ID: " + usuario1.getId());

            usuarioDAO.salvar(usuario2);
            System.out.println("Usuário 2 salvo com sucesso! ID: " + usuario2.getId());

            usuarioDAO.salvar(usuario3);
            System.out.println("Usuário 3 salvo com sucesso! ID: " + usuario3.getId());

            usuarioDAO.salvar(admin);
            System.out.println("Administrador salvo com sucesso! ID: " + admin.getId());

            usuarioDAO.salvar(usuario4);
            System.out.println("Carlos salvo com sucesso! ID: " + usuario4.getId());

            usuarioDAO.salvar(userSemIDDDDD);
            System.out.println("USersemid salvo com sucesso! ID: " + userSemIDDDDD.getId());

            usuarioDAO.salvar(usuarioSEMID);
            System.out.println("usuarioSEMID salvo com sucesso! ID: " + usuarioSEMID.getId());

            usuarioDAO.salvar(talita);
            System.out.println("Talita salva com sucesso! ID: " + talita.getId());

            usuarioDAO.salvar(admin2);
            System.out.println("Administrador 2 salvo com sucesso! ID: " + admin2.getId());

            usuarioDAO.salvar(arthur);
            System.out.println("Arthur salvo com sucesso! ID: " + arthur.getId());







            System.out.println("\n===== CADASTRANDO GÍRIAS =====");
            // Criando gírias
            Giria giria1 = new Giria(0, "Bora", usuario1);
            giria1.adicionarCategoria("Informal");
            giria1.adicionarCategoria("Jovem");
            giria1.adicionarRegiao("Rio de Janeiro");
            giriaDAO.salvar(giria1);
            System.out.println("Gíria 'Bora' salva com sucesso! ID: " + giria1.getId());

            Giria giria2 = new Giria(0, "Arreda", usuario2);
            giria2.adicionarCategoria("Regional");
            giria2.adicionarRegiao("Nordeste");
            giriaDAO.salvar(giria2);
            System.out.println("Gíria 'Arreda' salva com sucesso! ID: " + giria2.getId());

            Giria ainda = new Giria(0, "Ainda", talita);
            ainda.adicionarRegiao("Rio de Janeiro");
            ainda.adicionarCategoria("Informal");
            giriaDAO.salvar(ainda);
            System.out.println("Gíria 'Ainda' salva com sucesso! ID: " + ainda.getId());







            System.out.println("\n===== CADASTRANDO EXPLICAÇÕES =====");
            Explicacao explicacao1 = new Explicacao(0, "Expressão usada para convidar alguém a fazer algo",
                    "Bora pra praia?", usuario1, giria1);
            giria1.adicionarExplicacao(explicacao1);
            explicacaoDAO.salvar(explicacao1);
            System.out.println("Explicação para 'Bora' salva com sucesso! ID: " + explicacao1.getId());

            Explicacao explicacao2 = new Explicacao(0, "Expressão usada para afastar alguém ou algo",
                    "Arreda aí!", usuario2, giria2);
            giria2.adicionarExplicacao(explicacao2);
            explicacaoDAO.salvar(explicacao2);

            Explicacao explicacaoArreda = new Explicacao(0, "Teste explicao", "Exemplo de uso", usuario2, giria2);
            System.out.println("Explicação para 'Arreda' salva com sucesso! ID: " + explicacao2.getId());
            giria2.adicionarExplicacao(explicacaoArreda);
            explicacaoDAO.salvar(explicacaoArreda);

            Explicacao explicacaoAinda = new Explicacao(0, "Expressão usada paara confirmar ou afirmar algo", "Ainda pai", talita, ainda);
            ainda.adicionarExplicacao(explicacaoAinda);
            explicacaoDAO.salvar(explicacaoAinda);
            System.out.println("Explicação para 'Ainda' salva com sucesso! ID: " + explicacaoAinda.getId());




            System.out.println("\n===== APROVANDO CONTEÚDO =====");
            // Administrador aprova as gírias e explicações
            giria1.aprovar();
            giriaDAO.atualizar(giria1);
            System.out.println("Gíria 'Bora' aprovada com sucesso!");

            giria2.aprovar();
            giriaDAO.atualizar(giria2);
            System.out.println("Gíria 'Arreda' aprovada com sucesso!");

            explicacao1.aprovar();
            explicacaoDAO.atualizar(explicacao1);
            System.out.println("Explicação para 'Bora' aprovada com sucesso!");

            explicacao2.desaprovar();
            explicacaoDAO.atualizar(explicacao2);
            System.out.println("Explicação para 'Arreda' desaprovada com sucesso!");

            ainda.aprovar();
            giriaDAO.atualizar(ainda);
            System.out.println("Gíria 'Ainda' aprovada com sucesso!");

            explicacaoAinda.aprovar();
            explicacaoDAO.atualizar(explicacaoAinda);
            System.out.println("Explicação para 'Ainda' aprovada com sucesso!");





            System.out.println("\n===== ADICIONANDO VOTOS =====");
            // Criando votos
            Voto voto1 = new Voto(0, EnumVoto.POSITIVO, usuario1, giria2.getId(), "GIRIA");
            giria2.adicionarVoto(voto1);
            votoDAO.salvar(voto1);
            System.out.println("Voto positivo para 'Arreda' registrado com sucesso! ID: " + voto1.getId());

            Voto voto2 = new Voto(0, EnumVoto.POSITIVO, usuario2, giria1.getId(), "GIRIA");
            giria1.adicionarVoto(voto2);
            votoDAO.salvar(voto2);
            System.out.println("Voto positivo para 'Bora' registrado com sucesso! ID: " + voto2.getId());

            Voto voto3 = new Voto(0, EnumVoto.POSITIVO, admin, explicacao1.getId(), "EXPLICACAO");
            explicacao1.adicionarVoto(voto3);
            votoDAO.salvar(voto3);
            System.out.println("Voto positivo para explicação de 'Bora' registrado com sucesso! ID: " + voto3.getId());

            Voto votoAinda = new Voto(0, EnumVoto.POSITIVO, usuario2, ainda.getId(), "GIRIA");
            ainda.adicionarVoto(votoAinda);
            votoDAO.salvar(votoAinda);
            System.out.println("Voto positivo para 'Ainda' registrado com sucesso! ID: " + votoAinda.getId());

            Voto votoExplicacaoAinda = new Voto(0, EnumVoto.POSITIVO, usuario2, explicacaoAinda.getId(), "EXPLICACAO");
            explicacaoAinda.adicionarVoto(votoExplicacaoAinda);
            votoDAO.salvar(votoExplicacaoAinda);
            System.out.println("Voto positivo para explicação de 'Ainda' registrado com sucesso! ID: " + votoExplicacaoAinda.getId());



            // Salvando novas atualizacoes em todos os usuarios
            usuarioDAO.atualizar(usuario1);
            usuarioDAO.atualizar(usuario2);
            usuarioDAO.atualizar(talita);

            giriaDAO.atualizar(giria1);
            giriaDAO.atualizar(giria2);
            giriaDAO.atualizar(ainda);
            explicacaoDAO.atualizar(explicacao1);
            explicacaoDAO.atualizar(explicacao2);
            explicacaoDAO.atualizar(explicacaoAinda);

            giriaDAO.excluir(ainda.getId());







            System.out.println("\n===== LISTANDO TODOS OS USUÁRIOS =====");
            // Listando todos os usuários
            ArrayList<Object> usuarios = usuarioDAO.listarTodosLazyLoading();
            for (Object obj : usuarios) {
                Usuario usuario = (Usuario) obj;
                System.out.println("ID: " + usuario.getId() + " | Nome: " + usuario.getNome() +
                        " | Email: " + usuario.getEmail() + " | Reputação: " + usuario.getReputacao());
            }




            System.out.println("\n===== LISTANDO TODAS AS GÍRIAS =====");
            // Listando todas as gírias
            ArrayList<Object> girias = giriaDAO.listarTodosEagerLoading();
            ArrayList<Object> explicacoes = explicacaoDAO.listarTodosEagerLoading();
            for (Object obj : girias) {
                Giria giria = (Giria) obj;
                System.out.println("ID: " + giria.getId() + " | Termo: " + giria.getTermo() +
                        " | Aprovada: " + (giria.isAprovada() ? "Sim" : "Não") +
                        " | Propositor: " + giria.getUsuarioPropositor().getNome() + "| Pontuação: " + giria.getPontuacaoVotos());

                System.out.print("  Categorias: ");
                for (String categoria : giria.getCategorias()) {
                    System.out.print(categoria + ", ");
                }
                System.out.println();

                System.out.print("  Regiões: ");
                for (String regiao : giria.getRegioes()) {
                    System.out.print(regiao + ", ");
                }
                System.out.println();

                System.out.println("  Explicações:");
                for (Object x : explicacoes) {
                    Explicacao explicacao = (Explicacao) x;
                    if (explicacao.getGiriaAssociada().getId() == giria.getId()) {
                        System.out.println("    ID: " + explicacao.getId() + " | Definição: " + explicacao.getDefinicao() + " | Exemplo: "+  explicacao.getExemploUso() +
                                " | Aprovada: " + (explicacao.isAprovada() ? "Sim" : "Não") +
                                " | Propositor: " + explicacao.getUsuarioPropositor().getNome() + " | Pontuação: " + explicacao.getPontuacaoVotos());
                    }
                }
            }



            // Confirma todas as operações
            connection.commit();
            System.out.println("\nTodas as operações foram confirmadas com sucesso!");

        } catch (Exception e) {
            if (connection != null) {
                try {
                    // Desfaz todas as operações em caso de erro
                    connection.rollback();
                    System.err.println("Transação desfeita devido a um erro.");
                } catch (Exception rollbackEx) {
                    System.err.println("Erro ao desfazer transação: " + rollbackEx.getMessage());
                }
            }
            System.err.println("Erro na execução do programa: " + e.getMessage());
            e.printStackTrace();
        } finally {
            ConnectionFactory.closeConnection();
            System.out.println("Conexão com o banco de dados encerrada.");
        }
    }
}
