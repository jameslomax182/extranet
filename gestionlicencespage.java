package com.siris.extranet.pmeSecurite.licenses;

import static com.siris.extranet.pmeSecurite.PmeSecuriteConstant.APPLICATION_CSS;
import static com.siris.extranet.pmeSecurite.PmeSecuriteConstant.BARRE_PROGRESSION_JS;
import static com.siris.extranet.pmeSecurite.PmeSecuriteConstant.MAX_REINIT_PWD_SESSION_ATTR;
import static com.siris.extranet.pmeSecurite.PmeSecuriteConstant.MSG_NBR_INIT_PWD_ATTEINT;
import static com.siris.extranet.pmeSecurite.PmeSecuriteProperties.getMaxAffichagePagination;
import static com.siris.extranet.pmeSecurite.PmeSecuriteProperties.getMaxReinitPwd;
import static com.siris.extranet.pmeSecurite.PmeSecuriteProperties.getMsgPopupInfoEffacer;
import static com.siris.extranet.pmeSecurite.PmeSecuriteProperties.getNomCommercial;
import static com.siris.extranet.pmeSecurite.PmeSecuriteProperties.getWsPmeSecuriteUrl;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.rpc.ServiceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.PageCreator;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.AppendingStringBuffer;

import com.siris.extranet.commun.common.exception.BusinessException;
import com.siris.extranet.commun.web.BasePage;
import com.siris.extranet.commun.web.ExtranetBasePage;
import com.siris.extranet.commun.web.PageName;
import com.siris.extranet.commun.web.WicketSession;
import com.siris.extranet.pmeSecurite.PmeSecuriteConstant;
import com.siris.extranet.pmeSecurite.PmeSecuriteProperties;
import com.siris.extranet.pmeSecurite.commun.ContractSelectionPanel;
import com.siris.extranet.pmeSecurite.commun.ErrorPage;
import com.siris.extranet.pmeSecurite.factory.CrmFactory;
import com.siris.extranet.pmeSecurite.factory.DemandeFactory;
import com.siris.extranet.pmeSecurite.model.Demande;
import com.siris.extranet.pmeSecurite.utils.GenericSortableDataProvider;
import com.siris.extranet.wsclient.pmeSecurite.ArrayOfBusinessLicenseListNodeResponseResultinfobusinesslicenselistBusinesslicenseBusinessLicense;
import com.siris.extranet.wsclient.pmeSecurite.BusinessLicenseListNodeResponse;
import com.siris.extranet.wsclient.pmeSecurite.BusinessLicenseNodeResponse;
import com.siris.extranet.wsclient.pmeSecurite.ForticlientInfoNodeResponse;
import com.siris.extranet.wsclient.pmeSecurite.PmeSecuriteException;
import com.siris.extranet.wsclient.pmeSecurite.WSCaller;

@SuppressWarnings("serial")
public class GestionLicencesPage extends ExtranetBasePage {

    private static Log log = LogFactory.getLog(GestionLicencesPage.class);

    private List<Demande> demandeList;
    private ModalWindow modalWindow;
    private WebMarkupContainer demandeListDiv;
    private WebMarkupContainer wmc;
    private WebMarkupContainer tbodyLicenceList;
    private String numContrat;

    @Override
    public void renderHead(IHeaderResponse response) {
        response.renderJavaScriptReference(BARRE_PROGRESSION_JS);
        response.renderCSSReference(APPLICATION_CSS);
    }

    public GestionLicencesPage() {
        try {
            numContrat = null;
            boolean isContractPanel = true;
            List<String> contractList = null;
            CrmFactory crmFactory = new CrmFactory();
            contractList = crmFactory.getDeviceContractList(getUser().getIdClient());
            // verifier s'il a plusieur contrat
            if (contractList != null && contractList.size() == 1) {
                numContrat = contractList.get(0);
                isContractPanel = false;
            }
            Panel contractSelectionPanel = new ContractSelectionPanel("contractSelectionPanel", contractList) {

                @Override
                public void submit(AjaxRequestTarget target, String selectedContract) {
                    if(selectedContract != null && !selectedContract.isEmpty()) {
                        wmc.setVisible(true);
                        numContrat = selectedContract;
                        target.add(wmc);
                    }
                }
                
            };
            contractSelectionPanel.setVisible(isContractPanel);
            add(contractSelectionPanel);
            wmc = new WebMarkupContainer("mainPanel");
            wmc.setOutputMarkupPlaceholderTag(true);
            wmc.setVisible(!isContractPanel);
            add(wmc);
            // ajouter les composant du mainPanel s'il sera afficher
            if (!isContractPanel) {
                addMainPanelComponent(numContrat);
            }
        } catch (Exception e) {
            log.error("Exception ==> " + e.toString(), e);
      throw new RestartResponseException(ErrorPage.class, new PageParameters().set(PmeSecuriteConstant.ERROR_KEY, e.toString()));
        }

        pageName = PageName.SERVICES_ENT_S�CU_SAUV_PACK_DEVICE_LICENCES;
    }

    /**
     * 
     * @param wmc
     * @throws PmeSecuriteException
     * @throws ServiceException
     * @throws RemoteException
     * @throws MalformedURLException
     * @throws BusinessException
     */
    private void addMainPanelComponent(String contractId) throws MalformedURLException, RemoteException, ServiceException, PmeSecuriteException,
            BusinessException {
        wmc.add(new Label("numContrat", contractId));
        wmc.add(new AjaxLink<String>("popupProfil") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                modalWindow.setPageCreator(new PopupProfilPage());
                modalWindow.show(target);
            }
        });
        // tableau des licence
        BusinessLicenseListNodeResponse businessLicenseListNode = WSCaller.DPMListBusinessLicences(getWsPmeSecuriteUrl(), getUser().getIdClient(), contractId);
        ArrayOfBusinessLicenseListNodeResponseResultinfobusinesslicenselistBusinesslicenseBusinessLicense[] businessLicenseList = businessLicenseListNode
                .getResultInfoBusinessLicenseList().getListBusinessLicense();
        List<ArrayOfBusinessLicenseListNodeResponseResultinfobusinesslicenselistBusinesslicenseBusinessLicense> licenceList = Arrays
                .asList(businessLicenseList);
        ListView<ArrayOfBusinessLicenseListNodeResponseResultinfobusinesslicenselistBusinesslicenseBusinessLicense> licenceListDataView = new ListView<ArrayOfBusinessLicenseListNodeResponseResultinfobusinesslicenselistBusinesslicenseBusinessLicense>(
                "licenceList", licenceList) {
            @Override
            protected void populateItem(final ListItem<ArrayOfBusinessLicenseListNodeResponseResultinfobusinesslicenselistBusinesslicenseBusinessLicense> item) {
                int seatsLimit = (item.getModelObject().getSeatsLimit() != null ? Integer.parseInt(item.getModelObject().getSeatsLimit()) : 0);
                int seatsUsage = (item.getModelObject().getSeatsUsage() != null ? Integer.parseInt(item.getModelObject().getSeatsUsage()) : 0);
                item.add(new Label("profile", getNomCommercial(item.getModelObject().getProfile())));
                item.add(new Label("licenseKey", item.getModelObject().getLicenseKey()));
                item.add(new Label("seatsLimit", "" + seatsLimit));
                item.add(new Label("seatsUsage", "" + seatsUsage));
                Label seatsDispo = new Label("seatsDispo", "" + (seatsLimit - seatsUsage));
                // mettre en rouge s'il ne reste qu'un siege ou s'il reste moins
                // de 20%
                double pctUsage = (1.0 * seatsUsage / seatsLimit);
                if ((pctUsage >= 0.8 || (seatsLimit - seatsUsage) == 1)) {
                    seatsDispo.add(new AttributeModifier("bgColor", new Model<String>("red")));
                }
                item.add(seatsDispo);
                item.add(new AjaxLink<String>("visualisationButton", new Model<String>(item.getModelObject().getPassword())) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        try {
                            BusinessLicenseNodeResponse response = WSCaller.DPMDisplayClientLicenseAndChangePassword(getWsPmeSecuriteUrl(), getUser()
                                    .getIdClient(), numContrat, item.getModelObject().getLicenseName());
                            modalWindow.setPageCreator(new VisualisationPage(getModel()));
                            modalWindow.show(target);
                        } catch (Exception e) {
                            log.error("Exception ==> " + e.toString(), e);
                  throw new RestartResponseException(ErrorPage.class, new PageParameters().set(PmeSecuriteConstant.ERROR_KEY, e.toString()));
                        }
                    }
                });
                item.add(new AjaxLink<ArrayOfBusinessLicenseListNodeResponseResultinfobusinesslicenselistBusinesslicenseBusinessLicense>(
                        "reinitialisationButton", new Model(item.getModelObject())) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        modalWindow.setPageCreator(new ReinitialisationPage(item));
                        modalWindow.show(target);
                    }
                });
                // item.setOutputMarkupId(true) ;
                addPairImpairCss(item);
            }
        };
        licenceListDataView.setOutputMarkupId(true);
        tbodyLicenceList = new WebMarkupContainer("tbodyLicenceList");
        tbodyLicenceList.setOutputMarkupId(true);
        tbodyLicenceList.add(licenceListDataView);
        wmc.add(tbodyLicenceList);

        // add modalWindow
         modalWindow = new ModalWindow("modalWindow") {
          protected AppendingStringBuffer postProcessSettings(
              final AppendingStringBuffer settings) {
            settings.append("settings.ie8_src=\"/extranet/servlet/PmeSecuriteServlet/\" + settings.ie8_src;\n");
            return settings;
          }
        };
        modalWindow.setResizable(false);
        modalWindow.setInitialHeight(220);
        modalWindow.setInitialWidth(520);
        modalWindow.setCssClassName(ModalWindow.CSS_CLASS_GRAY);

        tbodyLicenceList.add(modalWindow);

        // tableau des demandes
        DemandeFactory factory = new DemandeFactory();
        demandeList = factory.getDemandeList(getUser().getIdClient(), contractId);
        // conteneur pour pouvoir faire des appel AJAX (supression de ligne)
        demandeListDiv = new WebMarkupContainer("demandeListDiv");
        demandeListDiv.setOutputMarkupId(true);
        wmc.add(demandeListDiv);
        // ne pas afficher le tableau s'il est vide
        demandeListDiv.setVisible(demandeList != null && demandeList.size() > 0);
        List<IColumn<Demande>> columns = new ArrayList<IColumn<Demande>>();
        columns.add(new PropertyColumn<Demande>(new Model<String>("Adresse mail"), "mail", "mail"));
        columns.add(new AbstractColumn<Demande>(new Model<String>("Profil de s�curit�")) {
            @Override
            public void populateItem(Item<ICellPopulator<Demande>> cellItem, String componentId, IModel<Demande> rowModel) {
                cellItem.add(new Label(componentId, getNomCommercial(rowModel.getObject().getProfil())));
            }
        });
        columns.add(new PropertyColumn<Demande>(new Model<String>("Date affectation par administrateur"), "dateDemande", "dateDemande"));
        columns.add(new AbstractColumn<Demande>(new Model<String>("Action")) {
            @Override
            public Component getHeader(String componentId) {
                // return super.getHeader(componentId);
                return new ActionHeaderFragment(componentId, getDisplayModel());
            }

            @Override
            public void populateItem(Item<ICellPopulator<Demande>> cellItem, String componentId, IModel<Demande> rowModel) {
                cellItem.add(new DelDemandeFragment(componentId, "delDemandeFragment", demandeListDiv, rowModel));
            }
        });
        AjaxFallbackDefaultDataTable<Demande> demandeListTable = new AjaxFallbackDefaultDataTable<Demande>("demandeList", columns,
                new GenericSortableDataProvider<Demande>(demandeList), getMaxAffichagePagination()) {
            @Override
            protected Item<Demande> newRowItem(String id, int index, IModel<Demande> model) {
                Item<Demande> item = super.newRowItem(id, index, model);
                item.add(new AttributeModifier("class", new Model<String>((item.getIndex() % 2 == 0 ? "lignePair" : "ligneImpair"))));
                return item;
            }
        };
        demandeListTable.setDefaultModel(new PropertyModel<List<Demande>>(this, "demandeList"));
        demandeListDiv.add(demandeListTable);
        // ajouter le formulaire
        // System.out.print("\n \n :"+clientLicence.size()) ;
        List<String> clientLicence = new ArrayList<String>();
        for (ArrayOfBusinessLicenseListNodeResponseResultinfobusinesslicenselistBusinesslicenseBusinessLicense businessLiscence : businessLicenseList) {
            clientLicence.add(businessLiscence.getLicenseKey());
        }
        wmc.add(new GestionLicencesForm("fortiClientForm", contractId, clientLicence));
    }

    private class VisualisationPage extends BasePage implements PageCreator {
        
        @Override
        public void renderHead(IHeaderResponse response) {
          response.renderCSSReference(APPLICATION_CSS);
        }

        public VisualisationPage(IModel<String> model) {
            add(new AjaxLink<String>("closeButton") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    modalWindow.close(target);
                }
            });
            // input de la cl�
            add(new Label("licenceKey", model));
        }

        @Override
        public Page createPage() {
            return this;
        }
    }

    public class DetailForticlientPage extends ExtranetBasePage implements PageCreator {

        @Override
        public void renderHead(IHeaderResponse response) {
          response.renderCSSReference(APPLICATION_CSS);
        }

        public DetailForticlientPage(String societeId, String contratId, String hostnameFc) {
            add(new AjaxLink<String>("closeButton") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    modalWindow.close(target);
                }
            });
            try {
                ForticlientInfoNodeResponse forticlientInfo = WSCaller.DPMDisplayFortiClientDevice(PmeSecuriteProperties.getWsPmeSecuriteUrl(), societeId,
                        contratId, hostnameFc);
                add(new Label("hostname", hostnameFc));
                add(new Label("avEngineVer", forticlientInfo.getResultInfoForticlientInfo().getAvEngineVer()));
                add(new Label("lastConnection", forticlientInfo.getResultInfoForticlientInfo().getLastConnection()));
            } catch (Exception e) {
                log.error("Exception ==> " + e.toString(), e);
        throw new RestartResponseException(ErrorPage.class, new PageParameters().set(PmeSecuriteConstant.ERROR_KEY, e.toString()));
            }
        }

        @Override
        public Page createPage() {
            return this;
        }
    }

    class PopupInfoPage extends BasePage implements PageCreator {

        @Override
        public void renderHead(IHeaderResponse response) {
          response.renderCSSReference(APPLICATION_CSS);
        }

        public PopupInfoPage(String message) {
            add(new AjaxLink<String>("closeButton") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    modalWindow.close(target);
                }
            });
            add(new Label("message", message));
        }

        @Override
        public Page createPage() {
            return this;
        }
    }

    class PopupProfilPage extends BasePage implements PageCreator {

        @Override
        public void renderHead(IHeaderResponse response) {
          response.renderCSSReference(APPLICATION_CSS);
        }

        public PopupProfilPage() {
            add(new AjaxLink<String>("closeButton") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    modalWindow.close(target);
                }
            });
        }

        @Override
        public Page createPage() {
            return this;
        }
    }

    private class ReinitialisationPage extends BasePage implements PageCreator {

        @Override
        public void renderHead(IHeaderResponse response) {
            response.renderJavaScriptReference(BARRE_PROGRESSION_JS);
            response.renderCSSReference(APPLICATION_CSS);
        }

        private FeedbackPanel feedbackPanel;

        public ReinitialisationPage(final ListItem<ArrayOfBusinessLicenseListNodeResponseResultinfobusinesslicenselistBusinesslicenseBusinessLicense> item) {
            // profil
            add(new Label("profil", getNomCommercial(item.getModelObject().getProfile())));
            // bouton annuler
            final AjaxLink<String> annulerButton = new AjaxLink<String>("annulerButton") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    modalWindow.close(target);
                }
            };
            add(annulerButton);
            // bouton confirm
            final AjaxLink<String> confirmButton = new AjaxLink<String>("confirmButton", new Model<String>(item.getModelObject().getProfile())) {
                @SuppressWarnings("unchecked")
                @Override
                public void onClick(AjaxRequestTarget target) {
                    if (target != null) {
                        Map<String, Integer> nbrInitPwdMap = (Map<String, Integer>) WicketSession.getHttpSession().getAttribute(MAX_REINIT_PWD_SESSION_ATTR);
                        // initialiser la map session
                        if (nbrInitPwdMap == null) {
                            nbrInitPwdMap = new HashMap<String, Integer>();
                        }
                        // initialiser l'entr�e pour le profil s�l�ctionn�
                        if (nbrInitPwdMap.get(item.getModelObject().getProfile()) == null) {
                            nbrInitPwdMap.put(item.getModelObject().getProfile(), 0);
                        }
                        if (nbrInitPwdMap.get(item.getModelObject().getProfile()) < getMaxReinitPwd()) {
                            try {
                                BusinessLicenseNodeResponse response = WSCaller.DPMChangePasswordClientLicense(getWsPmeSecuriteUrl(), getUser().getIdClient(),
                                        numContrat, item.getModelObject().getLicenseName());
                                item.getModelObject().setPassword(response.getResultInfoBusinessLicense().getListBusinessLicense()[0].getPassword());
                                target.add(tbodyLicenceList);
                                // mettre � jour les donn�es session
                                nbrInitPwdMap.put(item.getModelObject().getProfile(), (nbrInitPwdMap.get(item.getModelObject().getProfile()) + 1));
                                WicketSession.getHttpSession().setAttribute(MAX_REINIT_PWD_SESSION_ATTR, nbrInitPwdMap);
                                modalWindow.close(target);
                            } catch (Exception e) {
                                log.error("Exception ==> " + e.toString(), e);
                      throw new RestartResponseException(ErrorPage.class, new PageParameters().set(PmeSecuriteConstant.ERROR_KEY, e.toString()));
                            } finally {
                                target.add(feedbackPanel);
                            }
                        } else {
                            error(new StringResourceModel(MSG_NBR_INIT_PWD_ATTEINT, this, null).getString());
                            target.add(feedbackPanel);
                        }
                    }
                }

                protected IAjaxCallDecorator getAjaxCallDecorator() {

                    return new AjaxCallDecorator() {
                        @Override
                        public CharSequence decorateScript(Component c, CharSequence script) {
                            int nbrInit = 0;
                            Map<String, Integer> nbrInitPwdMap = (Map<String, Integer>) WicketSession.getHttpSession().getAttribute(MAX_REINIT_PWD_SESSION_ATTR);
                            if (nbrInitPwdMap != null && nbrInitPwdMap.get(getModelObject()) != null) {
                                nbrInit = nbrInitPwdMap.get(getModelObject());
                            }

                            return "checkNbrInit('" + nbrInit + "', '" + getMaxReinitPwd() + "');" + script;
                        }
                    };
                };
            };
            add(confirmButton);
            // feedBack
            feedbackPanel = new FeedbackPanel("feedbackPanel");
            feedbackPanel.setOutputMarkupId(true);
            add(feedbackPanel);
        }

        @Override
        public Page createPage() {
            return this;
        }
    }

    private void addPairImpairCss(ListItem item) {
        item.add(new AttributeModifier("class", new Model<String>((item.getIndex() % 2 == 0 ? "lignePair" : "ligneImpair"))));
    }

    private class DelDemandeFragment extends Fragment {

        public DelDemandeFragment(String id, String markupId, MarkupContainer markupProvider, IModel<Demande> model) {
            super(id, markupId, markupProvider);
            add(new AjaxLink<Demande>("delDemandeButton", model) {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    Demande demande = getModelObject();
                    DemandeFactory factory = new DemandeFactory();
                    if (log.isDebugEnabled()) {
                        log.debug("Suppression de la demande : " + demande);
                    }
                    try {
                        factory.deleteDemande(demande);
                        // supprimer la ligne HTML
                        demandeList.remove(demande);
                        demandeListDiv.setVisible(demandeList.size() > 0);
                        target.add(demandeListDiv);
                    } catch (BusinessException e) {
                        log.error("Exception ==> " + e.toString(), e);
              throw new RestartResponseException(ErrorPage.class, new PageParameters().set(PmeSecuriteConstant.ERROR_KEY, e.toString()));
                    }
                }
            });
        }
    }

    private class ActionHeaderFragment extends Fragment {
        public ActionHeaderFragment(String id, IModel<String> displayModel) {
            super(id, "actionHeaderFragment", demandeListDiv);
            add(new Label("actionLabel", displayModel));
            add(new AjaxLink<String>("popupAction") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    modalWindow.setPageCreator(new PopupInfoPage(getMsgPopupInfoEffacer()));
                    modalWindow.show(target);
                }
            });
        }
    }
}
