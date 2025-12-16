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
if (calculo.rra && !TipoProcessamento.PAGAMENTO_ANTERIOR.equals(calculo.tipoProcessamento)) {
    suspender 'Este evento não é calculado pela folha complementar de RRA. O valor total será calculado pela folha do pagamento anterior na qual este cálculo de RRA está vinculado. Após este cálculo, o sistema incluirá automaticamente o valor em cada parcela do processo de pagamento anterior de RRA que foram lançadas anteriormente em folhas complementares'
}
if (!calculo.rra && TipoProcessamento.PAGAMENTO_ANTERIOR.equals(calculo.tipoProcessamento)) {
    suspender 'Este evento não é calculado na folha de pagamento anterior pois o mesmo não se trata de um RRA. O valor deve ser lançado diretamente em cada folha complementar vinculada ao pagamento anterior selecionado'
}
if(Eventos.valorCalculadoMultiplosVinculos(evento.codigo, TipoValor.CALCULADO, calculo.tipoProcessamento, calculo.subTipoProcessamento) >= EncargosSociais.IRRF.parcelaIsentaAposentadoria) {
    suspender 'Este evento não será calculado pois a soma dos múltiplos vínculos ultrapassou o valor da parcela isenta'
}
def vaux = EncargosSociais.IRRF.parcelaIsentaAposentadoria - Eventos.valorCalculadoMultiplosVinculos(evento.codigo, TipoValor.CALCULADO, calculo.tipoProcessamento, calculo.subTipoProcessamento);
def base
if (calculo.rra){
    base = Bases.valor(Bases.IRRFRRA)
}else {
    base = Bases.valor(Bases.IRRF)
}
if (vaux > base){
    vaux = base;
}
valorCalculado = vaux;
