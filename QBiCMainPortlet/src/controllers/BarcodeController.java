///*******************************************************************************
// * QBiC Project qNavigator enables users to manage their projects. Copyright (C) "2016” Christopher
// * Mohr, David Wojnar, Andreas Friedrich
// *
// * This program is free software: you can redistribute it and/or modify it under the terms of the
// * GNU General Public License as published by the Free Software Foundation, either version 3 of the
// * License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
// * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// * General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License along with this program. If
// * not, see <http://www.gnu.org/licenses/>.
// *******************************************************************************/
//package controllers;
//
//import helpers.SheetBarcodesReadyRunnable;
//import helpers.TubeBarcodesReadyRunnable;
//
//import java.io.Serializable;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//
//import org.apache.commons.lang.StringUtils;
//
//import qbic.vaadincomponents.BarcodePreviewComponent;
//
//import logging.Log4j2Logger;
//import main.BarcodeCreator;
//import model.ExperimentBarcodeSummaryBean;
//import model.IBarcodeBean;
//import model.NewModelBarcodeBean;
//
//import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Experiment;
//import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Project;
//import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
//import ch.systemsx.cisd.openbis.plugin.query.shared.api.v1.dto.QueryTableModel;
//
//import com.vaadin.data.Property.ValueChangeEvent;
//import com.vaadin.data.Property.ValueChangeListener;
//import com.vaadin.server.Extension;
//import com.vaadin.ui.Button;
//import com.vaadin.ui.ProgressBar;
//import com.vaadin.ui.Button.ClickEvent;
//import com.vaadin.ui.ComboBox;
//import com.vaadin.ui.TabSheet.SelectedTabChangeEvent;
//import com.vaadin.ui.TabSheet.SelectedTabChangeListener;
//
//import de.uni_tuebingen.qbic.qbicmainportlet.WizardBarcodeView;
//import functions.Functions;
//
///**
// * Controls preparation and creation of barcode files
// * 
// * @author Andreas Friedrich
// * 
// */
//
//public class BarcodeController {
//
//  private WizardBarcodeView view;
//  private OpenBisClient openbis;
//  private BarcodeCreator creator;
//
//  List<IBarcodeBean> barcodeBeans;
//
//  logging.Logger logger = new Log4j2Logger(BarcodeController.class);
//
//  private List<String> barcodeExperiments =
//      new ArrayList<String>(Arrays.asList("Q_SAMPLE_EXTRACTION", "Q_SAMPLE_PREPARATION",
//          "Q_NGS_MEASUREMENT", "Q_MS_MEASUREMENT"));
//  private Map<String, String> barcodeSamplesWithTypes = new HashMap<String, String>() {
//    {
//      put("Q_BIOLOGICAL_SAMPLE", "Q_PRIMARY_TISSUE");
//      put("Q_TEST_SAMPLE", "Q_SAMPLE_TYPE");
//      put("Q_NGS_SINGLE_SAMPLE_RUN", "");
//      put("Q_MS_RUN", "");
//    }
//  };
//
//  /**
//   * @param bw WizardBarcodeView instance
//   * @param openbis OpenBisClient API
//   * @param barcodeScripts Path to different barcode creation scripts
//   * @param pathVar Path variable so python scripts can work when called from the JVM
//   */
//  public BarcodeController(WizardBarcodeView bw, OpenBisClient openbis, String barcodeScripts,
//      String pathVar) {
//    // view = bw;
//    this.openbis = openbis;
//    creator = new BarcodeCreator(barcodeScripts, pathVar);
//  }
//
//  public BarcodeController(OpenBisClient openbis, String barcodeScripts, String pathVar) {
//    this.openbis = openbis;
//    creator = new BarcodeCreator(barcodeScripts, pathVar);
//  }
//
//  /**
//   * Initializes all listeners
//   */
//  @SuppressWarnings("serial")
//  public void init(WizardBarcodeView bw) {
//    view = bw;
//
//    /**
//     * Button listeners
//     */
//    Button.ClickListener cl = new Button.ClickListener() {
//      @Override
//      public void buttonClick(ClickEvent event) {
//        String src = event.getButton().getCaption();
//        if (src.equals("Prepare Barcodes")) {
//          view.creationPressed();
//          Iterator<Extension> it = view.getDownloadButton().getExtensions().iterator();
//          if (it.hasNext())
//            view.getDownloadButton().removeExtension(it.next());
//          barcodeBeans = getSamplesFromExperimentSummaries(view.getExperiments());
//          // Collection<String> options = (Collection<String>) view.getPrepOptionGroup().getValue();
//          boolean overwrite = view.getOverwrite();
//          String project = view.getProjectCode();
//          ProgressBar bar = view.getProgressBar();
//          bar.setVisible(true);
//          if (view.getTabs().getSelectedTab() instanceof BarcodePreviewComponent) {
//            logger.info("Preparing barcodes (tubes) for project " + project);
//            creator.findOrCreateTubeBarcodesWithProgress(barcodeBeans, bar, view.getProgressInfo(),
//                new TubeBarcodesReadyRunnable(view, creator, barcodeBeans), overwrite);
//          } else {
//            logger.info("Preparing barcodes (sheet) for project " + project);
//            creator.findOrCreateSheetBarcodesWithProgress(barcodeBeans, bar, view.getProgressInfo(),
//                new SheetBarcodesReadyRunnable(view, creator, barcodeBeans));
//          }
//        }
//      }
//    };
//    for (Button b : view.getButtons())
//      b.addClickListener(cl);
//
//    /**
//     * Space selection listener
//     */
//    ValueChangeListener spaceSelectListener = new ValueChangeListener() {
//
//      @Override
//      public void valueChange(ValueChangeEvent event) {
//        view.resetProjects();
//        String space = view.getSpaceCode();
//        if (space != null) {
//          List<String> projects = new ArrayList<String>();
//          for (Project p : openbis.getProjectsOfSpace(space)) {
//            projects.add(p.getCode());
//          }
//          view.setProjectCodes(projects);
//        }
//      }
//
//    };
//    ComboBox space = view.getSpaceBox();
//    if (space != null)
//      space.addValueChangeListener(spaceSelectListener);
//
//    /**
//     * Project selection listener
//     */
//
//    ValueChangeListener projectSelectListener = new ValueChangeListener() {
//
//      @Override
//      public void valueChange(ValueChangeEvent event) {
//        view.resetExperiments();
//        String project = view.getProjectCode();
//        if (project != null) {
//          reactToProjectSelection(project);
//        }
//      }
//
//    };
//    ComboBox project = view.getProjectBox();
//    if (project != null)
//      project.addValueChangeListener(projectSelectListener);
//
//    /**
//     * Experiment selection listener
//     */
//
//    ValueChangeListener expSelectListener = new ValueChangeListener() {
//
//      @Override
//      public void valueChange(ValueChangeEvent event) {
//        barcodeBeans = null;
//        view.reset();
//        view.enablePrep(expSelected());// && optionSelected());
//        if (expSelected() && tubesSelected())
//          view.enablePreview(getUsefulSampleFromExperiment());
//      }
//    };
//    view.getExperimentTable().addValueChangeListener(expSelectListener);
//
//    SelectedTabChangeListener tabListener = new SelectedTabChangeListener() {
//      @Override
//      public void selectedTabChange(SelectedTabChangeEvent event) {
//        view.reset();
//        view.enablePrep(expSelected());
//        if (tubesSelected() && expSelected())
//          view.enablePreview(getUsefulSampleFromExperiment());
//        else
//          view.disablePreview();
//      }
//    };
//    view.getTabs().addSelectedTabChangeListener(tabListener);
//
//  }
//
//  public void reactToProjectSelection(String project) {
//    List<ExperimentBarcodeSummaryBean> beans = new ArrayList<ExperimentBarcodeSummaryBean>();
//    for (Experiment e : openbis.getExperimentsOfProjectByCode(project)) {
//      String type = e.getExperimentTypeCode();
//      List<Sample> samples = openbis.getSamplesofExperiment(e.getIdentifier());
//      if (samples.size() > 0 && correctType(e, samples)) {
//        String expID = e.getIdentifier();
//        List<String> ids = new ArrayList<String>();
//        for (Sample s : samples) {
//          if (Functions.isQbicBarcode(s.getCode()))
//            ids.add(s.getCode());
//        }
//        int numOfSamples = ids.size();
//        String bioType = null;
//        int i = 0;
//        if (type.equals(barcodeExperiments.get(0))) {
//          while (bioType == null) {
//            bioType = samples.get(i).getProperties().get("Q_PRIMARY_TISSUE");
//            i++;
//          }
//        }
//        if (type.equals(barcodeExperiments.get(1))) {
//          while (bioType == null) {
//            bioType = samples.get(i).getProperties().get("Q_SAMPLE_TYPE");
//            i++;
//          }
//        }
//        if (type.equals(barcodeExperiments.get(2))) {
//          bioType = e.getProperties().get("Q_SEQUENCING_TYPE") + "seq";
//        }
//        if (type.equals(barcodeExperiments.get(3))) {
//          bioType = "Wash";
//        }
//        beans.add(new ExperimentBarcodeSummaryBean(bioType, Integer.toString(numOfSamples), expID));
//      }
//    }
//    view.setExperiments(beans);
//  }
//
//  private boolean correctType(Experiment e, List<Sample> samples) {
//    String type = e.getExperimentTypeCode();
//    // type may be barcode
//    if (barcodeExperiments.contains(type)) {
//      // test if wash run, otherwise barcodes for protein/peptide samples are needed
//      if (type.equals("Q_MS_MEASUREMENT")) {
//        for (Sample s : samples) {
//          // if one sample has parents it's not a wash run experiment, no need to show it
//          if (!s.getParents().isEmpty())
//            return false;
//        }
//        // wash run experiment -> show
//        return true;
//      }
//      // other applicable type
//      return true;
//    }
//    // not a barcode type
//    return false;
//  }
//
//  private Sample getUsefulSampleFromExperiment() {
//    List<Sample> samples =
//        openbis.getSamplesofExperiment(view.getExperiments().iterator().next().fetchExperimentID());
//    int i = 0;
//    String code = samples.get(i).getCode();
//    while (!Functions.isQbicBarcode(code)) {
//      code = samples.get(i).getCode();
//      i++;
//    }
//    return samples.get(i);
//  }
//
//  private boolean tubesSelected() {
//    return view.getTabs().getSelectedTab() instanceof BarcodePreviewComponent;
//    // return ((Collection<String>) view.getPrepOptionGroup().getValue()) != null;// TODO
//    // .contains("Sample Tube Barcodes");
//  }
//
//  private boolean expSelected() {
//    return view.getExperiments().size() > 0;
//  }
//
//  protected List<IBarcodeBean> getSamplesFromExperimentSummaries(
//      Collection<ExperimentBarcodeSummaryBean> experiments) {
//    List<IBarcodeBean> samples = new ArrayList<IBarcodeBean>();
//    List<Sample> openbisSamples = new ArrayList<Sample>();
//    for (ExperimentBarcodeSummaryBean b : experiments) {
//      openbisSamples.addAll(openbis.getSamplesofExperiment(b.fetchExperimentID()));
//    }
//    Map<Sample, List<String>> parentMap = getParentMap(openbisSamples);
//    for (Sample s : openbisSamples) {
//      String type = s.getSampleTypeCode();
//      String bioType = "unknown";
//      if (barcodeSamplesWithTypes.containsKey(type)) {
//        if (barcodeSamplesWithTypes.get(type).isEmpty())
//          bioType = "NGS RUN";
//        else
//          bioType = s.getProperties().get(barcodeSamplesWithTypes.get(type));
//        samples.add(new NewModelBarcodeBean(s.getCode(), view.getCodedString(s),
//            view.getInfo1(s, StringUtils.join(parentMap.get(s), " ")),
//            view.getInfo2(s, StringUtils.join(parentMap.get(s), " ")), bioType, parentMap.get(s),
//            s.getProperties().get("Q_SECONDARY_NAME"), s.getProperties().get("Q_EXTERNALDB_ID")));
//      }
//    }
//    return samples;
//  }
//
//  protected Map<Sample, List<String>> getParentMap(List<Sample> samples) {
//    List<String> codes = new ArrayList<String>();
//    for (Sample s : samples) {
//      codes.add(s.getCode());
//    }
//    Map<String, Object> params = new HashMap<String, Object>();
//    params.put("codes", codes);
//    QueryTableModel resTable = openbis.getAggregationService("get-parentmap", params);
//    Map<String, List<String>> parentMap = new HashMap<String, List<String>>();
//
//    for (Serializable[] ss : resTable.getRows()) {
//      String code = (String) ss[0];
//      String parent = (String) ss[1];
//      if (parentMap.containsKey(code)) {
//        List<String> parents = parentMap.get(code);
//        parents.add(parent);
//        parentMap.put(code, parents);
//      } else {
//        parentMap.put(code, new ArrayList<String>(Arrays.asList(parent)));
//      }
//    }
//    Map<Sample, List<String>> res = new HashMap<Sample, List<String>>();
//    for (Sample s : samples) {
//      List<String> prnts = parentMap.get(s.getCode());
//      if (prnts == null)
//        prnts = new ArrayList<String>();
//      res.put(s, prnts);
//    }
//    return res;
//  }
//}
