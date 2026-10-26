# CIADI+ — Homologação ponta a ponta do SOS

## Objetivo
Confirmar, em ambiente de homologação, que um assistido de teste já existente no Dashboard é carregado apenas para utilizadores autorizados e que o alerta SOS aparece no Dashboard com a associação correta.

## Pré-condições
- Usar conta de teste autorizada e assistido fictício/de teste.
- Confirmar sessão autenticada no CIADI+ e vínculo ativo do responsável ao assistido.
- Não alterar nem eliminar dados de produção.
- Conceder autorização explícita para localização antes de validar o envio de coordenadas.

## Testes e critérios de aceitação
1. **Carregamento de assistidos:** o assistido de teste aparece na lista do CIADI+ quando o vínculo está ativo; vínculos inativos não aparecem.
2. **Seleção inequívoca:** se houver um único assistido autorizado, o SOS pode associá-lo automaticamente; se houver mais de um, exigir seleção explícita.
3. **Identificadores:** o alerta usa o ID real do assistido selecionado; nunca usar o ID do utilizador como substituto do ID do paciente.
4. **Sem assistido associado:** permitir o alerta do utilizador apenas se o backend aceitar essa modalidade; guardar o paciente como nulo, sem inventar uma associação.
5. **Privacidade de localização:** sem autorização, não enviar coordenadas; com autorização, validar que latitude/longitude chegam apenas aos destinatários autorizados.
6. **Dashboard:** o alerta aparece para perfis administrativos autorizados, com categoria, nível, data/hora e estado. O vínculo ao assistido deve corresponder ao ID enviado.
7. **Tempo real:** confirmar atualização da lista sem recarregar a página, via Supabase Realtime, quando a configuração de publicação estiver aplicada.
8. **Isolamento de acesso:** confirmar que um perfil não autorizado não consegue consultar alertas ou localização de outros utilizadores.
9. **Registo de evidência:** guardar resultado de cada teste, hora, versão do APK, commit testado e eventuais erros, sem expor tokens nem dados pessoais desnecessários.

## Verificação obrigatória do APK
- O artefacto deve ser gerado pelo workflow Build CIADI+ depois do commit que contém as correções.
- Confirmar que o head_sha da execução corresponde ao commit da branch feat/ciadi-plus-sos-whatsapp-homologacao que está a ser validado.
- Confirmar assinatura APK, SHA-256 e resultado da compilação antes de partilhar o ficheiro.
- Um APK produzido por um commit anterior não deve ser apresentado como versão atualizada.

## Estado atual
- Correções de código submetidas na branch feat/ciadi-plus-sos-whatsapp-homologacao.
- Teste automatizado adicionado para impedir a substituição do ID do assistido pelo ID do utilizador.
- A execução de CI para o commit atual, instalação do APK recente e teste real com Dashboard ainda precisam de confirmação.
- WhatsApp/e-mail não são considerados entregues até haver configuração do canal e evidência de envio/receção.

## Bloqueio de aprovação
Não aprovar para produção enquanto compilação, testes automatizados, vínculo do assistido, autorização de localização, RLS e receção no Dashboard não tiverem evidência verificável.
