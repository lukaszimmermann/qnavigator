package de.uni_tuebingen.qbic.qbicmainportlet;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.portlet.PortletSession;

import logging.Log4j2Logger;
import logging.Logger;
import model.DatasetBean;
import model.ExperimentBean;
import model.ProjectBean;
import model.SampleBean;

import org.apache.catalina.util.Base64;
import org.tepi.filtertable.FilterTreeTable;

import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.theme.ThemeDisplay;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomTable.RowHeaderMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.uni_tuebingen.qbic.util.DashboardUtil;

public class DatasetView extends VerticalLayout implements View {


  /**
   * 
   */
  private static final long serialVersionUID = 8672873911284888801L;

  private static Logger LOGGER = new Log4j2Logger(DatasetView.class);
  private final FilterTreeTable table;
  private HierarchicalContainer datasets;
  VerticalLayout vert;
  private final String DOWNLOAD_BUTTON_CAPTION = "Download";
  private final String VISUALIZE_BUTTON_CAPTION = "Visualize";
  static String navigateToLabel = "datasetview";
  private DataHandler datahandler;
  private final ButtonLink download = new ButtonLink(DOWNLOAD_BUTTON_CAPTION, new ExternalResource(
      ""));

  private final String[] FILTER_TABLE_COLUMNS = new String[] {"Select", "Project", "Sample",
      "Sample Type", "File Name", "Dataset Type", "Registration Date", "File Size"};

  public DatasetView(DataHandler dh) {

    this.datahandler = dh;

    this.vert = new VerticalLayout();
    this.table = buildFilterTable();
    // this.setContent(vert);
    this.addComponent(vert);
  }


  public DatasetView(HierarchicalContainer dataset) {
    this.vert = new VerticalLayout();
    this.datasets = dataset;
    this.table = buildFilterTable();
    this.buildLayout();
    this.setContainerDataSource(this.datasets);
    // this.setContent(vert);
    this.addComponent(vert);
  }


  public void setContainerDataSource(HierarchicalContainer newDataSource) {
    this.datasets = (HierarchicalContainer) newDataSource;
    this.table.setContainerDataSource(this.datasets);

    // TODO does this affect the datasetview?
    // this.table.setColumnCollapsed("state", true);

    this.table.setVisibleColumns((Object[]) FILTER_TABLE_COLUMNS);

    this.table.setSizeFull();
    this.buildLayout();
  }

  public HierarchicalContainer getContainerDataSource() {
    return this.datasets;
  }

  /**
   * Precondition: {DatasetView#table} has to be initialized. e.g. with
   * {DatasetView#buildFilterTable} If it is not, strange behaviour has to be expected. builds the
   * Layout of this view.
   */
  private void buildLayout() {
    this.vert.removeAllComponents();

    int browserWidth = UI.getCurrent().getPage().getBrowserWindowWidth();
    int browserHeight = UI.getCurrent().getPage().getBrowserWindowHeight();

    this.vert.setWidth("100%");
    this.setWidth(String.format("%spx", (browserWidth * 0.6)));
    this.setHeight(String.format("%spx", (browserHeight * 0.8)));

    MenuBar menubar = new MenuBar();
    // A top-level menu item that opens a submenu

    // set to true for the hack below
    menubar.setHtmlContentAllowed(true);
    this.vert.addComponent(menubar);

    menubar.addStyleName("qbicmainportlet");
    menubar.setWidth(100.0f, Unit.PERCENTAGE);
    MenuItem downloadProject = menubar.addItem("Download your data", null, null);
    downloadProject.setIcon(new ThemeResource("computer_test2.png"));
    downloadProject.setEnabled(false);

    MenuItem manage = menubar.addItem("Manage your data", null, null);
    manage.setIcon(new ThemeResource("barcode_test2.png"));
    manage.setEnabled(false);

    // Another top-level item
    MenuItem workflows = menubar.addItem("Run workflows", null, null);
    workflows.setIcon(new ThemeResource("dna_test2.png"));
    workflows.setEnabled(false);

    // Yet another top-level item
    MenuItem analyze = menubar.addItem("Analyze your data", null, null);
    analyze.setIcon(new ThemeResource("graph_test2.png"));
    analyze.setEnabled(false);

    VerticalLayout statistics = new VerticalLayout();
    HorizontalLayout statContent = new HorizontalLayout();
    statContent.setCaption("Statistics");
    statContent.setIcon(FontAwesome.BAR_CHART_O);
    statContent.addComponent(new Label(String.format("%s dataset(s).", this.datasets.size())));
    statContent.setMargin(true);
    statContent.setSpacing(true);
    statistics.addComponent(statContent);
    statistics.setMargin(true);
    this.vert.addComponent(statistics);


    // Table (containing datasets) section
    VerticalLayout tableSection = new VerticalLayout();
    HorizontalLayout tableSectionContent = new HorizontalLayout();

    tableSectionContent.setCaption("Registered Datasets");
    tableSectionContent.setIcon(FontAwesome.FLASK);
    tableSectionContent.addComponent(this.table);

    tableSectionContent.setMargin(true);
    tableSection.setMargin(true);

    tableSection.addComponent(tableSectionContent);
    this.vert.addComponent(tableSection);

    table.setWidth("100%");
    tableSection.setWidth("100%");
    tableSectionContent.setWidth("100%");

    // this.table.setSizeFull();

    HorizontalLayout buttonLayout = new HorizontalLayout();
    buttonLayout.setHeight(null);
    buttonLayout.setWidth("100%");
    buttonLayout.setSpacing(false);

    this.download.setEnabled(false);
    final Button visualize = new Button(VISUALIZE_BUTTON_CAPTION);
    visualize.setEnabled(false);
    buttonLayout.addComponent(this.download);
    buttonLayout.addComponent(visualize);

    Button checkAll = new Button("Select all datasets");
    checkAll.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        for (Object itemId : table.getItemIds()) {
          ((CheckBox) table.getItem(itemId).getItemProperty("Select").getValue()).setValue(true);
        }
      }
    });

    buttonLayout.addComponent(checkAll);

    /**
     * prepare download.
     */
    Map<String, AbstractMap.SimpleEntry<String, Long>> selected_datasets =
        new HashMap<String, AbstractMap.SimpleEntry<String, Long>>();

    PortletSession portletSession = ((QbicmainportletUI) UI.getCurrent()).getPortletSession();
    portletSession.setAttribute("qbic_download",
        new HashMap<String, AbstractMap.SimpleEntry<String, Long>>(),
        PortletSession.APPLICATION_SCOPE);
    String resourceUrl =
        (String) portletSession.getAttribute("resURL", PortletSession.APPLICATION_SCOPE);
    download.setResource(new ExternalResource("javascript:"));
    download.setEnabled(false);


    for (final Object itemId : this.table.getItemIds()) {
      setCheckedBox(itemId, (String) this.table.getItem(itemId).getItemProperty("CODE").getValue());
    }



    /*
     * Update the visualize button. It is only enabled, if the files can be visualized.
     */
    this.table.addValueChangeListener(new ValueChangeListener() {
      /**
       * 
       */
      private static final long serialVersionUID = -4875903343717437913L;


      /**
       * check for what selection can be visualized. If so, enable the button. TODO change to
       * checked.
       */
      @Override
      public void valueChange(ValueChangeEvent event) {
        // Nothing selected or more than one selected.
        Set<Object> selectedValues = (Set<Object>) event.getProperty().getValue();
        if (selectedValues == null || selectedValues.size() == 0 || selectedValues.size() > 1) {
          visualize.setEnabled(false);
          return;
        }
        // if one selected check whether its dataset type is either fastqc or qcml.
        // For now we only visulize these two file types.
        Iterator<Object> iterator = selectedValues.iterator();
        Object next = iterator.next();
        String datasetType =
            (String) table.getItem(next).getItemProperty("Dataset Type").getValue();
        // TODO: No hardcoding!!
        if (datasetType.equals("FASTQC") || datasetType.equals("QCML") || datasetType.equals("BAM")
            || datasetType.equals("VCF")) {
          visualize.setEnabled(true);
        } else {
          visualize.setEnabled(false);
        }
      }
    });


    // TODO Workflow Views should get those data and be happy
    /*
     * Send message that in datasetview the following was selected. WorkflowViews get those messages
     * and save them, if it is valid information for them.
     */
    this.table.addValueChangeListener(new ValueChangeListener() {
      /**
       * 
       */
      private static final long serialVersionUID = -3554627008191389648L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        // Nothing selected or more than one selected.
        Set<Object> selectedValues = (Set<Object>) event.getProperty().getValue();
        State state = (State) UI.getCurrent().getSession().getAttribute("state");
        ArrayList<String> message = new ArrayList<String>();
        message.add("DataSetView");
        if (selectedValues != null && selectedValues.size() == 1) {
          Iterator<Object> iterator = selectedValues.iterator();
          Object next = iterator.next();
          String datasetType =
              (String) table.getItem(next).getItemProperty("Dataset Type").getValue();
          message.add(datasetType);
          String project = (String) table.getItem(next).getItemProperty("Project").getValue();

          String space = datahandler.openBisClient.getProjectByCode(project).getSpaceCode();// .getIdentifier().split("/")[1];
          message.add(project);
          message.add((String) table.getItem(next).getItemProperty("Sample").getValue());
          message.add((String) table.getItem(next).getItemProperty("Sample Type").getValue());
          message.add((String) table.getItem(next).getItemProperty("dl_link").getValue());
          message.add((String) table.getItem(next).getItemProperty("File Name").getValue());
          message.add(space);
          // state.notifyObservers(message);
        } else {
          message.add("null");
        }// TODO
         // state.notifyObservers(message);

      }
    });


    // TODO get the GV to work here. Together with reverse proxy
    // Assumes that table Value Change listner is enabling or disabling the button if preconditions
    // are not fullfilled
    visualize.addClickListener(new ClickListener() {
      /**
       * 
       */
      private static final long serialVersionUID = 9015273307461506369L;

      @Override
      public void buttonClick(ClickEvent event) {
        Set<Object> selectedValues = (Set<Object>) table.getValue();
        Iterator<Object> iterator = selectedValues.iterator();
        Object next = iterator.next();
        String datasetCode = (String) table.getItem(next).getItemProperty("CODE").getValue();
        String datasetFileName =
            (String) table.getItem(next).getItemProperty("File Name").getValue();
        URL url;
        try {
          url = datahandler.openBisClient.getUrlForDataset(datasetCode, datasetFileName);
          Window subWindow =
              new Window("QC of Sample: "
                  + (String) table.getItem(next).getItemProperty("Sample").getValue());
          VerticalLayout subContent = new VerticalLayout();
          subContent.setMargin(true);
          subWindow.setContent(subContent);
          QbicmainportletUI ui = (QbicmainportletUI) UI.getCurrent();
          // Put some components in it
          Resource res = null;
          String datasetType =
              (String) table.getItem(next).getItemProperty("Dataset Type").getValue();
          final RequestHandler rh = new ProxyForGenomeViewerRestApi();
          boolean rhAttached = false;
          if (datasetType.equals("QCML")) {
            QcMlOpenbisSource re = new QcMlOpenbisSource(url);
            StreamResource streamres = new StreamResource(re, "test-file");
            streamres.setMIMEType("application/xml");
            res = streamres;
          } else if (datasetType.equals("FASTQC")) {
            res = new ExternalResource(url);
          } else if (datasetType.equals("BAM") || datasetType.equals("VCF")) {
            String filePath = (String) table.getItem(next).getItemProperty("dl_link").getValue();
            filePath = String.format("/store%s", filePath.split("store")[1]);
            String fileId = (String) table.getItem(next).getItemProperty("File Name").getValue();
            // fileId = "control.1kg.panel.samples.vcf.gz";
            // UI.getCurrent().getSession().addRequestHandler(rh);
            rhAttached = true;
            ThemeDisplay themedisplay =
                (ThemeDisplay) VaadinService.getCurrentRequest()
                    .getAttribute(WebKeys.THEME_DISPLAY);
            String hostTmp = "http://localhost:7778/vizrest/rest";// "http://localhost:8080/web/guest/mainportlet?p_p_id=QbicmainportletApplicationPortlet_WAR_QBiCMainPortlet_INSTANCE_5pPd5JQ8uGOt&p_p_lifecycle=2&p_p_state=normal&p_p_mode=view&p_p_cacheability=cacheLevelPage&p_p_col_id=column-1&p_p_col_count=1";
            // hostTmp +=
            // "&qbicsession=" + UI.getCurrent().getSession().getAttribute("gv-restapi-session")
            // + "&someblabla=";
            // String hostTmp = themedisplay.getURLPortal() +
            // UI.getCurrent().getPage().getLocation().getPath() + "?qbicsession=" +
            // UI.getCurrent().getSession().getAttribute("gv-restapi-session") + "&someblabla=" ;
            LOGGER.debug(hostTmp);
            String host = Base64.encode(hostTmp.getBytes());
            LOGGER.debug(host);
            String title = (String) table.getItem(next).getItemProperty("Sample").getValue();
            res =
                new ExternalResource(
                    String
                        .format(
                            "http://localhost:7778/genomeviewer/?host=%s&title=%s&fileid=%s&featuretype=alignments&filepath=%s&removeZeroGenotypes=false",
                            host, title, fileId, filePath));
          }
          LOGGER.debug(res.toString());
          BrowserFrame frame = new BrowserFrame("", res);
          if (rhAttached) {
            frame.addDetachListener(new DetachListener() {

              /**
               * 
               */
              private static final long serialVersionUID = 1534523447730906543L;

              @Override
              public void detach(DetachEvent event) {
                UI.getCurrent().getSession().removeRequestHandler(rh);
              }

            });
          }

          frame.setSizeFull();
          subContent.addComponent(frame);

          // Center it in the browser window
          subWindow.center();
          subWindow.setModal(true);
          subWindow.setSizeFull();

          frame.setHeight((int) (ui.getPage().getBrowserWindowHeight() * 0.8), Unit.PIXELS);
          // Open it in the UI
          ui.addWindow(subWindow);
        } catch (MalformedURLException e) {
          LOGGER.error(String.format(
              "Visualization failed because of malformedURL for dataset: %s", datasetCode));
          Notification
              .show(
                  "Given dataset has no file attached to it!! Please Contact your project manager. Or check whether it already has some data",
                  Notification.Type.ERROR_MESSAGE);
        }
      }
    });

    this.vert.addComponent(buttonLayout);

  }


  private void setCheckedBox(Object itemId, String parentFolder) {
    CheckBox itemCheckBox =
        (CheckBox) this.table.getItem(itemId).getItemProperty("Select").getValue();
    itemCheckBox.addValueChangeListener(new TableCheckBoxValueChangeListener(itemId, parentFolder));

    if (table.hasChildren(itemId)) {
      for (Object childId : table.getChildren(itemId)) {
        String newParentFolder =
            Paths.get(parentFolder,
                (String) this.table.getItem(itemId).getItemProperty("File Name").getValue())
                .toString();
        setCheckedBox(childId, newParentFolder);
      }
    }

  }

  private FilterTreeTable buildFilterTable() {
    FilterTreeTable filterTable = new FilterTreeTable();
    filterTable.setSizeFull();

    filterTable.setFilterDecorator(new DatasetViewFilterDecorator());
    filterTable.setFilterGenerator(new DatasetViewFilterGenerator());

    filterTable.setFilterBarVisible(true);

    filterTable.setSelectable(true);
    filterTable.setImmediate(true);
    filterTable.setMultiSelect(true);

    filterTable.setRowHeaderMode(RowHeaderMode.INDEX);

    filterTable.setColumnCollapsingAllowed(true);

    filterTable.setColumnReorderingAllowed(true);

    if (this.datasets != null) {
      filterTable.setContainerDataSource(this.datasets);
    }

    return filterTable;
  }

  @Override
  public void enter(ViewChangeEvent event) {
    String parameters = event.getParameters();
    if (parameters == null || parameters.equals(""))
      return;
    String[] params = parameters.split("&");
    if (params == null || params.length != 2)
      return;
    HashMap<String, String> map = new HashMap<String, String>();
    for (String p : params) {
      String[] kv = p.split("=");
      if (kv.length != 2)
        return;
      map.put(kv[0], kv[1]);
    }
    try {
      HierarchicalContainer datasetContainer = null;
      switch (map.get("type")) {
        case "project":
          datasetContainer = new HierarchicalContainer();
          datasetContainer.addContainerProperty("Select", CheckBox.class, null);
          datasetContainer.addContainerProperty("Project", String.class, null);
          datasetContainer.addContainerProperty("Sample", String.class, null);
          datasetContainer.addContainerProperty("Sample Type", String.class, null);
          datasetContainer.addContainerProperty("File Name", String.class, null);
          datasetContainer.addContainerProperty("File Type", String.class, null);
          datasetContainer.addContainerProperty("Dataset Type", String.class, null);
          datasetContainer.addContainerProperty("Registration Date", Timestamp.class, null);
          datasetContainer.addContainerProperty("Validated", Boolean.class, null);
          datasetContainer.addContainerProperty("File Size", String.class, null);
          datasetContainer.addContainerProperty("file_size_bytes", Long.class, null);
          datasetContainer.addContainerProperty("dl_link", String.class, null);
          datasetContainer.addContainerProperty("CODE", String.class, null);
          ProjectBean projectBean = datahandler.getProject(map.get("id"));
          BeanItemContainer<ExperimentBean> bic = projectBean.getExperiments();
          for (ExperimentBean eb : bic.getItemIds()) {
            BeanItemContainer<SampleBean> bsb = eb.getSamples();
            for (SampleBean sb : bsb.getItemIds()) {
              BeanItemContainer<DatasetBean> datasetbeans = sb.getDatasets();
              for (DatasetBean d : datasetbeans.getItemIds()) {
                String sample = sb.getCode();
                String sampleType = sb.getType();// this.openBisClient.getSampleByIdentifier(sample).getSampleTypeCode();
                String project = projectBean.getCode();
                Date date = d.getRegistrationDate();
                SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                String dateString = sd.format(date);
                Timestamp ts = Timestamp.valueOf(dateString);
                // recursive test
                registerDatasetInTable(d, datasetContainer, project, sample, ts, sampleType, null);
              }
            }

          }
          break;
        case "experiment":
          datasetContainer = new HierarchicalContainer();
          datasetContainer.addContainerProperty("Select", CheckBox.class, null);
          datasetContainer.addContainerProperty("Project", String.class, null);
          datasetContainer.addContainerProperty("Sample", String.class, null);
          datasetContainer.addContainerProperty("Sample Type", String.class, null);
          datasetContainer.addContainerProperty("File Name", String.class, null);
          datasetContainer.addContainerProperty("File Type", String.class, null);
          datasetContainer.addContainerProperty("Dataset Type", String.class, null);
          datasetContainer.addContainerProperty("Registration Date", Timestamp.class, null);
          datasetContainer.addContainerProperty("Validated", Boolean.class, null);
          datasetContainer.addContainerProperty("File Size", String.class, null);
          datasetContainer.addContainerProperty("file_size_bytes", Long.class, null);
          datasetContainer.addContainerProperty("dl_link", String.class, null);
          datasetContainer.addContainerProperty("CODE", String.class, null);
          ExperimentBean experimentBean = datahandler.getExperiment(map.get("id"));
          BeanItemContainer<SampleBean> samplesbeans = experimentBean.getSamples();
            for (SampleBean sb : samplesbeans.getItemIds()) {
              BeanItemContainer<DatasetBean> datasetbeans = sb.getDatasets();
              for (DatasetBean d : datasetbeans.getItemIds()) {
                String sample = sb.getCode();
                String sampleType = sb.getType();// this.openBisClient.getSampleByIdentifier(sample).getSampleTypeCode();
                String project = experimentBean.getId().split("/")[2];
                Date date = d.getRegistrationDate();
                SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                String dateString = sd.format(date);
                Timestamp ts = Timestamp.valueOf(dateString);
                // recursive test
                registerDatasetInTable(d, datasetContainer, project, sample, ts, sampleType, null);
              }
            }
          break;          
        case "sample":
          datasetContainer = new HierarchicalContainer();
          datasetContainer.addContainerProperty("Select", CheckBox.class, null);
          datasetContainer.addContainerProperty("Project", String.class, null);
          datasetContainer.addContainerProperty("Sample", String.class, null);
          datasetContainer.addContainerProperty("Sample Type", String.class, null);
          datasetContainer.addContainerProperty("File Name", String.class, null);
          datasetContainer.addContainerProperty("File Type", String.class, null);
          datasetContainer.addContainerProperty("Dataset Type", String.class, null);
          datasetContainer.addContainerProperty("Registration Date", Timestamp.class, null);
          datasetContainer.addContainerProperty("Validated", Boolean.class, null);
          datasetContainer.addContainerProperty("File Size", String.class, null);
          datasetContainer.addContainerProperty("file_size_bytes", Long.class, null);
          datasetContainer.addContainerProperty("dl_link", String.class, null);
          datasetContainer.addContainerProperty("CODE", String.class, null);
          SampleBean sampleBean = datahandler.getSample(map.get("id"));
              BeanItemContainer<DatasetBean> datasetbeans = sampleBean.getDatasets();
              for (DatasetBean d : datasetbeans.getItemIds()) {
                String sample = sampleBean.getCode();
                String sampleType = sampleBean.getType();// this.openBisClient.getSampleByIdentifier(sample).getSampleTypeCode();
                String project = sampleBean.getId().split("/")[2];
                Date date = d.getRegistrationDate();
                SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                String dateString = sd.format(date);
                Timestamp ts = Timestamp.valueOf(dateString);
                // recursive test
                registerDatasetInTable(d, datasetContainer, project, sample, ts, sampleType, null);
              }
          break;  
        default:
          datasetContainer = new HierarchicalContainer();
      }

      this.setContainerDataSource(datasetContainer);

    } catch (Exception e) {
      e.printStackTrace();
      LOGGER.error(String.format("getting dataset failed for dataset %s", map.toString()),
          e.getStackTrace());
    }
  }



  public void registerDatasetInTable(DatasetBean d, HierarchicalContainer dataset_container,
      String project, String sample, Timestamp ts, String sampleType, Object parent) {
    if (d.hasChildren()) {

      Object new_ds = dataset_container.addItem();

      List<DatasetBean> subList = d.getChildren();


      dataset_container.setChildrenAllowed(new_ds, true);

      dataset_container.getContainerProperty(new_ds, "Select").setValue(new CheckBox());

      dataset_container.getContainerProperty(new_ds, "Project").setValue(project);
      dataset_container.getContainerProperty(new_ds, "Sample").setValue(sample);
      dataset_container.getContainerProperty(new_ds, "Sample Type").setValue(
          d.getSample().getType());
      dataset_container.getContainerProperty(new_ds, "File Name").setValue(d.getName());
      dataset_container.getContainerProperty(new_ds, "File Type").setValue("Folder");
      dataset_container.getContainerProperty(new_ds, "Dataset Type").setValue("-");
      dataset_container.getContainerProperty(new_ds, "Registration Date").setValue(ts);
      dataset_container.getContainerProperty(new_ds, "Validated").setValue(true);
      dataset_container.getContainerProperty(new_ds, "dl_link").setValue(d.getDssPath());
      dataset_container.getContainerProperty(new_ds, "CODE").setValue(d.getCode());
      dataset_container.getContainerProperty(new_ds, "file_size_bytes").setValue(d.getFileSize());

      if (parent != null) {
        dataset_container.setParent(new_ds, parent);
      }

      for (DatasetBean file : subList) {
        registerDatasetInTable(file, dataset_container, project, sample, ts, sampleType, new_ds);
      }

    } else {
      // System.out.println("Now it should be a file: " + filelist[0].getPathInDataSet());

      Object new_file = dataset_container.addItem();
      dataset_container.setChildrenAllowed(new_file, false);

      dataset_container.getContainerProperty(new_file, "Select").setValue(new CheckBox());
      dataset_container.getContainerProperty(new_file, "Project").setValue(project);
      dataset_container.getContainerProperty(new_file, "Sample").setValue(sample);
      dataset_container.getContainerProperty(new_file, "Sample Type").setValue(sampleType);
      dataset_container.getContainerProperty(new_file, "File Name").setValue(d.getFileName());
      dataset_container.getContainerProperty(new_file, "File Type").setValue(d.getFileType());
      dataset_container.getContainerProperty(new_file, "Dataset Type").setValue(d.getType());
      dataset_container.getContainerProperty(new_file, "Registration Date").setValue(ts);
      dataset_container.getContainerProperty(new_file, "Validated").setValue(true);
      dataset_container.getContainerProperty(new_file, "File Size").setValue(
          DashboardUtil.humanReadableByteCount(d.getFileSize(), true));
      dataset_container.getContainerProperty(new_file, "dl_link").setValue(d.getDssPath());
      dataset_container.getContainerProperty(new_file, "CODE").setValue(d.getCode());
      dataset_container.getContainerProperty(new_file, "file_size_bytes").setValue(d.getFileSize());
      if (parent != null) {
        dataset_container.setParent(new_file, parent);
      }
    }
  }



  private class TableCheckBoxValueChangeListener implements ValueChangeListener {

    /**
     * 
     */
    private static final long serialVersionUID = -7177199525909283879L;
    private Object itemId;
    private String itemFolderName;

    public TableCheckBoxValueChangeListener(final Object itemId, String itemFolderName) {
      this.itemFolderName = itemFolderName;
      this.itemId = itemId;
    }

    @Override
    public void valueChange(ValueChangeEvent event) {

      PortletSession portletSession = ((QbicmainportletUI) UI.getCurrent()).getPortletSession();
      Map<String, AbstractMap.SimpleEntry<String, Long>> entries =
          (Map<String, AbstractMap.SimpleEntry<String, Long>>) portletSession.getAttribute(
              "qbic_download", PortletSession.APPLICATION_SCOPE);

      boolean itemSelected = (Boolean) event.getProperty().getValue();
      /*
       * String fileName = ""; Object parentId = table.getParent(itemId); //In order to prevent
       * infinity loop int folderDepth = 0; while(parentId != null && folderDepth < 100){ fileName =
       * Paths.get((String) table.getItem(parentId).getItemProperty("File Name").getValue(),
       * fileName).toString(); parentId = table.getParent(parentId); folderDepth++; }
       */

      valueChange(itemId, itemSelected, entries, itemFolderName);
      portletSession.setAttribute("qbic_download", entries, PortletSession.APPLICATION_SCOPE);

      if (entries == null || entries.isEmpty()) {
        download.setResource(new ExternalResource("javascript:"));
        download.setEnabled(false);
      } else {
        String resourceUrl =
            (String) portletSession.getAttribute("resURL", PortletSession.APPLICATION_SCOPE);
        download.setResource(new ExternalResource(resourceUrl));
        download.setEnabled(true);
      }

    }

    /**
     * updates entries (puts and removes) for selected table item and all its children. Means
     * Checkbox is updated. And in case download button is clicked all checked items will be
     * downloaded.
     * 
     * @param itemId Container id
     * @param itemSelected checkbox value of the item
     * @param entries all checked items
     * @param fileName fileName of current item
     */
    private void valueChange(Object itemId, boolean itemSelected,
        Map<String, SimpleEntry<String, Long>> entries, String fileName) {

      ((CheckBox) table.getItem(itemId).getItemProperty("Select").getValue())
          .setValue(itemSelected);
      fileName =
          Paths.get(fileName,
              (String) table.getItem(itemId).getItemProperty("File Name").getValue()).toString();

      // System.out.println(fileName);
      if (table.hasChildren(itemId)) {
        for (Object childId : table.getChildren(itemId)) {
          valueChange(childId, itemSelected, entries, fileName);
        }
      } else if (itemSelected) {
        String datasetCode = (String) table.getItem(itemId).getItemProperty("CODE").getValue();
        Long datasetFileSize =
            (Long) table.getItem(itemId).getItemProperty("file_size_bytes").getValue();
        entries.put(fileName, new AbstractMap.SimpleEntry<String, Long>(datasetCode,
            datasetFileSize));
      } else {
        entries.remove(fileName);
      }
    }
  }
}
