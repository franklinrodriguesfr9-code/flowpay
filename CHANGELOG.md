# Historico do FlowPay

Este arquivo registra a evolucao do FlowPay. Observacao importante: o repositorio Git foi iniciado quando o app ja estava na V4, entao as versoes V1 a V3 estao registradas como historico funcional/documental. O primeiro snapshot de codigo versionado e a V4.

## V4 - Atualizacao dinamica, escopos e notificacao configuravel

- Inicio com filtros para uma vez, pendente, pago, vencida e feito.
- Busca por nome, categoria e valor.
- Atualizacao centralizada apos criar, editar, excluir, pagar/fazer, importar backup ou limpar dados.
- Edicao e exclusao de recorrentes por escopo: apenas este, todos, deste em diante ou deste para tras.
- Alteracao de status pelo calendario: pendente, pago ou feito.
- Exclusoes passam a cancelar ocorrencias sem manter itens fantasmas nos totalizadores.
- Notificacoes com repeticao configuravel por minuto, hora ou dia.
- Politica de notificacao ao dispensar: continuar ate concluir ou parar ao dispensar.
- Perfil com status de permissoes e atalho para otimizacao de bateria.

## V3 - UI profissional, nota fiscal e lembrete insistente

- Barra inferior com icones reais.
- Calendario corrigido para evitar quebra de numeros.
- Tela de novo lancamento com data inicio, data fim obrigatoria para recorrentes e limite de 12 meses.
- Categoria especial Emitir Nota Fiscal como compromisso de entrada marcado como Feito.
- Cores de entrada/saida nos cards.
- Edicao, exclusao e prorrogacao pelo calendario.
- Relatorios CSV/TXT com vencimento, conclusao e dias em atraso.
- Perfil exibindo o nome do som escolhido.

## V2 - Recorrencia controlada e nova navegacao

- Navegacao com Inicio, Calendario, botao central, Resumo e Perfil.
- Lançamento unico substituindo a antiga aba Avulso.
- Cadastro com calendario/relogio nativos do Android.
- Categorias padrao e categoria Outros.
- Recorrencia sem geracao retroativa automatica.
- Limpeza de pendencias antigas nao feitas, preservando historico feito/pago.
- Resumo com quantidade/moeda, pagas, pendentes, vencidas e categorias.

## V1 - Base offline do app

- App Android nativo em Kotlin, Jetpack Compose e Room.
- Cadastro de compromissos financeiros mensais.
- Ocorrencias mensais, marcar como feito/pago e valor real.
- AlarmManager para lembretes offline.
- Canal de notificacao com som e vibracao.
- Backup completo `.flowpay.json`.
- Relatorio manual em CSV e TXT.
