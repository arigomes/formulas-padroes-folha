if (TipoMatricula.ESTAGIARIO.equals(matricula.tipo) || TipoMatricula.APOSENTADO.equals(matricula.tipo) || TipoMatricula.PENSIONISTA.equals(matricula.tipo)) {
    suspender"Este cálculo não é executado para Estágiario e Autônomo e Pensionistas"
}
vaux = Lancamentos.valor(evento)
if (vaux >= 0) {
    valorReferencia = vaux
} else {
    valorReferencia = Funcoes.getValorBaseMultiplosVinculos(Bases.INSS13, calculo.tipoProcessamento, calculo.subTipoProcessamento)
}
valorCalculado = valorReferencia
