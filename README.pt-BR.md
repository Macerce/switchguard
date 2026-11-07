[English](README.md) · [Türkçe](README.tr.md) · [Deutsch](README.de.md) · [Español](README.es.md) · **Português (Brasil)** · [Français](README.fr.md) · [Italiano](README.it.md) · [Nederlands](README.nl.md) · [Polski](README.pl.md) · [Русский](README.ru.md) · [Українська](README.uk.md) · [العربية](README.ar.md) · [हिन्दी](README.hi.md) · [Bahasa Indonesia](README.id.md) · [Tiếng Việt](README.vi.md) · [ไทย](README.th.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [日本語](README.ja.md) · [한국어](README.ko.md)

# SwitchGuard

O SwitchGuard monitora as tomadas e interruptores inteligentes das suas próprias contas **eWeLink / SONOFF** e **Tuya / Smart Life** e toca um alarme de verdade assim que algo muda — uma bomba que parou, um freezer desligado, um dispositivo que perdeu a conexão. Funciona em celulares e tablets Android, Android TV e relógios Wear OS, e também há um app para Windows.

<p>
  <img src="docs/screenshots/play/en/v215/01-devices.png" width="200" alt="Dispositivos">
  <img src="docs/screenshots/play/en/v215/02-energy.png" width="200" alt="Energia">
  <img src="docs/screenshots/play/en/v215/05-automations.png" width="200" alt="Automações">
  <img src="docs/screenshots/play/en/v215/08-alarm.png" width="200" alt="Alarme de dispositivo">
</p>

> **Idiomas:** o app para Windows está disponível em 20 idiomas desde a 0.13.0. No Android, a escolha de idioma no app (20 idiomas) chega com a **2.26**; a versão Android mais recente é a 2.25.0.

## Baixar

### Android (celular, tablet, TV)
- **Google Play**: https://play.google.com/store/apps/details?id=com.macerce.switchguard
- **APK**: [versão Android mais recente](https://github.com/Macerce/switchguard/releases/latest) — baixe `SwitchGuard-x.y.z.apk` e abra no celular. Se o SwitchGuard já estiver instalado pelo Google Play, desinstale-o primeiro (assinatura diferente). A versão do GitHub avisa quando sai uma mais nova.

### Windows 10 / 11
- **Instalador**: [versões para Windows](https://github.com/Macerce/switchguard/releases?q=Windows&expanded=true) — baixe `SwitchGuard-x.y.z-Kurulum.exe` da mais recente e execute. O app procura novas versões e se atualiza com um clique; as configurações são mantidas.

### Grátis e Pro
- Grátis: todos os recursos, para 1 dispositivo monitorado. Os dispositivos de demonstração permitem testar tudo sem hardware.
- Android: o Pro é uma compra única no Google Play (sem assinatura) e monitora todos os dispositivos; acompanha sua conta Google para um celular novo.
- Windows: uma chave de licença Pro (Configurações → cartão do plano) monitora todos os dispositivos. Para obter uma chave: **switchguardapp@gmail.com**

## Requisitos

- Android 8.0+ (API 26); Wear OS para o app do relógio
- eWeLink: um app de desenvolvedor eWeLink gratuito (App ID + App Secret) — o guia do app mostra passo a passo. A eWeLink permite uma sessão por conta e App ID, então cada celular, tablet, TV ou PC precisa da própria conta eWeLink com os dispositivos compartilhados ([dev.ewelink.cc](https://dev.ewelink.cc))
- Tuya / Smart Life: o Access ID e o Access Secret do seu próprio projeto gratuito no Tuya Cloud, vinculado à sua conta do app Smart Life / Tuya ([platform.tuya.com](https://platform.tuya.com))
- Windows 10 / 11 (64 bits) para o app do Windows

## Guias de configuração

- eWeLink (SONOFF): [English](docs/guide/ewelink.en.md) · [Türkçe](docs/guide/ewelink.tr.md)
- Tuya / Smart Life: [English](docs/guide/tuya.en.md) · [Türkçe](docs/guide/tuya.tr.md)

### Vídeos

- **New in 2.17 – alert categories, critical alerts, groups (EN):** https://youtube.com/shorts/iVM3zEiBt8A
- **2.17 yenilikleri – uyarı kategorileri, kritik uyarılar, gruplar (TR):** https://youtube.com/shorts/Qha83ziwylI
- New in 2.15 – smart alerts, scenes, energy reports (EN): https://youtube.com/shorts/bOtFBjHoefE
- 2.15 yenilikleri – akıllı uyarılar, sahneler, enerji raporu (TR): https://youtube.com/shorts/y3QRZvVoeK4
- New in 2.12 – a tour of the app (EN): https://youtube.com/shorts/jvm0hx0gATk
- 2.12 yenilikleri – uygulama turu (TR): https://youtube.com/shorts/2KeUgJrCCEU
- First setup in one minute / İlk kurulum (1 dk): https://youtube.com/shorts/jarBmMg2kYA
- **Getting eWeLink API keys / eWeLink API anahtarlarını alma (TR, sesli):** https://youtu.be/G5vQ_7CXTcM
- **Getting Tuya API keys / Tuya API anahtarlarını alma (TR, sesli):** https://youtu.be/8vPjJMoMaMU

## Recursos

### Monitoramento e alarme

- Atualizações ao vivo pelo WebSocket da eWeLink e pelo serviço de mensagens da Tuya, com ressincronização HTTP e verificações periódicas como reserva.
- O alarme toca até você desligá-lo — mesmo no modo silencioso — e abre em tela cheia sobre a tela de bloqueio; cada dispositivo pode ter seu próprio som.
- Regras por dispositivo: alarme, notificação ou nada quando um dispositivo desliga, liga, fica offline ou volta a ficar online.
- Alertas condicionais: toca quando um dispositivo fica ligado ou desligado por um tempo, ou sua potência passa de / cai abaixo de um limite (ex.: bomba abaixo de 50 W).
- Ignorar quedas breves, aviso quando o próprio monitoramento para (sem internet / nuvem), aviso alto quando a sessão eWeLink cai (ex.: mesma conta conectada em outro celular).
- Um cartão de prontidão mostra e corrige configurações do celular que podem impedir o alarme (restrição de bateria, permissão de tela cheia, Não perturbe, volume do alarme…).

### Dispositivos

- Blocos no estilo casa inteligente com botões rápidos, um botão por canal em dispositivos multicanal, estado e potência ao vivo.
- Página do dispositivo: regras, som de alarme próprio, categoria de alerta, histórico de energia e custo, timers eWeLink, sinal Wi-Fi e diagnóstico de quedas, consumo em espera e eventos recentes.
- **Organizar dispositivos**: Estrela = manter no topo · setas = mover · tamanho do cartão = pequeno ou grande · etiqueta = categoria. Mantenha um cartão pressionado para voltar aqui.

### Alertas

- Alertas de dispositivo e alertas condicionais em um só lugar, com filtro por categoria de alerta (Crítico / Importante / Normal).
- Configurações de alerta: som e volume do alarme, horário de silêncio, categorias de alerta, Telegram (“Avisar outra pessoa”) e alertas inteligentes — veja *Todas as configurações* abaixo.
- Alertas inteligentes: um único alerta para falta de energia, consumo incomum, orçamento mensal, relatório semanal, modo férias.

### Automações

- **Regras** — ações automáticas: *Se* algo acontecer, *então* faça algo, com atraso opcional.
- Gatilhos: ligar, desligar, ficar ligado por, ficar desligado por, ficar offline, voltar a ficar online, a potência passar de, a potência cair abaixo de, ao nascer do sol, ao pôr do sol, quando a temperatura externa subir acima de, quando a temperatura externa cair abaixo de, ao chegar, ao sair.
- Ações: Ligar, Desligar, Tocar alarme, Enviar notificação, Executar cena, Iniciar ciclo, Parar ciclo.
- **Somente quando**: Somente nestes horários, E somente se outro dispositivo…, Somente quando em casa / ausente.
- **Cenas** — Escolha o que acontece com cada dispositivo quando a cena é executada. Dispositivos deixados em “Pular” não são alterados.
- **Grupos** — Escolha os dispositivos do grupo. O grupo aparece na tela Dispositivos como um dispositivo: o botão dele liga ou desliga todos juntos, e você pode usá-lo em automações.
- **Ciclos e temporizadores** — Contagem regressiva, Ligar e depois desligar, Ciclo liga/desliga, Um por um, Etapas personalizadas. Início por botão, em um horário nos dias escolhidos ou durante uma faixa de horário; variação aleatória opcional (aparência de casa ocupada) e “parar se alterado manualmente”.
- As automações rodam neste celular enquanto o monitoramento está ativo. Para agendamentos que precisam funcionar mesmo com o celular desligado, use os temporizadores na página do dispositivo.

### Energia

- Visão por dia / mês / ano com total, custo estimado, potência média e de pico, hora/dia/mês de maior consumo e um resumo de destaques.
- Tarifa única ou em três faixas (dia / ponta / noite), preço por dispositivo ou preço padrão.
- Consumo em espera (a potência abaixo da qual o dispositivo nunca fica) e seu custo mensal.
- Relatório de energia em PDF (para imprimir ou compartilhar) ou CSV (Excel, Planilhas Google).
- O consumo é calculado pelo SwitchGuard a partir de leituras de potência feitas enquanto o monitoramento está ativo, então pode diferir um pouco do app do fabricante do dispositivo (eWeLink, Smart Life).

### Histórico

- Cada evento com data e hora e quem ou o que o causou (app, automação); filtro por dispositivo.
- Exportar para CSV (últimas 24 horas, 7 dias, 30 dias ou tudo) e limpar o histórico.

### Chat

- Olá. Você pode gerenciar seus dispositivos escrevendo: ligar ou desligar, perguntar o status, o consumo de energia ou a temperatura externa (os comandos funcionam em inglês ou turco). Toque no microfone para falar em vez de escrever (“Turn off kitchen”); a resposta é lida em voz alta. Os modelos prontos estão abaixo; toque em um para enviá-lo, mantenha pressionado para editar.
- Os comandos em texto livre funcionam offline e entendem inglês e turco; o microfone usa o reconhecimento de voz do celular e as respostas podem ser lidas em voz alta.
- Modelos: mensagens de um toque que também podem rodar sozinhas todo dia em um horário ou quando algo acontece.

### Tela de espera

- Um painel em tela cheia para um celular ou tablet carregando: relógio, clima, status crítico, potência ao vivo, atividade recente e o que roda a seguir. Também aparece sobre a tela de bloqueio.
- Abre os ajustes do Android: escolha o SwitchGuard em Protetor de tela e defina para iniciar durante o carregamento. O painel então substitui a tela de bloqueio enquanto estiver no carregador.

### Widgets, bloco de configurações rápidas, atalhos

- **SwitchGuard** — Mostra se o monitoramento está ativo e qualquer alarme tocando
- **Interruptor do SwitchGuard** — Liga e desliga um dispositivo ou canal com um toque. Também pode ser configurado para Em casa / Ausente ou para executar uma cena.
- **Grupo do SwitchGuard** — Bloco grande: ligue ou desligue cada dispositivo de um grupo individualmente
- **Comando de voz do SwitchGuard** — Um bloco: toque, diga (“turn off kitchen”) e ele executa
- **Bloco de configurações rápidas** — mostra o estado do monitoramento e o inicia ou para (o celular precisa ser desbloqueado antes); enquanto um alarme toca, abre o app.
- **Atalhos do app** — toque e segure o ícone do app para Em casa / Ausente e suas cenas.

### Em casa / Ausente

- **Em casa / Ausente** — As regras marcadas como "somente quando ausente" não rodam. Mude para Ausente quando sair (também pelo widget, pelo relógio ou pelo /away do Telegram).
- **Modo férias** — Você conta como ausente, o horário de silêncio fica desativado e um relatório chega toda manhã às 09:00. Termina sozinho.
- **Zonas** — Escolha uma área no mapa; as automações rodam quando seu celular entra ou sai dela (por exemplo, ligar a luz do jardim quando você chega em casa, desligar tudo quando sai). As imagens do mapa são baixadas do OpenStreetMap; sua localização é usada apenas neste celular.

### Wear OS

- O app de relógio SwitchGuard mostra alarmes (desligue no pulso), dispositivos (ligar/desligar), cenas e Em casa / Ausente; conecta-se ao app do celular.

### Android TV

- Funciona na Android TV com o controle remoto; o alarme aparece sobre o que estiver passando (“Exibir alarme sobre outros apps”).
- “Digitar pelo celular”: a TV mostra um QR code; você digita as chaves no celular e elas chegam criptografadas à TV pelo Wi-Fi de casa.
- “Receber de outro dispositivo”: copia chaves Tuya, regras e configurações do seu celular via Wi-Fi.

### Vários dispositivos

Celular, tablet, Android TV e PC: **cada dispositivo precisa da própria conta eWeLink com o recurso de desenvolvedor aprovado e das chaves de API dessa conta (App ID / Secret)** — a eWeLink permite um dispositivo conectado por conta. Compartilhe seus dispositivos com a conta extra no app eWeLink. As chaves Tuya podem ser usadas em vários dispositivos. O Pro é comprado uma vez e vale em todos os dispositivos Android com a mesma conta Google. Detalhes: [English](docs/guide/ewelink.en.md#using-several-devices-phone-tablet-android-tv) · [Türkçe](docs/guide/ewelink.tr.md#birden-fazla-cihazda-kullanım-telefon-tablet-android-tv)

### Demo e Pro

- **Dispositivos de demonstração** — Dispositivos virtuais para experimentar todos os recursos. Eles agem como se algo tivesse acontecido no próprio dispositivo (alguém o acionou, a energia ou o Wi-Fi caiu), então suas regras se aplicam e o alarme pode tocar. Acionar pelos botões acima conta como você e não toca o alarme.
- A versão gratuita monitora 1 dispositivo. O Pro monitora todos os seus dispositivos, com todos os recursos. Pagamento único, sem assinatura. Vinculado à sua conta Google, então vai com você para um novo celular.

## Todas as configurações

Tudo na ordem do app. A aba **Ajustes** reúne conexões e opções do app; alarme, horário de silêncio, Telegram e alertas inteligentes ficam na aba **Alertas** → Ajustes de alertas; as opções de cada dispositivo ficam na página dele.

### Ajustes → Contas

| Configuração | Para que serve |
|---|---|
| Pro / Obter o Pro | A versão gratuita monitora 1 dispositivo. O Pro monitora todos os seus dispositivos, com todos os recursos. Pagamento único, sem assinatura. Vinculado à sua conta Google, então vai com você para um novo celular. |
| Restaurar | Verifica de novo suas compras no Google Play (celular novo, reinstalação). |
| eWeLink — Entrar com eWeLink | Abre a própria página de login da eWeLink; o app nunca vê sua senha. |
| Credenciais da API | App ID, App Secret e região do seu app de desenvolvedor eWeLink. |
| Sair | O monitoramento será interrompido. Seus ajustes e histórico são mantidos. |
| Tuya / Smart Life | Digite o Access ID e o Access Secret do seu projeto Tuya Cloud (platform.tuya.com → Cloud → seu projeto → Overview). A conta do seu app Smart Life / Tuya precisa estar vinculada ao projeto. Data center: Europa Central, Oeste da América, China, Índia |
| Desconectar | Remove a conexão Tuya; os dispositivos Tuya deixam de ser monitorados. |
| Dispositivos de demonstração | Dispositivos virtuais para experimentar todos os recursos |

### Ajustes → Monitoramento

Como o SwitchGuard vigia seus dispositivos em segundo plano.

| Configuração | Para que serve |
|---|---|
| Confiabilidade máxima | Mantém o processador ativo para que nenhum evento seja atrasado. Usa mais bateria. |
| Ignorar desconexões breves | Aguarda este tempo antes de avisar que um dispositivo ficou offline. Se ele voltar a tempo, nenhum alerta é gerado. |
| Avisar se o monitoramento parar | Sem internet ou sem conexão com a nuvem por este tempo: você recebe um aviso |
| Intervalo da verificação de reserva | Usado apenas quando as atualizações instantâneas não estão disponíveis. |

### Ajustes → Permissões

Sem elas, o Android pode impedir que o alarme chegue até você.

| Configuração | Para que serve |
|---|---|
| Notificações | Alarmes e avisos não podem ser exibidos. |
| Atividade em segundo plano | O celular pode colocar o monitoramento para dormir; os alarmes podem atrasar ou não tocar. |
| Alarme em tela cheia | Com o celular bloqueado, o alarme não consegue abrir sua tela grande. |
| Alarme na tela de bloqueio (Xiaomi) | Com o celular bloqueado, a tela de alarme não consegue abrir. Toque em Corrigir, abra Outras permissões e ative “Mostrar na tela de bloqueio” e “Abrir novas janelas enquanto executa em segundo plano”. |
| Exibir alarme sobre outros apps | Necessário no Android TV para que o alarme apareça por cima do que estiver passando. Toque e permita o SwitchGuard. |
| Início em segundo plano (Xiaomi, Huawei, OPPO, vivo…) | Aparece em celulares que bloqueiam o início em segundo plano (Xiaomi, Huawei, OPPO, vivo…): permite que o monitoramento volte após reiniciar ou atualizar. Veja Solução de problemas. |

### Ajustes → Localização e zonas

| Configuração | Para que serve |
|---|---|
| Localização | Necessária para as regras de nascer/pôr do sol e temperatura externa. Fica neste dispositivo; para a temperatura, é enviada ao Open-Meteo arredondada para cerca de 100 m. |
| Zonas | Escolha uma área no mapa; as automações rodam quando seu celular entra ou sai dela (por exemplo, ligar a luz do jardim quando você chega em casa, desligar tudo quando sai). As imagens do mapa são baixadas do OpenStreetMap; sua localização é usada apenas neste celular. |

### Ajustes → Tela de espera

| Configuração | Para que serve |
|---|---|
| Abrir tela de espera | Toque na tela para “Abrir app” e “Fechar” |
| Usar como protetor de tela | Abre os ajustes do Android: escolha o SwitchGuard em Protetor de tela e defina para iniciar durante o carregamento. O painel então substitui a tela de bloqueio enquanto estiver no carregador. |
| Escurecer à noite | O texto esmaece entre 23:00 e 07:00 |

### Ajustes → Resumo diário

| Configuração | Para que serve |
|---|---|
| Notificação de resumo diário | Quantas vezes cada dispositivo ligou, quanto tempo ficou ligado e quanta energia usou hoje |
| Enviar às | Horário em que o resumo é enviado. |
| Mostrar o resumo de hoje agora | Mostra agora mesmo. |

### Ajustes → Backup e transferência

| Configuração | Para que serve |
|---|---|
| Fazer backup em arquivo | Regras, automações, ajustes e as chaves do Tuya e do eWeLink vão para um arquivo protegido por senha |
| Restaurar de um arquivo | Trocando de celular? Escolha o arquivo de backup e digite a senha dele |
| Enviar para outro dispositivo | Copie chaves do Tuya, regras e ajustes para uma TV ou tablet no mesmo Wi-Fi |
| Receber de outro dispositivo | Receba a configuração do seu celular pelo Wi-Fi |

A transferência por Wi-Fi nunca envia as chaves de API da eWeLink nem a sessão eWeLink: no outro dispositivo, informe as chaves de API dele e entre com a conta eWeLink dele. O backup em arquivo inclui as chaves eWeLink e Tuya, protegidas pela sua senha.

### Ajustes → Idioma

| Configuração | Para que serve |
|---|---|
| Idioma | Escolha o idioma do app, das notificações e dos alarmes. Idioma do celular ou um de 20 idiomas (a partir da 2.26). |

### Ajustes → Ajuda e sobre

| Configuração | Para que serve |
|---|---|
| Guia de configuração | Mostrar a configuração inicial novamente |
| Guia de configuração do eWeLink | Passo a passo com imagens (GitHub) |
| Guia de configuração do Tuya / Smart Life | Passo a passo com imagens (GitHub) |
| Enviar relatório de problema | Algo errado? Prepara um e-mail com detalhes de diagnóstico. Nada é compartilhado se você não enviar. |
| Excluir relatórios de falha salvos | Apaga os relatórios de falha salvos neste dispositivo. |
| Contato e feedback | Escreve um e-mail ao desenvolvedor (dúvidas, erros, ideias). |
| Política de privacidade | Abre a política de privacidade. |
| Sobre | Versão e aviso legal. A versão do GitHub verifica no GitHub se há uma mais nova. |

### Alertas

| Configuração | Para que serve |
|---|---|
| Alertas de dispositivos | Para o que cada dispositivo toca ou notifica |
| Alertas condicionais | Tocar quando um dispositivo fica ligado ou desligado por um tempo, ou quando a potência dele muda |
| O alarme está pronto / Os alarmes podem não chegar até você | O cartão de prontidão no topo: lista configurações do celular que podem impedir o alarme e abre a tela certa para corrigir; também há um botão “Testar alarme”. |

### Alertas → Alarme

| Configuração | Para que serve |
|---|---|
| Som do alarme | Som geral do alarme. Cada dispositivo pode ter o seu (página do dispositivo). |
| Volume do alarme | Volume próprio do alarme do SwitchGuard. |
| Vibrar | Vibrar enquanto o alarme toca. |
| Tocar no volume máximo | Aumenta o volume de alarme do celular ao máximo enquanto o alarme toca e pausa a música; restaurado quando você desliga o alarme |
| Tocar novamente se não resolvido | Depois que você desliga um alarme, ele toca de novo neste intervalo enquanto o dispositivo continuar no estado que o causou (ainda desligado, ainda ligado ou offline). Para quando o dispositivo volta ao normal. |
| Avisar outra pessoa | Configuração do Telegram (veja abaixo). |
| Testar alarme | Teste o alarme agora: com som ou em silêncio, imediatamente ou após 10 segundos |

### Alertas → Categorias de alerta

Todo dispositivo tem uma categoria de alerta. Se você não escolher uma, ela é definida automaticamente: um dispositivo monitorado com algum evento configurado como Alarme é Crítico, um que apenas envia notificações é Importante, os demais são Normais. Alertas condicionais e automações podem usar uma categoria como gatilho (por exemplo, “tocar o alarme quando qualquer dispositivo crítico ficar offline”).

| Configuração | Para que serve |
|---|---|
| Crítico · Importante · Normal · Auto | No modo automático: Crítico se algum evento toca um alarme, Importante se os eventos apenas notificam |
| Tocar também no horário de silêncio | Os alarmes tocam também no horário de silêncio / No horário de silêncio, os alarmes viram notificações |

### Alertas → Horário de silêncio

| Configuração | Para que serve |
|---|---|
| Horário de silêncio | Durante esses horários os alarmes viram notificações (dispositivos críticos ainda tocam) |
| Início / Fim | Horário de início e de fim do silêncio. |

Dispositivos em categorias de alerta configuradas para tocar no horário de silêncio (Crítico por padrão) continuam tocando.

### Alertas → Avisar outra pessoa

Se um alarme não for desligado a tempo, as pessoas que você escolher recebem uma mensagem no Telegram, e outra quando ele for desligado. Mesmo que seu celular esteja em outro cômodo à noite, alguém em casa ou um vizinho fica sabendo. As mensagens são enviadas por um bot do Telegram que você mesmo cria; nenhum outro servidor está envolvido.

| Configuração | Para que serve |
|---|---|
| 1. Bot do Telegram | No Telegram, abra @BotFather, envie /newbot e dê um nome ao seu bot. Cole abaixo o token que o BotFather fornecer. |
| 2. Pessoas a avisar | Peça a cada pessoa que abra @your_bot no Telegram e toque em Iniciar (você também pode adicionar o bot a um grupo da família). Depois toque em Procurar pessoas. |
| 3. Quando | Quanto tempo o alarme pode tocar antes de avisar as pessoas (ou na hora). |
| Enviar uma mensagem de teste | Envia uma mensagem de teste a todos da lista. |
| Desativar | Desativa o aviso pelo Telegram. |

### Alertas → Alertas inteligentes

Alertas que olham todos os seus dispositivos em conjunto: quedas de energia, consumo incomum e um resumo semanal. Com o Telegram você também pode consultar o status, assumir um alarme e acionar dispositivos remotamente.

| Configuração | Para que serve |
|---|---|
| Queda de energia: Detectar quedas de energia | Se todos os dispositivos monitorados ficarem offline juntos, você recebe um único alerta de “queda de energia” em vez de um por dispositivo, e uma mensagem com a duração quando a energia voltar. |
| Carregador como sensor de energia | Se este celular/tablet fica conectado à tomada no local, ficar desconectado por um minuto conta como queda de energia; também funciona quando a internet cai. A mensagem sai pelo Telegram usando dados móveis. |
| Em casa / Ausente: Alertas mais suaves em casa | Em casa, os alarmes de dispositivos não críticos viram notificações (como no horário de silêncio). Dispositivos críticos sempre tocam. |
| Modo férias | Você conta como ausente, o horário de silêncio fica desativado e um relatório chega toda manhã às 09:00. Termina sozinho. |
| Consumo: Consumo incomum | Toda manhã, compara ontem com as duas semanas anteriores. Um uso muito maior pode indicar uma falha; muito menor (um freezer que quase não funcionou) pode indicar que ele parou de funcionar. |
| Tarifa de eletricidade | O preço padrão para dispositivos sem preço próprio. Com tarifa de três faixas, cada hora é cobrada na sua própria faixa: dia 06–17, ponta 17–22, noite 22–06. |
| Orçamento mensal | Você é avisado uma vez por mês quando o custo estimado dos dispositivos com medição de energia atingir este valor. |
| Relatório semanal | Toda segunda-feira às 09:00: quedas de energia, alarmes e tempo de resposta, desconexões e consumo da semana passada. Também enviado ao Telegram, se configurado. |
| Mostrar o relatório desta semana | Mostra agora mesmo. |
| Telegram: Comandos e botão “Entendi” | As pessoas da sua lista podem enviar /status e assumir um alarme que está tocando. A mensagem do alarme ganha um botão “Entendi”: ao pressioná-lo, o celular é silenciado. |
| Permitir ligar e desligar dispositivos | Comandos /on e /off. Somente as pessoas da sua lista podem usá-los. |
| Aviso em etapas | Cada pessoa recebe a mensagem após a sua própria espera. Por exemplo, um vizinho após 5 minutos, um parente após 15, o grupo da família após 30. Se alguém tocar em “Entendi”, as próximas pessoas não são avisadas. |
| Ligar em alarme crítico | Quando um dispositivo da categoria Crítico dispara o alarme, a pessoa também recebe uma ligação da linha deste celular quando acabar a espera dela nos alertas em etapas. A ligação só serve para acordá-la: o que aconteceu está na mensagem do Telegram enviada ao mesmo tempo (o Android não permite que apps reproduzam som em uma chamada). Peça que salve este número como “Alarme SwitchGuard”. As ligações se repetem a cada 2 minutos, até 3 vezes, até o alarme ser desligado ou alguém tocar em “Entendi” no Telegram. Grupos não podem ser chamados. |
| Enviar alertas inteligentes ao Telegram | Quedas de energia, consumo incomum, o alerta de orçamento e o relatório semanal também são escritos no Telegram. |
| Watchdog | Dois dispositivos com o mesmo bot vigiam um ao outro (por exemplo, o celular e uma TV box). O dispositivo emissor mantém atualizada uma mensagem fixada na conversa do Telegram; o dispositivo vigilante dispara um alarme e avisa seus contatos se ela parar. Não desafixe essa mensagem. |

### Página do dispositivo (toque em um dispositivo)

| Configuração | Para que serve |
|---|---|
| Monitorar este dispositivo | As mudanças são registradas e alertadas conforme as regras abaixo |
| Quando isto acontecer | Desliga, Liga, Fica offline, Fica online → Alarme / Notificar / Nenhuma. O alarme toca até você desligá-lo. Notificar mostra uma notificação normal. Nenhuma apenas registra o evento no histórico. |
| Som para este dispositivo | Som de alarme próprio; senão, o som geral. |
| Categoria de alerta | No modo automático: Crítico se algum evento toca um alarme, Importante se os eventos apenas notificam |
| Energia | Potência, tensão, corrente; hoje e este mês; histórico por dia / mês / ano com custo; gerar relatório (PDF / CSV). |
| Preço por kWh | Preço por kWh deste dispositivo ou o preço padrão; também pode virar o padrão. |
| Temporizadores (no dispositivo) | Nenhum temporizador. Os temporizadores rodam no próprio dispositivo, mesmo com o celular desligado. (Dispositivos eWeLink: adicione ou edite timers de ligar/desligar em um horário nos dias escolhidos.) |
| Conexão | dBm é a intensidade do sinal: quanto mais perto de 0, melhor. Acima de -55 excelente, até -67 bom, até -75 regular, abaixo disso fraco. |
| Consumo em standby | A menor média horária dos últimos 7 dias: a potência abaixo da qual este dispositivo (e tudo o que está ligado nele) nunca cai, mesmo "sem fazer nada". TVs, modems e carregadores costumam consumir isso em standby. |
| Copiar dados do dispositivo | Copia os dados brutos do dispositivo para a área de transferência, útil para relatar um modelo sem suporte |

## Comandos do Telegram

As pessoas da sua lista podem enviar /status e assumir um alarme que está tocando. A mensagem do alarme ganha um botão “Entendi”: ao pressioná-lo, o celular é silenciado.

```
Comandos do SwitchGuard:
/status – monitoramento, alarme e dispositivos
/ack – assumir o alarme que está tocando
/on nome [canal] – ligar um dispositivo ou grupo (vários: /on kitchen, garden)
/off nome [canal] – desligar um dispositivo ou grupo
/watt [nome] – potência agora, hoje e consumo habitual
/temp – temperatura externa
/scene nome – executar uma cena
/home · /away – definir em casa / ausente

Você também pode escrever livremente (em inglês ou turco): “turn off kitchen and garden”, “heater watts”, “is the pump on”.

Ligar e desligar dispositivos e executar cenas precisa ser permitido no app (Alertas → Alertas inteligentes).
```

Os nomes de comando em inglês e em turco funcionam (ex.: /status = /durum, /on = /ac). Comandos só são aceitos dos chats que você escolheu.

## Permissões e por quê

| Permissão | Por quê |
|---|---|
| `POST_NOTIFICATIONS` | Mostrar alarmes, notificações e o estado do monitoramento. |
| `FOREGROUND_SERVICE` | Manter o monitoramento em segundo plano (a notificação de estado permanente). |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Pedir para ficar fora da otimização de bateria, para os alarmes não atrasarem. |
| `USE_FULL_SCREEN_INTENT` | Abrir o alarme em tela cheia sobre a tela de bloqueio. |
| `SYSTEM_ALERT_WINDOW` | Mostrar o alarme sobre outros apps (Android TV, Xiaomi). |
| `WAKE_LOCK` | Manter o processador acordado ao tratar eventos (“Confiabilidade máxima”). |
| `VIBRATE` | Vibrar durante o alarme. |
| `RECEIVE_BOOT_COMPLETED` | Reiniciar o monitoramento quando o celular reinicia. |
| `INTERNET` | Falar com a nuvem eWeLink / Tuya (e com Telegram / Open-Meteo / OpenStreetMap, se você usar). |
| `ACCESS_COARSE_LOCATION` | Só quando você toca em “Usar a localização deste dispositivo” para regras de nascer/pôr do sol e temperatura. |
| `ACCESS_FINE_LOCATION` | Zonas: uma área exata para regras de chegada / saída. |
| `ACCESS_BACKGROUND_LOCATION` | Zonas: perceber chegadas e saídas com o app fechado (pedido só ao criar uma zona). |
| `CALL_PHONE` | “Ligar em alarme crítico”: ligar para o número informado pela linha deste celular. |

## Privacidade

- Sem servidor próprio, sem análises, sem anúncios, sem rastreamento. Nada é vendido ou compartilhado.
- Chaves, tokens, dispositivos, regras, configurações e histórico (máx. 2.000 eventos) ficam só no seu dispositivo.
- O app só fala com as nuvens que você conecta (eWeLink `*.coolkit.cc` / `*.coolkit.cn`, Tuya `openapi.tuya*.com`) e, só se você usar, com Telegram, Open-Meteo (temperatura e clima; localização arredondada para cerca de 100 m) e blocos de mapa do OpenStreetMap.
- Sua senha eWeLink é digitada na própria página da eWeLink; o app nunca a vê.
- Os dados do app ficam fora do backup em nuvem do Android; backups em arquivo são protegidos por senha; a transferência por Wi-Fi é criptografada com um código de uso único e não sai da sua rede doméstica.
- As compras do Pro são processadas pelo Google Play.

Política de privacidade completa: [Google Sites](https://sites.google.com/view/switchguard-privacy) · [docs/privacy-policy.md](docs/privacy-policy.md)

## Solução de problemas e perguntas frequentes

### O alarme atrasa ou não toca

| Problema | O que fazer |
|---|---|
| A atividade em segundo plano está restrita | O celular pode colocar o monitoramento para dormir; os alarmes podem atrasar ou não tocar. |
| O alarme em tela cheia não é permitido | Com o celular bloqueado, o alarme não consegue abrir sua tela grande. |
| A Xiaomi bloqueia a tela de alarme | Com o celular bloqueado, a tela de alarme não consegue abrir. Toque em Corrigir, abra Outras permissões e ative “Mostrar na tela de bloqueio” e “Abrir novas janelas enquanto executa em segundo plano”. |
| As notificações de alarme estão reduzidas | A categoria de notificação “Alarmes” não está com prioridade alta, então o alarme não aparecerá em destaque nem ocupará a tela. |
| Não perturbe: silêncio total | Neste modo até os sons de alarme ficam mudos. |
| O volume do alarme está muito baixo | O volume de alarme do celular está no zero ou perto dele, então você pode não ouvir o SwitchGuard. Corrigir ativa “Tocar no volume máximo”. |
| As notificações estão desativadas | Alarmes e avisos não podem ser exibidos. |
| Há um segundo SwitchGuard neste celular | Outra cópia do SwitchGuard foi criada com o recurso Apps duplos (clone de app) do celular. Duas cópias em execução lado a lado tocam cada alarme duas vezes e desconectam uma à outra quando usam a mesma conta eWeLink; em celulares Xiaomi, as permissões desta cópia também podem aparecer como desativadas. Remova a segunda cópia: mantenha o ícone dela pressionado (o que tem o pequeno selo) e toque em Desinstalar. |

### Fabricantes que param apps em segundo plano

| Celular | O que fazer |
|---|---|
| Xiaomi | Necessário para o monitoramento reiniciar após uma reinicialização ou atualização. Toque e ative a Inicialização automática para o SwitchGuard. |
| Samsung | A Samsung pode colocar o app para dormir. Em Bateria, escolha "Sem restrições" e adicione o SwitchGuard a "Apps que nunca dormem" (Bateria → Limites de uso em segundo plano). |
| Huawei / Honor | Em Inicialização de apps, defina o SwitchGuard como "Gerenciar manualmente" e ative Inicialização automática, Inicialização secundária e Execução em segundo plano. |
| OPPO / realme / OnePlus | Ative a Inicialização automática para o SwitchGuard e, nos ajustes de Bateria dele, permita atividade em segundo plano. |
| vivo / iQOO | Permita que o SwitchGuard inicie em segundo plano e permita alto consumo de energia em segundo plano. |
| Tecno / Infinix | Ative a Inicialização automática para o SwitchGuard no Phone Master para que o monitoramento sobreviva a reinicializações e limpezas. |
| ASUS | Permita o SwitchGuard no Gerenciador de Inicialização Automática para que o monitoramento reinicie após uma reinicialização. |

### Outras perguntas

**O monitoramento parou e diz que minha sessão foi encerrada.**  
Dica: o eWeLink permite uma sessão por conta. Dê a cada celular sua própria conta eWeLink e compartilhe os dispositivos pelo app eWeLink.

**Um dispositivo Tuya compartilhado comigo não aparece.**  
Falta um dispositivo compartilhado com você? A Tuya lista apenas os dispositivos das suas casas. Peça ao proprietário que o adicione como membro da casa no Smart Life (Eu → Gerenciamento da casa → Adicionar membro).

**Os dispositivos Tuya pararam de atualizar.**  
O teste do Tuya Cloud expirou, então os dispositivos Tuya não estão sendo monitorados. Estenda-o em platform.tuya.com → Cloud → IoT Core → Extend trial period.

**As mudanças chegam atrasadas / não há atualizações instantâneas.**  
Atualizações instantâneas indisponíveis para este App ID; usando verificações periódicas (mudanças mais curtas que o intervalo podem passar despercebidas)

**Posso alternar entre a versão do Google Play e a do GitHub?**  
Sim, mas desinstale a outra antes: as assinaturas são diferentes. Faça backup em arquivo antes de desinstalar e restaure depois.

**Como testo o alarme sem acordar todo mundo?**  
Teste o alarme agora: com som ou em silêncio, imediatamente ou após 10 segundos

**Por que o consumo é diferente do app eWeLink / Smart Life?**  
O consumo é calculado pelo SwitchGuard a partir de leituras de potência feitas enquanto o monitoramento está ativo, então pode diferir um pouco do app do fabricante do dispositivo (eWeLink, Smart Life).

## App para Windows

O SwitchGuard para Windows (10 / 11) faz o mesmo monitoramento em um PC: fica na bandeja do sistema, toca um alarme em tela cheia no computador e compartilha as mesmas regras, automações e recursos de Telegram. Zonas, widgets, relógio e chamadas telefônicas são só do celular.

### Recursos

- As mesmas abas: Dispositivos, Alertas, Automações, Histórico, Chat, Ajustes; os comandos de voz do chat usam a digitação por voz do Windows.
- Ícone na bandeja: abrir, iniciar / parar o monitoramento, desligar o alarme, sair; fechar a janela não para o monitoramento.
- Janela de alarme em tela cheia, sons embutidos (Sirene, Bipe, Pulso, Carrilhão) ou seu próprio WAV / MP3.
- Tela de espera e protetor de tela do Windows: o painel abre quando o PC fica ocioso.
- Um notebook deixado na tomada funciona como sensor de falta de energia.
- Atualizações pelo GitHub com um clique; configurações e licença são mantidas. Backup em arquivo protegido por senha (não pode ser restaurado no celular).

### Configurações só do Windows

| Configuração | Para que serve |
|---|---|
| Windows | Para que o SwitchGuard vigie neste computador sem interrupção. Se algum estiver desligado, alarmes podem ser perdidos após uma reinicialização ou com o computador em suspensão. |
| Iniciar com o Windows | Abre na bandeja quando você entra no Windows |
| Manter o computador acordado | Durante o monitoramento, o computador não entra em suspensão |
| Usar como protetor de tela do Windows | Quando o computador fica ocioso, a tela de espera abre; mexer o mouse ou apertar uma tecla a fecha |
| Espera | Quanto tempo o computador precisa ficar ocioso antes de a tela de espera abrir |
| Configurações de protetor de tela do Windows | Ex.: “Ao reiniciar, exibir a tela de logon” |
| Abrir tela de espera | Tela cheia; Esc fecha, clique para “Abrir app”. A tela fica ligada enquanto estiver aberta. |
| Idioma e aparência: Tema | Claro, escuro ou igual ao Windows. |
| Idioma | Escolha o idioma do app, das notificações e dos alarmes. |
| Atualizações ao vivo do Tuya neste computador | Desligado: os dispositivos Tuya são verificados a cada 5 minutos |
| Queda de energia quando este computador passa para a bateria | Para um notebook deixado na tomada em casa: se ficar na bateria por um minuto, conta como queda de energia, mesmo sem internet (a mensagem do Telegram sai quando a conexão volta). |
| Este computador está na mesma rede elétrica que os dispositivos | Enquanto ele estiver na tomada não há queda de energia, então um dispositivo que cai é avisado na hora, em vez de após até 90 segundos. |
| Carregar de arquivo… | Use seu próprio WAV ou MP3 para o alarme ou a notificação (primeiros 60 segundos, máx. 20 MB). |
| Backup em arquivo… | Todos os ajustes, chaves de conta e sons em um arquivo protegido por senha |
| Ativar Pro | Sem licença o app monitora 1 dispositivo. Digite sua chave de licença para monitorar todos os seus dispositivos. |

### Instalar no Windows

1. Clique duas vezes no instalador (SwitchGuard-…-Kurulum.exe). Ele não pede direitos de administrador e instala em poucos segundos.
2. Se o Windows disser “O Windows protegeu o computador” (SmartScreen), clique em Mais informações → Executar assim mesmo. O app ainda não é assinado; por isso o aviso.
3. O SwitchGuard é instalado na sua pasta de usuário (AppData › Local › SwitchGuard) e cria atalhos na área de trabalho e no menu Iniciar.
4. Ele inicia sozinho na bandeja (ao lado do relógio) quando você entra no Windows. Fechar a janela não o encerra; para sair, use o ícone da bandeja → Sair.
5. Adicione suas contas em Ajustes → Contas (os passos do Tuya e do eWeLink estão abaixo).
6. Mudar para outro computador: no antigo, Ajustes → Backup e transferência → Backup em arquivo; instale no novo, Restaurar de arquivo e entre de novo no eWeLink.
7. Para desinstalar: Configurações do Windows → Aplicativos → Aplicativos instalados → SwitchGuard → Desinstalar.
8. Instalar uma nova versão: primeiro desinstale a antiga (passo 7) e depois execute o novo instalador. Seus ajustes, contas e sons são mantidos.

## Compilar

```
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # unit tests
```
Builds de release precisam de `keystore.properties` + keystore na raiz do projeto (não estão no repositório).

## Histórico de versões

[English](CHANGELOG.md) · [Türkçe](CHANGELOG.tr.md) · [Releases](https://github.com/Macerce/switchguard/releases)

## Contato

Dúvidas, relatos de erro e sugestões (ou abra uma issue aqui): **switchguardapp@gmail.com**

## Licença

O SwitchGuard é **source-available** (código-fonte disponível), não código aberto. Código-fonte: [PolyForm Noncommercial 1.0.0](LICENSE). O uso pessoal e outros usos não comerciais são livres; vender o código ou usá-lo comercialmente não é permitido. As versões até a 2.8.3, inclusive, foram publicadas sob GPL-3.0.

As versões oficiais (Google Play, [Releases](https://github.com/Macerce/switchguard/releases)) podem ser usadas por qualquer pessoa, inclusive empresas. Versões modificadas não podem usar o nome nem o logotipo do SwitchGuard. Licença comercial: **switchguardapp@gmail.com**

Sem afiliação, aval ou patrocínio da eWeLink, CoolKit ou ITEAD. Sonoff e eWeLink são marcas registradas de seus respectivos proprietários. Também não tem vínculo com a Tuya.
