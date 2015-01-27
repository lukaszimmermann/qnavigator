package de.uni_tuebingen.qbic.qbicmainportlet;

import java.util.List;

import model.ExperimentBarcodeSummaryBean;
import model.ExperimentBean;

import org.tepi.filtertable.FilterTable;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.StreamResource;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomTable.RowHeaderMode;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class BarcodeViewtest extends VerticalLayout implements View {

  /**
   * 
   */
  private static final long serialVersionUID = -6408708521132923624L;
  static String navigateToLabel = "experiment";
  FilterTable table;
  VerticalLayout expview_content;

  private String id;
  private Button export;

  public BarcodeViewtest(FilterTable table, IndexedContainer datasource, String id) {
    expview_content = new VerticalLayout();
    this.id = id;

    this.table = buildFilterTable();
    this.table.setSizeFull();

    expview_content.addComponent(this.table);
    expview_content.setComponentAlignment(this.table, Alignment.TOP_CENTER);
    // this.setContent(expview_content);
    this.addComponent(expview_content);

    table.setSelectable(true);
    table.setMultiSelect(true);
    
    List<ExperimentBarcodeSummaryBean> beans = parseExperimentBeans(datasource);
    BeanItemContainer<ExperimentBarcodeSummaryBean> beanContainer =
        new BeanItemContainer<ExperimentBarcodeSummaryBean>(ExperimentBarcodeSummaryBean.class);
    beanContainer.addAll(beans);
    table.setContainerDataSource(beanContainer);

    this.tableClickChangeTreeView();
  }

  private List<ExperimentBarcodeSummaryBean> parseExperimentBeans(
      IndexedContainer datasource) {
    for(Object id : datasource.getItemIds()) {
      datasource.getItem(id);
      //TODO
    }
  }


  public BarcodeViewtest() {
    // execute the above constructor with default settings, in order to have the same settings
    this(new FilterTable(), new IndexedContainer(), "No project selected");
  }

  public void setSizeFull() {
    this.table.setSizeFull();
    expview_content.setSizeFull();
    super.setSizeFull();
  }

  /**
   * sets the ContainerDataSource for showing it in a table and the id of the current Openbis
   * Experiment. The id is shown in the caption.
   * 
   * @param projectInformation
   * @param id
   */
  public void setContainerDataSource(ExperimentInformation expInformation, String id) {
    this.setStatistics(expInformation);

    HorizontalLayout buttonLayout = new HorizontalLayout();
    buttonLayout.setHeight(null);
    buttonLayout.setWidth("100%");
    buttonLayout.setSpacing(true);

    this.export = new Button("Export as TSV");
    buttonLayout.addComponent(this.export);

    this.expview_content.addComponent(buttonLayout);


    this.table.setContainerDataSource(expInformation.samples);
    this.id = id;

    DataHandler dh = (DataHandler) UI.getCurrent().getSession().getAttribute("datahandler");
    StreamResource sr = dh.getTSVStream(dh.containerToString(expInformation.samples), this.id);
    FileDownloader fileDownloader = new FileDownloader(sr);
    fileDownloader.extend(this.export);

    // this.updateCaption();
  }

  private void setStatistics(ExperimentInformation expInformation) {
    this.expview_content.removeAllComponents();

    int browserWidth = UI.getCurrent().getPage().getBrowserWindowWidth();
    int browserHeight = UI.getCurrent().getPage().getBrowserWindowHeight();

    expview_content.setWidth("100%");
    this.setWidth(String.format("%spx", (browserWidth * 0.6)));
    this.setHeight(String.format("%spx", (browserHeight * 0.8)));

    MenuBar menubar = new MenuBar();
    // A top-level menu item that opens a submenu

    // set to true for the hack below
    menubar.setHtmlContentAllowed(true);
    expview_content.addComponent(menubar);

    menubar.addStyleName("qbicmainportlet");
    menubar.setWidth(100.0f, Unit.PERCENTAGE);
    MenuItem downloadProject = menubar.addItem("Download your data", null, null);
    downloadProject.setIcon(new ThemeResource("computer_test2.png"));

    MenuItem manage = menubar.addItem("Manage your data", null, null);
    manage.setIcon(new ThemeResource("barcode_test2.png"));

    // Another submenu item with a sub-submenu
    MenuItem colds = manage.addItem("test2", null, null);

    colds.addItem("Milk", null, null);
    colds.addItem("Weissbier", null, null);

    // Another top-level item
    MenuItem snacks = menubar.addItem("Run workflows", null, null);
    snacks.setIcon(new ThemeResource("dna_test2.png"));

    snacks.addItem("Weisswurst", null, null);
    snacks.addItem("Bratwurst", null, null);
    snacks.addItem("Currywurst", null, null);

    // Yet another top-level item
    MenuItem servs = menubar.addItem("Analyze your data", null, null);
    servs.setIcon(new ThemeResource("graph_test2.png"));
    servs.addItem("Car Service", null, null);

    // general information
    VerticalLayout generalInfo = new VerticalLayout();
    VerticalLayout generalInfoContent = new VerticalLayout();
    generalInfoContent.setCaption("General Information");
    generalInfoContent.setIcon(FontAwesome.INFO);
    generalInfoContent.addComponent(new Label(String.format("Kind:\t %s",
        expInformation.experimentType)));
    generalInfoContent.setMargin(true);
    generalInfo.setMargin(true);

    generalInfo.addComponent(generalInfoContent);
    expview_content.addComponent(generalInfo);


    VerticalLayout statistics = new VerticalLayout();

    HorizontalLayout statContent = new HorizontalLayout();
    statContent.setCaption("Statistics");
    statContent.setIcon(FontAwesome.BAR_CHART_O);


    statContent.addComponent(new Label(String.format("%s sample(s),",
        expInformation.numberOfSamples)));
    statContent.addComponent(new Label(String.format("%s dataset(s).",
        expInformation.numberOfDatasets)));
    statContent.setMargin(true);
    statContent.setSpacing(true);

    if (expInformation.numberOfDatasets > 0) {

      String lastSample = "No samples available";
      if (expInformation.lastChangedSample != null) {
        lastSample = expInformation.lastChangedSample.split("/")[2];
      }
      statContent.addComponent(new Label(String.format(
          "Last change %s",
          String.format("occurred in sample %s (%s)", lastSample,
              expInformation.lastChangedDataset.toString()))));
    }


    statistics.addComponent(statContent);
    statistics.setMargin(true);
    expview_content.addComponent(statistics);

    // statistics.addComponent(new Label(expInformation.propertiesFormattedString,
    // ContentMode.HTML));

    // table section
    VerticalLayout tableSection = new VerticalLayout();
    HorizontalLayout tableSectionContent = new HorizontalLayout();
    tableSectionContent.setWidth("100%");
    tableSectionContent.setWidth("100%");
    table.setWidth("100%");
    tableSectionContent.setCaption("Registered Samples");
    tableSectionContent.setIcon(FontAwesome.FLASK);
    tableSectionContent.addComponent(this.table);

    tableSectionContent.setMargin(true);
    tableSection.setMargin(true);

    tableSection.addComponent(tableSectionContent);
    expview_content.addComponent(tableSection);

  }


  private void updateCaption() {
    this.setCaption(String.format("Viewing Experiment %s", id));
  }

  private void tableClickChangeTreeView() {
    table.setSelectable(true);
    table.setImmediate(true);
    this.table.addValueChangeListener(new ViewTablesClickListener(table, "Sample"));
  }

  private FilterTable buildFilterTable() {
    FilterTable filterTable = new FilterTable();
    filterTable.setSizeFull();

    filterTable.setFilterDecorator(new DatasetViewFilterDecorator());
    filterTable.setFilterGenerator(new DatasetViewFilterGenerator());

    filterTable.setFilterBarVisible(true);

    filterTable.setSelectable(true);
    filterTable.setImmediate(true);

    filterTable.setRowHeaderMode(RowHeaderMode.INDEX);

    filterTable.setColumnCollapsingAllowed(true);

    filterTable.setColumnReorderingAllowed(true);


    return filterTable;
  }


  @Override
  public void enter(ViewChangeEvent event) {
    String currentValue = event.getParameters();
    System.out.println("currentValue: " + currentValue);
    System.out.println("navigateToLabel: " + navigateToLabel);
    DataHandler dh = (DataHandler) UI.getCurrent().getSession().getAttribute("datahandler");
    try {
      // String type =
      // this.treeView.getContainerDataSource().getItem(currentValue).getItemProperty("type").getValue().toString();
      this.setContainerDataSource(dh.getExperimentInformation(currentValue), currentValue);
    } catch (Exception e) {
      System.out.println("Exception in ExperimentView.enter");
      // e.printStackTrace();
    }

  }

}