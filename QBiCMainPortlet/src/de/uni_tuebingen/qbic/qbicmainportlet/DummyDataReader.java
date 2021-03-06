/*******************************************************************************
 * QBiC Project qNavigator enables users to manage their projects.
 * Copyright (C) "2016”  Christopher Mohr, David Wojnar, Andreas Friedrich
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.uni_tuebingen.qbic.qbicmainportlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import model.SampleBean;

import com.vaadin.ui.UI;

public class DummyDataReader {

  private String data = "openBISdataTree.txt";
  private ArrayList<String> spaces = new ArrayList<String>();
  private HashMap<String, ArrayList<String>> spaceProjects =
      new HashMap<String, ArrayList<String>>();
  private HashMap<String, ArrayList<String>> projSamples = new HashMap<String, ArrayList<String>>();
  private HashMap<String, ArrayList<SampleBean>> sampData =
      new HashMap<String, ArrayList<SampleBean>>();

  DummyDataReader() throws IOException {
    fillData();
  }

  private void fillData() throws IOException {
    // String workingDir = System.getProperty("user.dir");
    // System.out.println("Current working directory : " + workingDir);
    UI.getCurrent().getSession().getService();
    // System.out.println(VaadinService.getCurrent().getBaseDirectory().getAbsolutePath());
    InputStream input = DummyDataReader.class.getClassLoader().getResourceAsStream(data);
    if (input == null) {
      // System.out.println("WUHHH");
    }
    InputStreamReader reader = new InputStreamReader(input);
    BufferedReader r = new BufferedReader(reader);
    int level = 0; // space: 0, proj: 1, samp: 2
    String currSpace = "";
    String currProj = "";
    while (r.ready()) {
      String line = r.readLine();
      if (line.equals("<") || line.equals("<<"))
        level++;
      else if (line.equals(">") || line.equals(">>"))
        level--;
      else {
        switch (level) {
          case 2:
            parseSamp(currProj, line);
            break;
          case 1:
            currProj = line;
            // System.out.println("adding project "+currProj);
            if (spaceProjects.containsKey(currSpace))
              spaceProjects.get(currSpace).add(currProj);
            else
              spaceProjects.put(currSpace, new ArrayList<String>(Arrays.asList(currProj)));
            break;
          default:
            currSpace = line;
            spaces.add(currSpace);
            // System.out.println("adding space "+currSpace);
            break;
        }
      }
    }
    r.close();
  }

  private void parseSamp(String project, String line) {
    String[] cats = line.split("\t");
    String id = cats[0];
    SampleBean b = null;// new SampleBean(id,cats[1], cats[3], cats[2], cats[5], cats[4],
                        // "unknown");
    // System.out.println("adding sample "+b);
    if (sampData.containsKey(id))
      sampData.get(id).add(b);
    else
      sampData.put(id, new ArrayList<SampleBean>(Arrays.asList(b)));
    if (projSamples.containsKey(project))
      projSamples.get(project).add(id);
    else
      projSamples.put(project, new ArrayList<String>(Arrays.asList(id)));
  }

  public ArrayList<String> getSpaces() {
    return spaces;
  }

  public ArrayList<String> getProjects(String space) {
    return spaceProjects.get(space);
  }

  public ArrayList<String> getSamples(String proj) {
    return projSamples.get(proj);
  }

  public ArrayList<SampleBean> getSampleData(String samp) {
    return sampData.get(samp);
  }

  public static void main(String[] args) throws IOException {
    new DummyDataReader();
  }
}
