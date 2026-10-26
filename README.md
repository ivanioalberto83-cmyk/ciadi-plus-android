# CIADI+

Aplicação Android do ecossistema CIADI, integrada ao projeto Supabase partilhado pelo site institucional e pelo Dashboard Windows.

## Integração configurada

- Projeto Supabase: `egpkkttbcaukqnnxyhjv` (URL configurada em `android-source/src/main/java/com/example/core/config/SupabaseConfig.kt`).
- Autenticação: Supabase Auth, perfil CIADI e sessão gerida pela aplicação.
- Dados: PostgREST com JWT do utilizador, sujeitos às políticas RLS do banco.
- SOS: o repositório contém a chamada à RPC `ciadi_criar_alerta_sos`, que regista o alerta e pode gerar notificações institucionais; o Dashboard lê os alertas autorizados em `ciadi_sos_alertas`.
- Chat e notificações: repositórios CIADI Chat/Notificações e suporte Realtime estão presentes no código.
- Formulários e acompanhamento A.T.: os repositórios e modelos usam tabelas CIADI dedicadas.

## Relação com o site e Dashboard

- Site: `ivanioalberto83-cmyk/CIADI-SITE-OFICIAL`
- Dashboard: `ivanioalberto83-cmyk/CIADI-`
- Site e Dashboard partilham o mesmo projeto Supabase. Os pedidos de triagem do site são encaminhados para a fila interna `ciadi_triagens` através da RPC validada `ciadi_registrar_triagem_publica`.
- A Central SOS do Dashboard lê a tabela `ciadi_sos_alertas`; a publicação Realtime e a política de leitura para gestão foram verificadas no projeto.

A configuração comum não prova, por si só, que todos os percursos funcionem ponta a ponta. A homologação tem de usar contas e pacientes fictícios, incluindo testes de acesso negado para utilizadores sem vínculo.

## Build automatizado

GitHub Actions: `.github/workflows/ciadi-plus-apk.yml`.

O workflow seleciona o projeto Android em `android-source`, audita referências a formulários clínicos e segredos, executa verificações de build e publica o APK debug como artefacto quando a execução termina com sucesso.

## Segurança e produção

- Apenas chave publicável no cliente Android; nunca incluir `service_role`, segredos de Edge Functions ou credenciais administrativas.
- A autorização final é feita por Auth, RLS e RPCs validadas no servidor, não apenas pela interface.
- Não testar com dados reais de crianças.
- O APK debug é para homologação; não equivale a uma release assinada de produção.
- Não declarar conformidade APD/AGT ou integração completa sem validação formal.

## Artefactos anteriores

Builds anteriores não garantem que o APK atual contenha os últimos commits. Consultar a execução mais recente do workflow antes de distribuir.
