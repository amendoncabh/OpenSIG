package br.com.opensig.comercial.client.controlador.comando.acao;

import java.util.Map;

import br.com.opensig.comercial.shared.modelo.ComConsumo;
import br.com.opensig.core.client.OpenSigCore;
import br.com.opensig.core.client.UtilClient;
import br.com.opensig.core.client.controlador.comando.AComando;
import br.com.opensig.core.client.controlador.comando.ComandoAcao;
import br.com.opensig.core.client.controlador.comando.ComandoExecutar;
import br.com.opensig.core.client.controlador.comando.ComandoFormulario;
import br.com.opensig.core.client.controlador.comando.IComando;
import br.com.opensig.core.client.controlador.filtro.ECompara;
import br.com.opensig.core.client.controlador.filtro.FiltroNumero;
import br.com.opensig.core.client.controlador.parametro.GrupoParametro;
import br.com.opensig.core.client.controlador.parametro.IParametro;
import br.com.opensig.core.client.controlador.parametro.ParametroBinario;
import br.com.opensig.core.client.controlador.parametro.ParametroObjeto;
import br.com.opensig.core.shared.modelo.EComando;
import br.com.opensig.core.shared.modelo.Sql;
import br.com.opensig.financeiro.client.visao.form.FormularioPagar;
import br.com.opensig.financeiro.shared.modelo.FinPagar;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.gwtext.client.data.Record;
import com.gwtext.client.widgets.Component;
import com.gwtext.client.widgets.MessageBox;
import com.gwtext.client.widgets.form.event.FormPanelListenerAdapter;

public class ComandoPagarConsumo extends ComandoAcao<ComConsumo> {

	private int consumoId;
	private AsyncCallback async;
	private FinPagar pagar;
	private IComando validaPagar;

	public ComandoPagarConsumo() {
		// finalizando
		AComando cmdFim = new AComando() {
			public void execute(Map contexto) {
				super.execute(contexto);
				Record rec = LISTA.getPanel().getSelectionModel().getSelected();
				rec.set("comConsumoPaga", true);
				rec.set("finPagar.finPagarId", pagar.getFinPagarId());
			}
		};
		// salvando o consumo com o status paga
		final ComandoExecutar<ComConsumo> cmdConsumo = new ComandoExecutar<ComConsumo>(cmdFim) {
			public void execute(Map contexto) {
				FiltroNumero fn = new FiltroNumero("comConsumoId", ECompara.IGUAL, consumoId);
				ParametroObjeto po = new ParametroObjeto("finPagar", pagar);
				ParametroBinario pb = new ParametroBinario("comConsumoPaga", 1);
				GrupoParametro gp = new GrupoParametro(new IParametro[] { po, pb });

				Sql sql = new Sql(new ComConsumo(consumoId), EComando.ATUALIZAR, fn, gp);
				setSqls(new Sql[] { sql });
				super.execute(contexto);
			}
		};
		async = new AsyncCallback() {
			public void onFailure(Throwable arg0) {
				MessageBox.alert(OpenSigCore.i18n.txtSalvar(), OpenSigCore.i18n.errSalvar());
			}

			public void onSuccess(Object arg0) {
				try {
					pagar = (FinPagar) arg0;
					if (pagar.getFinPagarId() > 0) {
						cmdConsumo.execute(contexto);
					} else {
						onFailure(null);
					}
				} catch (Exception e) {
					onFailure(e);
				}
			}
		};
		// validando pagar
	    validaPagar = new AComando() {
			public void execute(final Map contexto) {
				super.execute(contexto);
				Record rec = LISTA.getPanel().getSelectionModel().getSelected();

				if (rec.getAsBoolean("comConsumoFechada") && !rec.getAsBoolean("comConsumoPaga")) {
					consumoId = rec.getAsInteger("comConsumoId");
					abrirFinanceiro();
				} else {
					MessageBox.alert(OpenSigCore.i18n.txtAcesso(), OpenSigCore.i18n.txtAcessoNegado());
				}
			}
		};
	}

	/**
	 * @see ComandoAcao#execute(Map)
	 */
	public void execute(final Map contexto) {
		super.execute(contexto, new AsyncCallback() {
			public void onSuccess(Object result) {
				validaPagar.execute(contexto);
			}

			public void onFailure(Throwable caught) {
			}
		});
	}
	
	private void abrirFinanceiro() {
		final Record rec = LISTA.getPanel().getSelectionModel().getSelected();
		final FormularioPagar formPagar = new FormularioPagar(FORM.getFuncao());
		formPagar.setTitle(OpenSigCore.i18n.txtFinanceiro());
		
		formPagar.addListener(new FormPanelListenerAdapter() {
			public void onRender(Component component) {
				int empresaId = rec.getAsInteger("empEmpresa.empEmpresaId");
				formPagar.getHdnEmpresa().setValue(empresaId + "");
				formPagar.getHdnEntidade().setValue(rec.getAsString("empFornecedor.empEntidade.empEntidadeId"));
				formPagar.getCmbEntidade().setValue(rec.getAsString("empFornecedor.empEntidade.empEntidadeNome1"));
				formPagar.getCmbEntidade().disable();
				formPagar.getTxtNfe().setValue(rec.getAsInteger("comConsumoDocumento"));
				formPagar.getTxtNfe().disable();
				formPagar.getTxtValor().setValue(rec.getAsDouble("comConsumoValor"));
				String data = DateTimeFormat.getFormat(PredefinedFormat.DATE_MEDIUM).format(rec.getAsDate("comConsumoData"));
				formPagar.getDtCadastro().setValue(data);
				formPagar.getDtCadastro().disable();
				formPagar.mostrarDados();
				
				Scheduler.get().scheduleFixedPeriod(new RepeatingCommand() {
					public boolean execute() {
						if (formPagar.getTreeCategoria().getRoot().getChildNodes().length > 0) {
							formPagar.getTreeCategoria().selecionar(new String[] {UtilClient.CONF.get("categoria.padrao")});
							formPagar.getTreeCategoria().disable();
							return false;
						} else {
							return true;
						}
					}
				}, 1000);
			}
		});

		new ComandoFormulario(formPagar, async).execute(formPagar.getContexto());
	}
}
