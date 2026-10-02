# CIADI+ — Auditoria do APK 2.0.1 (2026-10-02)

## Evidência analisada

Foi analisado o APK `CIADI-plus-debug.apk` disponibilizado no artefacto local da build 2.0.1. Esta análise é de binário (APK), não substitui a inspeção do código-fonte Gradle.

## Componentes Android identificados

O binário contém referências a:

- `com.example.MainActivity`
- `com.example.core.session.SessionManager`
- `com.example.data.remote.client.SupabaseClientFactory`
- `com.example.data.repository.SupabaseModulesRepositoryImpl`
- `com.example.ui.screens.virtualclinic.SalaVideoScreen`
- `com.example.ui.screens.documents.DocumentsModuleScreen`
- `com.example.ui.screens.documents.DocumentDetailsScreen`

## Integração Supabase

Foram encontrados no binário:

- projeto Supabase: `egpkkttbcaukqnnxyhjv.supabase.co`
- Edge Function: `functions/v1/ciadi-video-token`
- componentes de sessão/autenticação
- repositório de módulos Supabase

## Sala Virtual

O APK contém uma implementação nativa `SalaVideoScreen` e referências a:

- WebView
- `livekit.cloud`
- preparação/encerramento de sala no repositório Supabase
- função `ciadi-video-token`
- controlos de microfone, vídeo, câmara frontal e altifalante
- chat com gravação no Supabase

Isto indica que a versão compilada já contém uma camada Android para a sala. Em paralelo, o ecossistema CIADI mantém a sala web como interface clínica.

## Ponto de atenção

A presença de `SalaVideoScreen` no APK significa que não devemos assumir que a única implementação da sala é o HTML. Antes de substituir a arquitetura, é necessário comparar o código-fonte da versão Android com `index_sala_ciadi_internacional.html`/sala web e decidir se:

1. a tela Android é apenas um invólucro/ponte;
2. a tela Android implementa parte da sala diretamente;
3. existe duplicação de lógica entre Android e web.

## Próxima verificação obrigatória

Para uma auditoria de código real, o pacote-fonte `ciadi+ (1).zip` deve estar disponível, porque o APK não permite validar com segurança:

- código Kotlin/Gradle original;
- AndroidManifest.xml em forma legível;
- configuração exata do WebView;
- interceptores HTTP;
- política de URLs permitidas;
- tratamento de sessão;
- regras de autorização antes do token LiveKit;
- testes unitários;
- configuração de build.

## Critério de segurança

Não foi considerado suficiente encontrar a URL da Edge Function no APK. A validação deve ocorrer no backend com a identidade da sessão autenticada e autorização do agendamento. Segredos de servidor e credenciais privilegiadas não devem estar no APK.

## Estado desta auditoria

Auditoria binária preliminar. Não é uma declaração de que a implementação está completa ou segura. O próximo passo é confrontar estes achados com o código-fonte Android e com a sala web.
