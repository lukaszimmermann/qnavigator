///*******************************************************************************
// * QBiC Project qNavigator enables users to manage their projects.
// * Copyright (C) "2016”  Christopher Mohr, David Wojnar, Andreas Friedrich
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// *******************************************************************************/
//package de.uni_tuebingen.qbic.qbicmainportlet;
//
//import helpers.Utils;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.List;
//
//import qbic.vaadincomponents.BarcodePreviewComponent;
//import qbic.vaadincomponents.SheetOptionComponent;
//
//import logging.Log4j2Logger;
//import model.ExperimentBarcodeSummaryBean;
//import model.ExperimentBean;
//import model.SampleToBarcodeFieldTranslator;
//import model.SortBy;
//
//import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
//
//import com.vaadin.data.util.BeanItemContainer;
//import com.vaadin.ui.Button;
//import com.vaadin.ui.ComboBox;
//import com.vaadin.ui.Component;
//import com.vaadin.ui.Label;
//import com.vaadin.ui.ProgressBar;
//import com.vaadin.ui.TabSheet;
//import com.vaadin.ui.Table;
//import com.vaadin.ui.VerticalLayout;
//import com.vaadin.ui.themes.ValoTheme;
//
//import controllers.BarcodeController;
//
///**
// * View class for the Sample Sheet and Barcode pdf creation
// * 
// * @author Andreas Friedrich
// * 
// */
//public class WizardBarcodeView extends VerticalLayout {
//
//  logging.Logger logger = new Log4j2Logger(WizardBarcodeView.class);
//  /**
//   * 
//   */
//  private static final long serialVersionUID = 5688919972212199869L;
//  private ComboBox spaceBox;
//  private ComboBox projectBox;
//  private Table experimentTable;
//  private Component tabsTab;
//  private TabSheet tabs;
//
//  private BarcodePreviewComponent tubePreview;
//
//  private SheetOptionComponent sheetPreview;
//  private Button prepareBarcodes;
//
//  private ProgressBar bar;
//  private Label info;
//  private Button download;
//
//  /**
//   * Creates a new component view for barcode creation
//   * 
//   * @param spaces List of available openBIS spaces
//   */
//  public WizardBarcodeView(List<String> spaces) {
//    SampleToBarcodeFieldTranslator translator = new SampleToBarcodeFieldTranslator();
//    setSpacing(true);
//    setMargin(true);
//
//    spaceBox = new ComboBox("Project", spaces);
//    // spaceBox.setStyleName(ProjectwizardUI.boxTheme);
//    spaceBox.setNullSelectionAllowed(false);
//    spaceBox.setImmediate(true);
//
//    projectBox = new ComboBox("Sub-Project");
//    // projectBox.setStyleName(ProjectwizardUI.boxTheme);
//    projectBox.setEnabled(false);
//    projectBox.setImmediate(true);
//    projectBox.setNullSelectionAllowed(false);
//
//    addComponent(Utils.questionize(spaceBox, "Name of the project", "Project Name"));
//    addComponent(Utils.questionize(projectBox, "QBiC 5 letter project code", "Sub-Project"));
//
//    experimentTable = new Table("Sample Overview");
//    experimentTable.setStyleName(ValoTheme.TABLE_SMALL);
//    experimentTable.setPageLength(1);
//    experimentTable.setContainerDataSource(new BeanItemContainer<ExperimentBarcodeSummaryBean>(
//        ExperimentBarcodeSummaryBean.class));
//    experimentTable.setSelectable(true);
//    experimentTable.setMultiSelect(true);
//    mapCols();
//    addComponent(Utils.questionize(experimentTable,
//        "This table gives an overview of tissue samples and extracted materials"
//            + " for which barcodes can be printed. You can select one or multiple rows.",
//        "Sample Overview"));
//
//    sheetPreview = new SheetOptionComponent(translator);
//    tubePreview = new BarcodePreviewComponent(translator);
//
//    tabs = new TabSheet();
//    tabs.setStyleName(ValoTheme.TABSHEET_FRAMED);
//    tabs.addTab(sheetPreview, "Sample Sheet");
//    tabs.addTab(tubePreview, "Barcode Stickers");
//    tabsTab = new CustomVisibilityComponent(tabs);
//    tabsTab.setVisible(false);
//    addComponent(Utils.questionize(tabsTab,
//        "Prepare an A4 sample sheet or qr codes for sample tubes.", "Barcode Preparation"));
//
//    info = new Label();
//    bar = new ProgressBar();
//    bar.setVisible(false);
//    addComponent(info);
//    addComponent(bar);
//
//    prepareBarcodes = new Button("Prepare Barcodes");
//    prepareBarcodes.setEnabled(false);
//    addComponent(prepareBarcodes);
//
//    download = new Button("Download");
//    download.setEnabled(false);
//    addComponent(download);
//  }
//
//  private void mapCols() {
//    experimentTable.setColumnHeader("amount", "Samples");
//    experimentTable.setColumnHeader("bio_Type", "Type");
//    experimentTable.setColumnHeader("experiment", "Experiment");
//  }
//
//  public WizardBarcodeView() {
//    SampleToBarcodeFieldTranslator translator = new SampleToBarcodeFieldTranslator();
//    setSpacing(true);
//    setMargin(true);
//    
//    spaceBox = new ComboBox();
//    projectBox = new ComboBox();
//
//    experimentTable = new Table("Sample Overview");
//    experimentTable.setStyleName(ValoTheme.TABLE_SMALL);
//    experimentTable.setPageLength(1);
//    experimentTable.setContainerDataSource(new BeanItemContainer<ExperimentBarcodeSummaryBean>(
//        ExperimentBarcodeSummaryBean.class));
//    experimentTable.setSelectable(true);
//    experimentTable.setMultiSelect(true);
//    mapCols();
//    addComponent(Utils.questionize(experimentTable,
//        "This table gives an overview of tissue samples and extracted materials"
//            + " for which barcodes can be created. You can select one or multiple rows.",
//        "Sample Overview"));
//
//    sheetPreview = new SheetOptionComponent(translator);
//    tubePreview = new BarcodePreviewComponent(translator);
//
//    tabs = new TabSheet();
//    tabs.setStyleName(ValoTheme.TABSHEET_FRAMED);
//    tabs.addTab(sheetPreview, "Sample Sheet");
//    tabs.addTab(tubePreview, "Barcode Stickers");
//    tabsTab = new CustomVisibilityComponent(tabs);
//    tabsTab.setVisible(false);
//    addComponent(Utils.questionize(tabsTab,
//        "Prepare an A4 sample sheet or qr codes for sample tubes.", "Barcode Preparation"));
//
//    info = new Label();
//    bar = new ProgressBar();
//    bar.setVisible(false);
//    addComponent(info);
//    addComponent(bar);
//
//    prepareBarcodes = new Button("Prepare Barcodes");
//    prepareBarcodes.setEnabled(false);
//    addComponent(prepareBarcodes);
//
//    download = new Button("Download");
//    download.setEnabled(false);
//    addComponent(download);
//  }
//
//  public boolean getOverwrite() {
//    return tubePreview.overwrite();
//  }
//
//  public void enableExperiments(boolean enable) {
//    experimentTable.setEnabled(enable);
//  }
//
//  public void creationPressed() {
//    enableExperiments(false);
//    spaceBox.setEnabled(false);
//    projectBox.setEnabled(false);
//    prepareBarcodes.setEnabled(false);
//  }
//
//  public void reset() {
//    info.setValue("");
//    download.setEnabled(false);
//    spaceBox.setEnabled(true);
//    projectBox.setEnabled(true);
//  }
//
//  public void resetProjects() {
//    projectBox.removeAllItems();
//    projectBox.setEnabled(false);
//    resetExperiments();
//  }
//
//  public void resetExperiments() {
//    experimentTable.setPageLength(1);
//    experimentTable.removeAllItems();
//    tabsTab.setVisible(false);
//  }
//
//  public String getSpaceCode() {
//    return (String) spaceBox.getValue();
//  }
//
//  public String getProjectCode() {
//    return (String) projectBox.getValue();
//  }
//
//  public ComboBox getSpaceBox() {
//    return spaceBox;
//  }
//
//  public ComboBox getProjectBox() {
//    return projectBox;
//  }
//
//  public Table getExperimentTable() {
//    return experimentTable;
//  }
//
//  public void setProjectCodes(List<String> projects) {
//    projectBox.addItems(projects);
//    projectBox.setEnabled(true);
//  }
//
//  public void setExperiments(List<ExperimentBarcodeSummaryBean> beans) {
//    BeanItemContainer<ExperimentBarcodeSummaryBean> c =
//        new BeanItemContainer<ExperimentBarcodeSummaryBean>(ExperimentBarcodeSummaryBean.class);
//    c.addAll(beans);
//    experimentTable.setContainerDataSource(c);
//    experimentTable.setPageLength(beans.size());
//    if (c.size() == 1)
//      experimentTable.select(c.getIdByIndex(0));
//  }
//
//  @SuppressWarnings("unchecked")
//  public Collection<ExperimentBarcodeSummaryBean> getExperiments() {
//    return (Collection<ExperimentBarcodeSummaryBean>) experimentTable.getValue();
//  }
//
//  public List<Button> getButtons() {
//    return new ArrayList<Button>(Arrays.asList(this.prepareBarcodes));
//  }
//
//  public ProgressBar getProgressBar() {
//    return bar;
//  }
//
//  public Label getProgressInfo() {
//    return info;
//  }
//
//  public void enablePrep(boolean enable) {
//    prepareBarcodes.setEnabled(enable);
//    tabsTab.setVisible(enable);
//  }
//
//  public SortBy getSorter() {
//    return sheetPreview.getSorter();
//  }
//
//  public void creationDone() {
//    enableExperiments(true);
//    bar.setVisible(false);
//  }
//
//  public void sheetReady() {
//    download.setEnabled(true);
//  }
//
//  public void tubesReady() {
//    download.setEnabled(true);
//  }
//
//  public void resetSpace() {
//    spaceBox.setValue(null);
//  }
//
//  public void disablePreview() {
//    tubePreview.setVisible(false);
//  }
//
//  public void enablePreview(Sample sample) {
//    tubePreview.setExample(sample);
//    tubePreview.setVisible(true);
//  }
//
//  public String getCodedString(Sample s) {
//    if (tabs.getSelectedTab() instanceof BarcodePreviewComponent)
//      return tubePreview.getCodeString(s);
//    else
//      return s.getCode();
//  }
//
//  public String getInfo1(Sample s, String parents) {
//    if (tabs.getSelectedTab() instanceof BarcodePreviewComponent)
//      return tubePreview.getInfo1(s);
//    else
//      return sheetPreview.getInfo1(s, parents);
//  }
//
//  public String getInfo2(Sample s, String parents) {
//    if (tabs.getSelectedTab() instanceof BarcodePreviewComponent)
//      return tubePreview.getInfo2(s);
//    else
//      return sheetPreview.getInfo2(s, parents);
//  }
//
//  public TabSheet getTabs() {
//    return tabs;
//  }
//
//  public Button getDownloadButton() {
//    return download;
//  }
//
//  public List<String> getHeaders() {
//    return sheetPreview.getHeaders();
//  }
//
//  public void initControl(BarcodeController barcodeController) {
//    barcodeController.init(this);
//  }
//}
