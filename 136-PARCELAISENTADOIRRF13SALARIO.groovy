Funcoes.somenteAposentadosPensionistas()
if (!EncargosSociais.IRRF.parcelaIsentaAposentadoria) {
    suspender "Para calcular a parcela isenta, o campo 'Parc. isenta de aposentadoria' deve estar preenchido na manutenção de estabelecimento vigente"
}
if (!servidor.dataNascimento) {
    suspender 'O evento não é calculado para pessoas que não tenham data de nascimento informada'
} else {
    if (Funcoes.idade(servidor.dataNascimento, calculo.competencia) < 65) {
        suspender 'O evento não é calculado para pessoas com idade menor que 65 anos'
    }
}
if (servidor.possuiMolestiaGrave) {
    suspender 'O evento não é calculado para pessoas que possuam moléstia grave'
}
if (!Funcoes.recebeDecimoTerceiro()) {
    suspender 'A matrícula não tem direito a receber décimo terceiro'
}
if (calculo.rra || TipoProcessamento.PAGAMENTO_ANTERIOR.equals(calculo.tipoProcessamento)) {
    suspender 'Este evento não é calculado em processamentos vinculados a pagamentos anteriores'
}
if(Eventos.valorCalculadoMultiplosVinculos(evento.codigo, TipoValor.CALCULADO, calculo.tipoProcessamento, calculo.subTipoProcessamento) >= EncargosSociais.IRRF.parcelaIsentaAposentadoria) {
    suspender 'Este evento não será calculado pois a soma dos múltiplos vínculos ultrapassou o valor da parcela isenta'
}
def vaux = EncargosSociais.IRRF.parcelaIsentaAposentadoria - Funcoes.buscaValorEvento13SalarioIntegralAdiantado() - Eventos.valorCalculadoMultiplosVinculos(evento.codigo, TipoValor.CALCULADO, calculo.tipoProcessamento, calculo.subTipoProcessamento);
if (vaux > Bases.valor(Bases.IRRF13)){
    vaux = Bases.valor(Bases.IRRF13);
}
valorCalculado = vaux;
