package br.com.opensig.fiscal.server.sped.fiscal.bloco0;

import java.util.ArrayList;
import java.util.List;

import org.beanio.BeanWriter;
import org.beanio.StreamFactory;

import br.com.opensig.comercial.shared.modelo.ComCompra;
import br.com.opensig.comercial.shared.modelo.ComConsumo;
import br.com.opensig.comercial.shared.modelo.ComFrete;
import br.com.opensig.comercial.shared.modelo.ComVenda;
import br.com.opensig.core.server.UtilServer;
import br.com.opensig.empresa.shared.modelo.EmpEndereco;
import br.com.opensig.empresa.shared.modelo.EmpEntidade;
import br.com.opensig.fiscal.server.sped.ARegistro;

public class Registro0150 extends ARegistro<Dados0150, EmpEntidade> {

	private List<Integer> entidades;

	@Override
	public void executar() {
		entidades = new ArrayList<Integer>();

		try {
			StreamFactory factory = StreamFactory.newInstance();
			factory.load(getClass().getResourceAsStream(bean));
			BeanWriter out = factory.createWriter("EFD", escritor);
			// compras
			for (ComCompra compra : compras) {
				if (!entidades.contains(compra.getEmpFornecedor().getEmpEntidade().getEmpEntidadeId())) {
					out.write(getDados(compra.getEmpFornecedor().getEmpEntidade()));
					out.flush();
					entidades.add(compra.getEmpFornecedor().getEmpEntidade().getEmpEntidadeId());
				}
			}
			// fretes
			for (ComFrete frete : fretes) {
				if (!entidades.contains(frete.getEmpTransportadora().getEmpEntidade().getEmpEntidadeId())) {
					out.write(getDados(frete.getEmpTransportadora().getEmpEntidade()));
					out.flush();
					entidades.add(frete.getEmpTransportadora().getEmpEntidade().getEmpEntidadeId());
				}
			}
			// vendas
			for (ComVenda venda : vendas) {
				if (!venda.getComVendaCancelada()) {
					if (!entidades.contains(venda.getEmpCliente().getEmpEntidade().getEmpEntidadeId())) {
						out.write(getDados(venda.getEmpCliente().getEmpEntidade()));
						out.flush();
						entidades.add(venda.getEmpCliente().getEmpEntidade().getEmpEntidadeId());
					}
				}
			}
			// consumos
			for (ComConsumo consumo : consumos) {
				if (!entidades.contains(consumo.getEmpFornecedor().getEmpEntidade().getEmpEntidadeId())) {
					out.write(getDados(consumo.getEmpFornecedor().getEmpEntidade()));
					out.flush();
					entidades.add(consumo.getEmpFornecedor().getEmpEntidade().getEmpEntidadeId());
				}
			}
		} catch (Exception e) {
			UtilServer.LOG.error("Erro na geracao do Registro -> " + bean, e);
		}
	}

	@Override
	protected Dados0150 getDados(EmpEntidade ent) throws Exception {
		Dados0150 d = new Dados0150();
		d.setCod_part(ent.getEmpEntidadeId() + "");
		d.setNome(ent.getEmpEntidadeNome1());
		if (ent.getEmpEntidadeDocumento1().length() == 18) {
			d.setCnpj(ent.getEmpEntidadeDocumento1().replaceAll("\\D", ""));
			d.setIe(ent.getEmpEntidadeDocumento2().replaceAll("\\D", ""));
		} else {
			d.setCpf(ent.getEmpEntidadeDocumento1().replaceAll("\\D", ""));
		}
		d.setSuframa(auth.getConf().get("sped.0000.suframa"));

		for (EmpEndereco end : ent.getEmpEnderecos()) {
			d.setEnd(end.getEmpEnderecoLogradouro());
			d.setNum(end.getEmpEnderecoNumero());
			d.setCompl(end.getEmpEnderecoComplemento());
			d.setBairro(end.getEmpEnderecoBairro());
			d.setCod_mun(end.getEmpMunicipio().getEmpMunicipioIbge());
			d.setCod_pais(end.getEmpMunicipio().getEmpEstado().getEmpPais().getEmpPaisIbge());
			break;
		}

		normalizar(d);
		qtdLinhas++;
		return d;
	}
}
