package de.uni_tuebingen.qbic.qbicmainportlet;

import helpers.UglyToPrettyNameMapper;

import java.io.IOException;
import java.io.StringWriter;
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
import java.util.Scanner;
import java.util.Set;

import javax.portlet.PortletSession;

import logging.Log4j2Logger;
import logging.Logger;
import model.DatasetBean;

import org.apache.catalina.util.Base64;
import org.apache.commons.io.IOUtils;
import org.tepi.filtertable.FilterTreeTable;

import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.theme.ThemeDisplay;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;
import com.vaadin.server.VaadinService;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.CustomTable.RowHeaderMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PopupView;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.uni_tuebingen.qbic.util.DashboardUtil;

public class DatasetComponent extends CustomComponent {

  /**
   * 
   */
  private static final long serialVersionUID = 8672873911284888801L;

  private VerticalLayout mainLayout;
  private static Logger LOGGER = new Log4j2Logger(DatasetView.class);
  private FilterTreeTable table;
  private HierarchicalContainer datasets;
  VerticalLayout vert;
  private final String DOWNLOAD_BUTTON_CAPTION = "Download";
  private final String VISUALIZE_BUTTON_CAPTION = "Visualize";
  public final static String navigateToLabel = "datasetview";
  private DataHandler datahandler;
  private ToolBar toolbar;
  private State state;
  private String resourceUrl;
  private final ButtonLink download = new ButtonLink(DOWNLOAD_BUTTON_CAPTION, new ExternalResource(
      ""));

  private final String[] FILTER_TABLE_COLUMNS = new String[] {"Select", "File Name",
      "Dataset Type", "Registration Date", "File Size"};

  private int numberOfDatasets;

  private UglyToPrettyNameMapper prettyNameMapper = new UglyToPrettyNameMapper();


  public DatasetComponent(DataHandler dh, State state, String resourceurl) {
    this.datahandler = dh;
    this.resourceUrl = resourceurl;
    this.state = state;

    this.setCaption("Datasets");

    this.initUI();
  }

  private void initUI() {
    vert = new VerticalLayout();
    table = buildFilterTable();
    mainLayout = new VerticalLayout(vert);

    this.setWidth(Page.getCurrent().getBrowserWindowWidth() * 0.8f, Unit.PIXELS);
    this.setCompositionRoot(mainLayout);
  }

  public void updateUI(String type, String id) {
    // vert = new VerticalLayout();
    // this.datasets = dataset;
    // table = buildFilterTable();
    // this.buildLayout();
    // this.setContainerDataSource(this.datasets);
    // this.setContent(vert);
    // mainLayout.addComponent(vert);

    if (id == null)
      return;
    try {
      HierarchicalContainer datasetContainer = new HierarchicalContainer();
      datasetContainer.addContainerProperty("Select", CheckBox.class, null);
      datasetContainer.addContainerProperty("Project", String.class, null);
      datasetContainer.addContainerProperty("Sample", String.class, null);
      // datasetContainer.addContainerProperty("Sample Type", String.class, null);
      datasetContainer.addContainerProperty("File Name", String.class, null);
      datasetContainer.addContainerProperty("File Type", String.class, null);
      datasetContainer.addContainerProperty("Dataset Type", String.class, null);
      datasetContainer.addContainerProperty("Registration Date", Timestamp.class, null);
      datasetContainer.addContainerProperty("Validated", Boolean.class, null);
      datasetContainer.addContainerProperty("File Size", String.class, null);
      datasetContainer.addContainerProperty("file_size_bytes", Long.class, null);
      datasetContainer.addContainerProperty("dl_link", String.class, null);
      datasetContainer.addContainerProperty("CODE", String.class, null);

      List<ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.DataSet> retrievedDatasets = null;

      switch (type) {
        case "project":
          String projectIdentifier = id;
          retrievedDatasets =
              datahandler.getOpenBisClient().getDataSetsOfProjectByIdentifierWithSearchCriteria(
                  projectIdentifier);
          break;

        case "experiment":
          String experimentIdentifier = id;
          retrievedDatasets =
              datahandler.getOpenBisClient().getDataSetsOfExperimentByCodeWithSearchCriteria(
                  experimentIdentifier);
          break;

        case "sample":
          String sampleIdentifier = id;
          String sampleCode = sampleIdentifier.split("/")[2];
          retrievedDatasets = datahandler.getOpenBisClient().getDataSetsOfSample(sampleCode);
          break;

        default:
          retrievedDatasets =
              new ArrayList<ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.DataSet>();
          break;
      }

      numberOfDatasets = retrievedDatasets.size();
      if (numberOfDatasets == 0) {
        helpers.Utils
            .Notification(
                "No datasets available.",
                "No data is available for this project. Please contact the project manager if this is not expected.",
                "warning");
        // new Notification("No datasets available.", "<br/>Please contact the project manager.",
        // Type.WARNING_MESSAGE, true).show(Page.getCurrent());
      } else {

        Map<String, String> samples = new HashMap<String, String>();

        // project same for all datasets
        String projectCode = retrievedDatasets.get(0).getExperimentIdentifier().split("/")[2];
        for (ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.DataSet dataset : retrievedDatasets) {
          samples.put(dataset.getCode(), dataset.getSampleIdentifierOrNull().split("/")[2]);
        }

        List<DatasetBean> dsBeans = datahandler.queryDatasetsForFolderStructure(retrievedDatasets);

        numberOfDatasets = dsBeans.size();

        for (DatasetBean d : dsBeans) {
          Date date = d.getRegistrationDate();
          SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
          String dateString = sd.format(date);
          Timestamp ts = Timestamp.valueOf(dateString);
          String sampleID = samples.get(d.getCode());

          registerDatasetInTable(d, datasetContainer, projectCode, sampleID, ts, null);
        }
      }

      this.setContainerDataSource(datasetContainer);

    } catch (Exception e) {
      e.printStackTrace();
      LOGGER.error(String.format("getting dataset failed for dataset %s %s", type, id),
          e.getStackTrace());
    }
  }

  public void setContainerDataSource(HierarchicalContainer newDataSource) {
    datasets = (HierarchicalContainer) newDataSource;
    table.setContainerDataSource(this.datasets);

    table.setVisibleColumns((Object[]) FILTER_TABLE_COLUMNS);

    table.setSizeFull();
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
    this.vert.setWidth("100%");

    // Table (containing datasets) section
    VerticalLayout tableSection = new VerticalLayout();
    HorizontalLayout tableSectionContent = new HorizontalLayout();

    // tableSectionContent.setCaption("Datasets");
    // tableSectionContent.setIcon(FontAwesome.FLASK);
    // tableSection.addComponent(new Label(String.format("This project contains %s dataset(s).",
    // numberOfDatasets)));
    tableSectionContent.setMargin(new MarginInfo(true, false, true, false));
    tableSection
        .addComponent(new Label(
            String
                .format(
                    "This view shows all datasets associated with this project. There are %s registered datasets.",
                    numberOfDatasets), Label.CONTENT_PREFORMATTED));
    tableSectionContent.addComponent(this.table);

    tableSection.setMargin(new MarginInfo(true, false, false, true));
    // tableSectionContent.setMargin(true);
    // tableSection.setMargin(true);

    tableSection.addComponent(tableSectionContent);
    this.vert.addComponent(tableSection);

    table.setWidth("100%");
    tableSection.setWidth("100%");
    tableSectionContent.setWidth("100%");

    // this.table.setSizeFull();

    HorizontalLayout buttonLayout = new HorizontalLayout();
    buttonLayout.setMargin(new MarginInfo(false, false, true, true));
    buttonLayout.setHeight(null);
    // buttonLayout.setWidth("100%");
    buttonLayout.setSpacing(true);

    final Button visualize = new Button(VISUALIZE_BUTTON_CAPTION);

    Button checkAll = new Button("Select all datasets");
    checkAll.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        for (Object itemId : table.getItemIds()) {
          ((CheckBox) table.getItem(itemId).getItemProperty("Select").getValue()).setValue(true);
        }
      }
    });

    Button uncheckAll = new Button("Unselect all datasets");
    uncheckAll.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        for (Object itemId : table.getItemIds()) {
          ((CheckBox) table.getItem(itemId).getItemProperty("Select").getValue()).setValue(false);
        }
      }
    });

    String content =
        "<p> In case of multiple file selections, Project Browser will create a tar archive.</p>"
            + "<hr>"
            + "<p> If you need help on extracting a tar archive file, follow the tips below: </p>"
            + "<p>"
            + FontAwesome.WINDOWS.getHtml()
            + " Windows </p>"
            + "<p> To open/extract TAR file on Windows, you can use 7-Zip, Easy 7-Zip, PeaZip.</p>"
            + "<hr>"
            + "<p>"
            + FontAwesome.APPLE.getHtml()
            + " MacOS </p>"
            + "<p> To open/extract TAR file on Mac, you can use Mac OS built-in utility Archive Utility,<br> or third-party freeware. </p>"
            + "<hr>"
            + "<p>"
            + FontAwesome.LINUX.getHtml()
            + " Linux </p>"
            + "<p> You need to use command tar. The tar is the GNU version of tar archiving utility. <br> "
            + "To extract/unpack a tar file, type: $ tar -xvf file.tar</p>";

    PopupView tooltip = new PopupView(new helpers.ToolTip(content));
    tooltip.setHeight("44px");

    HorizontalLayout help = new HorizontalLayout();
    help.setSizeFull();
    HorizontalLayout helpContent = new HorizontalLayout();
    // helpContent.setSizeFull();

    help.setMargin(new MarginInfo(false, false, false, true));
    Label helpText = new Label("Attention: Click here before Download!");
    helpContent.addComponent(new Label(FontAwesome.QUESTION_CIRCLE.getHtml(), ContentMode.HTML));
    helpContent.addComponent(helpText);
    helpContent.addComponent(tooltip);
    helpContent.setSpacing(true);

    help.addComponent(helpContent);
    help.setComponentAlignment(helpContent, Alignment.TOP_CENTER);

    buttonLayout.addComponent(checkAll);
    buttonLayout.addComponent(uncheckAll);
    buttonLayout.addComponent(visualize);
    buttonLayout.addComponent(this.download);

    /**
     * prepare download.
     */
    download.setEnabled(false);
    download.setResource(new ExternalResource("javascript:"));
    visualize.setEnabled(false);

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
        String fileName = (String) table.getItem(next).getItemProperty("File Name").getValue();
        // TODO: No hardcoding!!
        // if (datasetType.equals("FASTQC") || datasetType.equals("QCML") ||
        // datasetType.equals("BAM")
        // || datasetType.equals("VCF")) {
        if (datasetType.equals("Q_WF_MS_QUALITYCONTROL_RESULTS")
            && (fileName.endsWith(".html") || fileName.endsWith(".qcML"))) {
          visualize.setEnabled(true);
        } else if (datasetType.equals("Q_WF_MS_QUALITYCONTROL_LOGS")
            && (fileName.endsWith(".err") || fileName.endsWith(".out"))) {
          visualize.setEnabled(true);
        } else if (datasetType.equals("Q_WF_NGS_EPITOPE_PREDICTION_RESULTS")
            && fileName.endsWith(".tsv")) {
          visualize.setEnabled(true);
        } else if (fileName.endsWith(".html")) {
          visualize.setEnabled(true);
        } else {
          visualize.setEnabled(false);
        }
      }
    });


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

          String space = datahandler.getOpenBisClient().getProjectByCode(project).getSpaceCode();// .getIdentifier().split("/")[1];
          message.add(project);
          message.add((String) table.getItem(next).getItemProperty("Sample").getValue());
          // message.add((String) table.getItem(next).getItemProperty("Sample Type").getValue());
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
          Object parent = table.getParent(next);
          if (parent != null) {
            String parentDatasetFileName =
                (String) table.getItem(parent).getItemProperty("File Name").getValue();
            url =
                datahandler.getOpenBisClient().getUrlForDataset(datasetCode,
                    parentDatasetFileName + "/" + datasetFileName);
          } else {
            url = datahandler.getOpenBisClient().getUrlForDataset(datasetCode, datasetFileName);
          }

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
          if (datasetType.equals("Q_WF_MS_QUALITYCONTROL_RESULTS")
              && datasetFileName.endsWith(".qcML")) {
            QcMlOpenbisSource re = new QcMlOpenbisSource(url);
            StreamResource streamres = new StreamResource(re, datasetFileName);
            streamres.setMIMEType("application/xml");
            res = streamres;
          } else if (datasetType.equals("Q_WF_MS_QUALITYCONTROL_RESULTS")
              && datasetFileName.endsWith(".html")) {
            QcMlOpenbisSource re = new QcMlOpenbisSource(url);
            StreamResource streamres = new StreamResource(re, datasetFileName);
            streamres.setMIMEType("text/html");
            res = streamres;
          } else if (datasetType.equals("Q_WF_MS_QUALITYCONTROL_LOGS")
              && (datasetFileName.endsWith(".err") || datasetFileName.endsWith(".out"))) {
            QcMlOpenbisSource re = new QcMlOpenbisSource(url);
            StreamResource streamres = new StreamResource(re, datasetFileName);
            streamres.setMIMEType("text/plain");
            res = streamres;
          } else if (datasetType.equals("Q_WF_NGS_EPITOPE_PREDICTION_RESULTS")
              && datasetFileName.endsWith(".tsv")) {
            QcMlOpenbisSource re = new QcMlOpenbisSource(url);
            StreamResource streamres = new StreamResource(re, datasetFileName);
            streamres.setMIMEType("text/plain");
            res = streamres;

            StringWriter writer = new StringWriter();
            try {
              IOUtils.copy(streamres.getStream().getStream(), writer, "UTF-8");
            } catch (IOException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            String theString = writer.toString();

            Scanner scannerTest = new Scanner(streamres.getStream().getStream());
            scannerTest.useDelimiter(System.getProperty("line.separator"));
            while (scannerTest.hasNext()) {
              // parse line to get Emp Object
              Scanner scanner = new Scanner(scannerTest.next());
              scanner.useDelimiter("\\s*\t\\s*");
            }
            scannerTest.close();

          } else if (datasetType.equals("FASTQC")) {
            res = new ExternalResource(url);
          } else if (datasetFileName.endsWith(".html")) {
            QcMlOpenbisSource re = new QcMlOpenbisSource(url);
            StreamResource streamres = new StreamResource(re, datasetFileName);
            streamres.setMIMEType("text/html");
            res = streamres;
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
          LOGGER.debug("Is resource null?: " + String.valueOf(res == null));
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
    this.vert.addComponent(help);

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

    filterTable.setColumnCollapsingAllowed(false);

    filterTable.setColumnReorderingAllowed(true);

    if (this.datasets != null) {
      filterTable.setContainerDataSource(this.datasets);
    }

    return filterTable;
  }

  public void registerDatasetInTable(DatasetBean d, HierarchicalContainer dataset_container,
      String project, String sample, Timestamp ts, Object parent) {

    UglyToPrettyNameMapper mapper = new UglyToPrettyNameMapper();

    if (d.hasChildren()) {

      Object new_ds = dataset_container.addItem();

      List<DatasetBean> subList = d.getChildren();


      dataset_container.setChildrenAllowed(new_ds, true);

      dataset_container.getContainerProperty(new_ds, "Select").setValue(new CheckBox());

      dataset_container.getContainerProperty(new_ds, "Project").setValue(project);
      dataset_container.getContainerProperty(new_ds, "Sample").setValue(sample);
      // dataset_container.getContainerProperty(new_ds, "Sample Type").setValue(
      // d.getSample().getType());
      dataset_container.getContainerProperty(new_ds, "File Name").setValue(d.getName());
      dataset_container.getContainerProperty(new_ds, "File Type").setValue("Folder");
      dataset_container.getContainerProperty(new_ds, "Dataset Type").setValue(
          prettyNameMapper.getPrettyName(d.getType()) + " Folder");
      dataset_container.getContainerProperty(new_ds, "Registration Date").setValue(ts);
      dataset_container.getContainerProperty(new_ds, "Validated").setValue(true);
      dataset_container.getContainerProperty(new_ds, "dl_link").setValue(d.getDssPath());
      dataset_container.getContainerProperty(new_ds, "CODE").setValue(d.getCode());
      dataset_container.getContainerProperty(new_ds, "file_size_bytes").setValue(d.getFileSize());

      if (parent != null) {
        dataset_container.setParent(new_ds, parent);
      }

      for (DatasetBean file : subList) {
        registerDatasetInTable(file, dataset_container, project, sample, ts, new_ds);
      }

    } else {
      // System.out.println("Now it should be a file: " + filelist[0].getPathInDataSet());

      Object new_file = dataset_container.addItem();
      dataset_container.setChildrenAllowed(new_file, false);

      dataset_container.getContainerProperty(new_file, "Select").setValue(new CheckBox());
      dataset_container.getContainerProperty(new_file, "Project").setValue(project);
      dataset_container.getContainerProperty(new_file, "Sample").setValue(sample);
      // dataset_container.getContainerProperty(new_file, "Sample Type").setValue(sampleType);
      dataset_container.getContainerProperty(new_file, "File Name").setValue(d.getFileName());
      dataset_container.getContainerProperty(new_file, "File Type").setValue(d.getFileType());
      dataset_container.getContainerProperty(new_file, "Dataset Type").setValue(
          prettyNameMapper.getPrettyName(d.getType()));
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

  /**
   * The input should have the following form: type=openbis_type&id=openbis_id e.g.
   * type=sample&id=/ABI_SYSBIO/QMARI117AV It is specifically designed to be used in the case of
   * datasetView. In other cases there is no guarantee that it will work correctly. returns a map
   * with two entries: "type": "openbistype" "id" : "openbisId"
   * 
   * @param parameters
   * @return
   */
  public static Map<String, String> getMap(String parameters) {
    if (parameters == null || parameters.equals(""))
      return null;
    String[] params = parameters.split("&");
    // TODO check for length == 2 needed ?
    // if (params == null || params.length != 2)
    if (params == null || params.length > 3)
      return null;
    HashMap<String, String> map = new HashMap<String, String>();
    for (String p : params) {
      String[] kv = p.split("=");
      if (kv.length != 2)
        return null;
      map.put(kv[0], kv[1]);
    }
    return map;
  }

}
