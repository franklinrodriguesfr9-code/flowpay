# FlowPay

FlowPay e um app Android offline para controlar compromissos financeiros mensais, receber lembretes no celular, marcar pagamentos/compromissos como concluidos e exportar relatorios/backup.

## O que esta versao faz

- Versao atual: **V4**.
- Tela inicial com filtros de contas do mes: uma vez, pendentes, pagas, vencidas e feitas.
- Busca por nome, categoria ou valor.
- Botao central `+` para criar lancamento unico ou compromisso recorrente.
- Cadastro com seletor de data, seletor de horario, categorias padrao e valor variavel.
- Cadastro recorrente sem geracao retroativa automatica; retroativos precisam ser confirmados.
- Lista de pendencias do mes, com confirmacao de valor real ao marcar como feito/pago.
- Aba calendario para ver vencimentos por data.
- Edicao/exclusao de recorrentes por escopo: apenas este, todos, deste em diante ou deste para tras.
- Aba resumo com totais em quantidade/moeda, pagas, pendentes, vencidas, saldo e categorias.
- Aba perfil/configuracoes com backup, importacao, som, repeticao de notificacao e limpeza de dados.
- Lembretes offline usando `AlarmManager`, notificacao com som padrao e vibracao.
- Repeticao configuravel de notificacao por minuto, hora ou dia.
- Escolha de som de notificacao usando o seletor do Android.
- Relatorio mensal com entradas, saidas, saldo, pendencias, vencidas e totais por categoria.
- Exportacao manual de relatorio em CSV e TXT pelo compartilhamento do Android.
- Exportacao de backup completo em `.flowpay.json`.
- Importacao de backup completo para restaurar os dados do app.

## Como abrir

1. Instale o Android Studio recente se quiser usar a IDE.
2. Abra esta pasta como projeto.
3. Execute em um celular/emulador Android ou gere o APK pelo terminal.

## Como gerar APK

O projeto ja inclui Gradle Wrapper. O SDK Android foi configurado localmente em:

`C:\Users\NOME-USUARIO\AppData\Local\Android\Sdk`

Comandos validados:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

APK gerado:

`app\build\outputs\apk\debug\app-debug.apk`

## Backup e restauracao

- Use `Exportar relatorio CSV + TXT` para guardar ou analisar um periodo.
- Use `Exportar backup completo` para gerar o arquivo restauravel `.flowpay.json`.
- Use `Importar backup` para restaurar esse `.flowpay.json` em uma instalacao nova.
- A importacao substitui os dados atuais do app para evitar duplicidades.

## Permissoes importantes

- Android 13+: o app pede permissao de notificacoes.
- Android 12+: se o sistema bloquear alarmes exatos, o app mostra um aviso para ativar "Alarmes e lembretes" nas configuracoes.
- Alguns aparelhos podem atrasar repeticoes muito curtas quando entram em economia de bateria/Doze. O app inclui atalho para revisar otimizacao de bateria.

## Historico

Veja [CHANGELOG.md](CHANGELOG.md) para a evolucao registrada das versoes V1 a V4.
