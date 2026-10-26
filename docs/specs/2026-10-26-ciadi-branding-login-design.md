# CIADI+ Android — Design: identidade oficial e entrada por perfil

Data: 2026-10-26  
Repositório: `ivanioalberto83-cmyk/ciadi-plus-android`  
Branch: `fix/ciadi-official-branding-login-2026-10-26`  
Base preservada: `main`

## Objetivo
Atualizar a experiência de entrada e a identidade visual da aplicação Android CIADI+, usando apenas o logótipo institucional oficial verificável e mantendo a autorização real no Supabase.

## Decisões de produto
- O ecrã inicial mostra o logótipo institucional, o nome CIADI+ e um único botão «Entrar na minha conta».
- Ao tocar no botão, a pessoa escolhe uma via de entrada: «Administrador», «Profissional / A.T.» ou «Família / Responsável».
- A escolha de via é apenas orientação de interface; não concede privilégios. O perfil efetivo é determinado pela sessão autenticada e pelo perfil autorizado no backend/RLS.
- Não mostrar contas de teste, e-mails de utilizadores nem credenciais na interface.
- O logótipo deve ser o original oficial, sem redesenho. A imagem de cartão de visita publicada no site só pode ser usada como fonte de extração se o símbolo for claramente identificável e o recorte for validado; não usar o ícone genérico atualmente encontrado no repositório como se fosse oficial.
- Nos formulários clínicos, apresentar o logótipo oficial, «CIADI+» e o título do formulário no cabeçalho; documentos imprimíveis/exportados devem manter cabeçalho institucional legível.
- Não alterar o esquema do Supabase, dados, políticas RLS, nem permissões de backend nesta alteração visual.

## Limites e segurança
- Não mudar `main`; alterações apenas nesta branch.
- Não adicionar serviços pagos nem segredos.
- Não tratar a seleção visual de perfil como mecanismo de autorização.
- Não afirmar que o APK está pronto até o build e a assinatura serem verificados.
- A publicação no site institucional apresentou conteúdo suspeito em uma verificação anterior; validar o ficheiro de imagem e evitar executar ou confiar em conteúdo ativo do site.

## Entregáveis e verificação
1. Asset oficial de logótipo identificado, guardado localmente no projeto e verificado visualmente; não substituir por arte inventada.
2. Entrada em duas etapas com botão inicial e seletor de perfil, mantendo o fluxo existente de autenticação Supabase.
3. Cabeçalho CIADI+ nos formulários clínicos e documentos impressos/exportados aplicáveis.
4. Ícones Android launcher atualizados com o símbolo oficial e variantes de densidade.
5. Compilação `assembleDebug`, testes disponíveis, verificação de assinatura e SHA-256 do APK.
6. Revisão da diff para confirmar que `main`, backend e permissões não foram alterados.

## Critérios de aceitação
- A primeira vista tem um único botão de entrada.
- Os três caminhos aparecem após o botão; a seleção não altera o papel real do utilizador.
- Login, recuperação de palavra-passe e verificação de perfil continuam funcionais.
- O logótipo é o oficial, não o ícone genérico atualmente presente no repositório.
- Os formulários continuam a carregar/sincronizar como antes e mostram identidade CIADI+.
- O APK só é partilhado depois de build, assinatura e hash confirmados.

## Dependência ainda por resolver
O ficheiro disponível no repositório com nome `ic_ciadi_logo_1790332105105.jpg` é um ícone genérico de saúde e não corresponde ao logótipo institucional pretendido. O download direto da imagem publicada no site falhou neste ambiente. Antes de alterar os assets binários, é necessário obter uma cópia original acessível do ficheiro oficial ou confirmar um caminho de origem oficial alternativo.