if (!TipoMatricula.FUNCIONARIO.equals(matricula.tipo) && !TipoMatricula.AUTONOMO.equals(matricula.tipo) && !TipoMatricula.ESTAGIARIO.equals(matricula.tipo)) {
    suspender "Este cálculo é executado apenas para funcionários, autônomos ou estagiários"
}
if (TipoMatricula.ESTAGIARIO.equals(matricula.tipo)) {
    if (!estagiario.codESocial.equals("902")) {
        suspender "Não há desconto de contribuição previdenciária para estagiários com o 'Código eSocial' diferente de '902' informado na categoria do trabalhador"
    }
}
if (TipoMatricula.AUTONOMO.equals(matricula.tipo)) {
    if (autonomo.codESocial.equals("741")) {
        suspender "Não há desconto de contribuição previdenciária para autônomos da categoria MEI com o 'Código eSocial' igual a '741' informado na categoria do trabalhador"
    }
}
if (Funcoes.possuiPrevidenciaFederal(matricula.tipo)) {
    def base = 0
    def dtrescisao
    boolean possuiMultiploVinculo = matricula.possuiMultiploVinculo
    if (TipoMatricula.FUNCIONARIO.equals(matricula.tipo)) {
        dtrescisao = calculo.dataRescisao
    }
    def afasservmil = Funcoes.afasservmil()
    def afasacidtrab = Funcoes.afasacidtrab()
    def diafin
    if (dtrescisao == null) {
        if (calculo.quantidadeDiasCompetencia > 30 || afasservmil > 0 || afasacidtrab > 0) {
            diafin = 30
        } else {
            diafin = calculo.quantidadeDiasCompetencia
        }
    } else {
        diafin = Datas.dia(dtrescisao)
    }
    def abatinss = 0
    def diassd = afasservmil + afasacidtrab //dias sem direito
    if (diassd > 0) {
        abatinss = Numeros.arredonda(Funcoes.calcprop(Funcoes.remuneracao(matricula.tipo).valor, Funcoes.cnvdpbase(diassd)), 2)
    }
    def dias = diafin - diassd
    if (dias > 0) {
        def baseMatricula = Bases.valor(Bases.INSS) + Bases.valor(Bases.INSSFER)
        def baseInssOutraEmpresaFuncionario = Bases.valor(Bases.INSSOUTRA) + Bases.valorCalculado(Bases.INSSOUTRA, TipoProcessamento.FERIAS, SubTipoProcessamento.INTEGRAL) + Funcoes.buscaBaseDeOutrosProcessamentos(Bases.INSSOUTRA)
        def baseInssOutraEmpresaAutonomo = Bases.valor(Bases.INSSOUTAUTO) + Bases.valorCalculado(Bases.INSSOUTAUTO, TipoProcessamento.FERIAS, SubTipoProcessamento.INTEGRAL) + Funcoes.buscaBaseDeOutrosProcessamentos(Bases.INSSOUTAUTO)
        def baseprev = baseMatricula + baseInssOutraEmpresaFuncionario + baseInssOutraEmpresaAutonomo
        if (diassd == 0) {
            base = baseprev
        } else {
            base = baseprev - abatinss
        }
        if (base > 0) {
            def inssOutrosVinculos = 0
            if (possuiMultiploVinculo) {
                def baseMultiplosVinculos = Funcoes.getValorBaseMultiplosVinculos(Bases.INSS, calculo.tipoProcessamento, calculo.subTipoProcessamento)
                if (baseMultiplosVinculos > 0) {
                    base += baseMultiplosVinculos
                    baseprev += baseMultiplosVinculos
                }
                inssOutrosVinculos = Eventos.valorCalculadoMultiplosVinculos(evento.codigo, TipoValor.CALCULADO, calculo.tipoProcessamento, calculo.subTipoProcessamento)
                if (SubTipoProcessamento.COMPLEMENTAR.equals(calculo.subTipoProcessamento)) {
                    inssOutrosVinculos += Eventos.valorCalculadoMultiplosVinculos(evento.codigo, TipoValor.CALCULADO, calculo.tipoProcessamento, SubTipoProcessamento.INTEGRAL)
                }
                if (TipoProcessamento.MENSAL.equals(calculo.tipoProcessamento)) {
                    inssOutrosVinculos += Funcoes.getValorInssFeriasMultiplosVinculos()
                    inssOutrosVinculos += Eventos.valorCalculadoMultiplosVinculos(evento.codigo, TipoValor.CALCULADO, TipoProcessamento.RESCISAO, SubTipoProcessamento.INTEGRAL)
                    inssOutrosVinculos += Eventos.valorCalculadoMultiplosVinculos(evento.codigo, TipoValor.CALCULADO, TipoProcessamento.RESCISAO, SubTipoProcessamento.COMPLEMENTAR)
                }
                if (TipoProcessamento.RESCISAO.equals(calculo.tipoProcessamento)) {
                    inssOutrosVinculos += Funcoes.getValorInssFeriasMultiplosVinculos()
                    inssOutrosVinculos += Eventos.valorCalculadoMultiplosVinculos(evento.codigo, TipoValor.CALCULADO, TipoProcessamento.MENSAL, SubTipoProcessamento.INTEGRAL)
                    inssOutrosVinculos += Eventos.valorCalculadoMultiplosVinculos(evento.codigo, TipoValor.CALCULADO, TipoProcessamento.MENSAL, SubTipoProcessamento.COMPLEMENTAR)
                }
            }
            def vaux = Lancamentos.valor(evento)
            if (vaux >= 0) {
                valorReferencia = vaux
                valorCalculado = vaux
            } else {
                def baseint = baseprev
                def aux = Funcoes.afasadocao()
                def vmat = 0
                if (aux > 0) {
                    vmat = Eventos.valor(124)
                    baseint -= vmat
                }
                def max = EncargosSociais.RGPS.buscaMaior(1)
                if (baseint > max) {
                    vaux = baseint - max
                    Bases.compor(vaux, Bases.EXCEINSS)
                }
                vaux = base
                if (vaux > max) {
                    vaux = max
                }
                vaux -= baseInssOutraEmpresaAutonomo
                if (TipoMatricula.FUNCIONARIO.equals(matricula.tipo)) {
                    vaux2 = Numeros.trunca(vaux, 2)
                    if (funcionario.conselheiroTutelar) {
                        valorReferencia = 11
                    } else {
                        valorReferencia = EncargosSociais.RGPS.buscaContribuicao(vaux2, 2)
                    }
                } else {
                    if (TipoMatricula.ESTAGIARIO.equals(matricula.tipo)) {
                        valorReferencia = 11
                    } else {
                        if (EncargosSociaisFpas.ENTIDADE_BENEFICENTE.equals(EncargosSociais.codigoFpas) && (TipoMatricula.AUTONOMO.equals(matricula.tipo))) {
                            valorReferencia = 20
                        } else {
                            valorReferencia = EncargosSociais.RGPS.buscaMaior(2)
                            if (TipoMatricula.AUTONOMO.equals(matricula.tipo)) {
                                valorReferencia = 11
                            }
                        }
                    }
                }
                if (vmat > 0) {
                    base -= vmat
                    if (base > max) {
                        base = max
                    }
                    base -= baseInssOutraEmpresaAutonomo
                } else {
                    base = vaux
                }
                double inssfer = Funcoes.getInssFerias().valor
                double valorRetencaoInss
                if (TipoMatricula.AUTONOMO.equals(matricula.tipo) && autonomo.codESocial && autonomo.codESocial.startsWith('7')) {
                    base -= baseInssOutraEmpresaFuncionario
                } else {
                    valorRetencaoInss = Eventos.valor(169) + Eventos.valorCalculado(169, TipoValor.CALCULADO, TipoProcessamento.FERIAS, SubTipoProcessamento.INTEGRAL) + Funcoes.buscaValorDeOutrosProcessamentos(169)
                }
                if (Funcoes.inicioCompetencia() >= Datas.data(2020, 3, 1) && !TipoMatricula.ESTAGIARIO.equals(matricula.tipo) && !TipoMatricula.AUTONOMO.equals(matricula.tipo) && !funcionario.conselheiroTutelar) {
                    vaux = Funcoes.calculoProgressivoINSS(base)
                    valorCalculado = Numeros.arredonda(vaux, 2) - Numeros.arredonda(inssfer, 2) - inssOutrosVinculos - valorRetencaoInss
                } else {
                    vaux = (base * valorReferencia) / 100
                    valorCalculado = Numeros.trunca(vaux, 2) - Numeros.arredonda(inssfer, 2) - inssOutrosVinculos - valorRetencaoInss
                }
            }
            if (valorCalculado < 0) {
                Bases.compor(Numeros.absoluto(valorCalculado), Bases.DEVINSS)
            }
            Bases.compor(valorCalculado, Bases.DEDUCIRRFMES, Bases.ABATIRRF)
        }
    }
    if ((base != 0) || ((base == 0) && (diassd > 0) && (Funcoes.diastrab() == 0))) {
        Bases.compor(abatinss, Bases.ABATINSS)
    }
}
