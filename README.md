# Sify Comércio para Android

O aplicativo abre https://pdv.sifysistemas.com/ em uma WebView dedicada. O IndexedDB, os cookies e o cache do service worker ficam no perfil do aplicativo. A API usada pelo PDV publicado é https://api.sifysistemas.com/.

Use Android 8 ou posterior com Android System WebView atualizado. O primeiro acesso requer internet. Depois que o service worker concluir o cache, teste a reabertura em modo avião e a persistência de dados após fechar o app. Limpar os dados do aplicativo ou desinstalá-lo apaga o banco local. O banco do Chrome fica em outro perfil. Atualizações precisam manter a origem HTTPS, o ID do pacote e a mesma chave de assinatura.

## Cloudflare

O APK usa HTTPS e não contém credenciais da Cloudflare. O Tunnel encaminha o tráfego e a autenticação depende das políticas instaladas no Access ou no servidor. Para um header compartilhado exigido pelo backend, mantenha o valor em um serviço intermediário confiável e injete-o somente após validar a sessão ou outra credencial do cliente. Preserve o Bearer que o PDV já usa. Um header inserido em toda requisição pública ao túnel não comprova que ela veio do APK.

A WebView limita a navegação principal ao domínio do PDV. Uma política de Access que exija redirecionamento para outro domínio precisa de uma integração de autenticação compatível com esse fluxo.

## Build de teste

Com JDK 17, Gradle 8.10.2, Android SDK 35 e Build Tools 35.0.0, execute `gradle assembleDebug`. O APK fica em `app/build/outputs/apk/debug/app-debug.apk`. O GitHub Actions também compila e publica esse APK como artifact do workflow. Use a versão debug para homologação e uma chave de release persistente para distribuição e atualizações.

## Escopo da primeira versão

A WebView habilita o seletor de arquivos para importação de backup. A exportação atual do PWA usa links `blob:`, que exigem integração adicional com o Android. Impressão em janela separada, leitor com câmera e a ponte Electron para Pix também exigem integração própria. Valide esses fluxos antes de usar o app em produção.
